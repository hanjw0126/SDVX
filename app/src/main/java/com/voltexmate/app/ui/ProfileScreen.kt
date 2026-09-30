package com.voltexmate.app.ui

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.voltexmate.app.data.ClearTable
import com.voltexmate.app.data.MatchingEntry
import com.voltexmate.app.data.ProfileDetail
import com.voltexmate.app.data.UiLoad
import com.voltexmate.app.data.Urls
import com.voltexmate.app.data.VfInfo
import java.io.File
import java.util.Locale

@Composable
fun ProfileScreen(
    vm: MainViewModel,
    onPickAvatar: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenWeb: (String, String) -> Unit,
) {
    val state by vm.profile.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val avatar by vm.avatar.collectAsState()
    when (val s = state) {
        is UiLoad.Loading -> CenterLoading("공식 사이트에서 프로필을 불러오는 중…")
        is UiLoad.Fail -> FailView(s.message, s.sessionExpired, onRetry = { vm.refreshProfile() }, onRelogin = { vm.sessionExpired() })
        is UiLoad.Ok -> ProfileContent(s.data, refreshing, avatar, vm, onPickAvatar, onOpenRecent, onOpenWeb)
    }
}

@Composable
private fun ProfileContent(
    p: ProfileDetail,
    refreshing: Boolean,
    avatar: File?,
    vm: MainViewModel,
    onPickAvatar: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenWeb: (String, String) -> Unit,
) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var tableIdx by rememberSaveable { mutableIntStateOf(0) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScreenTitle("PROFILE")
                if (vm.isDemo) DemoTag()
                Spacer(Modifier.weight(1f))
                if (refreshing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Mochi.Lavender)
                IconButton(onClick = { vm.refreshProfile() }) { Icon(Icons.Rounded.Refresh, "새로고침", tint = Mochi.Ink) }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "메뉴", tint = Mochi.Ink) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("프로필 사진 변경") }, onClick = { menu = false; onPickAvatar() })
                        if (avatar != null) {
                            DropdownMenuItem(text = { Text("어필카드로 되돌리기") }, onClick = { menu = false; vm.clearAvatar() })
                        }
                        DropdownMenuItem(text = { Text("공식 프로필 페이지 열기") }, onClick = { menu = false; onOpenWeb(Urls.PROFILE, "공식 프로필") })
                        DropdownMenuItem(text = { Text("프로필 HTML 공유 (파서 디버그)") }, onClick = {
                            menu = false
                            val h = vm.htmlFor("profile")
                            if (h != null) HtmlShare.share(ctx, "sdvx_profile", h)
                            else Toast.makeText(ctx, "먼저 새로고침해 주세요", Toast.LENGTH_SHORT).show()
                        })
                        DropdownMenuItem(text = { Text("로그아웃") }, onClick = { menu = false; vm.logout() })
                    }
                }
            }
        }
        item { HeroCard(p, avatar, onPickAvatar) }
        item { VfCard(p.vf) }
        item { StatsCard(p) }
        if (p.clearTables.isNotEmpty()) {
            item { ClearTableCard(p.clearTables, tableIdx) { tableIdx = it } }
        }
        if (p.matching.isNotEmpty()) {
            item { MatchPreview(p.matching.take(3), onOpenRecent) }
        }
    }
}

@Composable
private fun HeroCard(p: ProfileDetail, avatar: File?, onPickAvatar: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(Mochi.Lavender, Mochi.Pink)))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .mochiClick(onClick = onPickAvatar)
                    .size(width = 94.dp, height = 120.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                if (avatar != null) {
                    AsyncImage(model = avatar, contentDescription = "프로필 사진", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    OfficialImage(p.apcardUrl, Modifier.fillMaxSize(), ContentScale.Crop) {
                        Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(44.dp))
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                p.appealTitle?.let {
                    Text(
                        it,
                        color = Mochi.Ink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.85f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(p.playerName, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    p.playerId,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.mochiClick {
                        clipboard.setText(AnnotatedString(p.playerId))
                        Toast.makeText(ctx, "SV-ID를 복사했어요", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }
}

@Composable
private fun VfCard(vf: VfInfo) {
    var target by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(vf.value) { target = vf.value.toFloat() }
    val shown by animateFloatAsState(target, tween(1400, easing = FastOutSlowInEasing), label = "vfCount")

    MochiCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OfficialVfBadge(vf, size = 112.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                SectionTitle("VOLFORCE")
                Text(String.format(Locale.US, "%.3f", shown), color = Mochi.Ink, fontSize = 36.sp, fontWeight = FontWeight.Black)
                Text("${vf.cls.label} ${vf.roman}", color = Color(vf.cls.c2), fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                val next = vf.nextAt
                if (next != null) {
                    MochiProgress(vf.progress)
                    val nv = VfInfo.of(next, null, null)
                    Text(
                        "${nv.cls.label} ${nv.roman}까지 +${String.format(Locale.US, "%.3f", next - vf.value)}",
                        color = Mochi.Sub,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                } else {
                    Text("최고 단계예요 👑", color = Mochi.Sub, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun fmt(n: Int): String = String.format(Locale.US, "%,d", n)

@Composable
private fun StatsCard(p: ProfileDetail) {
    MochiCard {
        SectionTitle("PLAY STATUS")
        val streak = listOfNotNull(p.streakDays?.let { "${it}일" }, p.streakWeeks?.let { "${it}주" }).joinToString(" · ")
        val tiles: List<Triple<String, String, Color>> = listOfNotNull(
            p.playCount?.let { Triple("플레이 횟수", "${fmt(it)}회", Mochi.Lavender) },
            if (streak.isNotEmpty()) Triple("연속 플레이", streak, Mochi.Pink) else null,
            p.pc?.let { Triple("보유 PC", fmt(it), Mochi.Sky) },
            p.blc?.let { Triple("보유 BLC", fmt(it), Mochi.Mint) },
            p.arenaPower?.let { Triple("ARENA POWER", fmt(it), Mochi.Pink) },
            p.blasterPass?.let { Triple("BLASTER PASS", if (it) "활성" else "비활성", Mochi.Lavender) },
        )
        tiles.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { tile -> StatTile(tile.first, tile.second, tile.third, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SkillPlate(p.skillCode, p.skillName, Modifier.weight(1f))
            val arena = p.arenaCode
            if (arena != null) {
                Spacer(Modifier.width(10.dp))
                ArenaIcon(arena, Modifier.size(72.dp))
            }
        }
        if (p.lastPlayedAt != null || p.lastShop != null) {
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Mochi.Bg)
                    .padding(12.dp),
            ) {
                p.lastPlayedAt?.let { IconLine(Icons.Rounded.DateRange, "최종 플레이  $it") }
                p.lastShop?.let { IconLine(Icons.Rounded.LocationOn, it) }
            }
        }
    }
}

@Composable
private fun ClearTableCard(tables: List<ClearTable>, selected: Int, onSelect: (Int) -> Unit) {
    val t = tables[selected.coerceIn(0, tables.lastIndex)]
    var hideZero by rememberSaveable { mutableStateOf(true) }
    MochiCard {
        SectionTitle("CLEAR · GRADE")
        Spacer(Modifier.height(10.dp))
        MochiPills(tables.map { it.title }, selected, onSelect)
        Spacer(Modifier.height(12.dp))
        val rows = if (hideZero) t.rows.filter { it.isTotal || it.values.any { v -> v > 0 } } else t.rows
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            Row {
                TableCell("", 72.dp, header = true)
                t.columns.forEach { TableCell(columnLabel(it), 50.dp, header = true, color = columnColor(it)) }
            }
            rows.forEach { r ->
                Row(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (r.isTotal) Mochi.Lavender.copy(alpha = 0.12f) else Color.Transparent),
                ) {
                    TableCell(r.label, 72.dp, header = true, color = diffColor(r.label))
                    r.values.forEach { v -> TableCell(if (v == 0) "·" else v.toString(), 50.dp, bold = r.isTotal) }
                }
            }
        }
        if (t.rows.size > 8) {
            Text(
                if (hideZero) "0인 행도 보기" else "0인 행 숨기기",
                color = Mochi.Lavender,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp).mochiClick { hideZero = !hideZero },
            )
        }
    }
}

@Composable
private fun TableCell(text: String, width: Dp, header: Boolean = false, bold: Boolean = false, color: Color = Mochi.Ink) {
    Box(Modifier.width(width).height(34.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            color = color,
            fontSize = if (header) 11.sp else 13.sp,
            maxLines = 1,
            fontWeight = if (header || bold) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun MatchPreview(items: List<MatchingEntry>, onOpenRecent: () -> Unit) {
    MochiCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("RECENT PLAY", Modifier.weight(1f))
            Text(
                "전체 보기 ›",
                color = Mochi.Lavender,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.mochiClick(onClick = onOpenRecent),
            )
        }
        items.forEach { m ->
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                TimeBubble(m.playedAt.takeLast(8).take(5))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(m.title, fontWeight = FontWeight.Bold, color = Mochi.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(m.artist, fontSize = 12.sp, color = Mochi.Sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
