package com.thiago.transcribetranslate.translation

import com.google.mlkit.nl.translate.TranslateLanguage

data class TranslationLanguage(
    val code: String,
    val name: String
)

object TranslationLanguages {
    val common = listOf(
        TranslationLanguage(TranslateLanguage.PORTUGUESE, "Português"),
        TranslationLanguage(TranslateLanguage.ENGLISH, "Inglês"),
        TranslationLanguage(TranslateLanguage.SPANISH, "Espanhol"),
        TranslationLanguage(TranslateLanguage.FRENCH, "Francês"),
        TranslationLanguage(TranslateLanguage.GERMAN, "Alemão"),
        TranslationLanguage(TranslateLanguage.ITALIAN, "Italiano"),
        TranslationLanguage(TranslateLanguage.JAPANESE, "Japonês"),
        TranslationLanguage(TranslateLanguage.KOREAN, "Coreano"),
        TranslationLanguage(TranslateLanguage.CHINESE, "Chinês"),
        TranslationLanguage(TranslateLanguage.ARABIC, "Árabe"),
        TranslationLanguage(TranslateLanguage.RUSSIAN, "Russo"),
        TranslationLanguage(TranslateLanguage.HINDI, "Hindi")
    )
}
