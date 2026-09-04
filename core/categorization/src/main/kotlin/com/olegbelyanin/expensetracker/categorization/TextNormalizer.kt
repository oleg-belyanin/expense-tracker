package com.olegbelyanin.expensetracker.categorization

import com.olegbelyanin.expensetracker.model.KeywordKind
import java.text.Normalizer
import java.util.Locale

data class KeywordFeature(val value: String, val kind: KeywordKind)

data class NormalizationResult(val normalizedName: String, val features: List<KeywordFeature>)

/**
 * Pipeline §6.1 AD-CAT-001: NFKC, lowercase, стоп-слова и русский Snowball
 * для обычных слов. Фразы в кавычках не стеммируются.
 */
class TextNormalizer(
    private val stopWords: Set<String> = DEFAULT_STOP_WORDS,
    private val mapYoToYe: Boolean = true,
    private val stemExceptions: Set<String> = DEFAULT_STEM_EXCEPTIONS,
) {
    private val stemmer = RussianSnowballStemmer()

    fun normalizePlain(raw: String): String = prepare(raw)

    /** Токены без стемминга — для prefix-поиска по первым буквам. */
    fun plainTokens(raw: String): List<String> = tokenize(prepare(raw)).filter { it !in stopWords }

    fun analyze(raw: String): NormalizationResult {
        val prepared = prepare(raw)
        val nameParts = mutableListOf<String>()
        val wordFeatures = linkedSetOf<String>()
        val phraseFeatures = linkedSetOf<String>()
        var lastIndex = 0
        for (match in QUOTE_REGEX.findAll(prepared)) {
            emitWords(prepared.substring(lastIndex, match.range.first), nameParts, wordFeatures)
            val phrase = collapseSpaces(match.groupValues[1])
            if (phrase.isNotEmpty()) {
                nameParts += "\"$phrase\""
                phraseFeatures += phrase
            }
            lastIndex = match.range.last + 1
        }
        emitWords(prepared.substring(lastIndex), nameParts, wordFeatures)
        val features = buildList {
            wordFeatures.forEach { add(KeywordFeature(it, KeywordKind.WORD)) }
            phraseFeatures.forEach { add(KeywordFeature(it, KeywordKind.PHRASE)) }
        }
        return NormalizationResult(
            normalizedName = nameParts.joinToString(" "),
            features = features,
        )
    }

    private fun emitWords(text: String, nameParts: MutableList<String>, wordFeatures: MutableSet<String>) {
        for (word in tokenize(text)) {
            if (word in stopWords) continue
            val stemmed = stemWord(word)
            if (stemmed.isEmpty()) continue
            nameParts += stemmed
            wordFeatures += stemmed
        }
    }

    private fun stemWord(word: String): String {
        if (word in stemExceptions || !hasCyrillic(word)) return word
        return stemmer.stem(word)
    }

    private fun prepare(raw: String): String = collapseSpaces(canonicalize(raw))

    private fun canonicalize(raw: String): String {
        var value = Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC)
        value = value.lowercase(Locale.ROOT)
        if (mapYoToYe) {
            value = value.replace('ё', 'е')
        }
        return value
    }

    private fun tokenize(text: String): List<String> = TOKEN_REGEX.findAll(text).map { it.value }.toList()

    private fun collapseSpaces(value: String): String = value.trim().replace(WHITESPACE_REGEX, " ")

    companion object {
        const val VERSION = 1

        val DEFAULT_STOP_WORDS: Set<String> = setOf(
            "а", "бы", "в", "во", "для", "до", "же", "и", "из", "или",
            "к", "ко", "ли", "на", "не", "но", "о", "об", "от", "по",
            "про", "с", "со", "у",
        )

        val DEFAULT_STEM_EXCEPTIONS: Set<String> = emptySet()

        private val WHITESPACE_REGEX = Regex("\\s+")
        private val QUOTE_REGEX = Regex("[\"«»“”](.+?)[\"«»“”]")
        private val TOKEN_REGEX = Regex("[\\p{L}\\p{N}]+")

        private fun hasCyrillic(word: String): Boolean = word.any { it in '\u0400'..'\u04FF' }
    }
}
