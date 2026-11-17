package com.example.data.ai

import java.util.Locale

sealed interface LivePdfCommand {
    data class JumpToPage(val page: Int) : LivePdfCommand
    data class StepPage(val delta: Int) : LivePdfCommand
}

/**
 * Parses Turkish voice transcripts and typed Live prompts into PDF navigation commands.
 * Supports:
 * - Explicit digit jumps: "sayfa 46", "46. sayfa", "s.46", "37'ye geçebilir misin?", "46 ya git"
 * - Turkish number words & ordinals: "kırk altıncı sayfa", "otuz yediye geç", "on beşi aç"
 * - Relative navigation: "sonraki sayfa", "sayfayı ilerlet", "bir sonraki", "önceki sayfa", "3 sayfa ileri"
 */
object LivePdfCommandParser {

    private val TR_LOCALE = Locale.forLanguageTag("tr-TR")

    private val NAV_HINT_REGEX = Regex(
        """\b(sayfa\w*|s\.|geç\w*|git\w*|aç\w*|dön\w*|atla\w*|çevir\w*|bak\w*|gel\w*|iler\w*|geri\w*)""",
        RegexOption.IGNORE_CASE
    )

    private val FORWARD_REL_REGEX = Regex(
        """(?:bir\s+)?(?:sonraki|öbür|diğer|sıradaki|peşindeki)\s+sayfa\w*|""" +
            """sayfa(?:yı|yi|ya|da|dan)?\s*(?:bir\s+)?(?:ilerlet\w*|ileri\s*(?:al|geç|git|çevir|atla)?|sonraki(?:ne)?\s*(?:geç|git)|atla\w*|geç\w*)|""" +
            """(?:bir\s+sayfa\s+(?:ileri|sonra)\w*)|""" +
            """(?:ileri|sonraki)\s+sayfa\w*\s*(?:geç\w*|git\w*|aç\w*)?"""
    )

    private val BACKWARD_REL_REGEX = Regex(
        """(?:bir\s+)?(?:önceki|evvelki|gerideki)\s+sayfa\w*|""" +
            """sayfa(?:yı|yi|ya|da|dan)?\s*(?:bir\s+)?(?:geri(?:\s*(?:al|getir|çevir|dön|git))?)|""" +
            """(?:bir\s+sayfa\s+geri\w*)|""" +
            """(?:geri|önceki)\s+sayfa\w*\s*(?:geç\w*|git\w*|dön\w*|aç\w*)?"""
    )

    private val MULTI_STEP_REGEX = Regex(
        """(\d+)\s+sayfa\s+(iler\w*|sonra|geri\w*|önce)"""
    )

    private val DIGIT_PAGE_REGEXES = listOf(
        // "sayfa 46", "sayfaya 46", "sayfa: 46", "s. 46", "s.46"
        Regex("""(?:sayfa\w*|s\.)\s*[:\-]?\s*(\d{1,4})\b"""),
        // "46. sayfa", "46 ncı sayfa", "46'ıncı sayfa", "46 nolu sayfa", "46 numaralı sayfa"
        Regex("""\b(\d{1,4})\s*(?:\.|['’]?(?:[ıiuü]?nc[ıiuü]|nolu|numaralı))?\s*sayfa\w*"""),
        // "37'ye geçebilir misin", "46 ya git", "12'yi aç", "5 e dön", "8 i atla"
        Regex("""\b(\d{1,4})\s*(?:['’]?\s*(?:y?[ae]|y?[ıiuü]|n?[ıiuü]?n))?\s*(?:nolu\s+|numaralı\s+)?(?:sayfa\w*\s*)?(?:geç\w*|git\w*|dön\w*|aç\w*|atla\w*|çevir\w*|bak\w*)""")
    )

    private val UNITS = mapOf(
        "bir" to 1, "birinci" to 1,
        "iki" to 2, "ikinci" to 2,
        "üç" to 3, "üçüncü" to 3,
        "dört" to 4, "dördüncü" to 4,
        "beş" to 5, "beşinci" to 5,
        "altı" to 6, "altıncı" to 6,
        "yedi" to 7, "yedinci" to 7,
        "sekiz" to 8, "sekizinci" to 8,
        "dokuz" to 9, "dokuzuncu" to 9
    )

    private val TENS = mapOf(
        "on" to 10, "onuncu" to 10,
        "yirmi" to 20, "yirminci" to 20,
        "otuz" to 30, "otuzuncu" to 30,
        "kırk" to 40, "kırkıncı" to 40,
        "elli" to 50, "ellinci" to 50,
        "altmış" to 60, "altmışıncı" to 60,
        "yetmiş" to 70, "yetmişinci" to 70,
        "seksen" to 80, "sekseninci" to 80,
        "doksan" to 90, "doksanıncı" to 90
    )

    private val HUNDREDS = mapOf(
        "yüz" to 100, "yüzüncü" to 100
    )

    private val CASE_SUFFIXES = listOf(
        "ıncı", "inci", "uncu", "üncü", "ncı", "nci", "ncu", "ncü",
        "ya", "ye", "yı", "yi", "yu", "yü", "da", "de", "ta", "te"
    )

    fun parse(text: String): LivePdfCommand? {
        val raw = text.trim()
        if (raw.isBlank()) return null

        val cleaned = raw
            .lowercase(TR_LOCALE)
            .replace('’', '\'')
            .replace(Regex("""\s+"""), " ")

        // 1. Multi-page relative steps like "5 sayfa ileri" / "3 sayfa geri"
        MULTI_STEP_REGEX.findAll(cleaned).lastOrNull()?.let { match ->
            val count = match.groupValues[1].toIntOrNull()
            val dir = match.groupValues[2]
            if (count != null && count in 1..500) {
                val sign = if (dir.startsWith("geri") || dir.startsWith("önce")) -1 else 1
                return LivePdfCommand.StepPage(sign * count)
            }
        }

        // 2. Explicit digit page jump (check most recent command in utterance)
        var bestDigitMatch: Pair<Int, Int>? = null // index -> pageNumber
        for (regex in DIGIT_PAGE_REGEXES) {
            for (match in regex.findAll(cleaned)) {
                val pageNum = match.groupValues[1].toIntOrNull()
                if (pageNum != null && pageNum in 1..10000) {
                    val pos = match.range.last
                    if (bestDigitMatch == null || pos >= bestDigitMatch.first) {
                        bestDigitMatch = pos to pageNum
                    }
                }
            }
        }
        if (bestDigitMatch != null) {
            return LivePdfCommand.JumpToPage(bestDigitMatch.second)
        }

        // 3. Turkish number words paired with page / navigation cues ("kırk altıncı sayfa", "otuz yediye geç")
        if (NAV_HINT_REGEX.containsMatchIn(cleaned)) {
            extractTurkishNumberWords(cleaned)?.let { pageNum ->
                if (pageNum in 1..10000) {
                    return LivePdfCommand.JumpToPage(pageNum)
                }
            }
        }

        // 4. Relative single-page advance / step back
        val lastForward = FORWARD_REL_REGEX.findAll(cleaned).lastOrNull()?.range?.last ?: -1
        val lastBackward = BACKWARD_REL_REGEX.findAll(cleaned).lastOrNull()?.range?.last ?: -1
        if (lastForward >= 0 || lastBackward >= 0) {
            return if (lastForward >= lastBackward) {
                LivePdfCommand.StepPage(1)
            } else {
                LivePdfCommand.StepPage(-1)
            }
        }

        return null
    }

    private fun extractTurkishNumberWords(cleaned: String): Int? {
        val tokens = cleaned
            .replace(Regex("""[^\p{L}\d\s']"""), " ")
            .split(" ")
            .filter { it.isNotBlank() }

        var lastNumber: Int? = null
        var current = 0
        var hasNumberToken = false

        for (rawTok in tokens) {
            val word = normalizeNumberWord(rawTok)
            val hundred = HUNDREDS[word]
            val ten = TENS[word]
            val unit = UNITS[word]

            when {
                hundred != null -> {
                    current = if (current in 1..9) current * 100 else current + 100
                    hasNumberToken = true
                }
                ten != null -> {
                    current += ten
                    hasNumberToken = true
                }
                unit != null -> {
                    current += unit
                    hasNumberToken = true
                }
                else -> {
                    if (hasNumberToken && current > 0) {
                        lastNumber = current
                    }
                    current = 0
                    hasNumberToken = false
                }
            }
        }
        if (hasNumberToken && current > 0) {
            lastNumber = current
        }
        return lastNumber
    }

    private fun normalizeNumberWord(rawToken: String): String {
        val noApostrophe = rawToken.substringBefore('\'')
        if (UNITS.containsKey(noApostrophe) || TENS.containsKey(noApostrophe) || HUNDREDS.containsKey(noApostrophe)) {
            return noApostrophe
        }
        for (suffix in CASE_SUFFIXES) {
            if (noApostrophe.length > suffix.length + 1 && noApostrophe.endsWith(suffix)) {
                val stem = noApostrophe.dropLast(suffix.length)
                if (UNITS.containsKey(stem) || TENS.containsKey(stem) || HUNDREDS.containsKey(stem)) {
                    return stem
                }
            }
        }
        // Handle consonant softening for "dört" -> "dörd..."
        if (noApostrophe.startsWith("dörd")) return "dört"
        return noApostrophe
    }
}
