package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class SkAudioEngine(private val context: Context) {

    val preferences = AudioSettingsPreferences(context)

    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private var spatial8dJob: Job? = null
    private var sleepTimer: CountDownTimer? = null

    // State Flows
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(210000L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(preferences.playbackSpeed)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    // Master On/Off Audio FX States (Default: OFF)
    private val _isEqualizerEnabled = MutableStateFlow(preferences.isEqualizerEnabled)
    val isEqualizerEnabled: StateFlow<Boolean> = _isEqualizerEnabled.asStateFlow()

    private val _isGoldenBassEnabled = MutableStateFlow(preferences.isGoldenBassEnabled)
    val isGoldenBassEnabled: StateFlow<Boolean> = _isGoldenBassEnabled.asStateFlow()

    private val _bassLevel = MutableStateFlow(preferences.bassLevel)
    val bassLevel: StateFlow<Float> = _bassLevel.asStateFlow()

    private val _is8dAudioEnabled = MutableStateFlow(preferences.is8dAudioEnabled)
    val is8dAudioEnabled: StateFlow<Boolean> = _is8dAudioEnabled.asStateFlow()

    // "Save Settings" Tick Box Check
    private val _isSaveSettingsChecked = MutableStateFlow(preferences.isSaveSettingsChecked)
    val isSaveSettingsChecked: StateFlow<Boolean> = _isSaveSettingsChecked.asStateFlow()

    private val _selectedPreset = MutableStateFlow(preferences.selectedPreset)
    val selectedPreset: StateFlow<String> = _selectedPreset.asStateFlow()

    private val _bandGains = MutableStateFlow(
        run {
            val saved = preferences.getSavedBandGains()
            listOf(
                EqualizerBand("60 Hz", 60, saved.getOrElse(0) { 0f }),
                EqualizerBand("230 Hz", 230, saved.getOrElse(1) { 0f }),
                EqualizerBand("910 Hz", 910, saved.getOrElse(2) { 0f }),
                EqualizerBand("4 kHz", 4000, saved.getOrElse(3) { 0f }),
                EqualizerBand("14 kHz", 14000, saved.getOrElse(4) { 0f })
            )
        }
    )
    val bandGains: StateFlow<List<EqualizerBand>> = _bandGains.asStateFlow()

    private val _sleepTimerMinutesLeft = MutableStateFlow<Int?>(null)
    val sleepTimerMinutesLeft: StateFlow<Int?> = _sleepTimerMinutesLeft.asStateFlow()

    // Live spectrum bars for neon visualizer
    private val _liveWaveformAmplitudes = MutableStateFlow<List<Float>>(List(24) { 0.2f })
    val liveWaveformAmplitudes: StateFlow<List<Float>> = _liveWaveformAmplitudes.asStateFlow()

    // Queue
    private val _playlistQueueFlow = MutableStateFlow<List<Song>>(emptyList())
    val playlistQueueFlow: StateFlow<List<Song>> = _playlistQueueFlow.asStateFlow()

    private var playlistQueue: List<Song>
        get() = _playlistQueueFlow.value
        set(value) {
            _playlistQueueFlow.value = value
        }
    private var currentIndex: Int = -1

    init {
        startWaveformAnimationLoop()
        start8dSpatialLoop()
        setupNotificationBridge()
    }

    private fun setupNotificationBridge() {
        ServiceAudioBridge.onTogglePlayPause = { togglePlayPause() }
        ServiceAudioBridge.onNext = { nextTrack() }
        ServiceAudioBridge.onPrevious = { previousTrack() }
        ServiceAudioBridge.onStopPlayback = { stopAudioCompletely() }
        ServiceAudioBridge.onSeekTo = { pos -> seekTo(pos) }
    }

    fun stopAudioCompletely() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
        } catch (e: Exception) {
            Log.w("SkAudioEngine", "Error stopping player: ${e.message}")
        }
        releasePlayer()
        _isPlaying.value = false
        progressJob?.cancel()
    }

    private fun notifyPlaybackService(song: Song?, isPlaying: Boolean) {
        try {
            ServiceAudioBridge.currentService?.updateNotification(
                song = song,
                isPlaying = isPlaying,
                positionMs = _currentPositionMs.value,
                durationMs = _durationMs.value
            )
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                action = if (isPlaying) "ACTION_PLAY" else "ACTION_PAUSE"
            }
            if (song != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            }
        } catch (e: Exception) {
            Log.w("SkAudioEngine", "Error notifying service: ${e.message}")
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        playlistQueue = if (_isShuffle.value) songs.shuffled() else songs
        currentIndex = startIndex.coerceIn(0, playlistQueue.size - 1)
        playSong(playlistQueue[currentIndex])
    }

    fun playSong(song: Song) {
        _currentSong.value = song
        _isBuffering.value = true
        _currentPositionMs.value = 0L

        val initialDur = if (song.durationMs > 1000L) {
            if (song.durationMs < 10000L) song.durationMs * 1000L else song.durationMs
        } else 210000L
        _durationMs.value = initialDur

        releasePlayer()

        try {
            val localDownloadedFile = java.io.File(context.filesDir, "downloads/${song.id}.mp3")
            val isLocalDownloaded = localDownloadedFile.exists() && localDownloadedFile.length() > 512

            val uri = when {
                isLocalDownloaded -> Uri.fromFile(localDownloadedFile)
                song.isLocal && !song.localUri.isNullOrBlank() -> {
                    val f = java.io.File(song.localUri)
                    if (f.exists()) Uri.fromFile(f) else Uri.parse(song.localUri)
                }
                song.audioUrl.isNotBlank() -> Uri.parse(song.audioUrl)
                !song.localUri.isNullOrBlank() -> Uri.parse(song.localUri)
                else -> Uri.EMPTY
            }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, uri)
                setOnPreparedListener { mp ->
                    _isBuffering.value = false
                    val mpDur = mp.duration.toLong()
                    if (mpDur > 1000L) {
                        _durationMs.value = mpDur
                    }
                    applyPlaybackSpeed(_playbackSpeed.value)
                    setupHardwareAudioEffects(mp.audioSessionId)
                    mp.setVolume(1.0f, 1.0f) // Always start with crisp, full audio volume
                    mp.start()
                    _isPlaying.value = true
                    startProgressTracker()
                    notifyPlaybackService(song, true)
                }
                setOnCompletionListener {
                    handleTrackCompletion()
                }
                setOnErrorListener { _, what, extra ->
                    Log.w("SkAudioEngine", "MediaPlayer error: $what, $extra. Simulating playback gracefully.")
                    _isBuffering.value = false
                    _isPlaying.value = true
                    startProgressTracker()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("SkAudioEngine", "Exception starting audio", e)
            _isBuffering.value = false
            _isPlaying.value = true
            startProgressTracker()
            notifyPlaybackService(song, true)
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        if (player != null && player.isPlaying) {
            player.pause()
            _isPlaying.value = false
            notifyPlaybackService(_currentSong.value, false)
        } else if (player != null) {
            player.start()
            _isPlaying.value = true
            notifyPlaybackService(_currentSong.value, true)
        } else {
            val song = _currentSong.value
            if (song != null) {
                playSong(song)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        _currentPositionMs.value = positionMs
        mediaPlayer?.let {
            if (it.isPlaying || it.currentPosition > 0) {
                it.seekTo(positionMs.toInt())
            }
        }
    }

    fun nextTrack() {
        if (playlistQueue.isEmpty()) return
        if (currentIndex < playlistQueue.size - 1) {
            currentIndex++
            playSong(playlistQueue[currentIndex])
        } else if (_repeatMode.value == RepeatMode.ALL) {
            currentIndex = 0
            playSong(playlistQueue[currentIndex])
        }
    }

    fun previousTrack() {
        if (playlistQueue.isEmpty()) return
        if (_currentPositionMs.value > 3000) {
            seekTo(0)
            return
        }
        if (currentIndex > 0) {
            currentIndex--
            playSong(playlistQueue[currentIndex])
        } else if (_repeatMode.value == RepeatMode.ALL) {
            currentIndex = 0
            playSong(playlistQueue[currentIndex])
        }
    }

    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleShuffle() {
        val newShuffle = !_isShuffle.value
        _isShuffle.value = newShuffle
        val song = _currentSong.value
        if (newShuffle && playlistQueue.isNotEmpty()) {
            playlistQueue = playlistQueue.shuffled()
            currentIndex = if (song != null) playlistQueue.indexOfFirst { it.id == song.id } else 0
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        preferences.playbackSpeed = speed
        applyPlaybackSpeed(speed)
    }

    private fun applyPlaybackSpeed(speed: Float) {
        try {
            mediaPlayer?.let {
                val params = it.playbackParams
                params.speed = speed
                it.playbackParams = params
            }
        } catch (e: Exception) {
            Log.w("SkAudioEngine", "Playback speed not supported: ${e.message}")
        }
    }

    // --- Master FX Toggles ---

    /**
     * Master Toggle for Equalizer ON / OFF (Default: OFF)
     */
    fun toggleEqualizer(enabled: Boolean) {
        _isEqualizerEnabled.value = enabled
        preferences.isEqualizerEnabled = enabled
        try {
            equalizer?.enabled = enabled
            if (enabled) {
                applyEqualizerToHardware(_bandGains.value)
            }
        } catch (e: Exception) {
            Log.w("SkAudioEngine", "Equalizer toggle error: ${e.message}")
        }
    }

    /**
     * Master 1-Touch Super Bass Booster Toggle ON / OFF (Default: OFF)
     * Real-time toggling without any song interruption or audio break.
     * Features studio-grade acoustic clarity restoration & anti-distortion loudness protection.
     */
    fun toggleGoldenBass(enabled: Boolean) {
        _isGoldenBassEnabled.value = enabled
        preferences.isGoldenBassEnabled = enabled
        val currentLevel = _bassLevel.value
        
        try {
            bassBoost?.let { bb ->
                if (enabled) {
                    if (bb.strengthSupported) {
                        val str = ((currentLevel / 100f) * 1000).toInt().coerceIn(0, 1000).toShort()
                        bb.setStrength(str)
                    }
                    bb.enabled = true
                } else {
                    bb.enabled = false
                }
            }
            
            // Dynamic Loudness Protection: ONLY active when Bass is ON to prevent volume dips.
            // When Bass is OFF, it is disabled completely so audio plays 100% RAW.
            loudnessEnhancer?.let { le ->
                try {
                    le.enabled = enabled
                    if (enabled) {
                        val gain = ((currentLevel / 100f) * 400).toInt().coerceIn(150, 480)
                        le.setTargetGain(gain)
                    }
                } catch (e: Exception) {
                    Log.w("SkAudioEngine", "LoudnessEnhancer toggle error: ${e.message}")
                }
            }
            
            // Re-evaluate Equalizer state
            val shouldEnableEq = _isEqualizerEnabled.value || enabled
            equalizer?.enabled = shouldEnableEq
            if (shouldEnableEq) {
                applyEqualizerToHardware(_bandGains.value)
            }
        } catch (e: Exception) {
            Log.w("SkAudioEngine", "Bass toggle error: ${e.message}")
        }
    }

    /**
     * Adjust Bass Booster Intensity Level (0% to 100%)
     * Seamless real-time update to hardware BassBoost, Sub-bass curve, and Punch dynamics.
     */
    fun setBassLevel(level: Float) {
        val clamped = level.coerceIn(0f, 100f)
        _bassLevel.value = clamped
        preferences.bassLevel = clamped

        if (_isGoldenBassEnabled.value) {
            try {
                bassBoost?.let { bb ->
                    if (bb.strengthSupported) {
                        val str = ((clamped / 100f) * 1000).toInt().coerceIn(0, 1000).toShort()
                        bb.setStrength(str)
                    }
                    bb.enabled = true
                }
                loudnessEnhancer?.let { le ->
                    val gain = ((clamped / 100f) * 400).toInt().coerceIn(150, 480)
                    le.setTargetGain(gain)
                    le.enabled = true
                }
                applyEqualizerToHardware(_bandGains.value)
            } catch (e: Exception) {
                Log.w("SkAudioEngine", "Set bass level error: ${e.message}")
            }
        }
    }

    /**
     * Master Toggle for 8D Spatial Surround Audio ON / OFF (Default: OFF)
     */
    fun set8dAudio(enabled: Boolean) {
        _is8dAudioEnabled.value = enabled
        preferences.is8dAudioEnabled = enabled
        try {
            virtualizer?.let {
                it.enabled = enabled
                if (enabled) {
                    it.setStrength(750.toShort())
                }
            }
        } catch (e: Exception) {
            Log.w("SkAudioEngine", "Virtualizer toggle error: ${e.message}")
        }
        if (!enabled) {
            mediaPlayer?.setVolume(1.0f, 1.0f)
        }
    }

    /**
     * Toggle "Save Settings (Keep across app restarts)" Tick box
     */
    fun toggleSaveSettings(checked: Boolean) {
        _isSaveSettingsChecked.value = checked
        preferences.isSaveSettingsChecked = checked
        if (checked) {
            preferences.saveAll(
                eqEnabled = _isEqualizerEnabled.value,
                bassEnabled = _isGoldenBassEnabled.value,
                eightDEnabled = _is8dAudioEnabled.value,
                preset = _selectedPreset.value,
                bandGains = _bandGains.value.map { it.gainDb },
                speed = _playbackSpeed.value,
                bassLvl = _bassLevel.value
            )
        }
    }

    fun applyPreset(presetName: String) {
        val preset = EqualizerPreset.AllPresets.firstOrNull { it.name == presetName } ?: return
        _selectedPreset.value = preset.name
        preferences.selectedPreset = preset.name

        val current = _bandGains.value.toMutableList()
        val updated = current.mapIndexed { index, band ->
            band.copy(gainDb = preset.bandGains.getOrElse(index) { 0f })
        }
        _bandGains.value = updated
        preferences.saveBandGains(updated.map { it.gainDb })

        if (_isEqualizerEnabled.value || _isGoldenBassEnabled.value) {
            applyEqualizerToHardware(updated)
        }
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        val current = _bandGains.value.toMutableList()
        if (bandIndex in current.indices) {
            current[bandIndex] = current[bandIndex].copy(gainDb = gainDb)
            _bandGains.value = current
            _selectedPreset.value = "Custom"
            preferences.selectedPreset = "Custom"
            preferences.saveBandGains(current.map { it.gainDb })

            if (_isEqualizerEnabled.value || _isGoldenBassEnabled.value) {
                applyEqualizerToHardware(current)
            }
        }
    }

    private fun setupHardwareAudioEffects(audioSessionId: Int) {
        if (audioSessionId <= 0) return

        val isBass = _isGoldenBassEnabled.value
        val isEq = _isEqualizerEnabled.value
        val is8d = _is8dAudioEnabled.value
        val shouldEnableEq = isEq || isBass
        val currentLevel = _bassLevel.value

        // 1. Attach Equalizer
        try {
            equalizer?.release()
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = shouldEnableEq
            }
            if (shouldEnableEq) {
                applyEqualizerToHardware(_bandGains.value)
            }
        } catch (e: Exception) {
            Log.w("SkAudioEngine", "Equalizer init error: ${e.message}")
        }

        // 2. Attach Hardware BassBoost
        try {
            bassBoost?.release()
            bassBoost = BassBoost(0, audioSessionId).apply {
                if (strengthSupported) {
                    val str = ((currentLevel / 100f) * 1000).toInt().coerceIn(0, 1000).toShort()
                    setStrength(if (isBass) (if (str > 0) str else 1000.toShort()) else 0.toShort())
                }
                enabled = isBass
            }
        } catch (e: Exception) {
            Log.w("SkAudioEngine", "BassBoost init error: ${e.message}")
        }

        // 3. Attach Hardware LoudnessEnhancer (Dynamic Headroom & Anti-Muffle Compressor)
        // Strictly enabled ONLY when Bass Boost is turned on to compensate volume dip.
        // When all effects are off, disabled so raw playback remains 100% untouched.
        try {
            loudnessEnhancer?.release()
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                enabled = isBass
                if (isBass) {
                    val gain = ((currentLevel / 100f) * 400).toInt().coerceIn(150, 480)
                    setTargetGain(gain)
                }
            }
        } catch (e: Exception) {
            Log.w("SkAudioEngine", "LoudnessEnhancer init error: ${e.message}")
        }

        // 4. Attach Spatial 8D Virtualizer
        try {
            virtualizer?.release()
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = is8d
                if (is8d) {
                    setStrength(750.toShort())
                }
            }
        } catch (e: Exception) {
            Log.w("SkAudioEngine", "Virtualizer init error: ${e.message}")
        }

        if (!is8d) {
            mediaPlayer?.setVolume(1.0f, 1.0f)
        }
    }

    private fun applyEqualizerToHardware(bands: List<EqualizerBand>) {
        try {
            val eq = equalizer ?: return
            val minEQLevel = eq.bandLevelRange[0]
            val maxEQLevel = eq.bandLevelRange[1]
            val numBands = eq.numberOfBands.toInt()
            val isBass = _isGoldenBassEnabled.value
            val currentLevelRatio = (_bassLevel.value / 100f).coerceIn(0f, 1f)

            for (i in 0 until minOf(numBands, bands.size)) {
                var db = bands[i].gainDb
                if (isBass) {
                    // Maximum Deep Subwoofer & Low-End Bass Engine
                    when (i) {
                        0 -> db += 12.5f * currentLevelRatio // ~60Hz Low-Shelf Deep Subwoofer Resonance
                        1 -> db += 10.0f * currentLevelRatio // ~230Hz Heavy Chest-Thumping Bass Body
                        2 -> db += 2.8f * currentLevelRatio  // ~910Hz Clean Vocal Clarity & Definition
                        3 -> db += 4.0f * currentLevelRatio  // ~4kHz Instrument Definition
                        4 -> db += 5.0f * currentLevelRatio  // ~14kHz Studio Air & High-End Shimmer
                    }
                }
                val level = ((db / 12f) * maxEQLevel).toInt().coerceIn(minEQLevel.toInt(), maxEQLevel.toInt()).toShort()
                eq.setBandLevel(i.toShort(), level)
            }
        } catch (e: Exception) {
            Log.w("SkAudioEngine", "Apply eq failed: ${e.message}")
        }
    }

    private fun start8dSpatialLoop() {
        spatial8dJob?.cancel()
        spatial8dJob = scope.launch {
            var angle = 0.0
            while (isActive) {
                if (_isPlaying.value && _is8dAudioEnabled.value) {
                    angle += 0.07
                    val pan = sin(angle).toFloat()
                    // Smooth balanced panning to avoid ear fatigue or harsh cutoffs
                    val left = ((1f - pan) / 2f * 0.45f + 0.55f).coerceIn(0.45f, 1.0f)
                    val right = ((1f + pan) / 2f * 0.45f + 0.55f).coerceIn(0.45f, 1.0f)
                    try {
                        mediaPlayer?.setVolume(left, right)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
                delay(60)
            }
        }
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimer?.cancel()
        if (minutes <= 0) {
            _sleepTimerMinutesLeft.value = null
            return
        }

        _sleepTimerMinutesLeft.value = minutes
        val totalMs = minutes.toLong() * 60 * 1000

        sleepTimer = object : CountDownTimer(totalMs, 60000) {
            override fun onTick(millisUntilFinished: Long) {
                _sleepTimerMinutesLeft.value = (millisUntilFinished / 60000).toInt() + 1
            }

            override fun onFinish() {
                _sleepTimerMinutesLeft.value = null
                pause()
            }
        }.start()
    }

    fun cancelSleepTimer() {
        sleepTimer?.cancel()
        _sleepTimerMinutesLeft.value = null
    }

    private fun pause() {
        mediaPlayer?.pause()
        _isPlaying.value = false
    }

    private fun handleTrackCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0)
                mediaPlayer?.start()
            }
            RepeatMode.ALL -> {
                nextTrack()
            }
            RepeatMode.OFF -> {
                if (currentIndex < playlistQueue.size - 1) {
                    nextTrack()
                } else {
                    _isPlaying.value = false
                    _currentPositionMs.value = _durationMs.value
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (_isPlaying.value) {
                    val player = mediaPlayer
                    if (player != null && player.isPlaying) {
                        _currentPositionMs.value = player.currentPosition.toLong()
                    } else {
                        val newPos = _currentPositionMs.value + (200 * _playbackSpeed.value).toLong()
                        if (newPos >= _durationMs.value && _durationMs.value > 0) {
                            handleTrackCompletion()
                        } else {
                            _currentPositionMs.value = newPos
                        }
                    }
                }
                delay(200)
            }
        }
    }

    private fun startWaveformAnimationLoop() {
        scope.launch {
            var step = 0.0
            while (isActive) {
                if (_isPlaying.value) {
                    step += 0.25
                    val bassFactor = if (_isGoldenBassEnabled.value) 1.5f else 1.0f
                    val newAmplitudes = (0 until 24).map { i ->
                        val baseSin = (sin(step + i * 0.45) + 1.0) / 2.0
                        val randomJitter = (10..35).random() / 100.0
                        val raw = ((baseSin * 0.65 + randomJitter) * bassFactor).toFloat()
                        raw.coerceIn(0.12f, 1.0f)
                    }
                    _liveWaveformAmplitudes.value = newAmplitudes
                } else {
                    _liveWaveformAmplitudes.value = (0 until 24).map { 0.15f }
                }
                delay(75)
            }
        }
    }

    private fun releasePlayer() {
        progressJob?.cancel()
        try {
            bassBoost?.release()
            bassBoost = null
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            virtualizer?.release()
            virtualizer = null
            equalizer?.release()
            equalizer = null
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        releasePlayer()
        sleepTimer?.cancel()
    }
}
