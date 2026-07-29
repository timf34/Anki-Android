// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2026 Tim Farrelly <timf34@gmail.com>

package com.ichi2.anki.blocker

import android.app.Activity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.ichi2.anki.R
import com.ichi2.anki.preferences.PreferencesActivity
import timber.log.Timber

/**
 * The app blocker's row on the deck picker: what is gated, how many gates were
 * passed today, and a warning when blocking has silently stopped.
 *
 * It stays hidden until the blocker has been set up at least once, so it costs
 * nothing for people who don't use the feature.
 */
object BlockerStatusBar {
    /** Binds and refreshes the row. Safe to call from `onResume` on every load. */
    fun refresh(activity: Activity) {
        val bar = activity.findViewById<View>(R.id.blocker_status_bar) ?: return
        if (!BlockerPrefs.hasAcceptedDisclosure) {
            bar.isVisible = false
            return
        }
        bar.isVisible = true
        bar.setOnClickListener {
            Timber.i("Blocker: opening settings from the deck picker")
            activity.startActivity(PreferencesActivity.getIntent(activity, BlockerSettingsFragment::class))
        }

        val inactive = BlockerStatus.isEnabledButInactive(activity)
        activity.findViewById<ImageView>(R.id.blocker_status_icon)?.setImageResource(
            if (inactive) R.drawable.ic_warning else R.drawable.ic_baseline_lock_24,
        )
        activity.findViewById<TextView>(R.id.blocker_status_summary)?.text =
            summary(activity, inactive)
    }

    private fun summary(
        activity: Activity,
        inactive: Boolean,
    ): String {
        if (inactive) return activity.getString(R.string.blocker_status_paused)
        if (!BlockerPrefs.isEnabled) return activity.getString(R.string.blocker_status_off)

        val apps = BlockerPrefs.blockedApps.size
        val sites = BlockerPrefs.blockedDomains.size
        if (apps == 0 && sites == 0) return activity.getString(R.string.blocker_nothing_blocked)

        val resources = activity.resources
        val gated =
            listOfNotNull(
                apps.takeIf { it > 0 }?.let { resources.getQuantityString(R.plurals.blocker_status_apps, it, it) },
                sites.takeIf { it > 0 }?.let { resources.getQuantityString(R.plurals.blocker_status_sites, it, it) },
            ).joinToString(", ")
        val unlocks = BlockerPrefs.unlocksToday
        if (unlocks == 0) return gated
        return activity.getString(
            R.string.blocker_status_with_unlocks,
            gated,
            resources.getQuantityString(R.plurals.blocker_status_unlocks, unlocks, unlocks),
        )
    }
}
