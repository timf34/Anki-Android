// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2026 Tim Farrelly <timf34@gmail.com>

package com.ichi2.anki.blocker

import androidx.annotation.StringRes
import androidx.core.content.edit
import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.R
import com.ichi2.anki.common.time.TimeManager
import com.ichi2.anki.libanki.DeckId
import java.util.TimeZone

/**
 * Blocker settings, kept separate from [com.ichi2.anki.settings.Prefs] so the
 * feature stays self-contained.
 */
object BlockerPrefs {
    const val DEFAULT_CARDS_REQUIRED = 1
    const val MAX_CARDS_REQUIRED = 3
    const val DEFAULT_UNLOCK_MINUTES = 5

    /** [DeckId] value meaning "no deck chosen yet": fall back to the currently selected deck. */
    const val NO_GATE_DECK: DeckId = 0

    private val prefs get() = AnkiDroidApp.sharedPrefs()

    private fun key(
        @StringRes resId: Int,
    ): String = AnkiDroidApp.appResources.getString(resId)

    /**
     * Whether the user has read and accepted the accessibility disclosure. The
     * blocker must not be enabled before this is true.
     */
    var hasAcceptedDisclosure: Boolean
        get() = prefs.getBoolean(key(R.string.blocker_disclosure_accepted_key), false)
        set(value) = prefs.edit { putBoolean(key(R.string.blocker_disclosure_accepted_key), value) }

    /**
     * Whether the blocker is on at all.
     *
     * Writing this, [blockedApps] or [blockedDomains] notifies the running service so
     * it re-applies its event filter; otherwise the change has no effect until the
     * service next restarts.
     */
    var isEnabled: Boolean
        get() = prefs.getBoolean(key(R.string.blocker_enabled_key), false)
        set(value) {
            prefs.edit { putBoolean(key(R.string.blocker_enabled_key), value) }
            BlockerController.notifyConfigChanged()
        }

    /** Package names of the apps the blocker gates. */
    var blockedApps: Set<String>
        get() = prefs.getStringSet(key(R.string.blocker_blocked_apps_key), null) ?: emptySet()
        set(value) {
            prefs.edit { putStringSet(key(R.string.blocker_blocked_apps_key), value) }
            BlockerController.notifyConfigChanged()
        }

    /** Website domains the blocker gates (bare hosts like `x.com`; subdomains match). */
    var blockedDomains: Set<String>
        get() = prefs.getStringSet(key(R.string.blocker_blocked_domains_key), null) ?: emptySet()
        set(value) {
            prefs.edit { putStringSet(key(R.string.blocker_blocked_domains_key), value) }
            BlockerController.notifyConfigChanged()
        }

    /** How many unique cards must be rated Good/Easy to open a gate. */
    var cardsRequired: Int
        get() = prefs.getInt(key(R.string.blocker_cards_required_key), DEFAULT_CARDS_REQUIRED).coerceIn(1, MAX_CARDS_REQUIRED)
        set(value) = prefs.edit { putInt(key(R.string.blocker_cards_required_key), value.coerceIn(1, MAX_CARDS_REQUIRED)) }

    /** How long an unlock lasts before the gate triggers again. */
    var unlockMinutes: Int
        get() = prefs.getInt(key(R.string.blocker_unlock_minutes_key), DEFAULT_UNLOCK_MINUTES).coerceAtLeast(1)
        set(value) = prefs.edit { putInt(key(R.string.blocker_unlock_minutes_key), value.coerceAtLeast(1)) }

    /** The deck gate reviews come from, or [NO_GATE_DECK] for the currently selected deck. */
    var gateDeckId: DeckId
        get() = prefs.getLong(key(R.string.blocker_gate_deck_key), NO_GATE_DECK)
        set(value) = prefs.edit { putLong(key(R.string.blocker_gate_deck_key), value) }

    /** Serialized unlock sessions; owned by [UnlockStore]. */
    var unlockSessionsJson: String?
        get() = prefs.getString(key(R.string.blocker_unlock_sessions_key), null)
        set(value) = prefs.edit { putString(key(R.string.blocker_unlock_sessions_key), value) }

    /**
     * How many gates have been passed today, for the deck picker's status card.
     * Resets on the first read or write of a new day.
     */
    val unlocksToday: Int
        get() = if (storedUnlockDay == currentDay()) prefs.getInt(key(R.string.blocker_unlocks_today_key), 0) else 0

    fun recordUnlock() {
        val today = currentDay()
        val count = if (storedUnlockDay == today) unlocksToday + 1 else 1
        prefs.edit {
            putInt(key(R.string.blocker_unlocks_today_key), count)
            putLong(key(R.string.blocker_unlocks_today_day_key), today)
        }
    }

    private val storedUnlockDay: Long
        get() = prefs.getLong(key(R.string.blocker_unlocks_today_day_key), -1)

    /**
     * Days since the epoch in local time. Deliberately not Anki's day cutoff: this
     * counts phone usage, which the user thinks about as calendar days.
     */
    private fun currentDay(): Long {
        val zone = TimeZone.getDefault()
        val nowMs = TimeManager.time.intTimeMS()
        return (nowMs + zone.getOffset(nowMs)) / MILLIS_PER_DAY
    }

    private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
}
