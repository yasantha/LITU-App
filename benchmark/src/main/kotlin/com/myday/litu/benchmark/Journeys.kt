package com.myday.litu.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.Until

/** Waits for a node and clicks it, retrying if Compose recomposes it underneath us. */
private fun MacrobenchmarkScope.click(selector: BySelector, timeoutMs: Long = 5_000) {
    repeat(3) {
        try {
            device.wait(Until.findObject(selector), timeoutMs)?.click()
            return
        } catch (_: StaleObjectException) {
            // Recomposed between find and click; look it up again.
        }
    }
}

/** Walks first-run onboarding into the free sample and answers a few questions. */
internal fun MacrobenchmarkScope.onboardingAndSample() {
    click(By.text("Skip"))
    click(By.text("I haven't booked yet"))
    click(By.text("Continue"))
    click(By.text("Not now"))
    repeat(3) {
        // Answer cards expose radio-button semantics.
        click(By.checkable(true))
        click(By.text("Check"))
        click(By.text("Next"), timeoutMs = 3_000)
    }
}
