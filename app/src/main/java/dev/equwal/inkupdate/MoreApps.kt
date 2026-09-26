// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (c) 2026 equwal
package dev.equwal.inkupdate

/**
 * One link of the "More apps" section: a name and one line about it, as string resources,
 * and the page that a tap opens.
 */
class MoreApp(val name: Int, val line: Int, val url: String)

/**
 * The other sites and apps of the same author, in the order of the catalog: the sites,
 * then the Android apps, then the list of all projects. Ink Update is not in it.
 */
val MORE_APPS = listOf(
    MoreApp(R.string.more_subread, R.string.more_subread_line, "https://subread.space/"),
    MoreApp(R.string.more_book_simulator, R.string.more_book_simulator_line, "https://booksimulator.com/"),
    MoreApp(R.string.more_honjimaku, R.string.more_honjimaku_line, "https://honjimaku.com/"),
    MoreApp(R.string.more_sbm_sync, R.string.more_sbm_sync_line, "https://sbmsync.com/"),
    MoreApp(R.string.more_subread_android, R.string.more_subread_android_line,
        "https://github.com/equwal/subread-android/releases/latest"),
    MoreApp(R.string.more_subread_overlay, R.string.more_subread_overlay_line,
        "https://github.com/equwal/subread-overlay/releases/latest"),
    MoreApp(R.string.more_subread_dictionary, R.string.more_subread_dictionary_line,
        "https://github.com/equwal/subread-dictionary/releases/latest"),
    MoreApp(R.string.more_subread_anki, R.string.more_subread_anki_line, "https://github.com/equwal/subread-anki"),
    MoreApp(R.string.more_subrep, R.string.more_subrep_line,
        "https://github.com/equwal/subrep-android/releases/latest"),
    MoreApp(R.string.more_sbm_android, R.string.more_sbm_android_line,
        "https://github.com/equwal/sbm-android/releases/latest"),
    MoreApp(R.string.more_rebind, R.string.more_rebind_line, "https://github.com/equwal/rebind/releases"),
    MoreApp(R.string.more_ink_recents, R.string.more_ink_recents_line,
        "https://github.com/equwal/ink-recents/releases/latest"),
    MoreApp(R.string.more_ink_dim, R.string.more_ink_dim_line, "https://github.com/equwal/ink-dim/releases/latest"),
    MoreApp(R.string.more_all_projects, R.string.more_all_projects_line, "https://recentlywritten.com/projects.html"),
)
