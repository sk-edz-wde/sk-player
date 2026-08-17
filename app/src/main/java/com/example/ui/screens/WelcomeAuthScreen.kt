package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.VipGoldBright
import kotlinx.coroutines.launch

enum class WelcomeScreenState {
    ONBOARDING,
    LOGIN
}

private data class OnboardingSlide(
    val tag: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val badges: List<String>
)

@Composable
fun WelcomeAuthScreen(
    isAuthLoading: Boolean,
    onLogin: (email: String, pass: String) -> Unit,
    onRegister: (email: String, pass: String, name: String) -> Unit,
    onQuickDemoLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var screenState by remember { mutableStateOf(WelcomeScreenState.ONBOARDING) }

    val slides = remember {
        listOf(
            OnboardingSlide(
                tag = "ULTRA HD STREAMING",
                title = "Lossless Audio.",
                subtitle = "Zero Compromise.",
                description = "Stream vast libraries of Tamil, Malayalam, Anirudh hits, Melody and chartbuster tracks with studio-master 320 kbps quality.",
                icon = Icons.Default.Headphones,
                accentColor = NeonCyan,
                badges = listOf("Tamil & Malayalam Hits", "320 kbps Master Audio", "Zero Ad Interruptions")
            ),
            OnboardingSlide(
                tag = "AUDIO DSP ENGINE",
                title = "8D Spatial &",
                subtitle = "100% Golden Bass.",
                description = "Real-time hardware sound engine with interactive 10-band equalizer, 8D binaural spatial surround, and dynamic live audio visualizer.",
                icon = Icons.Default.GraphicEq,
                accentColor = NeonFuchsia,
                badges = listOf("8D Spatial Surround", "100% Golden Bass Booster", "Live 10-Band EQ")
            ),
            OnboardingSlide(
                tag = "OFFLINE & REAL-TIME SYNC",
                title = "Every Track,",
                subtitle = "Everywhere.",
                description = "Download high-res songs directly to local storage. Real-time playlist and favorite synchronization across all your devices.",
                icon = Icons.Default.CloudDone,
                accentColor = VipGoldBright,
                badges = listOf("Offline High-Res Storage", "Real-Time Cloud Sync", "Swipe-Down Full Player")
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Crossfade(targetState = screenState, label = "welcome_crossfade") { state ->
            when (state) {
                WelcomeScreenState.ONBOARDING -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Header Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
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
                                Text(
                                    text = "SK PLAYER",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    ),
                                    color = TextPrimary
                                )
                            }

                            TextButton(
                                onClick = { screenState = WelcomeScreenState.LOGIN },
                                modifier = Modifier.testTag("skip_onboarding_button")
                            ) {
                                Text(
                                    text = "Skip",
                                    color = TextMuted,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }

                        // Onboarding Content Pager
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 12.dp)
                        ) { page ->
                            val slide = slides[page]
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Glowing Feature Icon Circle
                                Box(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    slide.accentColor.copy(alpha = 0.25f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                        .border(2.dp, slide.accentColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = slide.icon,
                                        contentDescription = null,
                                        tint = slide.accentColor,
                                        modifier = Modifier.size(68.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(28.dp))

                                // Slide Tag
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(slide.accentColor.copy(alpha = 0.15f))
                                        .border(1.dp, slide.accentColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = slide.tag,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = slide.accentColor
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Slide Headline
                                Text(
                                    text = slide.title,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 28.sp,
                                        lineHeight = 34.sp
                                    ),
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = slide.subtitle,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 28.sp,
                                        lineHeight = 34.sp
                                    ),
                                    color = slide.accentColor,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Slide Description
                                Text(
                                    text = slide.description,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // Feature Badges Row
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    slide.badges.forEach { badge ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(CyberSurfaceDark)
                                                .border(1.dp, CyberGlassBorder, RoundedCornerShape(12.dp))
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(slide.accentColor)
                                            )
                                            Text(
                                                text = badge,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Navigation & Next / Get Started Action
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Dot Page Indicators
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(slides.size) { index ->
                                    val isSelected = pagerState.currentPage == index
                                    val slideColor = slides[pagerState.currentPage].accentColor
                                    Box(
                                        modifier = Modifier
                                            .height(8.dp)
                                            .width(if (isSelected) 28.dp else 8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) slideColor else TextMuted.copy(alpha = 0.3f)
                                            )
                                    )
                                }
                            }

                            // Next / Get Started Button
                            val isLastPage = pagerState.currentPage == slides.size - 1
                            Button(
                                onClick = {
                                    if (isLastPage) {
                                        screenState = WelcomeScreenState.LOGIN
                                    } else {
                                        scope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("onboarding_next_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(NeonCyan, NeonFuchsia)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = if (isLastPage) "GET STARTED & LOGIN" else "NEXT",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            ),
                                            color = Color(0xFF04050A)
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = Color(0xFF04050A),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                WelcomeScreenState.LOGIN -> {
                    MandatoryLoginContent(
                        isAuthLoading = isAuthLoading,
                        onLogin = onLogin,
                        onRegister = onRegister,
                        onQuickDemoLogin = onQuickDemoLogin,
                        onBackToOnboarding = { screenState = WelcomeScreenState.ONBOARDING }
                    )
                }
            }
        }
    }
}

@Composable
private fun MandatoryLoginContent(
    isAuthLoading: Boolean,
    onLogin: (email: String, pass: String) -> Unit,
    onRegister: (email: String, pass: String, name: String) -> Unit,
    onQuickDemoLogin: () -> Unit,
    onBackToOnboarding: () -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back to features link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(
                    onClick = onBackToOnboarding,
                    modifier = Modifier.testTag("back_to_features_button")
                ) {
                    Text(
                        text = "← Back to Features",
                        color = NeonCyan,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            // App Brand Logo
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        1.5.dp,
                        Brush.linearGradient(listOf(NeonCyan, NeonFuchsia)),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.app_logo),
                    contentDescription = "SK Player Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SK PLAYER",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = TextPrimary
            )

            Text(
                text = if (isRegisterMode) "Create an account to unlock lossless streaming" else "Sign in to access your music & playlists",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Mode Selector Pill (Sign In / Register)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberSurfaceDark)
                    .border(1.dp, CyberGlassBorder, RoundedCornerShape(14.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!isRegisterMode) NeonCyan else Color.Transparent)
                        .clickable { isRegisterMode = false }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SIGN IN",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (!isRegisterMode) Color(0xFF04050A) else TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isRegisterMode) NeonFuchsia else Color.Transparent)
                        .clickable { isRegisterMode = true }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CREATE ACCOUNT",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isRegisterMode) Color.White else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Input Form
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isRegisterMode) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Display Name", color = TextSecondary) },
                        placeholder = { Text("e.g. Cyber Rider", color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = NeonCyan)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberGlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_display_name")
                    )
                }

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Email Address", color = TextSecondary) },
                    placeholder = { Text("your.email@example.com", color = TextMuted) },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = NeonCyan)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberGlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_email")
                )

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Password", color = TextSecondary) },
                    placeholder = { Text("Min 6 characters", color = TextMuted) },
                    leadingIcon = {
                        Icon(Icons.Default.Security, contentDescription = null, tint = NeonCyan)
                    },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                tint = TextSecondary
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberGlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_password")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Primary Submit Button
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
                        .height(52.dp)
                        .testTag("auth_submit_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRegisterMode) NeonFuchsia else NeonCyan
                    )
                ) {
                    if (isAuthLoading) {
                        CircularProgressIndicator(
                            color = Color(0xFF04050A),
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = if (isRegisterMode) "CREATE ACCOUNT & ENTER" else "SIGN IN & ENTER",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                            color = if (isRegisterMode) Color.White else Color(0xFF04050A)
                        )
                    }
                }

                // Divider OR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = CyberGlassBorder)
                    Text(
                        text = "OR QUICK ACCESS",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = CyberGlassBorder)
                }

                // Fast Instant Demo Login Button
                Button(
                    onClick = onQuickDemoLogin,
                    enabled = !isAuthLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(1.dp, NeonIndigo.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .testTag("quick_demo_login_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceDark)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = VipGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "One-Tap Instant Account Access",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Security Footnote
        Text(
            text = "🔒 Secured with Firebase Authentication. Your library, playlists, and audio settings are encrypted and backed up.",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
