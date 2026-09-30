package com.voltexmate.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltexmate.app.data.MatchingEntry
import com.voltexmate.app.data.ScoreUpdate
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

@Composable
fun RecentScreen(vm: MainViewModel) {
    val log by vm.matchLog.collectAsState()
    val updates by vm.updates.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScreenTitle("RECENT")
            if (vm.isDemo) DemoTag()
            Spacer(Modifier.weight(1f))
            if (refreshing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Mochi.Lavender)
            IconButton(onClick = { vm.refreshProfile() }) { Icon(Icons.Rounded.Refresh, "새로고침", tint = Mochi.Ink) }
        }
        MochiPills(listOf("플레이 로그", "점수 갱신"), tab, { tab = it }, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        Box(Modifier.weight(1f)) {
            if (tab == 0) PlayLog(log) else UpdateList(updates)
        }
    }
}

private fun dayLabel(day: String): String = runCatching {
    val d = LocalDate.parse(day, DateTimeFormatter.ofPattern("yyyy/MM/dd"))
    "${d.monthValue}월 ${d.dayOfMonth}일 (${d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)})"
}.getOrDefault(day)

@Composable
private fun DayHeader(day: String, count: Int) {
    Row(Modifier.padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(dayLabel(day), fontWeight = FontWeight.Black, color = Mochi.Ink, fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Pill("${count}곡", Mochi.Pink)
    }
}

@Composable
private fun PlayLog(log: List<MatchingEntry>) {
    if (log.isEmpty()) {
        EmptyView("🎧", "아직 기록이 없어요", "프로필을 새로고침하면 공식 사이트의 최근 플레이 이력을 가져와요.")
        return
    }
    val groups = remember(log) { log.groupBy { it.playedAt.take(10) } }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { InfoNote("공식 매칭 이력(최근 20곡)을 불러올 때마다 앱에 누적 저장해요. 공식 이력엔 점수·난이도가 없어서 점수 변화는 '점수 갱신' 탭에서 보여줘요.") }
        groups.forEach { (day, entries) ->
            item { DayHeader(day, entries.size) }
            items(entries) { PlayLogCard(it) }
        }
    }
}

@Composable
private fun PlayLogCard(m: MatchingEntry) {
    MochiCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TimeBubble(m.playedAt.takeLast(8).take(5))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(m.title, fontWeight = FontWeight.Bold, color = Mochi.Ink, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(m.artist, fontSize = 12.sp, color = Mochi.Sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (m.opponents.isNotEmpty()) {
            Row(
                Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("매칭", fontSize = 11.sp, color = Mochi.Sub)
                m.opponents.forEach { o ->
                    Pill(listOfNotNull(o.name, o.skill?.removePrefix("SKILL ")).joinToString(" · "), Mochi.Lavender)
                }
            }
        }
    }
}

@Composable
private fun UpdateList(ups: List<ScoreUpdate>) {
    if (ups.isEmpty()) {
        EmptyView("📈", "아직 갱신 기록이 없어요", "CSV를 가져올 때마다 이전 기록과 비교해 점수·클리어가 오른 차트를 쌓아요. 첫 가져오기는 기준점이 돼요.")
        return
    }
    val groups = remember(ups) {
        val f = SimpleDateFormat("yyyy/MM/dd", Locale.US)
        ups.groupBy { f.format(Date(it.at)) }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        groups.forEach { (day, list) ->
            item { DayHeader(day, list.size) }
            items(list) { UpdateCard(it) }
        }
    }
}

@Composable
private fun UpdateCard(u: ScoreUpdate) {
    val c = u.chart
    MochiCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(c.title, fontWeight = FontWeight.Bold, color = Mochi.Ink, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill("${c.difficulty.short} ${c.levelText}", Color(c.difficulty.color))
                    Pill(c.clear.short, Color(c.clear.color))
                    if (u.isNew) Pill("NEW", Mochi.Pink)
                    if (u.clearUp) Pill("CLEAR↑", Mochi.Mint)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(String.format(Locale.US, "%,d", c.score), fontWeight = FontWeight.Black, color = Mochi.Ink)
                if (!u.isNew && u.gain > 0) {
                    Text("+" + String.format(Locale.US, "%,d", u.gain), color = Mochi.Pink.darker(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
