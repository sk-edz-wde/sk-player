package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.AudioEngineQualityDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.EqualizerSheet
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.SleepTimerDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import com.example.ui.components.SongOptionsBottomSheet
import com.example.ui.components.VipActivationDialog
import com.example.ui.components.ReportLogDialog
import com.example.ui.components.SubmitReportDialog
import com.example.ui.components.LongPressOverlay
import com.example.ui.screens.FullscreenPlayerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LocalMusicScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.WelcomeAuthScreen
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberGlassBorder
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonFuchsia
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.MainViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.util.NotificationHelper.createNotificationChannels(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val currentUser by viewModel.currentUser.collectAsState()
                val isAuthLoading by viewModel.isAuthLoading.collectAsState()
                val context = LocalContext.current

                if (currentUser == null) {
                    WelcomeAuthScreen(
                        isAuthLoading = isAuthLoading,
                        onLogin = { email, pass -> viewModel.loginWithEmail(email, pass) },
                        onRegister = { email, pass, name -> viewModel.registerWithEmail(email, pass, name) },
                        onQuickDemoLogin = { viewModel.quickDemoLogin() }
                    )
                } else {
                    SkPlayerApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun SkPlayerApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isPlayerExpanded by viewModel.isPlayerExpanded.collectAsState()

    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.audioEngine.isPlaying.collectAsState()
    val currentPositionMs by viewModel.audioEngine.currentPositionMs.collectAsState()
    val durationMs by viewModel.audioEngine.durationMs.collectAsState()
    val repeatMode by viewModel.audioEngine.repeatMode.collectAsState()
    val isShuffle by viewModel.audioEngine.isShuffle.collectAsState()
    val liveWaveform by viewModel.audioEngine.liveWaveformAmplitudes.collectAsState()

    val currentUser by viewModel.currentUser.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val allSongs by viewModel.allSongs.collectAsState()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsState()
    val homeFilteredSongs by viewModel.homeFilteredSongs.collectAsState()
    val homeSearchQuery by viewModel.homeSearchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val dynamicCategories by viewModel.dynamicCategories.collectAsState()
    val playlistQueue by viewModel.playlistQueue.collectAsState()
    val allPlaylistSongIds by viewModel.allPlaylistSongIds.collectAsState()
    val randomCategory by viewModel.randomCategory.collectAsState()
    val songForPlaylist by viewModel.songForPlaylist.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchCategory by viewModel.searchCategory.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    val playlists by viewModel.playlists.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val activePlaylist by viewModel.activePlaylist.collectAsState()
    val activePlaylistSongs by viewModel.activePlaylistSongs.collectAsState()

    val localSongs by viewModel.localSongs.collectAsState()
    val isScanningLocal by viewModel.isScanningLocal.collectAsState()

    val showVipModal by viewModel.showVipModal.collectAsState()
    val showReportLog by viewModel.showReportLog.collectAsState()
    val longPressedSong by viewModel.longPressedSong.collectAsState()
    val songForReport by viewModel.songForReport.collectAsState()
    val userReports by viewModel.userReports.collectAsState()
    val notifications by viewModel.visibleNotifications.collectAsState()
    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsState()
    val showNotificationsDialog by viewModel.showNotificationsDialog.collectAsState()
    val showEqualizerSheet by viewModel.showEqualizerSheet.collectAsState()
    val showCreatePlaylistDialog by viewModel.showCreatePlaylistDialog.collectAsState()
    val showAddToPlaylistDialog by viewModel.showAddToPlaylistDialog.collectAsState()
    val showSleepTimerDialog by viewModel.showSleepTimerDialog.collectAsState()
    val showAudioQualityDialog by viewModel.showAudioQualityDialog.collectAsState()
    val selectedAudioQuality by viewModel.selectedAudioQuality.collectAsState()
    val selectedSongForOptions by viewModel.selectedSongForOptions.collectAsState()
    val vipKeys by viewModel.vipKeys.collectAsState()

    val isEqualizerEnabled by viewModel.audioEngine.isEqualizerEnabled.collectAsState()
    val isGoldenBassEnabled by viewModel.audioEngine.isGoldenBassEnabled.collectAsState()
    val bassLevel by viewModel.bassLevel.collectAsState()
    val is8dAudio by viewModel.audioEngine.is8dAudioEnabled.collectAsState()
    val isSaveSettingsChecked by viewModel.audioEngine.isSaveSettingsChecked.collectAsState()
    val eqBands by viewModel.audioEngine.bandGains.collectAsState()
    val eqPreset by viewModel.audioEngine.selectedPreset.collectAsState()
    val playbackSpeed by viewModel.audioEngine.playbackSpeed.collectAsState()
    val sleepMinutesLeft by viewModel.audioEngine.sleepTimerMinutesLeft.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Runtime Permission Request for Notifications and Storage on entry
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Permissions handled smoothly
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // Toast/Snackbar listener
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Back handling
    BackHandler(enabled = isPlayerExpanded || activePlaylist != null) {
        if (isPlayerExpanded) {
            viewModel.setPlayerExpanded(false)
        } else if (activePlaylist != null) {
            viewModel.closePlaylist()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CyberBackground,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                var offsetX by remember { mutableFloatStateOf(0f) }
                val currentOffset = offsetX
                val alpha by animateFloatAsState(
                    targetValue = (1f - (abs(currentOffset) / 250f)).coerceIn(0f, 1f),
                    label = "snackbar_alpha"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .offset { IntOffset(currentOffset.roundToInt(), 0) }
                        .graphicsLayer { this.alpha = alpha }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (abs(offsetX) > 80f) {
                                        snackbarData.dismiss()
                                    } else {
                                        offsetX = 0f
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    offsetX += dragAmount
                                }
                            )
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0F172A), Color(0xFF070913))
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.horizontalGradient(listOf(NeonCyan, NeonFuchsia)),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            snackbarData.dismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("swipeable_snackbar_popup")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val isFav = snackbarData.visuals.message.contains("favorite", ignoreCase = true) ||
                                    snackbarData.visuals.message.contains("liked", ignoreCase = true)
                            Icon(
                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isFav) NeonFuchsia else NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = snackbarData.visuals.message,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Swipe ➔",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                onClick = { snackbarData.dismiss() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Tab Content
            when (selectedTab) {
                0 -> HomeScreen(
                    songs = homeFilteredSongs,
                    allSongs = allSongs,
                    mostPlayedSongs = mostPlayedSongs,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    userProfile = userProfile,
                    categories = dynamicCategories,
                    randomCategory = randomCategory,
                    selectedCategory = selectedCategory,
                    searchQuery = homeSearchQuery,
                    onSearchQueryChange = { viewModel.setHomeSearchQuery(it) },
                    onSelectCategory = { viewModel.setCategory(it) },
                    onPlaySong = { song ->
                        viewModel.playSongFromList(
                            if (homeFilteredSongs.any { it.id == song.id }) homeFilteredSongs else allSongs,
                            song
                        )
                    },
                    onLongPressSong = { viewModel.setLongPressedSong(it) },
                    onOpenFullscreen = { viewModel.setPlayerExpanded(true) },
                    onPlayAll = {
                        if (homeFilteredSongs.isNotEmpty()) {
                            viewModel.playSongFromList(homeFilteredSongs, homeFilteredSongs.first())
                        }
                    },
                    onSongOptions = { viewModel.showSongOptions(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onOpenVipModal = { viewModel.setShowVipModal(true) },
                    notificationsCount = unreadNotificationsCount,
                    onOpenNotifications = { viewModel.setShowNotificationsDialog(true) }
                )

                1 -> SearchScreen(
                    searchQuery = searchQuery,
                    searchCategory = searchCategory,
                    categories = dynamicCategories,
                    allSongs = allSongs,
                    searchResults = searchResults,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    onCategoryChange = { viewModel.setSearchCategory(it) },
                    onPlaySong = { song ->
                        if (currentSong?.id == song.id) {
                            viewModel.setPlayerExpanded(true)
                        } else {
                            viewModel.playSongFromList(searchResults, song)
                        }
                    },
                    onLongPressSong = { viewModel.setLongPressedSong(it) },
                    onSongOptions = { viewModel.showSongOptions(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                )

                2 -> PlaylistsScreen(
                    playlists = playlists,
                    favoriteSongs = favoriteSongs,
                    activePlaylist = activePlaylist,
                    activePlaylistSongs = activePlaylistSongs,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    onOpenPlaylist = { viewModel.openPlaylist(it) },
                    onClosePlaylist = { viewModel.closePlaylist() },
                    onCreatePlaylistClick = { viewModel.setShowCreatePlaylistDialog(true) },
                    onDeletePlaylist = { viewModel.deletePlaylist(it) },
                    onPlaySong = { song ->
                        if (currentSong?.id == song.id) {
                            viewModel.setPlayerExpanded(true)
                        } else {
                            viewModel.playSongFromList(activePlaylistSongs, song)
                        }
                    },
                    onLongPressSong = { viewModel.setLongPressedSong(it) },
                    onPlayAll = { list ->
                        if (list.isNotEmpty()) viewModel.playSongFromList(list, list.first())
                    },
                    onSongOptions = { viewModel.showSongOptions(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                )

                3 -> LocalMusicScreen(
                    localSongs = localSongs,
                    isScanning = isScanningLocal,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    onPickFiles = { uris -> viewModel.addPickedAudioUris(uris, context) },
                    onPickFolder = { treeUri -> viewModel.scanSelectedFolder(treeUri, context) },
                    onScanDevice = { viewModel.scanLocalAudio() },
                    onClearLocal = { viewModel.clearLocalSongs() },
                    onPlaySong = { song ->
                        if (currentSong?.id == song.id) {
                            viewModel.setPlayerExpanded(true)
                        } else {
                            viewModel.playSongFromList(localSongs, song)
                        }
                    },
                    onLongPressSong = { viewModel.setLongPressedSong(it) },
                    onPlayAll = {
                        if (localSongs.isNotEmpty()) viewModel.playSongFromList(localSongs, localSongs.first())
                    },
                    onSongOptions = { viewModel.showSongOptions(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                )

                4 -> ProfileScreen(
                    userProfile = userProfile,
                    currentUser = currentUser,
                    isAuthLoading = isAuthLoading,
                    selectedQuality = selectedAudioQuality,
                    onLogin = { email, pass -> viewModel.loginWithEmail(email, pass) },
                    onRegister = { email, pass, name -> viewModel.registerWithEmail(email, pass, name) },
                    onSignOut = { viewModel.signOutUser() },
                    onOpenVipModal = { viewModel.setShowVipModal(true) },
                    onActivateFreeTrial = { viewModel.activateFreeTrial() },
                    onOpenEqualizer = { viewModel.setShowEqualizerSheet(true) },
                    onOpenSleepTimer = { viewModel.setShowSleepTimerDialog(true) },
                    onOpenAudioQuality = { viewModel.setShowAudioQualityDialog(true) },
                    onOpenReportLog = { viewModel.setShowReportLog(true) }
                )
            }

            // Bottom Stack: Mini Player Bar + 5-Tab Bar (hidden when expanded)
            if (!isPlayerExpanded) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    if (currentSong != null) {
                        MiniPlayerBar(
                            song = currentSong,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPositionMs,
                            durationMs = durationMs,
                            onExpand = { viewModel.setPlayerExpanded(true) },
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onNext = { viewModel.audioEngine.nextTrack() }
                        )
                    }

                    CyberBottomNavigationBar(
                        selectedTab = selectedTab,
                        onSelectTab = { tabIndex ->
                            if (selectedTab == tabIndex) {
                                when (tabIndex) {
                                    0 -> {
                                        viewModel.setCategory("All")
                                        viewModel.setHomeSearchQuery("")
                                    }
                                    1 -> {
                                        viewModel.setSearchQuery("")
                                        viewModel.setSearchCategory("All")
                                    }
                                    2 -> {
                                        viewModel.closePlaylist()
                                    }
                                    3 -> {
                                        // Vault / Local storage
                                    }
                                    4 -> {
                                        viewModel.setShowVipModal(false)
                                    }
                                }
                            } else {
                                viewModel.selectTab(tabIndex)
                            }
                        }
                    )
                }
            }

            // Fullscreen Player Animated Sheet
            AnimatedVisibility(
                visible = isPlayerExpanded,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                FullscreenPlayerScreen(
                    song = currentSong,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    repeatMode = repeatMode,
                    isShuffle = isShuffle,
                    liveWaveform = liveWaveform,
                    allSongs = allSongs,
                    playlistQueue = playlistQueue,
                    allPlaylistSongIds = allPlaylistSongIds,
                    isProActive = userProfile.isProActive,
                    isGoldenBassEnabled = isGoldenBassEnabled,
                    is8dAudio = is8dAudio,
                    onCollapse = { viewModel.setPlayerExpanded(false) },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.audioEngine.nextTrack() },
                    onPrevious = { viewModel.audioEngine.previousTrack() },
                    onSeek = { viewModel.audioEngine.seekTo(it) },
                    onToggleRepeat = { viewModel.audioEngine.toggleRepeatMode() },
                    onToggleShuffle = { viewModel.audioEngine.toggleShuffle() },
                    onToggleFavorite = { currentSong?.let { viewModel.toggleFavorite(it) } },
                    onToggleFavoriteSong = { viewModel.toggleFavorite(it) },
                    onOpenEqualizer = { viewModel.setShowEqualizerSheet(true) },
                    onOpenSleepTimer = { viewModel.setShowSleepTimerDialog(true) },
                    onToggleGoldenBass = { viewModel.toggleGoldenBass(it) },
                    onToggle8dAudio = { viewModel.toggle8dAudio(it) },
                    onOpenVipModal = { viewModel.setShowVipModal(true) },
                    onPlaySong = { viewModel.playSongFromList(allSongs, it) },
                    onOptionsClick = { currentSong?.let { viewModel.showSongOptions(it) } },
                    onAddToPlaylistClick = {
                        currentSong?.let { s ->
                            viewModel.setShowAddToPlaylistDialog(true, s)
                        }
                    },
                    onSongOptions = { viewModel.showSongOptions(it) },
                    onLongPressSong = { viewModel.setLongPressedSong(it) }
                )
            }
        }
    }

    // Modal Dialogs & Sheets
    if (showVipModal) {
        VipActivationDialog(
            isProActive = userProfile.isProActive,
            daysRemaining = userProfile.daysRemaining,
            onDismiss = { viewModel.setShowVipModal(false) },
            onActivateKey = { viewModel.activateVipKey(it) }
        )
    }
    
    if (showReportLog) {
        ReportLogDialog(
            reports = userReports,
            onWriteReport = {
                viewModel.setShowReportLog(false)
                viewModel.setSongForReport(
                    com.example.data.model.Song(
                        id = "general_feedback",
                        title = "General Feedback",
                        artist = "App",
                        album = "App",
                        audioUrl = ""
                    )
                )
            },
            onDismiss = { viewModel.setShowReportLog(false) }
        )
    }

    if (songForReport != null) {
        val targetSong = songForReport!!
        SubmitReportDialog(
            song = targetSong,
            currentUser = currentUser,
            userProfile = userProfile,
            isProActive = userProfile.isProActive,
            onSubmit = { msg ->
                val reportTitle = if (targetSong.id == "general_feedback") {
                    "General App Feedback"
                } else {
                    "${targetSong.title} (${targetSong.artist.ifBlank { "SK Artist" }})"
                }
                viewModel.submitReport(targetSong.id, reportTitle, msg)
                viewModel.setSongForReport(null)
            },
            onDismiss = { viewModel.setSongForReport(null) }
        )
    }

    if (showAudioQualityDialog) {
        AudioEngineQualityDialog(
            currentQuality = selectedAudioQuality,
            isProActive = userProfile.isProActive,
            onDismiss = { viewModel.setShowAudioQualityDialog(false) },
            onSelectQuality = { viewModel.setSelectedAudioQuality(it) },
            onOpenVipModal = { viewModel.setShowVipModal(true) }
        )
    }

    if (showNotificationsDialog) {
        com.example.ui.components.AdminNotificationsDialog(
            notifications = notifications,
            onSelectSong = { songId ->
                val songToPlay = allSongs.find { it.id == songId }
                if (songToPlay != null) {
                    viewModel.playSongFromList(allSongs, songToPlay)
                }
            },
            onDeleteNotification = { viewModel.deleteNotification(it) },
            onClearAll = { viewModel.clearAllNotifications() },
            onDismiss = { viewModel.setShowNotificationsDialog(false) }
        )
    }

    LongPressOverlay(
        song = longPressedSong,
        onDismiss = { viewModel.setLongPressedSong(null) },
        onPlayNow = {
            longPressedSong?.let { viewModel.playSongFromList(allSongs, it) }
            viewModel.setLongPressedSong(null)
        },
        onPlayNext = {
            longPressedSong?.let { viewModel.audioEngine.playSong(it) }
            viewModel.setLongPressedSong(null)
        },
        onToggleFavorite = {
            longPressedSong?.let { viewModel.toggleFavorite(it) }
            viewModel.setLongPressedSong(null)
        },
        onAddToPlaylist = {
            longPressedSong?.let { viewModel.setShowAddToPlaylistDialog(true, it) }
            viewModel.setLongPressedSong(null)
        },
        onReport = {
            val song = longPressedSong
            viewModel.setLongPressedSong(null)
            viewModel.setSongForReport(song)
        }
    )

    if (showEqualizerSheet) {
        EqualizerSheet(
            isEqualizerEnabled = isEqualizerEnabled,
            isGoldenBassEnabled = isGoldenBassEnabled,
            bassLevel = bassLevel,
            is8dAudioEnabled = is8dAudio,
            isSaveSettingsChecked = isSaveSettingsChecked,
            bandGains = eqBands,
            selectedPreset = eqPreset,
            playbackSpeed = playbackSpeed,
            isProActive = userProfile.isProActive,
            onDismiss = { viewModel.setShowEqualizerSheet(false) },
            onToggleEqualizer = { viewModel.audioEngine.toggleEqualizer(it) },
            onToggleGoldenBass = { viewModel.toggleGoldenBass(it) },
            onBassLevelChange = { viewModel.setBassLevel(it) },
            onToggle8dAudio = { viewModel.toggle8dAudio(it) },
            onToggleSaveSettings = { viewModel.audioEngine.toggleSaveSettings(it) },
            onBandChange = { idx, gain -> viewModel.audioEngine.setBandGain(idx, gain) },
            onPresetSelect = { viewModel.audioEngine.applyPreset(it) },
            onSpeedChange = { viewModel.audioEngine.setPlaybackSpeed(it) },
            onOpenVipModal = { viewModel.setShowVipModal(true) }
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { viewModel.setShowCreatePlaylistDialog(false) },
            onCreate = { name, cover -> viewModel.createPlaylist(name, cover) }
        )
    }

    if (showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            song = songForPlaylist,
            playlists = playlists,
            onDismiss = { viewModel.setShowAddToPlaylistDialog(false) },
            onSelectPlaylist = { plId ->
                songForPlaylist?.let { s -> viewModel.addSongToExistingPlaylist(plId, s) }
            },
            onCreateNewClick = { viewModel.setShowCreatePlaylistDialog(true) },
            onDeletePlaylist = { plId -> viewModel.deletePlaylist(plId) }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            minutesLeft = sleepMinutesLeft,
            onDismiss = { viewModel.setShowSleepTimerDialog(false) },
            onSetTimer = { viewModel.audioEngine.startSleepTimer(it) },
            onCancelTimer = { viewModel.audioEngine.cancelSleepTimer() }
        )
    }

    if (selectedSongForOptions != null) {
        SongOptionsBottomSheet(
            song = selectedSongForOptions!!,
            onDismiss = { viewModel.closeSongOptions() },
            onPlay = {
                viewModel.playSongFromList(allSongs, selectedSongForOptions!!)
            },
            onToggleFavorite = {
                viewModel.toggleFavorite(selectedSongForOptions!!)
            },
            onAddToPlaylist = {
                viewModel.setShowAddToPlaylistDialog(true, selectedSongForOptions!!)
                viewModel.closeSongOptions()
            },
            onReport = {
                val songToReport = selectedSongForOptions
                viewModel.closeSongOptions()
                viewModel.setSongForReport(songToReport)
            }
        )
    }
}

@Composable
fun CyberBottomNavigationBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
) {
    val items = listOf(
        Triple(0, Icons.Default.Home, "Discover"),
        Triple(1, Icons.Default.Search, "Search"),
        Triple(2, Icons.Default.LibraryMusic, "Playlists"),
        Triple(3, Icons.Default.SdStorage, "Vault"),
        Triple(4, Icons.Default.Person, "Profile")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberBackground.copy(alpha = 0.95f))
            .border(1.dp, CyberGlassBorder)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (index, icon, title) ->
            val isSelected = selectedTab == index
            val color = if (isSelected) NeonCyan else TextMuted

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectTab(index) }
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .testTag("tab_$title")
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    color = color,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
