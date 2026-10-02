package com.myday.litu.core.config

import android.content.Context
import com.google.firebase.FirebaseApp

/**
 * Firebase is configured only when a google-services.json is present for the build (litu-dev or
 * litu-prod). Without it every Firebase feature falls back to local behaviour, so the app still
 * works fully offline in development.
 */
object FirebaseAvailability {
    fun isConfigured(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()
}
