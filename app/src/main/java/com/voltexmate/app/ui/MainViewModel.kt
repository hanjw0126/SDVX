package com.voltexmate.app.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voltexmate.app.data.AvatarStore
import com.voltexmate.app.data.ChartScore
import com.voltexmate.app.data.CsvParser
import com.voltexmate.app.data.Demo
import com.voltexmate.app.data.MatchLogStore
import com.voltexmate.app.data.MatchingEntry
import com.voltexmate.app.data.Net
import com.voltexmate.app.data.OfficialSource
import com.voltexmate.app.data.ParseError
import com.voltexmate.app.data.Prefs
import com.voltexmate.app.data.ProfileDetail
import com.voltexmate.app.data.ProfileParser
import com.voltexmate.app.data.BoardParser
import com.voltexmate.app.data.RankingBoard
import com.voltexmate.app.data.RankingKind
import com.voltexmate.app.data.ScoreStore
import com.voltexmate.app.data.ScoreUpdate
import com.voltexmate.app.data.Session
import com.voltexmate.app.data.SessionExpiredError
import com.voltexmate.app.data.UiLoad
import com.voltexmate.app.data.Urls
import com.voltexmate.app.data.Vf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.IOException

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private val scoreStore = ScoreStore(app)
    private val matchStore = MatchLogStore(app)
    private val source = OfficialSource(Net.client)
    private val html = mutableMapOf<String, String>()

    private val _session = MutableStateFlow(Session.UNKNOWN)
    val session: StateFlow<Session> = _session.asStateFlow()
    val isDemo: Boolean get() = _session.value == Session.DEMO

    private val _profile = MutableStateFlow<UiLoad<ProfileDetail>>(UiLoad.Loading)
    val profile: StateFlow<UiLoad<ProfileDetail>> = _profile.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _scores = MutableStateFlow<List<ChartScore>>(emptyList())
    val scores: StateFlow<List<ChartScore>> = _scores.asStateFlow()

    private val _updates = MutableStateFlow<List<ScoreUpdate>>(emptyList())
    val updates: StateFlow<List<ScoreUpdate>> = _updates.asStateFlow()

    private val _matchLog = MutableStateFlow<List<MatchingEntry>>(emptyList())
    val matchLog: StateFlow<List<MatchingEntry>> = _matchLog.asStateFlow()

    private val _boards = MutableStateFlow<Map<RankingKind, UiLoad<List<RankingBoard>>>>(emptyMap())
    val boards: StateFlow<Map<RankingKind, UiLoad<List<RankingBoard>>>> = _boards.asStateFlow()

    private val _avatar = MutableStateFlow<File?>(null)
    val avatar: StateFlow<File?> = _avatar.asStateFlow()

    private val _busy = MutableStateFlow<String?>(null)
    val busy: StateFlow<String?> = _busy.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val ctx: Application get() = getApplication()

    init {
        _session.value = when {
            prefs.demo -> Session.DEMO
            prefs.loggedIn -> Session.LOGGED_IN
            else -> Session.LOGGED_OUT
        }
        _avatar.value = AvatarStore.current(app)
        loadLocal()
        if (_session.value != Session.LOGGED_OUT) refreshProfile()
    }

    fun toast(msg: String) {
        _messages.tryEmit(msg)
    }

    fun htmlFor(key: String): String? = html[key]

    fun currentProfile(): ProfileDetail? = when (val s = _profile.value) {
        is UiLoad.Ok -> s.data
        else -> null
    }

    private fun loadLocal() {
        if (isDemo) {
            _scores.value = Demo.scores
            _updates.value = Demo.updates
            _matchLog.value = Demo.profile.matching
            return
        }
        viewModelScope.launch {
            val s = withContext(Dispatchers.IO) { scoreStore.scores() }
            val u = withContext(Dispatchers.IO) { scoreStore.updates() }
            val m = withContext(Dispatchers.IO) { matchStore.load() }
            _scores.value = s
            _updates.value = u
            _matchLog.value = m
        }
    }

    private fun resetRemote() {
        html.clear()
        _boards.value = emptyMap()
        _profile.value = UiLoad.Loading
    }

    fun startDemo() {
        prefs.demo = true
        prefs.loggedIn = false
        resetRemote()
        _session.value = Session.DEMO
        loadLocal()
        refreshProfile()
    }

    fun onLoggedIn() {
        if (_session.value == Session.LOGGED_IN) return
        prefs.loggedIn = true
        prefs.demo = false
        resetRemote()
        _session.value = Session.LOGGED_IN
        loadLocal()
        refreshProfile()
    }

    fun logout() {
        val wasDemo = isDemo
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        prefs.loggedIn = false
        prefs.demo = false
        if (!wasDemo) {
            viewModelScope.launch(Dispatchers.IO) {
                scoreStore.clear()
                matchStore.clear()
            }
        }
        _scores.value = emptyList()
        _updates.value = emptyList()
        _matchLog.value = emptyList()
        resetRemote()
        _session.value = Session.LOGGED_OUT
    }

    fun sessionExpired() {
        prefs.loggedIn = false
        resetRemote()
        _session.value = Session.LOGGED_OUT
    }

    fun refreshProfile() {
        val demo = isDemo
        viewModelScope.launch {
            if (_profile.value is UiLoad.Ok) _refreshing.value = true else _profile.value = UiLoad.Loading
            val r: UiLoad<ProfileDetail> = load {
                if (demo) {
                    delay(500)
                    Demo.profile
                } else {
                    val page = source.fetch(Urls.PROFILE)
                    html["profile"] = page.html
                    val p = withContext(Dispatchers.Default) { ProfileParser.parse(page.doc) }
                    _matchLog.value = withContext(Dispatchers.IO) { matchStore.merge(p.matching) }
                    p
                }
            }
            if (r is UiLoad.Fail && !r.sessionExpired && _profile.value is UiLoad.Ok) {
                toast(r.message)
            } else {
                _profile.value = r
            }
            _refreshing.value = false
        }
    }

    fun loadRanking(kind: RankingKind, force: Boolean = false) {
        if (!force && _boards.value[kind] is UiLoad.Ok) return
        val demo = isDemo
        viewModelScope.launch {
            _boards.update { it + (kind to UiLoad.Loading) }
            val r: UiLoad<List<RankingBoard>> = load {
                if (demo) {
                    delay(400)
                    Demo.boards(kind)
                } else {
                    val page = source.fetch(kind.url, strict = false)
                    html[kind.name] = page.html
                    val myId = currentProfile()?.playerId
                    withContext(Dispatchers.Default) { BoardParser.parse(page.doc, myId) }
                }
            }
            _boards.update { it + (kind to r) }
        }
    }

    fun setAvatar(uri: Uri) {
        viewModelScope.launch {
            try {
                _avatar.value = withContext(Dispatchers.IO) { AvatarStore.save(ctx, uri) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toast(e.message ?: "이미지를 저장하지 못했어요")
            }
        }
    }

    fun clearAvatar() {
        AvatarStore.clear(ctx)
        _avatar.value = null
    }

    fun importCsvUri(uri: Uri) = runBusy("CSV 읽는 중…") {
        val bytes = withContext(Dispatchers.IO) {
            ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } ?: throw IOException("파일을 열 수 없어요")
        applyCsv(bytes)
    }

    fun downloadCsv(url: String) = runBusy("CSV 받는 중…") {
        if (url.startsWith("blob:") || url.startsWith("data:")) {
            throw IOException("이 다운로드는 앱이 직접 받을 수 없어요. 파일로 저장한 뒤 [CSV 파일 선택]을 써 주세요.")
        }
        val bytes = withContext(Dispatchers.IO) {
            val req = Request.Builder().url(url).header("Referer", Urls.DOWNLOAD).build()
            Net.client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                val type = resp.header("Content-Type").orEmpty()
                if (type.contains("html", ignoreCase = true)) {
                    throw IOException("CSV 대신 웹페이지가 왔어요. 파일로 저장한 뒤 [CSV 파일 선택]을 써 주세요.")
                }
                resp.body?.bytes() ?: ByteArray(0)
            }
        }
        applyCsv(bytes)
    }

    private suspend fun applyCsv(bytes: ByteArray) {
        val parsed = withContext(Dispatchers.Default) { CsvParser.parse(CsvParser.decode(bytes)) }
        if (parsed.isEmpty()) throw ParseError("CSV에서 차트를 찾지 못했어요")
        if (isDemo) {
            _scores.value = parsed
        } else {
            val (all, ups) = withContext(Dispatchers.IO) { scoreStore.merge(parsed, System.currentTimeMillis()) }
            _scores.value = all
            _updates.value = ups
        }
        toast("${parsed.size}개 차트를 가져왔어요")
    }

    fun exportVf(share: Boolean) {
        val top = Vf.top50(_scores.value)
        if (top.isEmpty()) {
            toast("먼저 CSV로 스코어를 가져와 주세요")
            return
        }
        val p = currentProfile()
        runBusy(if (share) "이미지 만드는 중…" else "PNG 저장 중…") {
            val bmp = VfExporter.render(ctx, p?.playerName ?: "PLAYER", p?.playerId.orEmpty(), p?.vf, top)
            if (share) {
                val uri = withContext(Dispatchers.IO) { VfExporter.shareUri(ctx, bmp) }
                val send = Intent(Intent.ACTION_SEND)
                    .setType("image/png")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                ctx.startActivity(Intent.createChooser(send, "VF 이미지 공유").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } else {
                val where = withContext(Dispatchers.IO) { VfExporter.save(ctx, bmp) }
                toast("저장했어요 · $where")
            }
        }
    }

    private fun runBusy(label: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = label
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: SessionExpiredError) {
                toast(e.message ?: "세션 만료")
                sessionExpired()
            } catch (e: Exception) {
                toast(e.message ?: e.javaClass.simpleName)
            } finally {
                _busy.value = null
            }
        }
    }

    private suspend fun <T> load(block: suspend () -> T): UiLoad<T> = try {
        UiLoad.Ok(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: SessionExpiredError) {
        UiLoad.Fail(e.message ?: "세션 만료", sessionExpired = true)
    } catch (e: Exception) {
        UiLoad.Fail(e.message ?: e.javaClass.simpleName)
    }
}
