package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsCricket
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.online.attemptAutoSignIn
import com.example.online.onGoogleSignInClicked
import com.example.ui.components.PremiumActionButton
import com.example.ui.components.staggeredEntrance
import com.example.ui.theme.LocalStadiumColors
import com.example.ui.theme.StadiumDisplayFont

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isCheckingAuto by remember { mutableStateOf(true) }
    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            onAuthSuccess = {
                isCheckingAuto = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingAuto = false
            },
        )
    }

    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current

    val infinite = rememberInfiniteTransition(label = "auth_crest_pulse")
    val crestPulse by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "crest_scale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_stadium_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            cs.background.copy(alpha = 0.56f),
                            cs.background.copy(alpha = 0.86f),
                            cs.background.copy(alpha = 0.98f),
                        )
                    )
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Pulsing Floodlit Crest Emblem
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .staggeredEntrance(0),
            ) {
                Box(
                    modifier = Modifier
                        .size(94.dp)
                        .graphicsLayer {
                            scaleX = crestPulse
                            scaleY = crestPulse
                        }
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(stadium.sixGold.copy(alpha = 0.32f), Color.Transparent)
                            )
                        ),
                )
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(stadium.sixGoldContainer, cs.background)
                            )
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.verticalGradient(listOf(stadium.sixGoldBright, stadium.sixGoldDeep)),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SportsCricket,
                        contentDescription = null,
                        tint = stadium.sixGoldBright,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.staggeredEntrance(1),
            ) {
                Text(
                    text = "ODD-EVEN CRICKET",
                    fontFamily = StadiumDisplayFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 46.sp,
                    lineHeight = 46.sp,
                    letterSpacing = 1.sp,
                    color = cs.onBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "SOLO • ONLINE ROOMS • TEAM DRAFT • FRIENDS",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + slideInVertically { it / 2 },
            ) {
                errorMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = cs.errorContainer,
                        border = BorderStroke(1.dp, cs.error),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onErrorContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }

            PremiumActionButton(
                text = when {
                    isCheckingAuto -> "CHECKING SESSION…"
                    isSigningIn -> "SIGNING IN…"
                    else -> "SIGN IN WITH GOOGLE"
                },
                onClick = {
                    errorMessage = null
                    isSigningIn = true
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        scope = scope,
                        onAuthSuccess = {
                            isSigningIn = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isSigningIn = false
                            errorMessage = msg
                        },
                        onAuthCancelled = {
                            isSigningIn = false
                        },
                    )
                },
                enabled = !isSigningIn && !isCheckingAuto,
                loading = isSigningIn || isCheckingAuto,
                height = 58.dp,
                modifier = Modifier
                    .staggeredEntrance(2)
                    .testTag("google_sign_in_button"),
            )
        }
    }
}
