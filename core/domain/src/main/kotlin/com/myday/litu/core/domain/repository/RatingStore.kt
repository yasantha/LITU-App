package com.myday.litu.core.domain.repository

import java.time.Instant

/** Remembers when the app last asked for a Play rating. */
interface RatingStore {
    suspend fun lastAsked(): Instant?
    suspend fun markAsked(at: Instant)
}
