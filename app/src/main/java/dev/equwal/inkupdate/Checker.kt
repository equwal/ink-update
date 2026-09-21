// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (c) 2026 equwal
package dev.equwal.inkupdate

import android.content.Context
import android.content.pm.PackageManager

/**
 * The check itself: what is installed, what the two hosts say, and what that
 * means.
 *
 * Every function here makes network requests. Call them from a background
 * thread only.
 */
object Checker {

    private const val GITHUB_ACCEPT = "application/vnd.github+json"

    /** A watched app that is on the device. */
    data class Installed(
        val watched: Watched,
        val versionName: String,
        val versionCode: Long,
        val installer: String?
    )

    /** A row of the screen: an installed app and what the last check found. */
    data class Row(val app: Installed, val result: Result?)

    /** The watched apps that are on the device, in the order of the list. */
    fun installed(context: Context): List<Installed> {
        val pm = context.packageManager
        val out = ArrayList<Installed>()
        for (w in Watch.ALL) {
            val info = try {
                pm.getPackageInfo(w.pkg, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            } catch (e: Exception) {
                null
            } ?: continue
            out.add(
                Installed(
                    watched = w,
                    versionName = info.versionName ?: "",
                    versionCode = info.longVersionCode,
                    installer = installerOf(pm, w.pkg)
                )
            )
        }
        return out
    }

    /** The rows to show, from what the store holds. This makes no request. */
    fun rows(context: Context, store: Store): List<Row> =
        installed(context).map { Row(it, store.result(it.watched.pkg)) }

    /**
     * Checks every installed watched app, keeps the results, and tells the
     * person about each new update.
     *
     * @return how many apps have an update.
     */
    fun checkAll(context: Context, store: Store): Int {
        val apps = installed(context)
        var updates = 0
        var ok = true
        for (app in apps) {
            val r = check(app)
            if (r is Result.Unknown) ok = false
            store.put(app.watched.pkg, r)
            if (r is Result.Update) {
                updates++
                // The version is marked only after the notification went out.
                // A person who turns notifications on later still hears about
                // the update at the next check.
                if (store.notified(app.watched.pkg) != r.versionName &&
                    Notify.update(context, app.watched, r)
                ) {
                    store.setNotified(app.watched.pkg, r.versionName)
                }
            }
        }
        store.checked(System.currentTimeMillis(), ok)
        return updates
    }

    /** The check of one app: F-Droid, then the Play store, then GitHub. */
    fun check(app: Installed): Result {
        val w = app.watched
        val fdroid = askFdroid(w)
        val fdroidWins = fdroid is FdroidAnswer.Found && fdroid.versionCode > app.versionCode

        // Two cases make the GitHub request waste: F-Droid already has a newer
        // build, and the Play store updates the app by itself.
        val github =
            if (fdroidWins || app.installer == Decide.PLAY_PACKAGE) GithubAnswer.None
            else askGithub(w)

        return Decide.source(
            app.installer, fdroid, github, app.versionCode, app.versionName
        )
    }

    private fun askFdroid(w: Watched): FdroidAnswer {
        val url = w.fdroidApi() ?: return FdroidAnswer.NotThere
        val page = w.fdroidPage() ?: return FdroidAnswer.NotThere
        val a = Net.get(url)
        return when {
            a.code == 0 -> FdroidAnswer.Failed("No answer")
            a.code == 404 -> FdroidAnswer.NotThere
            a.code !in 200..299 -> FdroidAnswer.Failed("F-Droid said " + a.code)
            else -> Decide.parseFdroid(a.body, page)
        }
    }

    private fun askGithub(w: Watched): GithubAnswer {
        val a = Net.get(w.githubApi(), GITHUB_ACCEPT)
        return when {
            a.code == 0 -> GithubAnswer.Failed("No answer")
            a.code == 403 || a.code == 429 -> GithubAnswer.Failed("Too many requests")
            a.code !in 200..299 -> GithubAnswer.Failed("GitHub said " + a.code)
            else -> Decide.parseGithub(a.body, w.preferAsset)
        }
    }

    /** The package that installed the app, as the system knows it. */
    private fun installerOf(pm: PackageManager, pkg: String): String? = try {
        pm.getInstallSourceInfo(pkg).installingPackageName
    } catch (e: Exception) {
        null
    }
}
