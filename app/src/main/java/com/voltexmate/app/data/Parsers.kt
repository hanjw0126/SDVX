package com.voltexmate.app.data

import org.json.JSONObject
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.text.Normalizer

private val WS = Regex("\\s+")
private val INT = Regex("""\d[\d,]*""")
private val NON_DIGIT = Regex("[^0-9]")
private val NON_NUM = Regex("[^0-9.]")

internal fun String?.clean(): String = orEmpty().replace('\u00a0', ' ').replace(WS, " ").trim()
internal fun String.nfkc(): String = Normalizer.normalize(this, Normalizer.Form.NFKC)
internal fun String.firstInt(): Int? = INT.find(this)?.value?.replace(",", "")?.toIntOrNull()
internal fun Element.imgSrc(): String? =
    selectFirst("img")?.let { img -> img.absUrl("src").ifEmpty { img.attr("src") } }

class ParseError(msg: String) : Exception(msg)

/** <script>ea_common_template.userstatus={...};</script> 의 state */
object UserStatusParser {
    private val RE = Regex(
        """ea_common_template\.userstatus\s*=\s*(\{.*?\})\s*;\s*</script>""",
        RegexOption.DOT_MATCHES_ALL,
    )

    fun parse(html: String): UserState? {
        val json = RE.find(html)?.groupValues?.get(1) ?: return null
        return runCatching {
            val st = JSONObject(json).getJSONObject("state")
            val courses = st.optJSONObject("course")?.keys()?.asSequence()?.toSet().orEmpty()
            UserState(
                login = st.optBoolean("login"),
                eapass = st.optBoolean("eapass"),
                playdata = st.optBoolean("playdata"),
                subscription = st.optBoolean("subscription"),
                courses = courses,
            )
        }.getOrNull()
    }
}

object ProfileParser {
    private val TABLES = listOf(
        "effect01" to "클리어",
        "effect02" to "그레이드",
        "level01" to "Lv 클리어",
        "level02" to "Lv 그레이드",
    )

    fun parse(doc: Document): ProfileDetail {
        val names = doc.select("#player_name > p").map { it.text().clean() }.filter { it.isNotEmpty() }
        val playerName = names.lastOrNull()
            ?: throw ParseError("프로필에서 플레이어 이름을 찾지 못했어요 (#player_name)")

        val items = doc.select("#profile_li > li").map { li -> li.selectFirst(".profile_col")?.text().clean() to li }
        fun item(label: String): Element? = items.firstOrNull { it.first.contains(label, ignoreCase = true) }?.second
        fun value(label: String): String? = item(label)?.selectFirst(".profile_cnt")?.text().clean().ifEmpty { null }

        val skillEl = item("スキル")?.selectFirst(".profile_skill")
        val arenaEl = doc.selectFirst("#profile_li .profile_arena")

        return ProfileDetail(
            playerName = playerName,
            playerId = doc.selectFirst("#player_id")?.text().clean(),
            appealTitle = if (names.size >= 2) names.first() else null,
            apcardUrl = doc.selectFirst("#apcard")?.imgSrc(),
            volforce = doc.selectFirst("#force_point")?.text().clean().toDoubleOrNull() ?: 0.0,
            vfClassId = doc.selectFirst("#force .force_class")?.id()?.firstInt(),
            vfTier = doc.selectFirst("#force .force_level")?.id()?.firstInt(),
            skillCode = skillEl?.classNames()?.firstOrNull { it.startsWith("skill_") }?.removePrefix("skill_"),
            skillName = skillEl?.text().clean().ifEmpty { null },
            arenaCode = arenaEl?.classNames()?.firstOrNull { it.startsWith("arena_") }?.removePrefix("arena_"),
            arenaPower = item("ARENA POWER")?.selectFirst(".profile_cnt p")?.text()?.firstInt(),
            playCount = value("プレー回数")?.firstInt(),
            pc = value("所持PC")?.firstInt(),
            blc = value("所持BLC")?.firstInt(),
            blasterPass = when {
                doc.selectFirst("#bpass_act") != null -> true
                doc.selectFirst("#bpass_dis") != null -> false
                else -> null
            },
            lastPlayedAt = value("最終プレー日時"),
            lastShop = value("最終プレー店舗")?.nfkc(),
            streakDays = value("連続プレー日数")?.firstInt(),
            streakWeeks = value("連続プレー週数")?.firstInt(),
            clearTables = clearTables(doc),
            matching = matching(doc),
        )
    }

    fun clearTables(doc: Document): List<ClearTable> = TABLES.mapNotNull { (id, title) ->
        val trs = doc.select("#$id table tr")
        if (trs.size < 2) return@mapNotNull null
        val cols = trs[0].select("th").drop(1).map { th ->
            th.selectFirst("img")?.attr("src")?.substringAfterLast('/')?.substringBefore('.')?.removePrefix("rival_")
                ?: if (th.text().contains("合計")) "total" else th.text().clean()
        }
        val rows = trs.drop(1).map { tr ->
            val th = tr.selectFirst("th")
            val total = th?.hasClass("sum") == true
            val label = when {
                total -> "합계"
                th?.selectFirst(".inf_grv_hvn") != null -> "INF~NBL"
                else -> th?.text().clean()
            }
            ClearRow(label, tr.select("td").map { it.text().firstInt() ?: 0 }, total)
        }
        ClearTable(id, title, cols, rows)
    }

    fun matching(doc: Document): List<MatchingEntry> =
        doc.select("#Matching_History #pc_table > .cat").mapNotNull { cat ->
            val inner = cat.children().filter { it.hasClass("inner") }
            val title = inner.getOrNull(1)?.selectFirst("p.music_name")?.text().clean()
            if (title.isEmpty()) return@mapNotNull null
            fun col(i: Int): List<String> = inner.getOrNull(i)?.select("p")?.map { it.text().clean() }.orEmpty()
            val names = col(2)
            val ids = col(3)
            val skills = col(4)
            val opponents = names.mapIndexedNotNull { i, n ->
                if (n.isEmpty()) null
                else Opponent(n, ids.getOrNull(i)?.ifEmpty { null }, skills.getOrNull(i)?.ifEmpty { null })
            }
            MatchingEntry(
                playedAt = inner[0].text().clean(),
                title = title,
                artist = inner[1].select("p").getOrNull(1)?.text().clean(),
                opponents = opponents,
            )
        }
}

/** 랭킹 페이지 범용 파서 (실제 HTML 미확인): 첫 칸이 순위인 행을 찾는다 */
object BoardParser {
    private val PID = Regex("""SV-\d{4}-\d{4}""")

    fun parse(doc: Document, myId: String?): List<RankingBoard> {
        val root = doc.selectFirst("#main") ?: doc.body()
        val boards = root.select("table").mapNotNull { table ->
            val entries = table.select("tr").mapNotNull { toEntry(it, it.children(), myId) }
            if (entries.size >= 3) RankingBoard(titleFor(table), entries) else null
        }
        if (boards.isNotEmpty()) return boards

        val rows = root.select("li, div").filter { el ->
            el.children().size >= 3 && el.children().none { c -> c.children().size >= 3 }
        }
        val entries = rows.mapNotNull { toEntry(it, it.children(), myId) }.distinctBy { it.rank to it.name }
        return if (entries.size >= 3) {
            listOf(RankingBoard(doc.selectFirst(".page-title")?.text().clean().ifEmpty { "랭킹" }, entries))
        } else emptyList()
    }

    private fun toEntry(row: Element, cells: List<Element>, myId: String?): BoardEntry? {
        if (cells.size < 3) return null
        val first = cells[0]
        val rank = first.text().clean().removeSuffix("位").trim().toIntOrNull()
            ?: first.selectFirst("img")?.attr("src")?.substringAfterLast('/')?.firstInt()
            ?: return null
        val texts = cells.drop(1).map { it.text().clean() }.filter { it.isNotEmpty() }
        val pid = PID.find(row.text())?.value
        val name = texts.firstOrNull { t -> !PID.containsMatchIn(t) && t.count { ch -> ch.isDigit() } * 2 <= t.length }
            ?: return null
        val value = texts.filter { it != name && !PID.containsMatchIn(it) }.maxByOrNull { t -> t.count { ch -> ch.isDigit() } }
            ?: return null
        val extra = texts.filter { it != name && it != value && !PID.containsMatchIn(it) }
            .joinToString(" · ").ifEmpty { null }
        return BoardEntry(rank, name, pid, value, extra, pid != null && pid == myId)
    }

    private fun titleFor(el: Element): String {
        var cur: Element? = el
        repeat(6) {
            var sib = cur?.previousElementSibling()
            while (sib != null) {
                val t = (if (sib.hasClass("head-title") || sib.tagName() in setOf("h2", "h3", "h4")) sib.text()
                else sib.selectFirst(".head-title, h2, h3, h4")?.text()).clean()
                if (t.isNotEmpty()) return t
                sib = sib.previousElementSibling()
            }
            cur = cur?.parent()
        }
        return "랭킹"
    }
}

/** 공식 スコアデータ CSV (헤더 키워드로 열을 찾음) */
object CsvParser {
    fun decode(bytes: ByteArray): String {
        val body = if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            bytes.copyOfRange(3, bytes.size)
        } else bytes
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(body)).toString()
        } catch (e: CharacterCodingException) {
            String(body, Charset.forName("windows-31j"))
        }
    }

    fun splitLine(line: String): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        quoted = false
                    }
                } else {
                    sb.append(c)
                }
            } else {
                when (c) {
                    '"' -> quoted = true
                    ',' -> {
                        out.add(sb.toString())
                        sb.setLength(0)
                    }
                    else -> sb.append(c)
                }
            }
            i++
        }
        out.add(sb.toString())
        return out
    }

    private fun find(header: List<String>, keys: List<String>, exclude: List<String> = emptyList()): Int {
        for (k in keys) {
            val i = header.indexOfFirst { h ->
                h.contains(k, ignoreCase = true) && exclude.none { ex -> h.contains(ex, ignoreCase = true) }
            }
            if (i >= 0) return i
        }
        return -1
    }

    fun parse(text: String): List<ChartScore> {
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').split('\n').filter { it.isNotBlank() }
        if (lines.size < 2) throw ParseError("CSV에 데이터가 없어요")
        val header = splitLine(lines[0]).map { it.trim().nfkc() }
        val iTitle = find(header, listOf("楽曲名", "曲名", "TITLE", "MUSIC"))
        val iArtist = find(header, listOf("アーティスト", "ARTIST"))
        val iDiff = find(header, listOf("難易度", "DIFFICULTY", "DIFF"))
        val iLevel = find(header, listOf("楽曲レベル", "レベル", "LEVEL"))
        val iClear = find(header, listOf("クリアランク", "クリアマーク", "クリア", "CLEAR"), listOf("回数", "COUNT"))
        val iScore = find(header, listOf("ハイスコア", "HIGH SCORE", "スコア", "SCORE"), listOf("EX", "グレード", "GRADE"))
        val iEx = find(header, listOf("EXスコア", "EX SCORE", "EXSCORE"))
        if (iTitle < 0 || iDiff < 0 || iScore < 0) {
            throw ParseError("CSV 형식을 알 수 없어요. 첫 줄: " + lines[0].take(120))
        }
        return lines.drop(1).mapNotNull { line ->
            val cells = splitLine(line).map { it.trim() }
            fun at(i: Int): String = if (i in cells.indices) cells[i] else ""
            val title = at(iTitle)
            if (title.isEmpty()) return@mapNotNull null
            val diff = Difficulty.parse(at(iDiff).nfkc()) ?: return@mapNotNull null
            ChartScore(
                title = title,
                artist = at(iArtist),
                difficulty = diff,
                level = at(iLevel).nfkc().replace(NON_NUM, "").toDoubleOrNull() ?: 0.0,
                score = at(iScore).nfkc().replace(NON_DIGIT, "").toIntOrNull() ?: 0,
                exScore = at(iEx).nfkc().replace(NON_DIGIT, "").toIntOrNull(),
                clear = ClearMark.parse(at(iClear).nfkc()),
            )
        }
    }
}
