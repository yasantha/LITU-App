package com.myday.litu.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Generates the Baseline Profile for startup and the first study session. */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule val rule = BaselineProfileRule()

    @Test fun generate() = rule.collect(packageName = TARGET_PACKAGE, includeInStartupProfile = true) {
        device.executeShellCommand("pm clear $TARGET_PACKAGE")
        startActivityAndWait()
        onboardingAndSample()
    }
}
