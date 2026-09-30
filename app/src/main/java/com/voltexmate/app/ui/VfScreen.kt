package com.voltexmate.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltexmate.app.data.ChartScore
import com.voltexmate.app.data.UiLoad
import com.voltexmate.app.data.Vf
import com.voltexmate.app.data.VfInfo
import java.util.Locale

@Composable
fun VfScreen(vm: MainViewModel, onOpenDownload: () -> Unit, onPickCsv: () -> Unit) {
    val scores by vm.scores.collectAsState()
    val profile by vm.profile.collectAsState()
    val top = remember(scores) { Vf.top50(scores) }
    val total = Vf.total(top)
    val officialVf: Double? = when (val s = profile) {
        is UiLoad.Ok -> s.data.volforce
        else -> null
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScreenTitle("VF TARGET")
                if (vm.isDemo) DemoTag()
            }
        }
        item {
            MochiCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OfficialVfBadge(VfInfo.of(total, null, null), size = 96.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        SectionTitle("TOP 50 VOLFORCE")
                        Text(String.format(Locale.US, "%.3f", total), color = Mochi.Ink, fontSize = 34.sp, fontWeight = FontWeight.Black)
                        if (officialVf != null) {
                            Text("공식 VF " + String.format(Locale.US, "%.3f", officialVf), color = Mochi.Sub, fontSize = 12.sp)
                        }
                        Text("VF에 반영되는 상위 ${top.size}개 차트", color = Mochi.Sub, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val avgLv = if (top.isEmpty()) "-" else String.format(Locale.US, "%.2f", top.map { it.level }.average())
                    val avgScore = if (top.isEmpty()) "-" else String.format(Locale.US, "%,d", top.map { it.score.toLong() }.average().toLong())
                    val cut = top.lastOrNull()?.force?.toString() ?: "-"
                    StatTile("평균 레벨", avgLv, Mochi.Lavender, Modifier.weight(1f))
                    StatTile("평균 점수", avgScore, Mochi.Pink, Modifier.weight(1.3f))
                    StatTile("커트라인", cut, Mochi.Mint, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MochiButton("PNG 저장", Modifier.weight(1f)) { vm.exportVf(share = false) }
                    MochiButton("공유", Modifier.weight(1f), light = true) { vm.exportVf(share = true) }
                }
            }
        }
        item {
            MochiCard {
                SectionTitle("SCORE SYNC")
                Spacer(Modifier.height(8.dp))
                Text("공식 스코어 CSV로 전체 기록을 가져와요.", color = Mochi.Ink, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MochiButton("공식 다운로드", Modifier.weight(1f), light = true, onClick = onOpenDownload)
                    MochiButton("CSV 파일 선택", Modifier.weight(1f), light = true, onClick = onPickCsv)
                }
                Spacer(Modifier.height(10.dp))
                InfoNote("공식 페이지에서 CSV를 내려받으면 자동으로 가져와요. 안 되면 파일로 저장한 뒤 [CSV 파일 선택]을 눌러 주세요. 가져올 때마다 이전 기록과 비교해 '최근 › 점수 갱신'에 쌓여요.")
            }
        }
        if (top.isEmpty()) {
            item { EmptyView("🎶", "아직 스코어가 없어요", "위의 버튼으로 공식 CSV를 가져오면 VF 대상곡을 계산해요.") }
        } else {
            itemsIndexed(top) { i, s -> ChartRow(i + 1, s) }
        }
    }
}

@Composable
private fun ChartRow(rank: Int, s: ChartScore) {
    MochiCard(padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val bubble = if (rank <= 3) Brush.linearGradient(listOf(Mochi.Lavender, Mochi.Pink)) else SolidColor(Mochi.Lavender.copy(alpha = 0.12f))
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(bubble),
                contentAlignment = Alignment.Center,
            ) {
                Text("$rank", fontWeight = FontWeight.Black, fontSize = 14.sp, color = if (rank <= 3) Color.White else Mochi.Ink)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.title, fontWeight = FontWeight.Bold, color = Mochi.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill("${s.difficulty.short} ${s.levelText}", Color(s.difficulty.color))
                    Pill(s.clear.short, Color(s.clear.color))
                    Pill(s.grade.label, Mochi.Sky)
                }
                Spacer(Modifier.height(4.dp))
                Text(String.format(Locale.US, "%,d", s.score), color = Mochi.Sub, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("${s.force}", color = Mochi.Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("FORCE", color = Mochi.Sub, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
