package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.audio.RepeatMode
import com.example.data.model.Song
import com.example.ui.components.SongItemRow
import com.example.ui.components.VipBadge
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberGlassBorder
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonFuchsia
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.example.ui.theme.VipGold

@Composable
fun FullscreenPlayerScreen(
    song: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    repeatMode: RepeatMode,
    isShuffle: Boolean,
    liveWaveform: List<Float>,
    allSongs: List<Song> = emptyList(),
    playlistQueue: List<Song> = emptyList(),
    allPlaylistSongIds: Set<String> = emptySet(),
    isProActive: Boolean = false,
    isGoldenBassEnabled: Boolean = false,
    is8dAudio: Boolean = false,
    isDownloading: Boolean = false,
    onCollapse: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleFavoriteSong: ((Song) -> Unit)? = null,
    onDownloadClick: () -> Unit = {},
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onToggleGoldenBass: ((Boolean) -> Unit)? = null,
    onToggle8dAudio: ((Boolean) -> Unit)? = null,
    onOpenVipModal: () -> Unit = {},
    onPlaySong: (Song) -> Unit = {},
    onOptionsClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit = {},
    onSongOptions: ((Song) -> Unit)? = null,
    onLongPressSong: ((Song) -> Unit)? = null
) {
    if (song == null) return

    var isUserSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }

    val effectivePosition = if (isUserSeeking) seekPosition.toLong() else currentPositionMs
    val rawSongDur = if (song.durationMs > 1000L) {
        if (song.durationMs < 10000L) song.durationMs * 1000L else song.durationMs
    } else 210000L
    val totalDuration = if (durationMs > 1000L) durationMs else rawSongDur

    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rotate_full")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "rotation"
    )
    val vinylRotation = if (isPlaying) rotation else 0f
    val coroutineScope = rememberCoroutineScope()
    val dragOffsetY = remember { Animatable(0f) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragOffsetY.value.coerceAtLeast(0f).roundToInt()) }
            .graphicsLayer {
                val progress = (dragOffsetY.value / 400f).coerceIn(0f, 1f)
                alpha = (1f - progress * 0.5f).coerceIn(0.2f, 1f)
                scaleX = (1f - progress * 0.05f).coerceIn(0.95f, 1f)
                scaleY = (1f - progress * 0.05f).coerceIn(0.95f, 1f)
            }
            .testTag("fullscreen_player_screen"),
        color = CyberBackground
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenHeight = maxHeight
            val isSmallScreen = screenHeight < 680.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Sleek Drag Handle & Top Pull Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { },
                                onDragEnd = {
                                    if (dragOffsetY.value > 60f) {
                                        onCollapse()
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(300)
                                            dragOffsetY.snapTo(0f)
                                        }
                                    } else {
                                        coroutineScope.launch {
                                            dragOffsetY.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = 500f))
                                        }
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        dragOffsetY.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = 500f))
                                    }
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    if (dragAmount > 0 || dragOffsetY.value > 0) {
                                        change.consume()
                                        val newOffset = (dragOffsetY.value + dragAmount).coerceAtLeast(0f)
                                        coroutineScope.launch {
                                            dragOffsetY.snapTo(newOffset)
                                        }
                                    }
                                }
                            )
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top drag pill indicator
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp, bottom = 6.dp)
                            .width(42.dp)
                            .height(4.5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(CyberGlassBorder.copy(alpha = 0.9f))
                    )

                    // Top Bar: Minimize, Now Playing Title, More
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onCollapse, modifier = Modifier.size(44.dp).testTag("player_collapse_button")) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Collapse",
                                tint = TextPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (song.isLocal) "LOCAL AUDIO ENGINE" else "PLAYING FROM CLOUD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = NeonCyan
                            )
                            Text(
                                text = song.category,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = TextSecondary
                            )
                        }

                        IconButton(onClick = onOptionsClick, modifier = Modifier.size(44.dp)) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Center Vinyl Disk / Album Sleeve Box Artwork with Tap-to-Toggle & Smooth Swipe
                var isSleeveBoxMode by remember { mutableStateOf(false) }
                val dragOffsetX = remember { Animatable(0f) }
                val coroutineScope = rememberCoroutineScope()
                val vinylSize = if (isSmallScreen) 200.dp else 240.dp
                val sleeveBoxWidth = if (isSmallScreen) 280.dp else 320.dp
                val sleeveBoxHeight = if (isSmallScreen) 180.dp else 210.dp

                val vinylSlideOut by animateDpAsState(
                    targetValue = if (isSleeveBoxMode) (if (isSmallScreen) 65.dp else 80.dp) else 0.dp,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 350f),
                    label = "vinyl_slide_out"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isSleeveBoxMode) sleeveBoxHeight else vinylSize)
                        .padding(vertical = 4.dp)
                        .offset { IntOffset(dragOffsetX.value.roundToInt(), 0) }
                        .pointerInput(song.id) {
                            detectHorizontalDragGestures(
                                onDragStart = { },
                                onDragEnd = {
                                    val currentVal = dragOffsetX.value
                                    if (currentVal < -80f) {
                                        // Swiped left -> Next Track
                                        coroutineScope.launch {
                                            dragOffsetX.animateTo(-350f, tween(150))
                                            onNext()
                                            dragOffsetX.snapTo(350f)
                                            dragOffsetX.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 380f))
                                        }
                                    } else if (currentVal > 80f) {
                                        // Swiped right -> Previous Track
                                        coroutineScope.launch {
                                            dragOffsetX.animateTo(350f, tween(150))
                                            onPrevious()
                                            dragOffsetX.snapTo(-350f)
                                            dragOffsetX.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 380f))
                                        }
                                    } else {
                                        coroutineScope.launch {
                                            dragOffsetX.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 450f))
                                        }
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        dragOffsetX.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 450f))
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    coroutineScope.launch {
                                        dragOffsetX.snapTo(dragOffsetX.value + dragAmount)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSleeveBoxMode) {
                        // --- SLEEVE BOX MODE (Matching Screenshot) ---
                        Box(
                            modifier = Modifier
                                .width(sleeveBoxWidth)
                                .height(sleeveBoxHeight)
                                .clickable { isSleeveBoxMode = !isSleeveBoxMode }
                                .testTag("sleeve_box_artwork_card"),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            // 1. Spinning Vinyl Disk sliding out from behind the sleeve jacket to the right
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .offset(x = vinylSlideOut)
                                    .size(if (isSmallScreen) 165.dp else 195.dp)
                                    .rotate(vinylRotation)
                                    .shadow(16.dp, CircleShape, spotColor = NeonCyan)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Color(0xFF22263A),
                                                Color(0xFF0F172A),
                                                Color(0xFF070913)
                                            )
                                        )
                                    )
                                    .border(2.dp, Brush.sweepGradient(listOf(NeonCyan, NeonFuchsia, NeonIndigo, NeonCyan)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                // Concentric vinyl grooves
                                Box(
                                    modifier = Modifier
                                        .size(if (isSmallScreen) 130.dp else 150.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(if (isSmallScreen) 100.dp else 115.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, Color(0x22FFFFFF), CircleShape)
                                )
                                // Center mini album sticker
                                AsyncImage(
                                    model = song.imageUrl,
                                    contentDescription = song.title,
                                    modifier = Modifier
                                        .size(if (isSmallScreen) 65.dp else 78.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, Color(0xFFFFFFFF), CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                // Center spindle hole
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(CyberBackground)
                                        .border(1.dp, Color(0xFFFFFFFF), CircleShape)
                                )
                            }

                            // 2. Square Album Cover Sleeve Jacket (Foreground, covers the left side of the disc)
                            Box(
                                modifier = Modifier
                                    .size(if (isSmallScreen) 175.dp else 200.dp)
                                    .shadow(20.dp, RoundedCornerShape(20.dp), spotColor = Color.Black)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(CyberCardBg)
                                    .border(
                                        width = 1.5.dp,
                                        brush = Brush.linearGradient(
                                            listOf(
                                                NeonCyan.copy(alpha = 0.8f),
                                                NeonFuchsia.copy(alpha = 0.5f),
                                                CyberGlassBorder
                                            )
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                            ) {
                                // Album Jacket Cover Artwork
                                if (song.imageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = song.imageUrl,
                                        contentDescription = song.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF070913))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(56.dp)
                                        )
                                    }
                                }

                                // Left Spine Trim Detail
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .width(6.dp)
                                        .fillMaxHeight()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(NeonCyan, NeonFuchsia, NeonIndigo)
                                            )
                                        )
                                )

                                // Top VIP Tag if applicable
                                if (song.isVipOnly) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                    ) {
                                        VipBadge()
                                    }
                                }

                                // Bottom "TAP TO CLOSE BOX" Cyber Ribbon
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, Color(0xDD0B0F19), Color(0xFA070913))
                                            )
                                        )
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Album,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "TAP TO CLOSE BOX",
                                            color = NeonCyan,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // --- FULL CENTER VINYL DISC MODE ---
                        Box(
                            modifier = Modifier
                                .size(vinylSize)
                                .clickable { isSleeveBoxMode = !isSleeveBoxMode }
                                .testTag("full_vinyl_artwork_card"),
                            contentAlignment = Alignment.Center
                        ) {
                            // Outer Grooved Vinyl Disc with Neon Glow Rim
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .rotate(vinylRotation)
                                    .shadow(24.dp, CircleShape, spotColor = NeonCyan)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Color(0xFF22263A),
                                                Color(0xFF0F172A),
                                                Color(0xFF070913)
                                            )
                                        )
                                    )
                                    .border(2.dp, Brush.sweepGradient(listOf(NeonCyan, NeonFuchsia, NeonIndigo, NeonCyan)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                // Concentric vinyl audio grooves
                                Box(
                                    modifier = Modifier
                                        .size(vinylSize * 0.75f)
                                        .clip(CircleShape)
                                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(vinylSize * 0.6f)
                                        .clip(CircleShape)
                                        .border(1.dp, Color(0x22FFFFFF), CircleShape)
                                )

                                // Center Album Art
                                if (song.imageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = song.imageUrl,
                                        contentDescription = song.title,
                                        modifier = Modifier
                                            .size(vinylSize * 0.46f)
                                            .clip(CircleShape)
                                            .border(2.dp, Color(0xFFFFFFFF), CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(vinylSize * 0.46f)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                                )
                                            )
                                            .border(2.dp, NeonCyan, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(vinylSize * 0.22f)
                                        )
                                    }
                                }

                                // Center Spindle Hole
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(CyberBackground)
                                        .border(1.5.dp, Color(0xFFFFFFFF), CircleShape)
                                )
                            }

                            // VIP Overlay Tag on Vinyl if isPro
                            if (song.isVipOnly) {
                                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                                    VipBadge()
                                }
                            }
                        }
                    }
                }

                // Song Title, Artist, Download & Favorite Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = if (isSmallScreen) 18.sp else 20.sp
                            ),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${song.artist} • ${song.album}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Add to Playlist Button
                        val isInPlaylist = allPlaylistSongIds.contains(song.id)
                        IconButton(
                            onClick = onAddToPlaylistClick,
                            modifier = Modifier.size(44.dp).testTag("player_add_playlist_button")
                        ) {
                            Icon(
                                imageVector = if (isInPlaylist) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = "Add to Playlist",
                                tint = if (isInPlaylist) NeonCyan else TextMuted,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Heart Favorite Button with instant highlight and glow
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(44.dp).testTag("player_toggle_favorite")
                        ) {
                            Icon(
                                imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (song.isFavorite) NeonFuchsia else TextMuted,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Waveform Realtime Visualizer Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val points = if (isPlaying && liveWaveform.isNotEmpty()) liveWaveform else song.waveformPoints
                    points.take(24).forEachIndexed { idx, amp ->
                        val barHeight = (amp.coerceIn(0.1f, 1.0f) * 24).dp
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(NeonCyan, if (idx % 2 == 0) NeonFuchsia else NeonIndigo)
                                    )
                                )
                        )
                    }
                }

                // Progress Slider & Timestamps
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = effectivePosition.toFloat(),
                        onValueChange = {
                            isUserSeeking = true
                            seekPosition = it
                        },
                        onValueChangeFinished = {
                            isUserSeeking = false
                            onSeek(seekPosition.toLong())
                        },
                        valueRange = 0f..totalDuration.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = Color(0x331E293B)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_progress_slider")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(effectivePosition),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = formatTime(totalDuration),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                // Playback Control Cluster: Shuffle, Prev, Play/Pause, Next, Repeat
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleShuffle, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) NeonCyan else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp).testTag("player_prev_button")) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = TextPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NeonCyan, NeonIndigo, NeonFuchsia)))
                            .border(2.dp, Color(0xFFFFFFFF), CircleShape)
                            .shadow(12.dp, CircleShape)
                            .testTag("player_play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color(0xFF04050A),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    IconButton(onClick = onNext, modifier = Modifier.size(48.dp).testTag("player_next_button")) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = TextPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    IconButton(onClick = onToggleRepeat, modifier = Modifier.size(44.dp)) {
                        val icon = when (repeatMode) {
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        }
                        val tint = when (repeatMode) {
                            RepeatMode.OFF -> TextMuted
                            RepeatMode.ALL -> NeonCyan
                            RepeatMode.ONE -> NeonFuchsia
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = "Repeat",
                            tint = tint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 4-Action Grid: Golden Bass, 8D Audio, DSP Equalizer, Schedule (Sleep Timer)
                // Fully visible, high contrast, never cut off
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. 1-Touch 100% Golden Bass Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isProActive && isGoldenBassEnabled) VipGold.copy(alpha = 0.25f)
                                else CyberCardBg
                            )
                            .border(
                                1.dp,
                                if (isProActive && isGoldenBassEnabled) VipGold else CyberGlassBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (!isProActive) {
                                    onOpenVipModal()
                                } else {
                                    onToggleGoldenBass?.invoke(!isGoldenBassEnabled)
                                }
                            }
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (!isProActive) Icons.Default.Lock else Icons.Default.Waves,
                                contentDescription = "Golden Bass",
                                tint = if (isGoldenBassEnabled) VipGold else if (!isProActive) VipGold else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isGoldenBassEnabled) "100% Bass" else "Bass OFF",
                                color = if (isGoldenBassEnabled) VipGold else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 2. 8D Audio Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (is8dAudio) NeonCyan.copy(alpha = 0.2f)
                                else CyberCardBg
                            )
                            .border(
                                1.dp,
                                if (is8dAudio) NeonCyan else CyberGlassBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (!isProActive) {
                                    onOpenVipModal()
                                } else {
                                    onToggle8dAudio?.invoke(!is8dAudio)
                                }
                            }
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (!isProActive) Icons.Default.Lock else Icons.Default.SurroundSound,
                                contentDescription = "8D Audio",
                                tint = if (is8dAudio) NeonCyan else if (!isProActive) VipGold else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "8D Audio",
                                color = if (is8dAudio) NeonCyan else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 3. DSP Equalizer Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberCardBg)
                            .border(1.dp, CyberGlassBorder, RoundedCornerShape(12.dp))
                            .clickable { onOpenEqualizer() }
                            .padding(horizontal = 4.dp)
                            .testTag("player_open_eq"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "EQ",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Equalizer",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 4. Schedule (Sleep Timer) Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberCardBg)
                            .border(1.dp, CyberGlassBorder, RoundedCornerShape(12.dp))
                            .clickable { onOpenSleepTimer() }
                            .padding(horizontal = 4.dp)
                            .testTag("player_open_timer"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Schedule",
                                tint = NeonFuchsia,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Schedule",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Sleek Vertical Scroll List: ACTIVE QUEUE & ALL PLAYLIST TRACKS
                val queueTracks = remember(playlistQueue, allSongs) {
                    if (playlistQueue.isNotEmpty()) playlistQueue else allSongs
                }

                if (queueTracks.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "PLAYING QUEUE • ALL TRACKS",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "${queueTracks.size} tracks",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                        }

                        queueTracks.forEach { queueSong ->
                            val isCurrent = (queueSong.id == song.id)
                            SongItemRow(
                                song = queueSong,
                                isCurrentSong = isCurrent,
                                isPlaying = isCurrent && isPlaying,
                                onClick = { onPlaySong(queueSong) },
                                onLongClick = { onLongPressSong?.invoke(queueSong) ?: onSongOptions?.invoke(queueSong) },
                                onOptionsClick = { onSongOptions?.invoke(queueSong) },
                                onFavoriteClick = { onToggleFavoriteSong?.invoke(queueSong) }
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
