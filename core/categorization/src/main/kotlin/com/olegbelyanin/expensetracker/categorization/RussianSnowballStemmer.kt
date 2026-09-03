package com.olegbelyanin.expensetracker.categorization

import org.tartarus.snowball.ext.russianStemmer

/**
 * Тонкая обёртка над официальным `russianStemmer` из libstemmer_java 3.1.1.
 * Сгенерированный класс не потокобезопасен — вызовы сериализуются.
 */
internal class RussianSnowballStemmer {
    private val stemmer = russianStemmer()

    fun stem(word: String): String = synchronized(stemmer) {
        stemmer.setCurrent(word)
        stemmer.stem()
        stemmer.getCurrent()
    }
}
