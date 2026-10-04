// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (c) 2026 equwal
package dev.equwal.inkupdate

import org.json.JSONArray
import org.json.JSONObject

/** What F-Droid said about one app. */
sealed class FdroidAnswer {

    /** F-Droid does not have the app. */
    object NotThere : FdroidAnswer()

    /** F-Droid has the app. [page] is the page for a person to open. */
    data class Found(
        val versionCode: Long,
        val versionName: String,
        val page: String
    ) : FdroidAnswer()

    /** The request failed, or the answer made no sense. */
    data class Failed(val reason: String) : FdroidAnswer()
}

/** What GitHub said about one repository. */
sealed class GithubAnswer {

    /** The repository has no release that a person can get. */
    object None : GithubAnswer()

    /**
     * The newest release that is not a draft. Every release of these
     * repositories is a pre-release, so the app must not use /releases/latest.
     */
    data class Found(
        val tag: String,
        val page: String,
        val apk: String?
    ) : GithubAnswer()

    /** The request failed, or the answer made no sense. */
    data class Failed(val reason: String) : GithubAnswer()
}

/** What the app knows about one watched app after a check. */
sealed class Result {

    /** The installed version is the newest one found. */
    object UpToDate : Result()

    /** A newer version is there. [page] opens it, [apk] downloads it. */
    data class Update(
        val source: String,
        val versionName: String,
        val page: String,
        val apk: String?
    ) : Result()

    /** The Play store installed the app, so the Play store updates it. */
    object LeftToPlay : Result()

    /** Nobody answered. */
    data class Unknown(val reason: String) : Result()
}

/**
 * The order of the search, and the reading of the two answers.
 *
 * Nothing here touches Android, so the unit tests run on the JVM.
 */
object Decide {

    const val FDROID = "F-Droid"
    const val GITHUB = "GitHub"
    const val PLAY = "Google Play"

    /** The package name of the Play store client. */
    const val PLAY_PACKAGE = "com.android.vending"

    /** F-Droid and its common clients. They update the app themselves. */
    val FDROID_CLIENTS = setOf("org.fdroid.fdroid", "org.fdroid.basic", "com.machiav3lli.fdroid")

    fun fromFdroidClient(installer: String?): Boolean = installer in FDROID_CLIENTS

    /**
     * Where the update comes from. The order is F-Droid, Google Play, GitHub.
     *
     * 1. F-Droid holds a newer build: take it. The F-Droid client updates the
     *    app by itself after the person opens the page once.
     * 2. The Play store installed the app: the Play store updates it. Stop.
     * 3. A F-Droid client installed the app: it updates the app. Stop. F-Droid
     *    builds lag behind GitHub, and the app must not offer a sideload then.
     * 4. GitHub holds a newer release: take it.
     *
     * @param installer the package that installed the app, from the system.
     */
    fun source(
        installer: String?,
        fdroid: FdroidAnswer,
        github: GithubAnswer,
        installedCode: Long,
        installedName: String
    ): Result {
        if (fdroid is FdroidAnswer.Found && fdroid.versionCode > installedCode) {
            return Result.Update(FDROID, fdroid.versionName, fdroid.page, null)
        }
        if (installer == PLAY_PACKAGE) return Result.LeftToPlay
        if (fromFdroidClient(installer)) return Result.UpToDate

        if (github is GithubAnswer.Found) {
            val theirs = Version.parse(github.tag)
            val ours = Version.parse(installedName)
            if (theirs == null || ours == null) {
                return Result.Unknown("Version not understood")
            }
            return if (theirs > ours) {
                Result.Update(GITHUB, Version.display(github.tag), github.page, github.apk)
            } else {
                Result.UpToDate
            }
        }

        // GitHub gave no release. F-Droid answered, so the app is up to date.
        if (fdroid is FdroidAnswer.Found) return Result.UpToDate
        if (github is GithubAnswer.Failed) return Result.Unknown(github.reason)
        if (fdroid is FdroidAnswer.Failed) return Result.Unknown(fdroid.reason)

        // Neither place has the app, so there is nothing newer to get.
        return Result.UpToDate
    }

    /**
     * Reads the answer of https://f-droid.org/api/v1/packages/<id>.
     *
     * The answer {"error":"NOT_FOUND"} means that F-Droid does not have the
     * app. That is not a failure.
     *
     * This function never throws.
     */
    fun parseFdroid(body: String?, page: String): FdroidAnswer {
        if (body.isNullOrBlank()) return FdroidAnswer.Failed("Empty answer")
        return try {
            val o = JSONObject(body)
            if (o.has("error")) return FdroidAnswer.NotThere
            if (!o.has("suggestedVersionCode")) return FdroidAnswer.Failed("No version")
            val code = o.getLong("suggestedVersionCode")
            val packages = o.optJSONArray("packages") ?: JSONArray()
            var name = ""
            for (i in 0 until packages.length()) {
                val p = packages.optJSONObject(i) ?: continue
                if (p.optLong("versionCode", -1L) == code) {
                    name = p.optString("versionName", "")
                    break
                }
            }
            if (name.isEmpty() && packages.length() > 0) {
                name = packages.optJSONObject(0)?.optString("versionName", "") ?: ""
            }
            FdroidAnswer.Found(code, name, page)
        } catch (e: Exception) {
            FdroidAnswer.Failed("Answer not understood")
        }
    }

    /**
     * Reads the answer of
     * https://api.github.com/repos/<owner>/<repo>/releases?per_page=10.
     *
     * The first entry that is not a draft wins. A pre-release counts, because
     * every release of these repositories is a pre-release.
     *
     * @param preferAsset a word in the name of the APK to take first.
     *
     * This function never throws.
     */
    fun parseGithub(body: String?, preferAsset: String? = null): GithubAnswer {
        if (body.isNullOrBlank()) return GithubAnswer.Failed("Empty answer")
        return try {
            val list = JSONArray(body)
            for (i in 0 until list.length()) {
                val r = list.optJSONObject(i) ?: continue
                if (r.optBoolean("draft", false)) continue
                val tag = r.optString("tag_name", "")
                if (tag.isEmpty()) return GithubAnswer.Failed("No tag")
                val page = r.optString("html_url", "")
                return GithubAnswer.Found(tag, page, apkOf(r, preferAsset))
            }
            GithubAnswer.None
        } catch (e: Exception) {
            GithubAnswer.Failed("Answer not understood")
        }
    }

    /** The APK of a release, or null. A name that holds [prefer] wins. */
    private fun apkOf(release: JSONObject, prefer: String?): String? {
        val assets = release.optJSONArray("assets") ?: return null
        var first: String? = null
        for (i in 0 until assets.length()) {
            val a = assets.optJSONObject(i) ?: continue
            val name = a.optString("name", "").lowercase()
            if (!name.endsWith(".apk")) continue
            val url = a.optString("browser_download_url", "")
            if (url.isEmpty()) continue
            if (prefer != null && name.contains(prefer.lowercase())) return url
            if (first == null) first = url
        }
        return first
    }
}
