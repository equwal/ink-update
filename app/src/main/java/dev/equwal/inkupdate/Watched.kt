// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (c) 2026 equwal
package dev.equwal.inkupdate

/**
 * One app that Ink Update watches.
 *
 * @param title what the screen calls the app.
 * @param pkg the package name on the device.
 * @param fdroidId the id on F-Droid, or null when the app is not there.
 * @param repo the GitHub repository, as "owner/name".
 * @param preferAsset a word in the name of the APK to take first, when a
 *        release holds more than one APK. Null takes the first APK.
 */
data class Watched(
    val title: String,
    val pkg: String,
    val fdroidId: String?,
    val repo: String,
    val preferAsset: String? = null
) {
    /** The page that F-Droid shows for the app. The F-Droid client opens it. */
    fun fdroidPage(): String? = fdroidId?.let { "https://f-droid.org/packages/$it/" }

    /** The F-Droid answer about the app. */
    fun fdroidApi(): String? = fdroidId?.let { "https://f-droid.org/api/v1/packages/$it" }

    /** The last ten releases of the repository. */
    fun githubApi(): String = "https://api.github.com/repos/$repo/releases?per_page=10"

    /** The page in the Play store. The Play client opens it. */
    fun playPage(): String = "market://details?id=$pkg"
}

/**
 * The list of watched apps. To watch one more app, add a line.
 *
 * Only the apps that are installed are shown and checked.
 */
object Watch {

    val ALL: List<Watched> = listOf(
        Watched(
            title = "Rebind",
            pkg = "dev.equwal.assistkey",
            fdroidId = null,
            repo = "equwal/rebind",
            preferAsset = "full"
        ),
        Watched(
            title = "Ink Recents",
            pkg = "dev.equwal.inkrecents",
            fdroidId = "dev.equwal.inkrecents",
            repo = "equwal/ink-recents"
        ),
        Watched(
            title = "Ink Dim",
            pkg = "dev.equwal.inkdim",
            fdroidId = "dev.equwal.inkdim",
            repo = "equwal/ink-dim"
        ),
        Watched(
            title = "Ink Update",
            pkg = "dev.equwal.inkupdate",
            fdroidId = "dev.equwal.inkupdate",
            repo = "equwal/ink-update"
        )
    )
}
