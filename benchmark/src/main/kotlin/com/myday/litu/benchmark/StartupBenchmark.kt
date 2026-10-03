package com.myday.litu.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cold start target: under 1.5 s mid-range, under 2.5 s on the low-end reference device. Next
 * question under 100 ms with no dropped frames on feedback (spec section 14).
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule val rule = MacrobenchmarkRule()

    @Test fun coldStartWithBaselineProfile() = startup(CompilationMode.Partial())

    @Test fun coldStartWithoutCompilation() = startup(CompilationMode.None())

    private fun startup(mode: CompilationMode) = rule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = mode,
        iterations = 10,
        startupMode = StartupMode.COLD,
        setupBlock = { pressHome() },
    ) { startActivityAndWait() }

    @Test fun answeringQuestions() = rule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = 5,
        setupBlock = {
            device.executeShellCommand("pm clear $TARGET_PACKAGE")
            startActivityAndWait()
        },
    ) { onboardingAndSample() }
}

internal const val TARGET_PACKAGE = BuildConfig.TARGET_PACKAGE
