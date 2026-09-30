package com.voltexmate.app.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.voltexmate.app.R
import com.voltexmate.app.data.Net
import com.voltexmate.app.data.Urls

private const val LOGIN_CHECK_JS =
    "(function(){try{var s=ea_common_template.userstatus.state;return (s&&s.login)?'1':'0';}catch(e){return '0';}})();"

@Composable
fun LoginScreen(onLoggedIn: () -> Unit, onDemo: () -> Unit) {
    var showWeb by rememberSaveable { mutableStateOf(false) }
    if (showWeb) {
        OfficialWebScreen(
            url = Urls.LOGIN,
            title = "e-amusement 로그인",
            onClose = { showWeb = false },
            detectLogin = true,
            onLoggedIn = onLoggedIn,
        )
        return
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Mochi.Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(112.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(Brush.linearGradient(listOf(Mochi.Lavender, Mochi.Pink))),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(112.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("VOLTEX MATE", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Mochi.Ink, letterSpacing = 3.sp)
        Text("SOUND VOLTEX ∇ 플레이 데이터를 말랑하게", color = Mochi.Sub, fontSize = 13.sp)
        Spacer(Modifier.height(24.dp))
        MochiCard {
            listOf(
                "코나미 공식 로그인 페이지(p.eagate.573.jp)에서 직접 로그인해요.",
                "비밀번호는 앱이 보거나 저장하지 않아요. 로그인 쿠키만 기기에 남아요.",
                "상세 플레이 데이터·CSV는 e-amusement 베이직 코스가 필요할 수 있어요.",
                "KONAMI와 무관한 비공식 팬메이드 앱이에요.",
            ).forEach { line ->
                Row(Modifier.padding(vertical = 4.dp)) {
                    Text("•", color = Mochi.Lavender, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(8.dp))
                    Text(line, color = Mochi.Ink, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        MochiButton("공식 사이트로 로그인", Modifier.fillMaxWidth()) { showWeb = true }
        Spacer(Modifier.height(10.dp))
        MochiButton("데모 모드로 둘러보기", Modifier.fillMaxWidth(), light = true, onClick = onDemo)
    }
}

/** 앱 안에서 공식 페이지 열기 (로그인 감지 / CSV 다운로드 가로채기) */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OfficialWebScreen(
    url: String,
    title: String,
    onClose: () -> Unit,
    detectLogin: Boolean = false,
    onLoggedIn: () -> Unit = {},
    onDownload: ((String) -> Unit)? = null,
) {
    val holder = remember { arrayOfNulls<WebView>(1) }
    val fired = remember { booleanArrayOf(false) }
    var loading by remember { mutableStateOf(true) }
    val loggedCb by rememberUpdatedState(onLoggedIn)
    val downloadCb by rememberUpdatedState(onDownload)
    val fireLogin: () -> Unit = {
        if (!fired[0]) {
            fired[0] = true
            loggedCb()
        }
    }

    BackHandler {
        val w = holder[0]
        if (w != null && w.canGoBack()) w.goBack() else onClose()
    }
    DisposableEffect(Unit) {
        onDispose {
            holder[0]?.destroy()
            holder[0] = null
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Mochi.Bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "닫기", tint = Mochi.Ink) }
            Text(
                title,
                Modifier.weight(1f),
                fontWeight = FontWeight.Black,
                color = Mochi.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Mochi.Lavender)
            IconButton(onClick = { holder[0]?.reload() }) { Icon(Icons.Rounded.Refresh, "새로고침", tint = Mochi.Ink) }
            if (detectLogin) {
                IconButton(onClick = fireLogin) { Icon(Icons.Rounded.Check, "로그인 완료", tint = Mochi.Lavender) }
            }
        }
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { c ->
                WebView(c).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    Net.userAgent = settings.userAgentString
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                            loading = true
                        }

                        override fun onPageFinished(view: WebView?, pageUrl: String?) {
                            loading = false
                            CookieManager.getInstance().flush()
                            if (detectLogin && pageUrl != null && pageUrl.contains("/game/sdvx/")) {
                                view?.evaluateJavascript(LOGIN_CHECK_JS) { r ->
                                    if (r != null && r.contains("1")) fireLogin()
                                }
                            }
                        }
                    }
                    setDownloadListener { dUrl, _, _, _, _ -> downloadCb?.invoke(dUrl) }
                    loadUrl(url)
                    holder[0] = this
                }
            },
        )
    }
}
