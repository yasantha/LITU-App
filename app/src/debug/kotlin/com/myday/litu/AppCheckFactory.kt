package com.myday.litu

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/** Debug builds use the App Check debug provider; register its token in the Firebase console. */
internal object AppCheckFactory {
    fun get(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
}
