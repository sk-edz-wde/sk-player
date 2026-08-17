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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.audio.EqualizerBand
import com.example.audio.EqualizerPreset
import com.example.ui.theme.CyberGlassBorder
import com.example.ui.theme.CyberSurfaceCard
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonFuchsia
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import androidx.compose.ui.graphics.SolidColor
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VipGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    isEqualizerEnabled: Boolean,
    isGoldenBassEnabled: Boolean,
    bassLevel: Float = 80f,
    is8dAudioEnabled: Boolean,
    isSaveSettingsChecked: Boolean,
    bandGains: List<EqualizerBand>,
    selectedPreset: String,
    playbackSpeed: Float,
    isProActive: Boolean = false,
    onDismiss: () -> Unit,
    onToggleEqualizer: (Boolean) -> Unit,
    onToggleGoldenBass: (Boolean) -> Unit,
    onBassLevelChange: (Float) -> Unit = {},
    onToggle8dAudio: (Boolean) -> Unit,
    onToggleSaveSettings: (Boolean) -> Unit,
    onBandChange: (Int, Float) -> Unit,
    onPresetSelect: (String) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onOpenVipModal: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CyberSurfaceDark,
        tonalElevation = 10.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(scrollState)
        ) {
            // Header with Master EQ Toggle & Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEqualizerEnabled) Brush.linearGradient(listOf(NeonCyan, NeonIndigo))
                                else SolidColor(Color(0xFF1E293B))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = if (isEqualizerEnabled) Color(0xFF04050A) else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "DSP EQUALIZER",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (isEqualizerEnabled) NeonCyan else TextPrimary
                        )
                        Text(
                            text = if (isEqualizerEnabled) "Hardware Engine Active" else "Engine OFF (Flat Audio)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isEqualizerEnabled) NeonCyan else TextMuted
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = isEqualizerEnabled,
                        onCheckedChange = onToggleEqualizer,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = Color(0x5506B6D4),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = Color(0x221E293B)
                        ),
                        modifier = Modifier.testTag("master_eq_switch")
                    )

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Save Settings / Keep on App Restart Tick Box Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSaveSettingsChecked) Color(0xFF0F2327)
                        else CyberSurfaceCard
                    )
                    .border(
                        1.dp,
                        if (isSaveSettingsChecked) NeonCyan.copy(alpha = 0.5f) else CyberGlassBorder,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onToggleSaveSettings(!isSaveSettingsChecked) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = isSaveSettingsChecked,
                            onCheckedChange = onToggleSaveSettings,
                            colors = CheckboxDefaults.colors(
                                checkedColor = NeonCyan,
                                checkmarkColor = Color(0xFF04050A),
                                uncheckedColor = TextMuted
                            ),
                            modifier = Modifier.size(24.dp).testTag("save_settings_checkbox")
                        )
                        Column {
                            Text(
                                text = "Save Settings (Keep across app restarts)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSaveSettingsChecked) NeonCyan else TextPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (isSaveSettingsChecked) "Saved! Audio settings will stay the same when you exit and return"
                                else "Default: OFF when app restarts. Tick to keep your settings",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // VIP Super Golden Bass Booster Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (isGoldenBassEnabled && isProActive) Brush.verticalGradient(
                            listOf(Color(0xFF2E1C00), Color(0xFF191000), CyberSurfaceCard)
                        )
                        else SolidColor(CyberSurfaceCard)
                    )
                    .border(
                        1.dp,
                        if (isGoldenBassEnabled && isProActive) VipGold else CyberGlassBorder,
                        RoundedCornerShape(18.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Header row with Icon, Title, and Switch/Unlock
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isGoldenBassEnabled) VipGold.copy(alpha = 0.2f) else Color(0x221E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Waves, contentDescription = null, tint = VipGold, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "GOLDEN BASS BOOSTER",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                        color = if (isGoldenBassEnabled && isProActive) VipGold else TextPrimary,
                                        fontSize = 14.sp
                                    )
                                    if (!isProActive) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(VipGold.copy(alpha = 0.2f))
                                                .clickable { onOpenVipModal() }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("🔒 VIP", color = VipGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(
                                    text = if (isGoldenBassEnabled && isProActive) "${bassLevel.toInt()}% Deep Subwoofer Bass"
                                    else "Deep Subwoofer Bass Boost",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isGoldenBassEnabled && isProActive) VipGold else TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (isProActive) {
                            Switch(
                                checked = isGoldenBassEnabled,
                                onCheckedChange = { onToggleGoldenBass(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = VipGold,
                                    checkedTrackColor = Color(0x55F59E0B),
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = Color(0x221E293B)
                                ),
                                modifier = Modifier.testTag("golden_bass_switch")
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(VipGold.copy(alpha = 0.15f))
                                    .clickable { onOpenVipModal() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Unlock", color = VipGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Level Slider & Quick Bass Presets (When Enabled & Pro)
                    if (isGoldenBassEnabled && isProActive) {
                        HorizontalDivider(color = VipGold.copy(alpha = 0.2f), thickness = 1.dp)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DEEP BASS INTENSITY",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = VipGold,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "${bassLevel.toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                    color = VipGold,
                                    fontSize = 13.sp
                                )
                            }

                            Slider(
                                value = bassLevel,
                                onValueChange = { onBassLevelChange(it) },
                                valueRange = 0f..100f,
                                steps = 20,
                                colors = SliderDefaults.colors(
                                    thumbColor = VipGold,
                                    activeTrackColor = VipGold,
                                    inactiveTrackColor = Color(0x442A1C00)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bass_level_slider")
                            )

                            // Quick Bass Selection Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val bassPresets = listOf(
                                    35f to "Warm",
                                    65f to "Deep",
                                    85f to "Heavy",
                                    100f to "Max Sub"
                                )
                                bassPresets.forEach { (presetVal, label) ->
                                    val isSelected = (bassLevel - presetVal).let { it >= -5f && it <= 5f }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) VipGold
                                                else Color(0x221E293B)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) VipGold else CyberGlassBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { onBassLevelChange(presetVal) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFF04050A) else TextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "✨ Anti-muffle acoustic clarity keeps vocals & background instruments crisp and clear.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 8D Spatial Surround Audio Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (is8dAudioEnabled && isProActive) Brush.verticalGradient(
                            listOf(Color(0xFF062226), Color(0xFF0B141E), CyberSurfaceCard)
                        )
                        else SolidColor(CyberSurfaceCard)
                    )
                    .border(
                        1.dp,
                        if (is8dAudioEnabled && isProActive) NeonCyan else CyberGlassBorder,
                        RoundedCornerShape(18.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (is8dAudioEnabled) NeonCyan.copy(alpha = 0.2f) else Color(0x221E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SurroundSound, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "8D SPATIAL SURROUND",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                    color = if (is8dAudioEnabled && isProActive) NeonCyan else TextPrimary,
                                    fontSize = 14.sp
                                )
                                if (!isProActive) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(VipGold.copy(alpha = 0.2f))
                                            .clickable { onOpenVipModal() }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("🔒 VIP", color = VipGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(
                                text = if (is8dAudioEnabled && isProActive) "360° Studio Sound Stage Active"
                                else "Dynamic binaural panning",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (is8dAudioEnabled && isProActive) NeonCyan else TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (isProActive) {
                        Switch(
                            checked = is8dAudioEnabled,
                            onCheckedChange = { onToggle8dAudio(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = Color(0x5506B6D4),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = Color(0x221E293B)
                            ),
                            modifier = Modifier.testTag("eight_d_switch")
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonCyan.copy(alpha = 0.15f))
                            .clickable { onOpenVipModal() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Unlock", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Presets Horizontal Row
            Text(
                text = "EQUALIZER PRESETS",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(EqualizerPreset.AllPresets) { preset ->
                    val isSelected = selectedPreset == preset.name
                    NeonChip(
                        text = preset.name,
                        isSelected = isSelected,
                        onClick = {
                            if (!isEqualizerEnabled) {
                                onToggleEqualizer(true)
                            }
                            onPresetSelect(preset.name)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5 Band Sliders Layout (Vertical Gain Bars)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberSurfaceCard)
                    .border(1.dp, CyberGlassBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "+12 dB", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(text = "0 dB", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(text = "-12 dB", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bandGains.forEachIndexed { index, band ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = String.format("%+.0f dB", band.gainDb),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isEqualizerEnabled) TextMuted
                                    else if (band.gainDb > 0) NeonCyan
                                    else if (band.gainDb < 0) NeonFuchsia
                                    else TextSecondary
                                )

                                Slider(
                                    value = band.gainDb,
                                    onValueChange = {
                                        if (!isEqualizerEnabled) {
                                            onToggleEqualizer(true)
                                        }
                                        onBandChange(index, it)
                                    },
                                    valueRange = -12f..12f,
                                    steps = 24,
                                    colors = SliderDefaults.colors(
                                        thumbColor = if (isEqualizerEnabled) NeonCyan else TextMuted,
                                        activeTrackColor = if (isEqualizerEnabled) NeonIndigo else Color(0x331E293B),
                                        inactiveTrackColor = Color(0x331E293B)
                                    ),
                                    modifier = Modifier
                                        .height(140.dp)
                                        .padding(vertical = 4.dp)
                                        .testTag("eq_band_slider_$index")
                                )

                                Text(
                                    text = band.frequencyLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (isEqualizerEnabled) TextPrimary else TextMuted,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Playback Speed Selector
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSurfaceCard)
                    .border(1.dp, CyberGlassBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = VipGold, modifier = Modifier.size(18.dp))
                        Text(
                            text = "PLAYBACK SPEED & TEMPO",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "${playbackSpeed}x",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = VipGold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        speeds.forEach { speed ->
                            val isSelected = playbackSpeed == speed
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) VipGold else Color(0x221E293B))
                                    .clickable { onSpeedChange(speed) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${speed}x",
                                    color = if (isSelected) Color(0xFF04050A) else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
