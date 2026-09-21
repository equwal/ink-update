// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (c) 2026 equwal
package dev.equwal.inkupdate

/**
 * A version number, with no Android type in it, so the unit tests run on the JVM.
 *
 * The app compares two kinds of text: a git tag ("v0.0.15-alpha") and an
 * installed versionName ("0.0.15-alpha-full"). Both must give the same version.
 *
 * The rules:
 *
 *  - A leading "v" or "V" is not part of the number.
 *  - Up to four numeric parts, separated by full stops. Missing parts are 0.
 *  - The stage is alpha, beta, rc, or none. None is the newest.
 *  - Text after the stage, like "-full" or "-debug", says nothing. It is dropped.
 *  - Text that has no number at the start is not a version. [parse] gives null.
 *
 * [parse] never throws.
 */
data class Version(
    val parts: List<Int>,
    val stage: Int
) : Comparable<Version> {

    override fun compareTo(other: Version): Int {
        for (i in 0 until PARTS) {
            val d = parts[i].compareTo(other.parts[i])
            if (d != 0) return d
        }
        return stage.compareTo(other.stage)
    }

    override fun toString(): String =
        parts.joinToString(".") + STAGE_NAMES[stage]

    companion object {

        /** How many numeric parts a version keeps. */
        const val PARTS = 4

        const val ALPHA = 0
        const val BETA = 1
        const val RC = 2

        /** No stage word. A finished release is newer than its own rc. */
        const val FINAL = 3

        private val STAGE_NAMES = arrayOf("-alpha", "-beta", "-rc", "")

        /** More than this many digits in one part is not a version number. */
        private const val MAX_DIGITS = 9

        fun parse(text: String?): Version? {
            if (text == null) return null
            var s = text.trim()
            if (s.isEmpty()) return null
            if (s[0] == 'v' || s[0] == 'V') s = s.substring(1)

            val parts = ArrayList<Int>(PARTS)
            var i = 0
            while (i < s.length && parts.size < PARTS) {
                var digits = 0
                var value = 0
                while (i < s.length && s[i] in '0'..'9') {
                    if (digits >= MAX_DIGITS) return null
                    value = value * 10 + (s[i] - '0')
                    digits++
                    i++
                }
                if (digits == 0) return null
                parts.add(value)
                if (i < s.length && s[i] == '.') i++ else break
            }
            if (parts.isEmpty()) return null
            while (parts.size < PARTS) parts.add(0)

            return Version(parts, stageOf(s.substring(i)))
        }

        /** The stage word in the text that follows the numbers. */
        private fun stageOf(rest: String): Int {
            val r = rest.lowercase()
            return when {
                r.contains("alpha") -> ALPHA
                r.contains("beta") -> BETA
                r.contains("rc") -> RC
                else -> FINAL
            }
        }

        /** The text to show for a tag: the same tag without the leading "v". */
        fun display(tag: String): String {
            val s = tag.trim()
            return if (s.isNotEmpty() && (s[0] == 'v' || s[0] == 'V')) s.substring(1) else s
        }
    }
}
