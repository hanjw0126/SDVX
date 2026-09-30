package com.voltexmate.app.data

import java.util.Locale
import kotlin.math.floor

enum class Difficulty(val short: String, val color: Long) {
    NOV("NOV", 0xFFB667FA),
    ADV("ADV", 0xFFE0A800),
    EXH("EXH", 0xFFF03A4A),
    MXM("MXM", 0xFF8D93A0),
    INF("INF", 0xFFE040C8),
    GRV("GRV", 0xFFFF8A1E),
    HVN("HVN", 0xFF3FB8E8),
    VVD("VVD", 0xFFFF5FA8),
    XCD("XCD", 0xFF4B7BFF),
    NBL("NBL", 0xFF7A5CFF),
    ULT("ULT", 0xFFC9A227);

    companion object {
        fun parse(raw: String): Difficulty? {
            val u = raw.trim().uppercase(Locale.ROOT)
            return when {
                u.startsWith("NOV") -> NOV
                u.startsWith("ADV") -> ADV
                u.startsWith("EXH") -> EXH
                u.startsWith("MXM") || u.startsWith("MAX") -> MXM
                u.startsWith("INF") -> INF
                u.startsWith("GRV") || u.startsWith("GRA") -> GRV
                u.startsWith("HVN") || u.startsWith("HEA") -> HVN
                u.startsWith("VVD") || u.startsWith("VIV") -> VVD
                u.startsWith("XCD") || u.startsWith("EXC") -> XCD
                u.startsWith("NBL") || u.startsWith("NAB") -> NBL
                u.startsWith("ULT") -> ULT
                else -> null
            }
        }
    }
}

/** MAXXIVE 계수 1.04 는 추정값 */
enum class ClearMark(val label: String, val short: String, val coef: Double, val color: Long) {
    NONE("NO PLAY", "-", 0.0, 0xFFBDBAD0),
    PLAYED("PLAYED", "PLAY", 0.5, 0xFF9A96B5),
    COMP("COMPLETE", "COMP", 1.0, 0xFF3CB6A0),
    EX_COMP("EXCESSIVE COMPLETE", "EX", 1.02, 0xFFFF6F9C),
    MAXXIVE("MAXXIVE COMPLETE", "MXV", 1.04, 0xFF9B6BFF),
    UC("ULTIMATE CHAIN", "UC", 1.05, 0xFFFF9D2E),
    PUC("PERFECT", "PUC", 1.10, 0xFFE6B800);

    companion object {
        fun parse(raw: String): ClearMark {
            val u = raw.uppercase(Locale.ROOT).replace(" ", "")
            return when {
                u.isEmpty() || u.contains("NOPLAY") -> NONE
                u.contains("PERFECT") || u == "PUC" -> PUC
                u.contains("ULTIMATE") || u == "UC" -> UC
                u.contains("MAXXIVE") || u == "MXV" -> MAXXIVE
                u.contains("EXCESSIVE") || u.startsWith("EX") || u.contains("HARD") -> EX_COMP
                u.contains("COMP") || u.contains("CLEAR") -> COMP
                u.contains("PLAY") || u.contains("FAIL") || u.contains("CRASH") -> PLAYED
                else -> NONE
            }
        }
    }
}

enum class Grade(val label: String, val coef: Double, val minScore: Int) {
    S("S", 1.05, 9_900_000),
    AAA_P("AAA+", 1.02, 9_800_000),
    AAA("AAA", 1.00, 9_700_000),
    AA_P("AA+", 0.97, 9_500_000),
    AA("AA", 0.94, 9_300_000),
    A_P("A+", 0.91, 9_000_000),
    A("A", 0.88, 8_700_000),
    B("B", 0.85, 7_500_000),
    C("C", 0.82, 6_500_000),
    D("D", 0.80, 0);

    companion object {
        fun of(score: Int): Grade = entries.first { score >= it.minScore }
    }
}

data class ChartScore(
    val title: String,
    val artist: String,
    val difficulty: Difficulty,
    val level: Double,
    val score: Int,
    val exScore: Int?,
    val clear: ClearMark,
) {
    val key: String get() = "$title|${difficulty.name}"
    val grade: Grade get() = Grade.of(score)
    val force: Int get() = Vf.chartForce(level, score, clear)
    val levelText: String
        get() = if (level % 1.0 == 0.0) level.toInt().toString() else String.format(Locale.US, "%.1f", level)
}

object Vf {
    /** floor(레벨 × 점수/1천만 × 등급계수 × 클리어계수 × 20) */
    fun chartForce(level: Double, score: Int, clear: ClearMark): Int {
        if (clear == ClearMark.NONE || score <= 0 || level <= 0.0) return 0
        val g = Grade.of(score).coef
        return floor(level * (score.coerceAtMost(10_000_000) / 10_000_000.0) * g * clear.coef * 20.0).toInt()
    }

    fun top50(scores: List<ChartScore>): List<ChartScore> =
        scores.filter { it.force > 0 }.sortedByDescending { it.force }.take(50)

    fun total(top: List<ChartScore>): Double = top.sumOf { it.force } / 1000.0
}

/** id = 공식 force_icon_XX 번호 */
enum class VfClassInfo(val id: Int, val label: String, val min: Double, val step: Double, val c1: Long, val c2: Long) {
    SIENNA(1, "SIENNA", 0.0, 2.5, 0xFFC08A62, 0xFF8A5A3B),
    COBALT(2, "COBALT", 10.0, 0.5, 0xFF6FA8FF, 0xFF3A5BD9),
    DANDELION(3, "DANDELION", 12.0, 0.5, 0xFFFFE27A, 0xFFF2B233),
    CYAN(4, "CYAN", 14.0, 0.25, 0xFF7FF0F0, 0xFF1FB8C9),
    SCARLET(5, "SCARLET", 15.0, 0.25, 0xFFFF8A8A, 0xFFE02D4B),
    CORAL(6, "CORAL", 16.0, 0.25, 0xFFFFB3C7, 0xFFFF6F9C),
    ARGENTO(7, "ARGENTO", 17.0, 0.25, 0xFFE9ECF3, 0xFF9AA2B6),
    ELDORA(8, "ELDORA", 18.0, 0.25, 0xFFFFE9A3, 0xFFD9A520),
    CRIMSON(9, "CRIMSON", 19.0, 0.25, 0xFFFF7A8E, 0xFFA3122E),
    IMPERIAL(10, "IMPERIAL", 20.0, 1.0, 0xFFD6B8FF, 0xFF6A2FD6);

    companion object {
        fun byId(id: Int?): VfClassInfo? = entries.firstOrNull { it.id == id }
        fun of(vf: Double): VfClassInfo = entries.last { vf >= it.min }
    }
}

data class VfInfo(val value: Double, val cls: VfClassInfo, val tier: Int) {
    val roman: String get() = listOf("I", "II", "III", "IV").getOrElse(tier - 1) { "" }
    val nextAt: Double? get() = if (cls == VfClassInfo.IMPERIAL && tier >= 4) null else cls.min + cls.step * tier
    val progress: Float
        get() {
            val lo = cls.min + cls.step * (tier - 1)
            val hi = nextAt ?: return 1f
            return ((value - lo) / (hi - lo)).toFloat().coerceIn(0f, 1f)
        }

    companion object {
        fun of(v: Double, classId: Int?, tier: Int?): VfInfo {
            val official = VfClassInfo.byId(classId)
            if (official != null && tier != null && tier in 1..4) return VfInfo(v, official, tier)
            val c = VfClassInfo.of(v)
            return VfInfo(v, c, (((v - c.min) / c.step).toInt() + 1).coerceIn(1, 4))
        }
    }
}

data class ProfileDetail(
    val playerName: String,
    val playerId: String,
    val appealTitle: String?,
    val apcardUrl: String?,
    val volforce: Double,
    val vfClassId: Int?,
    val vfTier: Int?,
    val skillCode: String?,
    val skillName: String?,
    val arenaCode: String?,
    val arenaPower: Int?,
    val playCount: Int?,
    val pc: Int?,
    val blc: Int?,
    val blasterPass: Boolean?,
    val lastPlayedAt: String?,
    val lastShop: String?,
    val streakDays: Int?,
    val streakWeeks: Int?,
    val clearTables: List<ClearTable>,
    val matching: List<MatchingEntry>,
) {
    val vf: VfInfo get() = VfInfo.of(volforce, vfClassId, vfTier)
}

data class ClearTable(val id: String, val title: String, val columns: List<String>, val rows: List<ClearRow>)
data class ClearRow(val label: String, val values: List<Int>, val isTotal: Boolean)

data class MatchingEntry(val playedAt: String, val title: String, val artist: String, val opponents: List<Opponent>) {
    val key: String get() = "$playedAt|$title"
}

data class Opponent(val name: String, val playerId: String?, val skill: String?)

data class UserState(
    val login: Boolean,
    val eapass: Boolean,
    val playdata: Boolean,
    val subscription: Boolean,
    val courses: Set<String>,
)

enum class RankingKind(val label: String, val short: String, val url: String) {
    SCORE("스코어 랭킹", "스코어", Urls.RANKING),
    WEEKLY("위클리 스코어 어택", "위클리", Urls.WEEKLY),
    BATTLE("배틀 랭킹", "배틀", Urls.BATTLE),
}

data class RankingBoard(val title: String, val entries: List<BoardEntry>)

data class BoardEntry(
    val rank: Int,
    val name: String,
    val playerId: String?,
    val value: String,
    val extra: String?,
    val isMe: Boolean,
)

data class ScoreUpdate(val at: Long, val chart: ChartScore, val prevScore: Int?, val prevClear: ClearMark?) {
    val isNew: Boolean get() = prevScore == null
    val gain: Int get() = chart.score - (prevScore ?: 0)
    val clearUp: Boolean get() = prevClear != null && chart.clear.ordinal > prevClear.ordinal
}

sealed interface UiLoad<out T> {
    data object Loading : UiLoad<Nothing>
    data class Ok<T>(val data: T) : UiLoad<T>
    data class Fail(val message: String, val sessionExpired: Boolean = false) : UiLoad<Nothing>
}

enum class Session { UNKNOWN, LOGGED_OUT, LOGGED_IN, DEMO }
