package com.voltexmate.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltexmate.app.data.Session
import com.voltexmate.app.data.Urls

@Composable
fun AppRoot(vm: MainViewModel) {
    var splashDone by rememberSaveable { mutableStateOf(false) }
    val session by vm.session.collectAsState()
    if (!splashDone) {
        SplashScreen { splashDone = true }
        return
    }
    when (session) {
        Session.UNKNOWN -> CenterLoading("준비 중…")
        Session.LOGGED_OUT -> LoginScreen(onLoggedIn = { vm.onLoggedIn() }, onDemo = { vm.startDemo() })
        else -> MainScaffold(vm)
    }
}

private data class WebTarget(val url: String, val title: String, val csv: Boolean = false)

@Composable
private fun MainScaffold(vm: MainViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var web by remember { mutableStateOf<WebTarget?>(null) }
    val busy by vm.busy.collectAsState()
    val ctx = LocalContext.current
    LaunchedEffect(vm) {
        vm.messages.collect { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.setAvatar(uri)
    }
    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importCsvUri(uri)
    }
    val pickCsv: () -> Unit = {
        csvPicker.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/octet-stream", "*/*"))
    }
    val openWeb: (String, String) -> Unit = { url, title ->
        if (vm.isDemo) {
            vm.toast("데모 모드에서는 공식 페이지를 열 수 없어요")
        } else {
            web = WebTarget(url, title)
        }
    }
    val openDownload: () -> Unit = {
        if (vm.isDemo) {
            vm.toast("데모 모드에서는 공식 페이지를 열 수 없어요")
        } else {
            web = WebTarget(Urls.DOWNLOAD, "스코어 CSV 다운로드", csv = true)
        }
    }
    val csvHandler: (String) -> Unit = { url ->
        web = null
        vm.downloadCsv(url)
    }

    Box(Modifier.fillMaxSize().background(Mochi.Bg)) {
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            when (tab) {
                0 -> ProfileScreen(
                    vm,
                    onPickAvatar = { avatarPicker.launch("image/*") },
                    onOpenRecent = { tab = 2 },
                    onOpenWeb = openWeb,
                )
                1 -> VfScreen(vm, onOpenDownload = openDownload, onPickCsv = pickCsv)
                2 -> RecentScreen(vm)
                else -> RankingScreen(vm, onOpenWeb = openWeb)
            }
        }
        BottomBar(tab, { tab = it }, Modifier.align(Alignment.BottomCenter))

        busy?.let { label ->
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Mochi.Ink)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(label, color = Color.White, fontSize = 13.sp)
            }
        }

        web?.let { w ->
            OfficialWebScreen(
                url = w.url,
                title = w.title,
                onClose = { web = null },
                onDownload = if (w.csv) csvHandler else null,
            )
        }
    }
}

@Composable
private fun BottomBar(tab: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val items: List<Pair<ImageVector, String>> = listOf(
        Icons.Rounded.Person to "프로필",
        Icons.Rounded.Star to "VF",
        Icons.Rounded.DateRange to "최근",
        Icons.Rounded.List to "랭킹",
    )
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth()
            .shadow(18.dp, shape, ambientColor = Mochi.Lavender.copy(alpha = 0.35f), spotColor = Mochi.Lavender.copy(alpha = 0.35f))
            .clip(shape)
            .background(Color.White)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, item ->
            val on = i == tab
            val bg by animateColorAsState(if (on) Mochi.Lavender else Color.Transparent, label = "tabBg")
            Row(
                Modifier
                    .weight(if (on) 1.6f else 1f)
                    .mochiClick { onSelect(i) }
                    .clip(shape)
                    .background(bg)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(item.first, item.second, tint = if (on) Color.White else Mochi.Sub, modifier = Modifier.size(22.dp))
                AnimatedVisibility(visible = on) {
                    Text(
                        item.second,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}
