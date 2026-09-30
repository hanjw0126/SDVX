package com.voltexmate.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltexmate.app.R
import kotlinx.coroutines.delay

/** 앱 아이콘이 말랑하게 튀어나오는 2.5초 로딩 화면 */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val done by rememberUpdatedState(onDone)
    val pop = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 260f)) }
    LaunchedEffect(Unit) {
        delay(2500)
        done()
    }
    val t = rememberInfiniteTransition(label = "breath")
    val breath by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b",
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Mochi.Lavender, Mochi.Pink))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(156.dp)
                    .graphicsLayer {
                        val s = pop.value
                        scaleX = s * (1f + 0.06f * breath)
                        scaleY = s * (1f - 0.05f * breath)
                    }
                    .clip(RoundedCornerShape(48.dp))
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(156.dp))
            }
            Spacer(Modifier.height(22.dp))
            Text("VOLTEX MATE", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Text("for SOUND VOLTEX players", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
            Spacer(Modifier.height(28.dp))
            MochiDots(colors = listOf(Color.White, Color.White.copy(alpha = 0.8f), Color.White.copy(alpha = 0.6f)))
        }
    }
}
