package com.myday.litu.core.domain.repository

import java.time.LocalTime

/** App build facts that features show or send (About screen, question reports). */
interface AppInfo {
    val versionName: String
    val versionCode: Long
}

/** One daily notification at the user's chosen time (spec section 3, item 10). */
interface ReminderScheduler {
    fun schedule(time: LocalTime, enabled: Boolean)
}
