package com.voltexmate.app.data

import java.util.Locale
import java.util.Random

object Demo {
    val profile: ProfileDetail by lazy {
        ProfileDetail(
            playerName = "MOCHI", playerId = "SV-0000-0000", appealTitle = "말랑말랑 볼테러", apcardUrl = null,
            volforce = 17.737, vfClassId = 7, vfTier = 3, skillCode = "none", skillName = null,
            arenaCode = "none", arenaPower = 0, playCount = 20, pc = 8000, blc = 12677, blasterPass = false,
            lastPlayedAt = "2025/12/26 20:48:28", lastShop = "GAMEPLAZA VM", streakDays = 3, streakWeeks = 1,
            clearTables = listOf(
                ClearTable(
                    "effect01", "클리어",
                    listOf("mark_play", "mark_comp", "mark_comp_ex", "mark_comp_max", "mark_uc", "mark_per", "total"),
                    listOf(
                        ClearRow("NOV", listOf(12, 0, 0, 0, 0, 0, 12), false),
                        ClearRow("ADV", listOf(70, 115, 39, 0, 4, 0, 228), false),
                        ClearRow("EXH", listOf(85, 260, 99, 0, 18, 0, 462), false),
                        ClearRow("MXM", listOf(68, 120, 47, 0, 1, 0, 236), false),
                        ClearRow("INF~NBL", listOf(21, 23, 4, 0, 0, 0, 48), false),
                        ClearRow("ULT", listOf(0, 0, 0, 0, 0, 0, 0), false),
                        ClearRow("합계", listOf(256, 518, 189, 0, 23, 0, 986), true),
                    ),
                ),
            ),
            matching = listOf(
                MatchingEntry("2025/12/26 20:48:09", "EGOISM -Rebuild-", "HAMA topground", emptyList()),
                MatchingEntry("2025/12/26 20:45:18", "Lisa-RICCIA", "DJ YOSHITAKA", emptyList()),
                MatchingEntry(
                    "2025/12/26 20:34:45", "voltississimo", "BEMANI Sound Team \"PHQUASE\"",
                    listOf(Opponent("RIVAL-A", "SV-1111-1111", "SKILL Lv10"), Opponent("RIVAL-B", "SV-2222-2222", "SKILL Lv∞")),
                ),
                MatchingEntry("2025/12/25 21:02:06", "Nebulas", "BEMANI Sound Team \"Yvya\"", emptyList()),
            ),
        )
    }

    val scores: List<ChartScore> by lazy {
        val titles = listOf(
            "EGOISM -Rebuild-", "Lisa-RICCIA", "voltississimo", "Nebulas", "HE4VEN", "XROSS INFECTION",
            "Blastix Riotz", "iLLness LiLin", "Max Burning!!", "Destr0yer", "VALLIS-NERIA", "Far east nightbird",
            "INF-B《L-aste》", "unfinished", "KHAMEN BREAK", "Lachryma《Re:Queen'M》", "鬼華-修羅の舞-",
            "ΣmbryØ", "Opium and Purple haze", "Growth Memories",
        )
        val diffs = listOf(Difficulty.EXH, Difficulty.MXM, Difficulty.INF, Difficulty.GRV, Difficulty.HVN, Difficulty.VVD, Difficulty.XCD, Difficulty.NBL)
        val levels = listOf(16.0, 17.0, 17.5, 18.0, 18.3, 18.6, 19.0, 19.2)
        val clears = listOf(ClearMark.PLAYED, ClearMark.COMP, ClearMark.COMP, ClearMark.EX_COMP, ClearMark.UC)
        val r = Random(573)
        (0 until 90).map { i ->
            val suffix = if (i >= titles.size) " (${i / titles.size + 1})" else ""
            ChartScore(
                title = titles[i % titles.size] + suffix,
                artist = "DEMO ARTIST",
                difficulty = diffs[i % diffs.size],
                level = levels[r.nextInt(levels.size)],
                score = 9_300_000 + r.nextInt(700_000),
                exScore = null,
                clear = clears[r.nextInt(clears.size)],
            )
        }
    }

    val updates: List<ScoreUpdate> by lazy {
        val now = System.currentTimeMillis()
        scores.take(6).mapIndexed { i, c ->
            ScoreUpdate(
                at = now - i * 7 * 3_600_000L,
                chart = c,
                prevScore = if (i % 3 == 0) null else c.score - 23_450 * (i + 1),
                prevClear = if (i % 3 == 0) null else if (i % 2 == 0) ClearMark.PLAYED else c.clear,
            )
        }
    }

    fun boards(kind: RankingKind): List<RankingBoard> = listOf(
        RankingBoard(
            when (kind) {
                RankingKind.SCORE -> "스코어 랭킹 (데모)"
                RankingKind.WEEKLY -> "이번 주 과제곡 (데모)"
                RankingKind.BATTLE -> "배틀 랭킹 (데모)"
            },
            (1..20).map { i ->
                BoardEntry(
                    rank = i,
                    name = if (i == 7) "MOCHI" else "PLAYER$i",
                    playerId = String.format(Locale.US, "SV-%04d-%04d", i * 37, i * 91),
                    value = if (kind == RankingKind.BATTLE) "${3000 - i * 55} pt"
                    else String.format(Locale.US, "%,d", 10_000_000 - i * 1234),
                    extra = null,
                    isMe = i == 7,
                )
            },
        ),
    )

    fun songPage(q: SongQuery): SongPage {
        val key = q.keyword.trim()
        val all = scores.map { it.title.substringBefore(" (") }.distinct()
            .filter { key.isEmpty() || it.contains(key, ignoreCase = true) }
        val pages = ((all.size + 9) / 10).coerceAtLeast(1)
        val p = q.page.coerceIn(1, pages)
        val songs = all.drop((p - 1) * 10).take(10).map { t ->
            SongItem(t, "DEMO ARTIST", null, listOf("NOV", "ADV", "EXH", "MXM").map { ChartLink(it, Urls.RANKING + "#demo-$it") })
        }
        return SongPage(songs, p, pages)
    }
}
