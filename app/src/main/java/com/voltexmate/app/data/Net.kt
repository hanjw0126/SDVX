package com.voltexmate.app.data

import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

object Urls {
    const val HOST = "https://p.eagate.573.jp"
    const val BASE = "$HOST/game/sdvx/vii/"
    const val PROFILE = "${BASE}playdata/profile/index.html"
    const val MUSIC = "${BASE}playdata/musicdata/index.html"
    const val DOWNLOAD = "${BASE}playdata/download/index.html"
    const val RANKING = "${BASE}ranking/index.html"
    const val WEEKLY = "${BASE}ranking/weekly/index.html"
    const val BATTLE = "${BASE}ranking/battle/index.html"
    const val LOGIN = "$HOST/gate/p/login.html?path=/game/sdvx/vii/playdata/profile/index.html"
}

/** 공식 CDN 이미지 (profile.css 에서 확인한 경로). 앱에 포함하지 않고 불러오기만 함 */
object Assets {
    private const val CDN = "https://eacache.s.konaminet.jp/game/sdvx/vii/images/playdata/"
    fun vfIcon(classId: Int) = CDN + String.format(Locale.US, "profile/force_icon_%02d.png", classId)
    fun vfStars(tier: Int) = CDN + String.format(Locale.US, "profile/force_%02d.png", tier)
    fun skill(code: String) = CDN + if (code == "none") "profile/skill_nolv.png" else "profile/skill_$code.png"
    fun arena(code: String) = CDN + when (code) {
        "none" -> "profile/arenaicon_none.png"
        "u" -> "profile/arenaicon_ult.png"
        else -> "profile/arenaicon_$code.png"
    }
}

/** WebView 로그인 쿠키를 OkHttp 와 공유 */
class WebViewCookieJar : CookieJar {
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val cm = CookieManager.getInstance()
        cookies.forEach { cm.setCookie(url.toString(), it.toString()) }
        cm.flush()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val raw = CookieManager.getInstance().getCookie(url.toString()) ?: return emptyList()
        return raw.split(";").mapNotNull { Cookie.parse(url, it.trim()) }
    }
}

object Net {
    const val DEFAULT_UA =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

    @Volatile
    var userAgent: String = DEFAULT_UA

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(WebViewCookieJar())
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request()
                val b = req.newBuilder()
                if (req.header("User-Agent") == null) b.header("User-Agent", userAgent)
                if (req.header("Referer") == null && req.url.host.endsWith("konaminet.jp")) b.header("Referer", Urls.BASE)
                chain.proceed(b.build())
            }
            .build()
    }
}

class SessionExpiredError : IOException("로그인 세션이 만료됐어요. 다시 로그인해 주세요.")
class PlayDataError(msg: String) : IOException(msg)

class OfficialSource(private val http: OkHttpClient) {
    class Page(val url: String, val html: String, val doc: Document)

    /** strict = 로그인·플레이데이터 필수 페이지(프로필 등) */
    suspend fun fetch(url: String, strict: Boolean = true): Page = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url)
            .header("Accept-Language", "ja-JP,ja;q=0.9,ko;q=0.8")
            .build()
        http.newCall(request).execute().use { resp ->
            val finalUrl = resp.request.url
            if (finalUrl.encodedPath.startsWith("/gate/p/login")) throw SessionExpiredError()
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} (${finalUrl.encodedPath})")
            val html = resp.body?.string().orEmpty()
            val st = UserStatusParser.parse(html)
            if (st != null && strict) {
                if (!st.login) throw SessionExpiredError()
                if (!st.eapass) throw PlayDataError("참조 중인 e-amusement pass가 없어요. 공식 사이트에서 카드를 '참조 중'으로 설정해 주세요.")
                if (!st.playdata) throw PlayDataError("SOUND VOLTEX ∇ 플레이 데이터가 없어요.")
            }
            Page(finalUrl.toString(), html, Jsoup.parse(html, finalUrl.toString()))
        }
    }
}
