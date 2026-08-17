package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberGlassBorder
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonFuchsia
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

data class AudioQualityOption(
    val title: String,
    val bitrate: String,
    val description: String,
    val isVipOnly: Boolean = false,
    val tag: String
)

@Composable
fun AudioEngineQualityDialog(
    currentQuality: String,
    isProActive: Boolean,
    onDismiss: () -> Unit,
    onSelectQuality: (String) -> Unit,
    onOpenVipModal: () -> Unit
) {
    val qualityOptions = listOf(
        AudioQualityOption(
            title = "Studio Master DSP",
            bitrate = "960 kbps",
            description = "32-bit Float 96kHz Studio Master • Pure Golden Bass Hi-Fi",
            isVipOnly = true,
            tag = "960 kbps (Studio Master)"
        ),
        AudioQualityOption(
            title = "Ultra Hi-Res FLAC",
            bitrate = "920 kbps",
            description = "Lossless 24-bit/96kHz direct stream with 8D Spatial Audio",
            isVipOnly = true,
            tag = "920 kbps (Lossless FLAC)"
        ),
        AudioQualityOption(
            title = "Ultra HD Master",
            bitrate = "320 kbps",
            description = "Studio Grade 32-bit DSP • Golden Bass Boost",
            isVipOnly = true,
            tag = "320 kbps (Ultra HD)"
        ),
        AudioQualityOption(
            title = "High Definition",
            bitrate = "256 kbps",
            description = "Crisp and clear balanced audio output",
            isVipOnly = false,
            tag = "256 kbps (High Definition)"
        ),
        AudioQualityOption(
            title = "Standard Data Saver",
            bitrate = "128 kbps",
            description = "Optimized for mobile data savings",
            isVipOnly = false,
            tag = "128 kbps (Standard)"
        )
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.6f), NeonFuchsia.copy(alpha = 0.4f))),
                    shape = RoundedCornerShape(24.dp)
                )
                .testTag("audio_quality_dialog"),
            colors = CardDefaults.cardColors(containerColor = CyberCardBg),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.15f))
                            .border(1.dp, NeonCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Audio Engine Quality",
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Audio Engine Quality",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Realtime Audio DSP Engine Bitrate",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                qualityOptions.forEach { opt ->
                    val isSelected = currentQuality.contains(opt.bitrate, ignoreCase = true) || currentQuality == opt.tag
                    val isLocked = opt.isVipOnly && !isProActive

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                when {
                                    isSelected -> NeonCyan.copy(alpha = 0.15f)
                                    isLocked -> Color.Black.copy(alpha = 0.35f)
                                    else -> CyberCardBg.copy(alpha = 0.5f)
                                }
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = when {
                                    isSelected -> NeonCyan
                                    isLocked -> GoldPrimary.copy(alpha = 0.4f)
                                    else -> CyberGlassBorder
                                },
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                if (isLocked) {
                                    onDismiss()
                                    onOpenVipModal()
                                } else {
                                    onSelectQuality(opt.tag)
                                    onDismiss()
                                }
                            }
                            .padding(14.dp)
                            .testTag("quality_option_${opt.bitrate}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = opt.title,
                                        color = if (isSelected) NeonCyan else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = opt.bitrate,
                                        color = if (opt.isVipOnly) GoldPrimary else NeonCyan,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                    if (opt.isVipOnly) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(GoldPrimary.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "VIP",
                                                color = GoldPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = opt.description,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else if (isLocked) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "VIP Locked",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("close_quality_dialog_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGlassBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
