package com.voltexmate.app.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("voltexmate", Context.MODE_PRIVATE)

    var demo: Boolean
        get() = sp.getBoolean("demo", false)
        set(v) { sp.edit().putBoolean("demo", v).apply() }

    var loggedIn: Boolean
        get() = sp.getBoolean("logged_in", false)
        set(v) { sp.edit().putBoolean("logged_in", v).apply() }
}

object AvatarStore {
    private fun files(ctx: Context): List<File> =
        ctx.filesDir.listFiles()?.filter { it.name.startsWith("avatar_") }.orEmpty()

    fun current(ctx: Context): File? = files(ctx).maxByOrNull { it.lastModified() }

    fun save(ctx: Context, uri: Uri): File {
        val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("이미지를 열 수 없어요")
        clear(ctx)
        val f = File(ctx.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
        f.writeBytes(bytes)
        return f
    }

    fun clear(ctx: Context) {
        files(ctx).forEach { it.delete() }
    }
}

internal fun ChartScore.toJson(): JSONObject = JSONObject()
    .put("t", title).put("a", artist).put("d", difficulty.name)
    .put("l", level).put("s", score).put("x", exScore ?: -1).put("c", clear.name)

internal fun JSONObject.toChart(): ChartScore = ChartScore(
    title = getString("t"),
    artist = optString("a"),
    difficulty = Difficulty.valueOf(getString("d")),
    level = optDouble("l", 0.0),
    score = optInt("s"),
    exScore = optInt("x", -1).takeIf { it >= 0 },
    clear = runCatching { ClearMark.valueOf(optString("c")) }.getOrDefault(ClearMark.NONE),
)

private fun readArray(f: File): List<JSONObject> = runCatching {
    val a = JSONArray(f.readText())
    (0 until a.length()).map { a.getJSONObject(it) }
}.getOrDefault(emptyList())

/** CSV 로 가져온 스코어 + 가져올 때마다 비교한 갱신 기록 */
class ScoreStore(context: Context) {
    private val scoresFile = File(context.filesDir, "scores.json")
    private val updatesFile = File(context.filesDir, "updates.json")

    @Synchronized
    fun scores(): List<ChartScore> = runCatching { readArray(scoresFile).map { it.toChart() } }.getOrDefault(emptyList())

    @Synchronized
    fun updates(): List<ScoreUpdate> = runCatching {
        readArray(updatesFile).map { o ->
            ScoreUpdate(
                at = o.getLong("at"),
                chart = o.getJSONObject("c").toChart(),
                prevScore = o.optInt("ps", -1).takeIf { it >= 0 },
                prevClear = runCatching { ClearMark.valueOf(o.optString("pc")) }.getOrNull(),
            )
        }
    }.getOrDefault(emptyList())

    @Synchronized
    fun merge(fresh: List<ChartScore>, now: Long): Pair<List<ChartScore>, List<ScoreUpdate>> {
        val old = scores().associateBy { it.key }
        val newUpdates = if (old.isEmpty()) emptyList() else fresh.mapNotNull { c ->
            val o = old[c.key]
            when {
                o == null -> if (c.score > 0) ScoreUpdate(now, c, null, null) else null
                c.score > o.score || c.clear.ordinal > o.clear.ordinal -> ScoreUpdate(now, c, o.score, o.clear)
                else -> null
            }
        }
        val merged = LinkedHashMap(old)
        fresh.forEach { merged[it.key] = it }
        val all = merged.values.toList()
        val ups = (newUpdates + updates()).take(MAX_UPDATES)
        scoresFile.writeText(JSONArray(all.map { it.toJson() }).toString())
        updatesFile.writeText(JSONArray(ups.map { u ->
            JSONObject().put("at", u.at).put("c", u.chart.toJson())
                .put("ps", u.prevScore ?: -1).put("pc", u.prevClear?.name ?: "")
        }).toString())
        return all to ups
    }

    @Synchronized
    fun clear() {
        scoresFile.delete()
        updatesFile.delete()
    }

    private companion object {
        const val MAX_UPDATES = 300
    }
}

/** 공식 매칭 이력은 최근 20곡뿐이라 받아올 때마다 누적 저장 */
class MatchLogStore(context: Context) {
    private val file = File(context.filesDir, "match_log.json")

    @Synchronized
    fun load(): List<MatchingEntry> = runCatching { readArray(file).map { it.toEntry() } }.getOrDefault(emptyList())

    @Synchronized
    fun merge(fresh: List<MatchingEntry>): List<MatchingEntry> {
        val all = (fresh + load()).distinctBy { it.key }.sortedByDescending { it.playedAt }.take(MAX)
        file.writeText(JSONArray(all.map { it.toJson() }).toString())
        return all
    }

    @Synchronized
    fun clear() {
        file.delete()
    }

    private fun MatchingEntry.toJson(): JSONObject = JSONObject()
        .put("t", playedAt).put("title", title).put("artist", artist)
        .put("opp", JSONArray(opponents.map {
            JSONObject().put("n", it.name).put("id", it.playerId ?: "").put("s", it.skill ?: "")
        }))

    private fun JSONObject.toEntry(): MatchingEntry {
        val arr = optJSONArray("opp") ?: JSONArray()
        return MatchingEntry(
            playedAt = getString("t"),
            title = getString("title"),
            artist = optString("artist"),
            opponents = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Opponent(o.getString("n"), o.optString("id").ifEmpty { null }, o.optString("s").ifEmpty { null })
            },
        )
    }

    private companion object {
        const val MAX = 1000
    }
}
