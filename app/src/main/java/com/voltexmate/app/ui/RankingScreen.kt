package com.voltexmate.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
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
import com.voltexmate.app.data.ChartLink
import com.voltexmate.app.data.RankDetail
import com.voltexmate.app.data.RankingBoard
import com.voltexmate.app.data.RankingKind
import com.voltexmate.app.data.UiLoad
import com.voltexmate.app.data.SongItem
import com.voltexmate.app.data.SongQuery
import com.voltexmate.app.data.SongSearch
import com.voltexmate.app.data.SongSearchParser
import com.voltexmate.app.data.Urls
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.util.Locale

/** 공식 RANKING 메뉴: 스코어 랭킹 / 위클리 스코어 어택 / 배틀 랭킹 */
@Composable
fun RankingScreen(vm: MainViewModel, onOpenWeb: (String, String) -> Unit) {
    val kinds = RankingKind.entries
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val kind = kinds[tab.coerceIn(0, kinds.lastIndex)]
    val boards by vm.boards.collectAsState()
    val seasons by vm.seasons.collectAsState()
    val season by vm.season.collectAsState()
    val detail by vm.detail.collectAsState()
    val songQuery by vm.songQuery.collectAsState()
    LaunchedEffect(kind) { if (kind == RankingKind.SCORE) vm.loadSongsIfNeeded() else vm.loadRanking(kind) }
    BackHandler(enabled = detail != null) { vm.closeDetail() }

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
                modifier = Modifier.mochiClick { onOpenWeb(if (kind == RankingKind.SCORE) SongSearchParser.url(songQuery) else kind.url, kind.label) }.padding(8.dp),
            )
            IconButton(onClick = { if (kind == RankingKind.SCORE) vm.searchSongs(songQuery) else vm.loadRanking(kind, force = true) }) { Icon(Icons.Rounded.Refresh, "새로고침", tint = Mochi.Ink) }
        }
        MochiPills(kinds.map { it.short }, tab, { tab = it; vm.closeDetail() }, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        if (kind == RankingKind.BATTLE && seasons.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                items(seasons) { (value, label) ->
                    RkChip(label, Mochi.Lavender, selected = value == season) { vm.selectSeason(value) }
                }
            }
        }
        Box(Modifier.weight(1f)) {
            val d = detail
            if ((kind == RankingKind.WEEKLY || kind == RankingKind.SCORE) && d != null) {
                RkDetailView(d, vm, onOpenWeb)
            } else if (kind == RankingKind.SCORE) {
                SongSearchPanel(vm, onOpenWeb)
            } else {
                BoardList(
                    label = kind.label,
                    state = boards[kind] ?: UiLoad.Loading,
                    vm = vm,
                    webUrl = kind.url,
                    shareKey = kind.name,
                    onRetry = { vm.loadRanking(kind, true) },
                    onOpenWeb = onOpenWeb,
                )
            }
        }
    }
}

@Composable
private fun RkDetailView(d: RankDetail, vm: MainViewModel, onOpenWeb: (String, String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.closeDetail() }) { Icon(Icons.Rounded.Close, "닫기", tint = Mochi.Ink) }
            Text(
                d.title,
                fontWeight = FontWeight.Bold,
                color = Mochi.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                "공식 보기",
                color = Mochi.Lavender,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.mochiClick { onOpenWeb(d.url, d.title) }.padding(8.dp),
            )
        }
        Box(Modifier.weight(1f)) {
            BoardList(
                label = d.title,
                state = d.state,
                vm = vm,
                webUrl = d.url,
                shareKey = "WEEKLY_DETAIL",
                onRetry = { vm.openWeeklyChart(d.title, d.url) },
                onOpenWeb = onOpenWeb,
            )
        }
    }
}

@Composable
private fun BoardList(
    label: String,
    state: UiLoad<List<RankingBoard>>,
    vm: MainViewModel,
    webUrl: String,
    shareKey: String,
    onRetry: () -> Unit,
    onOpenWeb: (String, String) -> Unit,
) {
    val ctx = LocalContext.current
    when (state) {
        is UiLoad.Loading -> CenterLoading("$label 불러오는 중…")
        is UiLoad.Fail -> FailView(state.message, state.sessionExpired, onRetry = onRetry, onRelogin = { vm.sessionExpired() })
        is UiLoad.Ok -> {
            if (state.data.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    EmptyView("🔍", "랭킹 표를 찾지 못했어요", "로그인이 필요한 페이지이거나 구조가 예상과 달라요. 공식 페이지를 앱에서 열거나, HTML을 공유해 주시면 파서를 맞출 수 있어요.")
                    MochiButton("공식 페이지 열기", Modifier.fillMaxWidth()) { onOpenWeb(webUrl, label) }
                    Spacer(Modifier.height(10.dp))
                    MochiButton("HTML 공유", Modifier.fillMaxWidth(), light = true) {
                        val h = vm.htmlFor(shareKey)
                        if (h != null) HtmlShare.share(ctx, "sdvx_" + shareKey.lowercase(Locale.ROOT), h)
                        else vm.toast("공유할 HTML이 없어요")
                    }
                }
            } else {
                // 300명 이상도 부드럽게: 행 단위로 지연 렌더링
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                ) {
                    state.data.forEachIndexed { bi, b ->
                        item(key = "board-$bi") {
                            RkBoardHeader(b) { c -> vm.openWeeklyChart("${b.title} [${c.label}]", c.url) }
                        }
                        items(b.entries.size) { i -> BoardRow(b.entries[i]) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RkBoardHeader(b: RankingBoard, onChart: (ChartLink) -> Unit) {
    MochiCard(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp), padding = 14.dp) {
        SectionTitle(b.title)
        val note = b.note
        if (!note.isNullOrEmpty()) {
            Text(note, fontSize = 12.sp, color = Mochi.Sub)
        }
        if (b.charts.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                b.charts.forEach { c -> RkChip(c.label, rkDiffColor(c.label)) { onChart(c) } }
            }
        }
    }
}

@Composable
private fun RkChip(text: String, color: Color, selected: Boolean = false, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) Color.White else color,
        fontWeight = FontWeight.Black,
        fontSize = 12.sp,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) color else color.copy(alpha = 0.15f))
            .mochiClick { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

private fun rkDiffColor(label: String): Color = when (label) {
    "NOV" -> Color(0xFF8E7CFF)
    "ADV" -> Color(0xFFE0A91F)
    "EXH" -> Color(0xFFE5566E)
    "MXM" -> Color(0xFF8C95A8)
    "INF", "GRV", "HVN", "VVD", "XCD", "NBL" -> Color(0xFFD0529B)
    "ULT" -> Color(0xFF3F7BE0)
    else -> Mochi.Lavender
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
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (e.isMe) Mochi.Pink.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.85f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
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

/** 스코어 랭킹: 공식 곡 검색 → 난이도 칩 → 차트별 순위 */
@Composable
private fun SongSearchPanel(vm: MainViewModel, onOpenWeb: (String, String) -> Unit) {
    val ctx = LocalContext.current
    val focus = LocalFocusManager.current
    val q by vm.songQuery.collectAsState()
    val state by vm.songs.collectAsState()
    var text by rememberSaveable { mutableStateOf(q.keyword) }
    fun go(nq: SongQuery) {
        focus.clearFocus()
        vm.searchSongs(nq)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.9f))
                .padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = Mochi.Sub)
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TextStyle(color = Mochi.Ink, fontSize = 15.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { go(q.copy(keyword = text.trim(), page = 1)) }),
                modifier = Modifier.weight(1f).padding(vertical = 14.dp),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) Text("곡명 검색", color = Mochi.Sub, fontSize = 15.sp)
                        inner()
                    }
                },
            )
            if (text.isNotEmpty()) {
                IconButton(onClick = {
                    text = ""
                    go(q.copy(keyword = "", page = 1))
                }) { Icon(Icons.Rounded.Close, "지우기", tint = Mochi.Sub) }
            }
            Text(
                "검색",
                color = Mochi.Lavender,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .mochiClick { go(q.copy(keyword = text.trim(), page = 1)) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 10.dp),
        ) {
            item {
                RkChip("전체", Mochi.Lavender, selected = q.category <= 0) { go(q.copy(keyword = text.trim(), category = -1, page = 1)) }
            }
            items(SongSearch.CATEGORIES) { (value, label) ->
                RkChip(label, Mochi.Lavender, selected = q.category == value) { go(q.copy(keyword = text.trim(), category = value, page = 1)) }
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
        ) {
            item {
                RkChip("최근 플레이순", Mochi.Pink, selected = q.recent) { go(q.copy(keyword = text.trim(), recent = !q.recent, page = 1)) }
            }
            item {
                RkChip("레벨 전체", Mochi.Sky, selected = q.level <= 0) { go(q.copy(keyword = text.trim(), level = -1, page = 1)) }
            }
            items(SongSearch.LEVELS) { lv ->
                RkChip("Lv" + SongSearch.levelLabel(lv), Mochi.Sky, selected = q.level == lv) { go(q.copy(keyword = text.trim(), level = lv, page = 1)) }
            }
        }
        Box(Modifier.weight(1f)) {
            when (val st = state) {
                null, is UiLoad.Loading -> CenterLoading("곡 목록 불러오는 중…")
                is UiLoad.Fail -> FailView(st.message, st.sessionExpired, onRetry = { vm.searchSongs(q) }, onRelogin = { vm.sessionExpired() })
                is UiLoad.Ok -> {
                    val data = st.data
                    if (data.songs.isEmpty()) {
                        Column(
                            Modifier.fillMaxSize().padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            EmptyView("🔍", "검색 결과가 없어요", "다른 키워드나 조건으로 찾아보세요. 계속 비어 있으면 공식 페이지를 열거나 HTML을 공유해 주세요.")
                            MochiButton("공식 페이지 열기", Modifier.fillMaxWidth()) { onOpenWeb(SongSearchParser.url(q), RankingKind.SCORE.label) }
                            Spacer(Modifier.height(10.dp))
                            MochiButton("HTML 공유", Modifier.fillMaxWidth(), light = true) {
                                val h = vm.htmlFor(RankingKind.SCORE.name)
                                if (h != null) HtmlShare.share(ctx, "sdvx_score", h) else vm.toast("공유할 HTML이 없어요")
                            }
                        }
                    } else {
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                        ) {
                            items(data.songs) { song ->
                                SongCard(song) { c -> vm.openWeeklyChart("${song.title} [${c.label}]", c.url, RankingKind.SCORE) }
                            }
                            item {
                                SongPager(data.page, data.pages) { pg -> go(q.copy(page = pg)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongCard(song: SongItem, onChart: (ChartLink) -> Unit) {
    val ctx = LocalContext.current
    MochiCard(Modifier.fillMaxWidth().padding(top = 10.dp), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(Mochi.Lavender.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("♪", color = Mochi.Lavender, fontSize = 22.sp, fontWeight = FontWeight.Black)
                val jacket = song.jacket
                if (jacket != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(ctx).data(jacket).addHeader("Referer", Urls.RANKING).crossfade(true).build(),
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, fontWeight = FontWeight.Bold, color = Mochi.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (song.artist.isNotEmpty()) {
                    Text(song.artist, fontSize = 12.sp, color = Mochi.Sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (song.charts.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                song.charts.forEach { c -> RkChip(c.label, rkDiffColor(c.label)) { onChart(c) } }
            }
        }
    }
}

@Composable
private fun SongPager(page: Int, pages: Int, onPage: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val prev = page > 1
        val next = page < pages
        RkChip("≪", if (prev) Mochi.Lavender else Mochi.Sub) { if (prev) onPage(1) }
        RkChip("‹ 이전", if (prev) Mochi.Lavender else Mochi.Sub) { if (prev) onPage(page - 1) }
        Text("$page / $pages", fontWeight = FontWeight.Black, color = Mochi.Ink, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 6.dp))
        RkChip("다음 ›", if (next) Mochi.Lavender else Mochi.Sub) { if (next) onPage(page + 1) }
        RkChip("≫", if (next) Mochi.Lavender else Mochi.Sub) { if (next) onPage(pages) }
    }
}
