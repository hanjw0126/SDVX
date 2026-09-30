package com.voltexmate.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltexmate.app.data.BoardEntry
import com.voltexmate.app.data.RankingBoard
import com.voltexmate.app.data.RankingKind
import com.voltexmate.app.data.UiLoad
import java.util.Locale

/** 공식 RANKING 메뉴: 스코어 랭킹 / 위클리 스코어 어택 / 배틀 랭킹 */
@Composable
fun RankingScreen(vm: MainViewModel, onOpenWeb: (String, String) -> Unit) {
    val kinds = RankingKind.entries
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val kind = kinds[tab.coerceIn(0, kinds.lastIndex)]
    val boards by vm.boards.collectAsState()
    LaunchedEffect(kind) { vm.loadRanking(kind) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScreenTitle("RANKING")
            if (vm.isDemo) DemoTag()
            Spacer(Modifier.weight(1f))
            Text(
                "공식 보기",
                color = Mochi.Lavender,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.mochiClick { onOpenWeb(kind.url, kind.label) }.padding(8.dp),
            )
            IconButton(onClick = { vm.loadRanking(kind, force = true) }) { Icon(Icons.Rounded.Refresh, "새로고침", tint = Mochi.Ink) }
        }
        MochiPills(kinds.map { it.short }, tab, { tab = it }, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        Box(Modifier.weight(1f)) {
            BoardList(kind, boards[kind] ?: UiLoad.Loading, vm, onOpenWeb)
        }
    }
}

@Composable
private fun BoardList(kind: RankingKind, state: UiLoad<List<RankingBoard>>, vm: MainViewModel, onOpenWeb: (String, String) -> Unit) {
    val ctx = LocalContext.current
    when (state) {
        is UiLoad.Loading -> CenterLoading("${kind.label} 불러오는 중…")
        is UiLoad.Fail -> FailView(state.message, state.sessionExpired, onRetry = { vm.loadRanking(kind, true) }, onRelogin = { vm.sessionExpired() })
        is UiLoad.Ok -> {
            if (state.data.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    EmptyView("🔍", "랭킹 표를 찾지 못했어요", "페이지 구조가 예상과 달라요. 공식 페이지를 앱에서 열거나, HTML을 공유해 주시면 파서를 맞출 수 있어요.")
                    MochiButton("공식 페이지 열기", Modifier.fillMaxWidth()) { onOpenWeb(kind.url, kind.label) }
                    Spacer(Modifier.height(10.dp))
                    MochiButton("HTML 공유", Modifier.fillMaxWidth(), light = true) {
                        val h = vm.htmlFor(kind.name)
                        if (h != null) HtmlShare.share(ctx, "sdvx_" + kind.name.lowercase(Locale.ROOT), h)
                        else vm.toast("공유할 HTML이 없어요")
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.data) { b -> BoardCard(b) }
                }
            }
        }
    }
}

@Composable
private fun BoardCard(b: RankingBoard) {
    MochiCard(padding = 14.dp) {
        SectionTitle(b.title)
        b.entries.forEach { BoardRow(it) }
    }
}

@Composable
private fun BoardRow(e: BoardEntry) {
    val medal = when (e.rank) {
        1 -> Color(0xFFFFC94D)
        2 -> Color(0xFFB9C0CF)
        3 -> Color(0xFFE39E72)
        else -> Mochi.Lavender.copy(alpha = 0.14f)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (e.isMe) Mochi.Pink.copy(alpha = 0.16f) else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(medal), contentAlignment = Alignment.Center) {
            Text("${e.rank}", fontWeight = FontWeight.Black, fontSize = 13.sp, color = if (e.rank <= 3) Color.White else Mochi.Ink)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    e.name,
                    fontWeight = FontWeight.Bold,
                    color = Mochi.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (e.isMe) {
                    Spacer(Modifier.width(6.dp))
                    Pill("나", Mochi.Pink)
                }
            }
            val sub = listOfNotNull(e.playerId, e.extra).joinToString(" · ")
            if (sub.isNotEmpty()) {
                Text(sub, fontSize = 11.sp, color = Mochi.Sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(e.value, fontWeight = FontWeight.Black, color = Mochi.Ink, fontSize = 15.sp)
    }
}
