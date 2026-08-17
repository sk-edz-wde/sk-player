package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberGlassBorder
import com.example.ui.theme.CyberGlassFill
import com.example.ui.theme.CyberSurfaceCard
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonFuchsia
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VipGold
import com.example.ui.theme.VipGoldBright
import com.google.firebase.auth.FirebaseUser

@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    currentUser: FirebaseUser?,
    isAuthLoading: Boolean,
    selectedQuality: String = "320 kbps (Ultra HD)",
    onLogin: (email: String, pass: String) -> Unit,
    onRegister: (email: String, pass: String, name: String) -> Unit,
    onSignOut: () -> Unit,
    onOpenVipModal: () -> Unit,
    onActivateFreeTrial: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenAudioQuality: () -> Unit = {},
    onOpenReportLog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }

    val isLoggedIn = currentUser != null

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(top = 12.dp)
            .testTag("profile_screen"),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Header Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACCOUNT & PROFILE",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = TextPrimary
                )
            }
        }

        if (!isLoggedIn) {
            // Local Device Guest Status Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberGlassFill)
                        .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GUEST MODE (LOCAL DEVICE)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = NeonCyan
                            )
                            Text(
                                text = "Your playlists and liked songs are saved locally on this phone only (like mobile game guest mode).",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Account Login / Register Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(CyberSurfaceCard)
                        .border(1.dp, NeonCyan, RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Text(
                                text = if (isRegisterMode) "CREATE ACCOUNT" else "ACCOUNT SIGN IN",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = NeonCyan
                            )
                        }

                        Text(
                            text = if (isRegisterMode) "Sign up to sync your playlists and VIP status across devices." else "Sign in with your registered account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        if (isRegisterMode) {
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                placeholder = { Text("Display Name (e.g. Cyber Rider)", color = TextMuted, fontSize = 12.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = CyberGlassBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            placeholder = { Text("Email Address", color = TextMuted, fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = CyberGlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            placeholder = { Text("Password (min 6 chars)", color = TextMuted, fontSize = 12.sp) },
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = CyberGlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (isRegisterMode) {
                                    onRegister(emailInput, passwordInput, nameInput)
                                } else {
                                    onLogin(emailInput, passwordInput)
                                }
                            },
                            enabled = !isAuthLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            if (isAuthLoading) {
                                CircularProgressIndicator(color = Color(0xFF04050A), modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = if (isRegisterMode) "CREATE ACCOUNT" else "SIGN IN",
                                    color = Color(0xFF04050A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isRegisterMode) "Already have an account?" else "Don't have an account?",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                                Text(
                                    text = if (isRegisterMode) "Sign In" else "Create Account",
                                    color = NeonFuchsia,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Profile Avatar & Status Card for Logged In User
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(CyberSurfaceCard)
                        .border(
                            1.5.dp,
                            if (userProfile.isProActive) Brush.horizontalGradient(listOf(VipGold, NeonCyan)) else SolidColor(CyberGlassBorder),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(listOf(NeonCyan, NeonIndigo, NeonFuchsia, NeonCyan))
                                )
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(CyberSurfaceDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (userProfile.isProActive) VipGoldBright else NeonCyan,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = userProfile.displayName.ifBlank { currentUser.email?.substringBefore("@") ?: "SK User" },
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            if (userProfile.isProActive) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = VipGoldBright, modifier = Modifier.size(20.dp))
                            }
                        }

                        Text(
                            text = currentUser.email ?: userProfile.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        Text(
                            text = "UID: ${currentUser.uid.take(14)}...",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (userProfile.isProActive) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x33F59E0B))
                                    .border(1.dp, VipGold, RoundedCornerShape(12.dp))
                                    .clickable { onOpenVipModal() }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "👑 VIP PRO ACTIVE (${userProfile.daysRemaining} Days Left)",
                                    color = VipGoldBright,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            if (!userProfile.hasUsedFreeTrial && !currentUser.uid.startsWith("guest_")) {
                                Button(
                                    onClick = onActivateFreeTrial,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    modifier = Modifier.testTag("activate_freetrial_profile_btn").fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF04050A))
                                        Text("ACTIVATE 1-DAY FREE TRIAL", color = Color(0xFF04050A), fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    }
                                }
                            } else if (userProfile.hasUsedFreeTrial) {
                                Text("1-Day Free Trial already used.", color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 6.dp))
                            }

                            Button(
                                onClick = onOpenVipModal,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VipGold),
                                modifier = Modifier.testTag("activate_vip_profile_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF04050A))
                                    Text("ACTIVATE VIP PRO KEY", color = Color(0xFF04050A), fontWeight = FontWeight.Black, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TextButton(onClick = onSignOut) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null, tint = NeonFuchsia, modifier = Modifier.size(16.dp))
                                Text("Sign Out", color = NeonFuchsia, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section: Audio & DSP Engine Tools
        item {
            Text(
                text = "AUDIO ENGINE SETTINGS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberSurfaceCard)
                    .border(1.dp, CyberGlassBorder, RoundedCornerShape(20.dp))
            ) {
                ProfileSettingsItem(
                    icon = Icons.Default.GraphicEq,
                    iconTint = NeonCyan,
                    title = "5-Band DSP Equalizer & Presets",
                    subtitle = "Adjust bass, mid, treble, and surround 8D audio",
                    onClick = onOpenEqualizer
                )

                HorizontalDivider(color = CyberGlassBorder)

                ProfileSettingsItem(
                    icon = Icons.Default.Timer,
                    iconTint = VipGold,
                    title = "Sleep Timer",
                    subtitle = "Set countdown to auto-pause music playback",
                    onClick = onOpenSleepTimer
                )

                HorizontalDivider(color = CyberGlassBorder)

                ProfileSettingsItem(
                    icon = Icons.Default.Headphones,
                    iconTint = NeonFuchsia,
                    title = "Audio Engine Quality",
                    subtitle = "$selectedQuality • DSP Float Precision",
                    onClick = onOpenAudioQuality
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberSurfaceCard)
                    .border(1.dp, CyberGlassBorder, RoundedCornerShape(20.dp))
                    .padding(vertical = 4.dp)
            ) {
                ProfileSettingsItem(
                    icon = Icons.Default.List,
                    iconTint = TextPrimary,
                    title = "Report Log",
                    subtitle = "View status of your submitted reports",
                    onClick = onOpenReportLog
                )
            }
        }

        // App Version Info
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SK Edz Player v3.2.0",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Ultra HD 320k • Spatial 360° Sound Engine",
                        color = TextMuted.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSettingsItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}
