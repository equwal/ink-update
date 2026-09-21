// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (c) 2026 equwal
package dev.equwal.inkupdate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/** [Version.parse] and the order of two versions. */
class VersionTest {

    private fun v(text: String): Version {
        val p = Version.parse(text)
        assertNotNull("parse failed: " + text, p)
        return p!!
    }

    @Test
    fun readsTheParts() {
        assertEquals(listOf(0, 0, 15, 0), v("v0.0.15-alpha").parts)
        assertEquals(Version.ALPHA, v("v0.0.15-alpha").stage)
        assertEquals(listOf(3, 7, 0, 0), v("V3.7").parts)
        assertEquals(Version.FINAL, v("V3.7").stage)
        assertEquals(listOf(5, 3, 0, 0), v("v5.3.0").parts)
        assertEquals(Version.BETA, v("0.1.0-beta").stage)
        assertEquals(Version.RC, v("2.0-rc1").stage)
        assertEquals(listOf(1, 2, 3, 4), v("1.2.3.4").parts)
    }

    /** A tag and an installed versionName must give the same version. */
    @Test
    fun theTagEqualsTheInstalledName() {
        assertEquals(v("v0.0.15-alpha"), v("0.0.15-alpha-full"))
        assertEquals(v("v0.1.0"), v("0.1.0-debug"))
        assertEquals(v("0.1.0"), v("0.1.0.0"))
    }

    @Test
    fun ordersByNumberThenStage() {
        assertTrue(v("0.1.0-beta") > v("0.0.15-alpha"))
        assertTrue(v("0.1.0") > v("0.1.0-beta"))
        assertTrue(v("0.1.0-beta") > v("0.1.0-alpha"))
        assertTrue(v("0.1.0") > v("0.1.0-rc1"))
        assertTrue(v("v5.3.0") > v("v5.2.11"))
        assertTrue(v("3.7") < v("3.10"))
        assertTrue(v("0.0.16-alpha") > v("0.0.15-alpha-full"))
        assertEquals(0, v("1.2.3").compareTo(v("v1.2.3")))
    }

    @Test
    fun junkIsNotAVersion() {
        for (s in listOf("", "   ", "v", "alpha", "-1.0", "latest", "v.1.2", "x1.2")) {
            assertNull("must be null: " + s, Version.parse(s))
        }
        assertNull(Version.parse(null))
    }

    /** A part of more than nine digits is not a version number. */
    @Test
    fun aVeryLongNumberIsNotAVersion() {
        assertNull(Version.parse("12345678901234567890.1"))
    }

    @Test
    fun parseNeverThrows() {
        val r = Random(20260921L)
        val letters = "0123456789.-vV abcRC+/\\\"{}é中"
        for (i in 0 until 5000) {
            val n = r.nextInt(12)
            val sb = StringBuilder()
            for (j in 0 until n) sb.append(letters[r.nextInt(letters.length)])
            Version.parse(sb.toString())
        }
    }

    // ---- properties --------------------------------------------------------

    private fun random(r: Random): Version = Version(
        List(Version.PARTS) { r.nextInt(12) },
        r.nextInt(4)
    )

    private fun sign(n: Int): Int = if (n < 0) -1 else if (n > 0) 1 else 0

    @Test
    fun compareIsAntisymmetric() {
        val r = Random(4242L)
        for (i in 0 until 20000) {
            val a = random(r)
            val b = random(r)
            assertEquals(
                a.toString() + " vs " + b,
                sign(a.compareTo(b)),
                -sign(b.compareTo(a))
            )
        }
    }

    @Test
    fun compareIsTransitive() {
        val r = Random(99L)
        for (i in 0 until 20000) {
            val a = random(r)
            val b = random(r)
            val c = random(r)
            if (a <= b && b <= c) {
                assertTrue(a.toString() + " " + b + " " + c, a <= c)
            }
        }
    }

    /** Text made from a version parses back to the same version. */
    @Test
    fun theTextRoundTrips() {
        val r = Random(7L)
        for (i in 0 until 20000) {
            val a = random(r)
            assertEquals(a, Version.parse("v" + a))
        }
    }
}
