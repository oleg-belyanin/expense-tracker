package com.olegbelyanin.expensetracker.categorization

import com.olegbelyanin.expensetracker.model.KeywordKind
import java.text.Normalizer
import java.util.Locale

data class KeywordFeature(val value: String, val kind: KeywordKind)

data class NormalizationResult(val normalizedName: String, val features: List<KeywordFeature>)

/**
 * Pipeline §6.1 AD-CAT-001 без Snowball (стеммер — этап B3).
 */
class TextNormalizer(private val stopWords: Set<String> = DEFAULT_STOP_WORDS, private val mapYoToYe: Boolean = true) {
    fun normalizePlain(raw: String): String {
        val collapsed = collapseSpaces(canonicalize(raw))
        return collapsed
    }

    fun analyze(raw: String): NormalizationResult {
        val prepared = canonicalize(raw)
        val phrases = mutableListOf<String>()
        val withoutQuotes = QUOTE_REGEX.replace(prepared) { match ->
            val phrase = collapseSpaces(match.groupValues[1])
            if (phrase.isNotEmpty()) {
                phrases += phrase
            }
            " "
        }
        val words = tokenize(withoutQuotes)
            .filterNot { it in stopWords }
            .distinct()
        val features = buildList {
            words.forEach { add(KeywordFeature(it, KeywordKind.WORD)) }
            phrases.distinct().forEach { add(KeywordFeature(it, KeywordKind.PHRASE)) }
        }
        val normalizedName = buildNormalizedName(words, phrases, prepared)
        return NormalizationResult(
            normalizedName = normalizedName,
            features = features,
        )
    }

    private fun canonicalize(raw: String): String {
        var value = Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC)
        value = value.lowercase(Locale.ROOT)
        if (mapYoToYe) {
            value = value.replace('ё', 'е')
        }
        return value
    }

    private fun tokenize(text: String): List<String> = TOKEN_REGEX.findAll(text).map { it.value }.toList()

    private fun buildNormalizedName(words: List<String>, phrases: List<String>, prepared: String): String {
        val phraseQueue = ArrayDeque(phrases)
        val rebuilt = QUOTE_REGEX.replace(prepared) { match ->
            val phrase = collapseSpaces(match.groupValues[1])
            if (phrase.isEmpty()) {
                " "
            } else {
                "\"${phraseQueue.removeFirstOrNull() ?: phrase}\""
            }
        }
        val leftoverWords = tokenize(rebuilt)
            .filterNot { it in stopWords || it.startsWith("\"") }
        val phraseTokens = QUOTE_REGEX.findAll(rebuilt).map { it.value.trim() }
        return collapseSpaces((leftoverWords + phraseTokens).joinToString(" "))
            .ifEmpty { words.firstOrNull() ?: phrases.firstOrNull().orEmpty() }
    }

    private fun collapseSpaces(value: String): String = value.trim().replace(WHITESPACE_REGEX, " ")

    companion object {
        val DEFAULT_STOP_WORDS: Set<String> = setOf(
            "а", "бы", "в", "во", "для", "до", "же", "и", "из", "или",
            "к", "ко", "ли", "на", "не", "но", "о", "об", "от", "по",
            "про", "с", "со", "у",
        )
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val QUOTE_REGEX = Regex("[\"«»“”](.+?)[\"«»“”]")
        private val TOKEN_REGEX = Regex("[\\p{L}\\p{N}]+")
    }
}
