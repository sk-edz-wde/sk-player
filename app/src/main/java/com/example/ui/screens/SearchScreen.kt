package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
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

@Composable
fun SearchScreen(
    searchQuery: String,
    searchCategory: String,
    categories: List<String> = listOf("All"),
    allSongs: List<Song> = emptyList(),
    searchResults: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onPlaySong: (Song) -> Unit,
    onLongPressSong: (Song) -> Unit = {},
    onSongOptions: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-focus search input when opening Search screen
    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // Ignore if layout not ready
        }
    }

    // Spotify-like Top Match calculation when query is typed
    val topMatchSong: Song? = remember(searchQuery, searchResults) {
        if (searchQuery.isNotBlank() && searchResults.isNotEmpty()) {
            val q = searchQuery.trim().lowercase()
            searchResults.firstOrNull { it.title.lowercase() == q }
                ?: searchResults.firstOrNull { it.title.lowercase().startsWith(q) }
                ?: searchResults.firstOrNull { it.artist.lowercase().startsWith(q) }
                ?: searchResults.firstOrNull()
        } else {
            null
        }
    }

    // Top trending genre/category cards background gradients
    val categoryGradients = remember {
        listOf(
            listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)),
            listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)),
            listOf(Color(0xFF00B4DB), Color(0xFF0083B0)),
            listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
            listOf(Color(0xFFF2994A), Color(0xFFF2C94C)),
            listOf(Color(0xFFEC008C), Color(0xFFFC6767)),
            listOf(Color(0xFF7F00FF), Color(0xFFE100FF)),
            listOf(Color(0xFF1F4037), Color(0xFF99F2C8))
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(top = 12.dp)
            .testTag("search_screen")
    ) {
        // Spotify Style Title
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            ),
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        // Spotify-style Floating Search Input Bar with Auto Enter Key Trigger
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = "What do you want to listen to?",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (searchQuery.isNotBlank()) NeonCyan else TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        onQueryChange("")
                    }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    // If user hits Enter and there's a top match or results, user gets immediate confirmation
                }
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
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .focusRequester(focusRequester)
                .testTag("search_text_input")
        )

        // Dynamic Genre/Category Filter Chips
        if (categories.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    NeonChip(
                        text = cat,
                        isSelected = searchCategory == cat,
                        onClick = { onCategoryChange(cat) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // When Query IS BLANK and Category is ALL -> Show Spotify "Browse All" / "Categories" & "Trending" Explore Grid
        val isBrowsing = searchQuery.isBlank() && (searchCategory.isBlank() || searchCategory.equals("All", ignoreCase = true))

        if (isBrowsing) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section: Explore Categories
                item {
                    Text(
                        text = "Browse all categories",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                // Grid of visual genre boxes like Spotify
                val browseCategories = categories.filter { !it.equals("All", ignoreCase = true) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        browseCategories.chunked(2).forEachIndexed { rowIndex, rowCats ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowCats.forEachIndexed { colIndex, catName ->
                                    val gradIndex = (rowIndex * 2 + colIndex) % categoryGradients.size
                                    val gradient = categoryGradients[gradIndex]
                                    val catCount = allSongs.count { it.hasCategory(catName) }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(84.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Brush.linearGradient(gradient))
                                            .clickable {
                                                onCategoryChange(catName)
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = catName,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp
                                            ),
                                            color = Color.White,
                                            modifier = Modifier.align(Alignment.TopStart),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Text(
                                            text = "$catCount tracks",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xDDFFFFFF),
                                            modifier = Modifier.align(Alignment.BottomStart)
                                        )
                                    }
                                }
                                if (rowCats.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Section: Popular Realtime Songs
                if (allSongs.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Discover all tracks (${allSongs.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }
                    }

                    items(allSongs, key = { "browse_${it.id}" }) { song ->
                        SongItemRow(
                            song = song,
                            isCurrentSong = currentSong?.id == song.id,
                            isPlaying = isPlaying,
                            onClick = { onPlaySong(song) },
                            onLongClick = { onLongPressSong(song) },
                            onOptionsClick = { onSongOptions(song) },
                            onFavoriteClick = { onToggleFavorite(song) }
                        )
                    }
                }
            }
        } else {
            // When Query IS ACTIVE -> Real-Time Instant Live Spotify Search Results
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Spotify "Top Result" Hero Card
                if (topMatchSong != null) {
                    item {
                        Text(
                            text = "TOP RESULT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(CyberSurfaceCard)
                                .border(1.dp, CyberGlassBorder, RoundedCornerShape(18.dp))
                                .clickable { onPlaySong(topMatchSong) }
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Image
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1E293B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (topMatchSong.imageUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = topMatchSong.imageUrl,
                                            contentDescription = topMatchSong.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    if (topMatchSong.isVipOnly) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(3.dp)
                                        ) {
                                            VipBadge(text = "VIP")
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = topMatchSong.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (currentSong?.id == topMatchSong.id) NeonCyan else TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${topMatchSong.artist} • ${topMatchSong.category}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0x3300FFFF))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "SONG",
                                                color = NeonCyan,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (topMatchSong.playCount > 0) {
                                            Text(
                                                text = "${topMatchSong.playCount} plays",
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                // Big Play Button
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (currentSong?.id == topMatchSong.id && isPlaying) NeonFuchsia else NeonCyan
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (currentSong?.id == topMatchSong.id && isPlaying) Icons.Default.Equalizer else Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color(0xFF04050A),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Songs List
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SONGS & MATCHES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = TextSecondary
                        )
                        Text(
                            text = "${searchResults.size} found",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan
                        )
                    }
                }

                if (searchResults.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x221E293B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SearchOff,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No results found for \"$searchQuery\"",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Please check the spelling or search by artist name, tag, or album",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted,
                                    modifier = Modifier.padding(top = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(searchResults, key = { it.id }) { song ->
                        SongItemRow(
                            song = song,
                            isCurrentSong = currentSong?.id == song.id,
                            isPlaying = isPlaying,
                            onClick = { onPlaySong(song) },
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
