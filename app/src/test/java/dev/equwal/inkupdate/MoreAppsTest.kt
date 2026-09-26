// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (c) 2026 equwal
package dev.equwal.inkupdate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The list of the More apps section. */
class MoreAppsTest {

    @Test fun `the list leaves out Ink Update`() {
        assertFalse(MORE_APPS.any { "equwal/ink-update" in it.url })
    }

    @Test fun `each link opens an https page`() {
        for (app in MORE_APPS) assertTrue(app.url, app.url.startsWith("https://"))
    }

    @Test fun `the list keeps the order of the catalog`() {
        assertEquals(
            listOf("https://subread.space/", "https://booksimulator.com/", "https://honjimaku.com/", "https://sbmsync.com/"),
            MORE_APPS.take(4).map { it.url }
        )
        assertEquals("https://recentlywritten.com/projects.html", MORE_APPS.last().url)
        // The catalog has 15 entries. Ink Update is the one that is not in the list.
        assertEquals(14, MORE_APPS.size)
    }
}
