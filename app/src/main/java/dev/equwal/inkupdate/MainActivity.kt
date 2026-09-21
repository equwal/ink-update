// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (c) 2026 equwal
package dev.equwal.inkupdate

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.widget.LinearLayout
import dev.equwal.inkupdate.Ui.button
import dev.equwal.inkupdate.Ui.header
import dev.equwal.inkupdate.Ui.note
import dev.equwal.inkupdate.Ui.page
import dev.equwal.inkupdate.Ui.primaryButton
import dev.equwal.inkupdate.Ui.row
import dev.equwal.inkupdate.Ui.title

/**
 * The one screen.
 *
 * It shows each installed watched app, what is installed, and what the last
 * check found. The check runs on a background thread, because the main thread
 * must not wait for the network.
 */
class MainActivity : Activity() {

    /** Another app asks for a check with this action. Rebind uses it. */
    private val checkAction = "dev.equwal.inkupdate.CHECK"

    private lateinit var store: Store
    private var checking = false

    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        store = Store(this)

        val first = !store.started
        if (first) {
            store.started = true
            if (store.daily) CheckJob.schedule(this)
            ask()
        }
        draw()
        if (first || intent?.action == checkAction) check()
    }

    override fun onNewIntent(i: Intent?) {
        super.onNewIntent(i)
        intent = i
        if (i?.action == checkAction) check()
    }

    override fun onResume() {
        super.onResume()
        if (!checking) draw()
    }

    // ---- the screen --------------------------------------------------------

    private fun draw() {
        val col = page(this)
        col.title(getString(R.string.app_name))
        col.note(status())

        val rows = Checker.rows(this, store)
        if (rows.isEmpty()) {
            col.note(getString(R.string.none_installed))
        } else {
            for (r in rows) appRow(col, r)
        }

        col.primaryButton(
            if (checking) getString(R.string.checking) else getString(R.string.check_now)
        ) { check() }

        col.row(
            title = getString(R.string.daily),
            state = if (store.daily) getString(R.string.on) else getString(R.string.off)
        ) { setDaily(!store.daily) }

        if (!notificationsOn()) {
            col.row(
                title = getString(R.string.notifications),
                state = getString(R.string.off)
            ) { ask() }
        }

        col.header(getString(R.string.about))
        col.row(title = getString(R.string.version), subtitle = BuildConfig.VERSION_NAME)
        col.row(title = getString(R.string.licence), subtitle = "GPL-3.0-or-later")
        col.row(title = getString(R.string.source_code)) { open(SOURCE) }
        col.row(title = getString(R.string.coffee)) { open(KOFI) }
    }

    /** One watched app: what is installed, and what the check found. */
    private fun appRow(col: LinearLayout, r: Checker.Row) {
        val result = r.result
        val state = when (result) {
            is Result.Update -> getString(R.string.update_to, result.versionName)
            is Result.UpToDate -> getString(R.string.up_to_date)
            is Result.LeftToPlay -> getString(R.string.play_updates)
            else -> getString(R.string.no_answer)
        }
        val page = when (result) {
            is Result.Update -> result.page
            is Result.LeftToPlay -> r.app.watched.playPage()
            else -> null
        }
        col.row(
            title = r.app.watched.title,
            subtitle = r.app.versionName,
            state = state,
            onClick = if (page == null) null else ({ open(page) })
        )
    }

    /** The one line that says when the app last looked. */
    private fun status(): String = when {
        checking -> getString(R.string.checking)
        store.checkedAt == 0L -> getString(R.string.never_checked)
        else -> {
            val ago = DateUtils.getRelativeTimeSpanString(
                store.checkedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
            ).toString()
            if (store.lastOk) getString(R.string.checked, ago)
            else getString(R.string.could_not_check, ago)
        }
    }

    // ---- the work ----------------------------------------------------------

    private fun check() {
        if (checking) return
        checking = true
        draw()
        Thread {
            try {
                Checker.checkAll(applicationContext, store)
            } catch (e: Exception) {
                store.checked(System.currentTimeMillis(), false)
            }
            runOnUiThread {
                checking = false
                if (!isFinishing && !isDestroyed) draw()
            }
        }.start()
    }

    private fun setDaily(on: Boolean) {
        store.daily = on
        if (on) CheckJob.schedule(this) else CheckJob.cancel(this)
        draw()
    }

    // ---- notifications -----------------------------------------------------

    private fun notificationsOn(): Boolean =
        getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() ?: false

    /** Asks for the notification permission. Android shows the box once. */
    private fun ask() {
        if (notificationsOn()) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onRequestPermissionsResult(
        code: Int,
        permissions: Array<out String>,
        results: IntArray
    ) {
        super.onRequestPermissionsResult(code, permissions, results)
        if (!checking) draw()
    }

    // ---- links -------------------------------------------------------------

    private fun open(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            // No app takes the link. There is nothing to do about it.
        }
    }

    private companion object {
        const val SOURCE = "https://github.com/equwal/ink-update"
        const val KOFI = "https://ko-fi.com/truex"
    }
}
