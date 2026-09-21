// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (c) 2026 equwal
package dev.equwal.inkupdate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two answers, as the hosts really send them.
 *
 * The F-Droid answers come from https://f-droid.org/api/v1/packages/.
 * The GitHub answer keeps the shape of
 * https://api.github.com/repos/equwal/rebind/releases?per_page=10.
 */
class JsonTest {

    private val page = "https://f-droid.org/packages/org.fdroid.fdroid/"

    // ---- F-Droid -----------------------------------------------------------

    /** The real answer for org.fdroid.fdroid. */
    private val fdroidReal = """
        {"packageName":"org.fdroid.fdroid","suggestedVersionCode":1023052,
        "packages":[{"versionName":"2.0-rc1","versionCode":2000041},
        {"versionName":"2.0-rc0","versionCode":2000040},
        {"versionName":"2.0-alpha11","versionCode":2000011},
        {"versionName":"1.23.2","versionCode":1023052}]}
    """.trimIndent()

    @Test
    fun readsARealFdroidAnswer() {
        val a = Decide.parseFdroid(fdroidReal, page)
        assertTrue(a is FdroidAnswer.Found)
        a as FdroidAnswer.Found
        // The suggested build, not the newest one. F-Droid suggests 1.23.2.
        assertEquals(1023052L, a.versionCode)
        assertEquals("1.23.2", a.versionName)
        assertEquals(page, a.page)
    }

    /** All four watched apps get this answer today. It is not a failure. */
    @Test
    fun notFoundMeansNotThere() {
        assertEquals(
            FdroidAnswer.NotThere,
            Decide.parseFdroid("""{"error":"NOT_FOUND"}""", page)
        )
    }

    @Test
    fun aBrokenFdroidAnswerFails() {
        assertTrue(Decide.parseFdroid("not json", page) is FdroidAnswer.Failed)
        assertTrue(Decide.parseFdroid("", page) is FdroidAnswer.Failed)
        assertTrue(Decide.parseFdroid(null, page) is FdroidAnswer.Failed)
        assertTrue(Decide.parseFdroid("""{"packageName":"a"}""", page) is FdroidAnswer.Failed)
        assertTrue(Decide.parseFdroid("[1,2,3]", page) is FdroidAnswer.Failed)
    }

    // ---- GitHub ------------------------------------------------------------

    /**
     * A draft first, then two pre-releases. Every release of these
     * repositories is a pre-release, so /releases/latest would give nothing.
     */
    private val githubReal = """
        [
          {"id":1,"tag_name":"v0.0.17-alpha","name":"Rebind 0.0.17-alpha",
           "draft":true,"prerelease":true,
           "html_url":"https://github.com/equwal/rebind/releases/tag/untagged-1",
           "assets":[]},
          {"id":2,"tag_name":"v0.0.15-alpha","name":"Rebind 0.0.15-alpha",
           "draft":false,"prerelease":true,
           "html_url":"https://github.com/equwal/rebind/releases/tag/v0.0.15-alpha",
           "published_at":"2026-09-20T18:04:11Z",
           "assets":[
             {"name":"rebind-0.0.15-alpha-play.aab",
              "browser_download_url":"https://github.com/equwal/rebind/releases/download/v0.0.15-alpha/rebind-0.0.15-alpha-play.aab"},
             {"name":"rebind-0.0.15-alpha-play.apk",
              "browser_download_url":"https://github.com/equwal/rebind/releases/download/v0.0.15-alpha/rebind-0.0.15-alpha-play.apk"},
             {"name":"rebind-0.0.15-alpha-full.apk",
              "browser_download_url":"https://github.com/equwal/rebind/releases/download/v0.0.15-alpha/rebind-0.0.15-alpha-full.apk"}
           ]},
          {"id":3,"tag_name":"v0.0.14-alpha","draft":false,"prerelease":true,
           "html_url":"https://github.com/equwal/rebind/releases/tag/v0.0.14-alpha",
           "assets":[]}
        ]
    """.trimIndent()

    @Test
    fun skipsTheDraftAndTakesThePreRelease() {
        val a = Decide.parseGithub(githubReal, "full")
        assertTrue(a is GithubAnswer.Found)
        a as GithubAnswer.Found
        assertEquals("v0.0.15-alpha", a.tag)
        assertEquals("https://github.com/equwal/rebind/releases/tag/v0.0.15-alpha", a.page)
        assertEquals(
            "https://github.com/equwal/rebind/releases/download/" +
                "v0.0.15-alpha/rebind-0.0.15-alpha-full.apk",
            a.apk
        )
    }

    /** Without a wanted word, the first APK wins. The .aab is not an APK. */
    @Test
    fun takesTheFirstApkWhenNothingIsPreferred() {
        val a = Decide.parseGithub(githubReal) as GithubAnswer.Found
        assertEquals(
            "https://github.com/equwal/rebind/releases/download/" +
                "v0.0.15-alpha/rebind-0.0.15-alpha-play.apk",
            a.apk
        )
    }

    @Test
    fun aReleaseWithNoApkGivesNoLink() {
        val one = """
            [{"tag_name":"v0.1.0","draft":false,"prerelease":false,
              "html_url":"https://github.com/equwal/ink-dim/releases/tag/v0.1.0",
              "assets":[{"name":"notes.txt",
                "browser_download_url":"https://example.invalid/notes.txt"}]}]
        """.trimIndent()
        val a = Decide.parseGithub(one, "full") as GithubAnswer.Found
        assertEquals("v0.1.0", a.tag)
        assertNull(a.apk)
    }

    @Test
    fun anEmptyArrayMeansNoRelease() {
        assertEquals(GithubAnswer.None, Decide.parseGithub("[]"))
    }

    @Test
    fun onlyDraftsMeanNoRelease() {
        val drafts = """[{"tag_name":"v9.9.9","draft":true,"assets":[]}]"""
        assertEquals(GithubAnswer.None, Decide.parseGithub(drafts))
    }

    @Test
    fun aBrokenGithubAnswerFails() {
        assertTrue(Decide.parseGithub("not json") is GithubAnswer.Failed)
        assertTrue(Decide.parseGithub("") is GithubAnswer.Failed)
        assertTrue(Decide.parseGithub(null) is GithubAnswer.Failed)
        // The rate limit answer is an object, not an array.
        assertTrue(
            Decide.parseGithub("""{"message":"API rate limit exceeded"}""")
                is GithubAnswer.Failed
        )
        assertTrue(Decide.parseGithub("""[{"draft":false}]""") is GithubAnswer.Failed)
    }

    // ---- the two together --------------------------------------------------

    /** What Rebind gets today: not on F-Droid, a newer pre-release on GitHub. */
    @Test
    fun rebindToday() {
        val f = Decide.parseFdroid("""{"error":"NOT_FOUND"}""", page)
        val g = Decide.parseGithub(githubReal, "full")
        val r = Decide.source(null, f, g, 14L, "0.0.14-alpha-full")
        assertTrue(r is Result.Update)
        r as Result.Update
        assertEquals(Decide.GITHUB, r.source)
        assertEquals("0.0.15-alpha", r.versionName)
        assertTrue(r.apk!!.endsWith("-full.apk"))
    }
}
