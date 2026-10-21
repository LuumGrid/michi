package com.luum.michi.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cold-start TTID with no compilation vs the generated Baseline Profile.
 *
 * 10 iterations per mode: on a non-rooted phone the CPU clocks are not fixed,
 * so 3 runs are too noisy to compare the two modes. The metric reports
 * min/median/max; the report uses all three.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartupNone() = startup(CompilationMode.None())

    @Test
    fun coldStartupBaselineProfile() = startup(
        CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require)
    )

    private fun startup(compilationMode: CompilationMode) = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilationMode,
        startupMode = StartupMode.COLD,
        iterations = 10,
        // Unmeasured: leave the app in its normal (non-first-launch) state so
        // the sign-in sheet never skews a measured start.
        setupBlock = {
            startActivityAndWait()
            dismissAuthIfShown()
            pressHome()
        },
        measureBlock = {
            pressHome()
            startActivityAndWait()
        },
    )
}
