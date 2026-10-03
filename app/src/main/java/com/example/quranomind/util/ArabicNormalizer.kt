package com.example.quranomind.util

import java.security.MessageDigest

object ArabicNormalizer {

    private val TASHKEEL_REGEX = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")

    fun normalize(text: String): String {
        var result = text.trim()
        // Remove diacritics / tashkeel
        result = TASHKEEL_REGEX.replace(result, "")
        // Normalize alefs
        result = result.replace(Regex("[\\u0622\\u0623\\u0625]"), "\u0627")
        // Normalize taa marbuta
        result = result.replace('\u0629', '\u0647')
        // Normalize alif maqsura
        result = result.replace('\u0649', '\u064A')
        return result.lowercase()
    }

    fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
