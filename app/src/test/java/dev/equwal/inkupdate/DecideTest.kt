// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (c) 2026 equwal
package dev.equwal.inkupdate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The order F-Droid, Google Play, GitHub. */
class DecideTest {

    private val fdroidPage = "https://f-droid.org/packages/dev.equwal.inkdim/"
    private val ghPage = "https://github.com/equwal/ink-dim/releases/tag/v0.1.1"
    private val apk = "https://github.com/equwal/ink-dim/releases/download/v0.1.1/ink-dim-0.1.1.apk"

    private fun fdroidNewer() = FdroidAnswer.Found(2L, "0.1.1", fdroidPage)
    private fun fdroidSame() = FdroidAnswer.Found(1L, "0.1.0", fdroidPage)
    private fun githubNewer() = GithubAnswer.Found("v0.1.1", ghPage, apk)
    private fun githubSame() = GithubAnswer.Found("v0.1.0", ghPage, apk)

    @Test
    fun fdroidWinsOverGithub() {
        val r = Decide.source(null, fdroidNewer(), githubNewer(), 1L, "0.1.0")
        assertTrue(r is Result.Update)
        r as Result.Update
        assertEquals(Decide.FDROID, r.source)
        assertEquals("0.1.1", r.versionName)
        assertEquals(fdroidPage, r.page)
        // F-Droid installs the app by itself, so there is no APK link.
        assertNull(r.apk)
    }

    @Test
    fun fdroidNotThereFallsToGithub() {
        val r = Decide.source(null, FdroidAnswer.NotThere, githubNewer(), 1L, "0.1.0")
        assertTrue(r is Result.Update)
        r as Result.Update
        assertEquals(Decide.GITHUB, r.source)
        assertEquals("0.1.1", r.versionName)
        assertEquals(ghPage, r.page)
        assertEquals(apk, r.apk)
    }

    @Test
    fun fdroidNotThereAndPlayInstalledLeavesItToPlay() {
        val r = Decide.source(
            Decide.PLAY_PACKAGE, FdroidAnswer.NotThere, githubNewer(), 1L, "0.1.0"
        )
        assertEquals(Result.LeftToPlay, r)
    }

    @Test
    fun fdroidClientInstallNeverOffersGithub() {
        val r = Decide.source(
            "org.fdroid.fdroid", FdroidAnswer.NotThere, githubNewer(), 1L, "0.1.0"
        )
        assertEquals(Result.UpToDate, r)
    }

    /** The Play store does not beat a newer build on F-Droid. */
    @Test
    fun fdroidStillWinsOverPlay() {
        val r = Decide.source(
            Decide.PLAY_PACKAGE, fdroidNewer(), GithubAnswer.None, 1L, "0.1.0"
        )
        assertTrue(r is Result.Update)
        assertEquals(Decide.FDROID, (r as Result.Update).source)
    }

    @Test
    fun anotherInstallerDoesNotLeaveItToPlay() {
        val r = Decide.source(
            "com.android.packageinstaller", FdroidAnswer.NotThere, githubNewer(), 1L, "0.1.0"
        )
        assertEquals(Decide.GITHUB, (r as Result.Update).source)
    }

    @Test
    fun theSameVersionIsUpToDate() {
        assertEquals(
            Result.UpToDate,
            Decide.source(null, FdroidAnswer.NotThere, githubSame(), 1L, "0.1.0")
        )
        assertEquals(
            Result.UpToDate,
            Decide.source(null, fdroidSame(), githubSame(), 1L, "0.1.0")
        )
    }

    /** A tag that is older than what is installed is not an update. */
    @Test
    fun anOlderTagIsUpToDate() {
        val r = Decide.source(
            null, FdroidAnswer.NotThere,
            GithubAnswer.Found("v0.0.9-alpha", ghPage, apk), 1L, "0.1.0"
        )
        assertEquals(Result.UpToDate, r)
    }

    /** Rebind: the tag has no "-full", the installed name has. */
    @Test
    fun rebindTagsAndNamesLineUp() {
        val page = "https://github.com/equwal/rebind/releases/tag/v0.0.16-alpha"
        val full = "https://github.com/equwal/rebind/releases/download/" +
            "v0.0.16-alpha/rebind-0.0.16-alpha-full.apk"
        val r = Decide.source(
            null, FdroidAnswer.NotThere,
            GithubAnswer.Found("v0.0.16-alpha", page, full), 15L, "0.0.15-alpha-full"
        )
        assertTrue(r is Result.Update)
        r as Result.Update
        assertEquals("0.0.16-alpha", r.versionName)
        assertEquals(full, r.apk)

        val same = Decide.source(
            null, FdroidAnswer.NotThere,
            GithubAnswer.Found("v0.0.15-alpha", page, full), 15L, "0.0.15-alpha-full"
        )
        assertEquals(Result.UpToDate, same)
    }

    @Test
    fun nobodyAnsweredIsUnknown() {
        val r = Decide.source(
            null, FdroidAnswer.Failed("No answer"), GithubAnswer.Failed("No answer"),
            1L, "0.1.0"
        )
        assertTrue(r is Result.Unknown)
        assertEquals("No answer", (r as Result.Unknown).reason)
    }

    /** F-Droid answered, so a failed GitHub request does not hide the answer. */
    @Test
    fun fdroidAnsweredAndGithubFailed() {
        val r = Decide.source(
            null, fdroidSame(), GithubAnswer.Failed("Too many requests"), 1L, "0.1.0"
        )
        assertEquals(Result.UpToDate, r)
    }

    /** GitHub answered, so a failed F-Droid request does not hide the answer. */
    @Test
    fun githubAnsweredAndFdroidFailed() {
        val r = Decide.source(
            null, FdroidAnswer.Failed("No answer"), githubNewer(), 1L, "0.1.0"
        )
        assertEquals(Decide.GITHUB, (r as Result.Update).source)
    }

    /** Neither place has the app. There is nothing newer to get. */
    @Test
    fun nothingAnywhereIsUpToDate() {
        assertEquals(
            Result.UpToDate,
            Decide.source(null, FdroidAnswer.NotThere, GithubAnswer.None, 1L, "0.1.0")
        )
    }

    @Test
    fun aVersionNameThatMakesNoSenseIsUnknown() {
        val r = Decide.source(
            null, FdroidAnswer.NotThere, githubNewer(), 1L, "nightly"
        )
        assertTrue(r is Result.Unknown)
    }
}
