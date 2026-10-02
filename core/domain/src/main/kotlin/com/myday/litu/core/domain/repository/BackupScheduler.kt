package com.myday.litu.core.domain.repository

/** Asks core/sync to back up after a session ends (only acts for users with a linked account). */
fun interface BackupScheduler {
    fun requestBackup()
}
