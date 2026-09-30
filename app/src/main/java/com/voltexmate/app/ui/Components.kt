package com.voltexmate.app.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.voltexmate.app.data.Assets
import com.voltexmate.app.data.Difficulty
import com.voltexmate.app.data.VfInfo
import java.io.File
import java.util.Locale

/** 누르면 말랑하게 눌리는 클릭 */
fun Modifier.mochiClick(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.93f else 1f,
        spring(dampingRatio = 0.42f, stiffness = 520f),
        label = "mochi",
    )
    val haptic = LocalHapticFeedback.current
    Modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = source, indication = null, enabled = enabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}

@Composable
fun MochiCard(modifier: Modifier = Modifier, padding: Dp = 18.dp, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, ambientColor = Mochi.Lavender.copy(alpha = 0.25f), spotColor = Mochi.Lavender.copy(alpha = 0.25f))
            .clip(shape)
            .background(Color.White)
            .padding(padding),
        content = content,
    )
}

@Composable
fun MochiPills(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Mochi.Lavender.copy(alpha = 0.10f))
            .padding(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            val bg by animateColorAsState(if (on) Color.White else Color.Transparent, label = "pillBg")
            val fg by animateColorAsState(if (on) Mochi.Ink else Mochi.Sub, label = "pillFg")
            Box(
                Modifier
                    .weight(1f)
                    .mochiClick { onSelect(i) }
                    .clip(RoundedCornerShape(50))
                    .background(bg)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = fg, fontSize = 13.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
            }
        }
    }
}

@Composable
fun MochiButton(text: String, modifier: Modifier = Modifier, light: Boolean = false, onClick: () -> Unit) {
    val brush = if (light) {
        Brush.linearGradient(listOf(Mochi.Lavender.copy(alpha = 0.14f), Mochi.Pink.copy(alpha = 0.14f)))
    } else {
        Brush.linearGradient(listOf(Mochi.Lavender, Mochi.Pink))
    }
    Box(
        modifier
            .mochiClick(onClick = onClick)
            .clip(RoundedCornerShape(50))
            .background(brush)
            .padding(horizontal = 22.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (light) Mochi.Ink else Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun MochiDots(modifier: Modifier = Modifier, colors: List<Color> = listOf(Mochi.Lavender, Mochi.Pink, Mochi.Sky)) {
    val t = rememberInfiniteTransition(label = "dots")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEachIndexed { i, c ->
            val y by t.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes<Float> {
                        durationMillis = 900
                        0f at 0
                        1f at 250
                        0f at 500
                        0f at 900
                    },
                    initialStartOffset = StartOffset(i * 150),
                ),
                label = "dot$i",
            )
            Box(
                Modifier
                    .size(12.dp)
                    .graphicsLayer { translationY = -10f * y * density }
                    .clip(CircleShape)
                    .background(c),
            )
        }
    }
}

@Composable
fun MochiProgress(fraction: Float, modifier: Modifier = Modifier) {
    var target by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(fraction) { target = fraction }
    val f by animateFloatAsState(target, tween(1200, easing = FastOutSlowInEasing), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(50))
            .background(Mochi.Lavender.copy(alpha = 0.14f)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(f.coerceIn(0.04f, 1f))
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(Mochi.Lavender, Mochi.Pink))),
        )
    }
}

@Composable
fun OfficialImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    fallback: @Composable () -> Unit = {},
) {
    if (url.isNullOrBlank()) {
        Box(modifier, contentAlignment = Alignment.Center) { fallback() }
        return
    }
    val ctx = LocalContext.current
    val request = remember(url) { ImageRequest.Builder(ctx).data(url).crossfade(true).build() }
    SubcomposeAsyncImage(
        model = request,
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale,
        loading = {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(Mochi.Lavender.copy(alpha = 0.08f)))
        },
        error = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { fallback() }
        },
    )
}

/** 공식 VF 클래스 이미지 + 단계 별 이미지. 불러오지 못하면 직접 그린 메달 */
@Composable
fun OfficialVfBadge(vf: VfInfo, modifier: Modifier = Modifier, size: Dp = 120.dp) {
    val t = rememberInfiniteTransition(label = "vfFloat")
    val fl by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float",
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        OfficialImage(
            Assets.vfIcon(vf.cls.id),
            Modifier
                .size(size)
                .graphicsLayer {
                    translationY = -5f * fl * density
                    rotationZ = (fl - 0.5f) * 4f
                },
        ) { VfFallbackMedal(vf, size) }
        Spacer(Modifier.height(4.dp))
        OfficialImage(Assets.vfStars(vf.tier), Modifier.width(size * 0.8f).height(size * 0.16f)) {
            Text("★".repeat(vf.tier), color = Color(0xFFFFC94D), fontSize = (size.value * 0.11f).sp)
        }
    }
}

@Composable
private fun VfFallbackMedal(vf: VfInfo, size: Dp) {
    Box(
        Modifier
            .size(size * 0.9f)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(vf.cls.c1), Color(vf.cls.c2)))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(vf.cls.label, color = Color.White, fontWeight = FontWeight.Black, fontSize = (size.value * 0.11f).sp)
            Text(vf.roman, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.16f).sp)
        }
    }
}

@Composable
fun SkillPlate(code: String?, name: String?, modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(227f / 72f), contentAlignment = Alignment.Center) {
        OfficialImage(Assets.skill(code ?: "none"), Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(Mochi.Lavender.copy(alpha = 0.12f)))
        }
        if (!name.isNullOrBlank()) {
            Text(name, Modifier.padding(start = 28.dp), fontWeight = FontWeight.Black, fontSize = 18.sp, color = Mochi.Ink)
        }
    }
}

@Composable
fun ArenaIcon(code: String, modifier: Modifier = Modifier) {
    OfficialImage(Assets.arena(code), modifier) { Text("ARENA", color = Mochi.Sub, fontSize = 10.sp) }
}

@Composable
fun StatTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label, fontSize = 11.sp, color = accent.darker(), fontWeight = FontWeight.Bold, maxLines = 1)
        Text(value, fontSize = 18.sp, color = Mochi.Ink, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = color.darker(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
    )
}

@Composable
fun TimeBubble(time: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Mochi.Lavender, Mochi.Pink)))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(time, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, color = Mochi.Sub, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
}

@Composable
fun IconLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, null, tint = Mochi.Lavender, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = Mochi.Ink, fontSize = 13.sp)
    }
}

@Composable
fun InfoNote(text: String) {
    Text(
        "💡 $text",
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Mochi.Sky.copy(alpha = 0.12f))
            .padding(12.dp),
        color = Mochi.Ink,
        fontSize = 12.sp,
    )
}

@Composable
fun DemoTag() {
    Spacer(Modifier.width(8.dp))
    Pill("DEMO", Mochi.Pink)
}

@Composable
fun ScreenTitle(text: String) {
    Text(text, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Mochi.Ink, letterSpacing = 2.sp)
}

@Composable
fun CenterLoading(text: String) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MochiDots()
        Spacer(Modifier.height(14.dp))
        Text(text, color = Mochi.Sub)
    }
}

@Composable
fun EmptyView(emoji: String, title: String, desc: String) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 44.sp)
        Spacer(Modifier.height(8.dp))
        Text(title, color = Mochi.Ink, fontWeight = FontWeight.Black, fontSize = 17.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(desc, color = Mochi.Sub, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun FailView(message: String, sessionExpired: Boolean, onRetry: () -> Unit, onRelogin: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(if (sessionExpired) "🔑" else "🥲", fontSize = 44.sp)
        Spacer(Modifier.height(10.dp))
        Text(message, color = Mochi.Ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        MochiButton(if (sessionExpired) "다시 로그인" else "다시 시도") {
            if (sessionExpired) onRelogin() else onRetry()
        }
    }
}

fun columnLabel(key: String): String = when {
    key == "total" -> "합계"
    key.startsWith("mark_") -> when (key.removePrefix("mark_")) {
        "play" -> "PLAY"
        "comp" -> "COMP"
        "comp_ex" -> "EX"
        "comp_max" -> "MXV"
        "uc" -> "UC"
        "per" -> "PUC"
        else -> key
    }
    key.startsWith("grade_") -> key.removePrefix("grade_").uppercase(Locale.ROOT).replace("_PLUS", "+")
    else -> key
}

fun columnColor(key: String): Color = when (key.removePrefix("mark_")) {
    "play" -> Color(0xFF9A96B5)
    "comp" -> Color(0xFF3CB6A0)
    "comp_ex" -> Color(0xFFFF6F9C)
    "comp_max" -> Color(0xFF9B6BFF)
    "uc" -> Color(0xFFFF9D2E)
    "per" -> Color(0xFFE6B800)
    "total" -> Mochi.Ink
    else -> Mochi.Lavender
}

fun diffColor(label: String): Color {
    val d = Difficulty.parse(label) ?: return Mochi.Ink
    return Color(d.color)
}

/** 파서 디버그용 HTML 공유 */
object HtmlShare {
    fun share(context: Context, name: String, html: String) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val f = File(dir, "$name.html").apply { writeText(html) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/html")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, "HTML 공유").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
