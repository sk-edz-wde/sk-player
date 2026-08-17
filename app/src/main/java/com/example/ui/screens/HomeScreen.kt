package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.data.model.UserProfile
import com.example.ui.components.NeonChip
import com.example.ui.components.SongItemRow
import com.example.ui.components.VipBadge
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberGlassBorder
import com.example.ui.theme.CyberSurfaceCard
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonFuchsia
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VipGold

@Composable
fun HomeScreen(
    songs: List<Song>,
    allSongs: List<Song>,
    mostPlayedSongs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    userProfile: UserProfile,
    categories: List<String>,
    randomCategory: String?,
    selectedCategory: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onPlaySong: (Song) -> Unit,
    onOpenFullscreen: () -> Unit = {},
    onPlayAll: () -> Unit,
    onSongOptions: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onOpenVipModal: () -> Unit,
    notificationsCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onLongPressSong: (Song) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val topTracks = remember(mostPlayedSongs, allSongs, selectedCategory) {
        val baseList = if (selectedCategory.equals("All", ignoreCase = true)) {
            mostPlayedSongs.ifEmpty { allSongs }
        } else {
            val filtered = allSongs.filter { it.hasCategory(selectedCategory) }
            val filteredMostPlayed = mostPlayedSongs.filter { it.hasCategory(selectedCategory) }
            filteredMostPlayed.ifEmpty { filtered }
        }
        baseList.take(10)
    }

    val homeCategories = remember(categories) {
        val list = mutableListOf("All")
        list.addAll(categories.filter { !it.equals("All", ignoreCase = true) })
        list.distinct()
    }

    val randomCategorySongs = remember(randomCategory, allSongs) {
        if (randomCategory != null) {
            allSongs.filter { it.hasCategory(randomCategory) }.shuffled().take(10)
        } else {
            emptyList()
        }
    }

    val handleSongClick: (Song) -> Unit = { clickedSong ->
        if (currentSong?.id == clickedSong.id) {
            onOpenFullscreen()
        } else {
            onPlaySong(clickedSong)
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    var isSearchActive by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            try {
                searchFocusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore focus error
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .testTag("home_screen")
    ) {
        // Main Home Screen Content (When search overlay is inactive)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = com.example.R.drawable.app_logo),
                                    contentDescription = "SK Player Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "SK EDZ",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        ),
                                        color = NeonCyan
                                    )
                                    Text(
                                        text = "PLAYER",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Light,
                                            letterSpacing = 1.sp
                                        ),
                                        color = NeonFuchsia
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Text(
                                        text = "Real-Time Audio • ${allSongs.size} Tracks",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Admin Announcements Bell with unclipped floating badge
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable(onClick = onOpenNotifications)
                                    .testTag("admin_notifications_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(CyberSurfaceDark)
                                        .border(1.dp, CyberGlassBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Announcements",
                                        tint = if (notificationsCount > 0) NeonCyan else TextSecondary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                if (notificationsCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(NeonFuchsia)
                                            .border(1.5.dp, CyberBackground, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (notificationsCount > 99) "99+" else notificationsCount.toString(),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }

                            // VIP Pro Badge / Trigger Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (userProfile.isProActive) {
                                            Brush.horizontalGradient(listOf(Color(0xFFD97706), Color(0xFFF59E0B)))
                                        } else {
                                            Brush.horizontalGradient(listOf(Color(0x33F59E0B), Color(0x33D97706)))
                                        }
                                    )
                                    .border(1.dp, VipGold, RoundedCornerShape(16.dp))
                                    .clickable(onClick = onOpenVipModal)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("vip_header_badge")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (userProfile.isProActive) Color(0xFF04050A) else VipGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (userProfile.isProActive) "VIP ACTIVE" else "PRO",
                                        color = if (userProfile.isProActive) Color(0xFF04050A) else VipGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Clickable Search Bar Trigger
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CyberSurfaceDark)
                            .border(1.dp, CyberGlassBorder, RoundedCornerShape(16.dp))
                            .clickable { isSearchActive = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("home_search_bar_trigger")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) searchQuery else "Search songs, artists, tags...",
                                color = if (searchQuery.isNotBlank()) TextPrimary else TextMuted,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Top Horizontal Cards Carousel: Always displays MOST PLAYED TRACKS
            val topShelfSongs = topTracks
            if (topShelfSongs.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5722),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "MOST PLAYED TRACKS",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = TextPrimary
                                )
                            }

                            Text(
                                text = "Real-Time Top",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFFFF5722)
                            )
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.padding(top = 10.dp)
                        ) {
                            itemsIndexed(topShelfSongs, key = { _, s -> "most_played_${s.id}" }) { index, song ->
                                val isThisPlaying = currentSong?.id == song.id && isPlaying
                                val rankNumber = index + 1

                                SongCardItem(
                                    song = song,
                                    rankNumber = rankNumber,
                                    isCurrentSong = isThisPlaying,
                                    onClick = { handleSongClick(song) },
                                    onLongClick = { onLongPressSong(song) }
                                )
                            }
                        }
                    }
                }
            }

            // Random Category Shelf
            if (selectedCategory.equals("All", ignoreCase = true) && randomCategory != null && randomCategorySongs.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${randomCategory.uppercase()} COLLECTION",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = "Discover",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = NeonIndigo
                            )
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.padding(top = 10.dp)
                        ) {
                            items(randomCategorySongs, key = { s -> "random_${s.id}" }) { song ->
                                val isThisPlaying = currentSong?.id == song.id && isPlaying

                                SongCardItem(
                                    song = song,
                                    rankNumber = null, // No rank for random collection
                                    isCurrentSong = isThisPlaying,
                                    onClick = { handleSongClick(song) },
                                    onLongClick = { onLongPressSong(song) }
                                )
                            }
                        }
                    }
                }
            }

            // Horizontal Category Chips (Curated on Home: All, Trending, Tamil)
            if (homeCategories.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "CATEGORIES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            items(homeCategories) { category ->
                                NeonChip(
                                    text = category,
                                    isSelected = selectedCategory.equals(category, ignoreCase = true),
                                    onClick = { onSelectCategory(category) }
                                )
                            }
                        }
                    }
                }
            }

            // Removed Dynamic Category Shelves (Slide Cards) as requested

            // Song Library Header (Full Card List for ALL TRACKS)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedCategory.equals("All", ignoreCase = true)) "ALL TRACKS" else "${selectedCategory.uppercase()} TRACKS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "${songs.size} tracks",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

                // Syncing / Empty State for Main Home
                if (songs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(CyberSurfaceCard)
                                .border(1.dp, CyberGlassBorder, RoundedCornerShape(20.dp))
                                .padding(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (allSongs.isNotEmpty() && !selectedCategory.equals("All", ignoreCase = true)) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = NeonFuchsia,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = "No tracks in '$selectedCategory'",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = TextPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "No songs are tagged with this category yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = { onSelectCategory("All") },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("SHOW ALL TRACKS", color = Color(0xFF04050A), fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = NeonCyan,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "Loading tracks...",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = TextPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Connecting to real-time audio library...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Vertical List of Songs (Full Cards)
                items(songs, key = { "home_item_${it.id}" }) { song ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SongItemRow(
                            song = song,
                            isCurrentSong = currentSong?.id == song.id,
                            isPlaying = isPlaying,
                            onClick = { handleSongClick(song) },
                            onLongClick = { onLongPressSong(song) },
                                onOptionsClick = { onSongOptions(song) },
                            onFavoriteClick = { onToggleFavorite(song) }
                        )
                    }
                }
        }

        // FULL SCREEN SEARCH OVERLAY
        AnimatedVisibility(
            visible = isSearchActive,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -40 })
        ) {
            BackHandler {
                isSearchActive = false
                onSearchQueryChange("")
                keyboardController?.hide()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CyberBackground)
                    .testTag("search_fullscreen_overlay")
            ) {
                // Top Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            isSearchActive = false
                            onSearchQueryChange("")
                            keyboardController?.hide()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = {
                            Text(
                                text = "Search tracks, artists, albums, tags...",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = { keyboardController?.hide() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberGlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CyberSurfaceDark,
                            unfocusedContainerColor = CyberSurfaceDark,
                            cursorColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(searchFocusRequester)
                            .testTag("overlay_search_text_input")
                    )
                }

                // Dynamic Category Filter Chips in Search Overlay
                if (categories.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            NeonChip(
                                text = category,
                                isSelected = selectedCategory.equals(category, ignoreCase = true),
                                onClick = { onSelectCategory(category) }
                            )
                        }
                    }
                }

                // Search Results List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "SEARCH RESULTS" else "ALL TRACKS",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = NeonCyan
                            )
                            Text(
                                text = "${songs.size} tracks",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }

                    if (songs.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 24.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(CyberSurfaceCard)
                                    .border(1.dp, CyberGlassBorder, RoundedCornerShape(20.dp))
                                    .padding(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "No songs found for \"$searchQuery\"" else "No songs found",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Try searching with title, artist, movie or tag",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    items(songs, key = { "search_res_${it.id}" }) { song ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            SongItemRow(
                                song = song,
                                isCurrentSong = currentSong?.id == song.id,
                                isPlaying = isPlaying,
                                onClick = { handleSongClick(song) },
                                onLongClick = { onLongPressSong(song) },
                                onOptionsClick = { onSongOptions(song) },
                                onFavoriteClick = { onToggleFavorite(song) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SongCardItem(
    song: Song,
    rankNumber: Int?,
    isCurrentSong: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CyberSurfaceCard)
            .border(
                width = if (isCurrentSong) 2.dp else 1.dp,
                brush = if (isCurrentSong) {
                    Brush.sweepGradient(listOf(NeonCyan, NeonFuchsia, NeonCyan))
                } else {
                    Brush.linearGradient(listOf(CyberGlassBorder, Color(0x22FFFFFF)))
                },
                shape = RoundedCornerShape(20.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(10.dp)
            .testTag("song_card_${song.id}")
    ) {
        Column {
            // Album Art with Rank / Category Badge & Play Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                if (song.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = song.imageUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Clean vibrant Music Note icon when no cover image exists
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                // Dark gradient bottom overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xCC050814))
                            )
                        )
                )

                // Rank Badge (#1, #2, etc.) or Category Pill
                if (rankNumber != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (rankNumber) {
                                    1 -> Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))
                                    2 -> Brush.horizontalGradient(listOf(Color(0xFFE0E0E0), Color(0xFFB0BEC5)))
                                    3 -> Brush.horizontalGradient(listOf(Color(0xFFCD7F32), Color(0xFF8D6E63)))
                                    else -> Brush.horizontalGradient(listOf(Color(0x88000000), Color(0x88000000)))
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "#$rankNumber",
                            color = if (rankNumber <= 3) Color(0xFF04050A) else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xBB04050A))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = song.category.ifBlank { "Music" },
                            color = NeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // VIP Badge if pro-only
                if (song.isVipOnly) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        VipBadge(text = "VIP")
                    }
                }

                // Info pill at bottom-left (Play count or Duration)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (song.playCount > 0) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF5722),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "${song.playCount} plays",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = song.durationFormatted,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Play/Pause Action Icon
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isCurrentSong) NeonFuchsia else NeonCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCurrentSong) Icons.Default.Equalizer else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF04050A),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Song Title
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isCurrentSong) NeonCyan else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Artist & Category
            Text(
                text = "${song.artist} • ${song.category}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CategoryShelfRow(
    title: String,
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onPlaySong: (Song) -> Unit,
    onLongPressSong: (Song) -> Unit = {},
    onSeeAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = NeonIndigo,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    ),
                    color = TextPrimary
                )
            }

            Text(
                text = "${songs.size} tracks",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = NeonCyan,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onSeeAll)
                    .padding(4.dp)
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(top = 10.dp)
        ) {
            items(songs, key = { "shelf_item_${it.id}" }) { song ->
                val isThisPlaying = currentSong?.id == song.id && isPlaying
                SongCardItem(
                    song = song,
                    rankNumber = null,
                    isCurrentSong = isThisPlaying,
                    onClick = { onPlaySong(song) },
                    onLongClick = { onLongPressSong(song) }
                )
            }
        }
    }
}
