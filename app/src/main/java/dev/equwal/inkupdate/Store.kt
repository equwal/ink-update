// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (c) 2026 equwal
package dev.equwal.inkupdate

import android.content.Context
import org.json.JSONObject

/**
 * What the app remembers between checks: the last result for each app, the
 * time of the last check, the daily switch, and the version of the last
 * notification.
 *
 * The results are one JSON object in the shared preferences. A failed check
 * keeps the last good result, so a bad network does not hide a known update.
 */
class Store(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("inkupdate", Context.MODE_PRIVATE)

    // ---- the daily job -----------------------------------------------------

    /** True while the daily check is on. It is on until the person turns it off. */
    var daily: Boolean
        get() = prefs.getBoolean(DAILY, true)
        set(v) = prefs.edit().putBoolean(DAILY, v).apply()

    /** True after the first start did its work. */
    var started: Boolean
        get() = prefs.getBoolean(STARTED, false)
        set(v) = prefs.edit().putBoolean(STARTED, v).apply()

    // ---- the last check ----------------------------------------------------

    /** When the last check ran, in milliseconds. 0 means never. */
    val checkedAt: Long get() = prefs.getLong(CHECKED_AT, 0L)

    /** True when every request of the last check gave an answer. */
    val lastOk: Boolean get() = prefs.getBoolean(LAST_OK, true)

    fun checked(at: Long, ok: Boolean) {
        prefs.edit().putLong(CHECKED_AT, at).putBoolean(LAST_OK, ok).apply()
    }

    // ---- the results -------------------------------------------------------

    /**
     * Keeps the result of one app. A [Result.Unknown] does not replace a result
     * that the app already has.
     */
    fun put(pkg: String, r: Result) {
        if (r is Result.Unknown && result(pkg) != null) return
        val all = results()
        all.put(pkg, write(r))
        prefs.edit().putString(RESULTS, all.toString()).apply()
    }

    /** The last result of one app, or null when there is none. */
    fun result(pkg: String): Result? {
        val o = results().optJSONObject(pkg) ?: return null
        return read(o)
    }

    // ---- what the app told the person --------------------------------------

    /** The version of the last notification for one app. */
    fun notified(pkg: String): String? = prefs.getString(NOTIFIED + pkg, null)

    fun setNotified(pkg: String, versionName: String) {
        prefs.edit().putString(NOTIFIED + pkg, versionName).apply()
    }

    // ---- JSON --------------------------------------------------------------

    private fun results(): JSONObject = try {
        JSONObject(prefs.getString(RESULTS, "{}") ?: "{}")
    } catch (e: Exception) {
        JSONObject()
    }

    private fun write(r: Result): JSONObject = JSONObject().apply {
        when (r) {
            is Result.Update -> {
                put(KIND, "update")
                put("source", r.source)
                put("version", r.versionName)
                put("page", r.page)
                if (r.apk != null) put("apk", r.apk)
            }
            is Result.UpToDate -> put(KIND, "ok")
            is Result.LeftToPlay -> put(KIND, "play")
            is Result.Unknown -> {
                put(KIND, "unknown")
                put("reason", r.reason)
            }
        }
    }

    private fun read(o: JSONObject): Result? = when (o.optString(KIND)) {
        "update" -> Result.Update(
            o.optString("source"),
            o.optString("version"),
            o.optString("page"),
            if (o.has("apk")) o.optString("apk") else null
        )
        "ok" -> Result.UpToDate
        "play" -> Result.LeftToPlay
        "unknown" -> Result.Unknown(o.optString("reason"))
        else -> null
    }

    private companion object {
        const val DAILY = "daily"
        const val STARTED = "started"
        const val CHECKED_AT = "checkedAt"
        const val LAST_OK = "lastOk"
        const val RESULTS = "results"
        const val NOTIFIED = "notified."
        const val KIND = "kind"
    }
}
