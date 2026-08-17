package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseUser

@Composable
fun SubmitReportDialog(
    song: Song,
    currentUser: FirebaseUser?,
    userProfile: com.example.data.model.UserProfile? = null,
    isProActive: Boolean = false,
    onSubmit: (message: String) -> Unit,
    onDismiss: () -> Unit
) {
    val isSpecificSong = song.id.isNotBlank() && song.id != "general_feedback"
    val songQuickChips = remember {
        listOf(
            "Audio Not Playing / Broken",
            "Wrong Song Name / Artist",
            "Audio Quality / Noise Issue",
            "Playback Stalling / Buffering",
            "Incorrect Category / Tag"
        )
    }

    val generalQuickChips = remember {
        listOf(
            "New Song Request",
            "Audio Player Feature",
            "UI / Display Bug",
            "App Lag / Performance",
            "General Feedback"
        )
    }

    val chips = if (isSpecificSong) songQuickChips else generalQuickChips
    var selectedChip by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }

    val userDisplay = if (currentUser?.email != null) {
        currentUser.email!!
    } else if (userProfile != null && userProfile.displayName.isNotBlank() && userProfile.displayName != "SK User") {
        userProfile.displayName
    } else {
        "Guest User (Local Device)"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xDD04050A))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .border(
                        1.5.dp,
                        Brush.verticalGradient(
                            if (isProActive) listOf(
                                VipGold,
                                NeonFuchsia.copy(alpha = 0.9f),
                                NeonCyan.copy(alpha = 0.8f)
                            ) else listOf(
                                NeonFuchsia.copy(alpha = 0.9f),
                                NeonIndigo.copy(alpha = 0.5f),
                                NeonCyan.copy(alpha = 0.8f)
                            )
                        ),
                        RoundedCornerShape(26.dp)
                    )
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .testTag("submit_report_dialog"),
                color = CyberSurfaceDark,
                tonalElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isProActive) VipGold.copy(alpha = 0.2f) else NeonFuchsia.copy(alpha = 0.15f))
                                    .border(1.dp, if (isProActive) VipGold else NeonFuchsia.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isSpecificSong) Icons.Default.ReportProblem else Icons.Default.EditNote,
                                    contentDescription = null,
                                    tint = if (isProActive) VipGold else NeonFuchsia,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = if (isSpecificSong) "REPORT TRACK ISSUE" else "SUBMIT FEEDBACK & REPORT",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = if (isProActive) VipGold else NeonFuchsia
                                )
                                Text(
                                    text = if (isProActive) "Priority Support • VIP Active 👑" else if (isSpecificSong) "Attached to current track" else "General app feedback",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isProActive) VipGoldBright else TextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CyberGlassFill)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // IF SPECIFIC SONG: ATTACHED TRACK CARD
                    if (isSpecificSong) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                            color = CyberSurfaceCard
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF131828)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (song.coverUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = song.coverUrl,
                                            contentDescription = song.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = NeonCyan.copy(alpha = 0.15f),
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, NeonCyan)
                                        ) {
                                            Text(
                                                text = "ATTACHED TRACK",
                                                color = NeonCyan,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }

                                        if (song.category.isNotBlank()) {
                                            Text(
                                                text = song.category.uppercase(),
                                                color = TextMuted,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = song.artist.ifBlank { "Unknown Artist" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // User Account Identity indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isProActive) VipGold.copy(alpha = 0.12f) else CyberGlassFill)
                            .border(1.dp, if (isProActive) VipGold.copy(alpha = 0.4f) else Color.Transparent, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isProActive) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = VipGold,
                                    modifier = Modifier.padding(end = 2.dp)
                                ) {
                                    Text(
                                        text = "👑 VIP PRO",
                                        color = Color(0xFF04050A),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Reporting as:",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isProActive) VipGoldBright else TextMuted
                            )
                        }
                        Text(
                            text = userDisplay,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isProActive) VipGold else NeonCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Issue Chips
                    Text(
                        text = if (isSpecificSong) "Select Issue Type:" else "Category:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chips.forEach { chipText ->
                            val isSelected = selectedChip == chipText
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) NeonFuchsia else CyberGlassFill,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) NeonFuchsia else CyberGlassBorder
                                ),
                                modifier = Modifier.clickable {
                                    if (isSelected) {
                                        selectedChip = null
                                    } else {
                                        selectedChip = chipText
                                    }
                                }
                            ) {
                                Text(
                                    text = chipText,
                                    color = if (isSelected) Color(0xFF04050A) else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description text input
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = {
                            Text(
                                text = if (isSpecificSong) "Describe the audio problem, broken timestamp, or incorrect metadata..." else "Share your thoughts, feature suggestions, or details about the issue...",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        },
                        label = { Text("Details & Description", color = TextSecondary, fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("report_description_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonFuchsia,
                            unfocusedBorderColor = CyberGlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonFuchsia
                        ),
                        shape = RoundedCornerShape(14.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGlassFill),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("CANCEL", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val finalMessage = buildString {
                                    if (!selectedChip.isNullOrBlank()) {
                                        append("[$selectedChip] ")
                                    }
                                    if (description.isNotBlank()) {
                                        append(description.trim())
                                    } else if (!selectedChip.isNullOrBlank()) {
                                        append(selectedChip)
                                    } else {
                                        append(if (isSpecificSong) "Reported audio issue" else "Feedback submission")
                                    }
                                }
                                onSubmit(finalMessage)
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(46.dp)
                                .testTag("submit_report_confirm_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonFuchsia),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isSpecificSong) "SUBMIT REPORT" else "SUBMIT FEEDBACK",
                                color = Color(0xFF04050A),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
