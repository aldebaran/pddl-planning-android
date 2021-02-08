package com.softbankrobotics.pddlplanning.test

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.softbankrobotics.pddlplanning.PlanSearchFunction
import io.mockk.every
import io.mockk.mockk
import org.junit.runner.RunWith

/**
 * This instrumented test checks the test helpers that require a context to work.
 * It is an example for testing third-party planner implementations.
 * Note that we use a naive mock planner implementation, for which tests do not pass.
 * This test is provided as a usage example.
 */
@RunWith(AndroidJUnit4::class)
class ExamplePlanningInstrumentedTest : PlanningInstrumentedTest {
    override val logTag: String = "ExamplePlanningInstrumentedTest"
    override val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val searchPlanMock = mockk<PlanSearchFunction>().also {
        every { it.invoke(any(), any(), any()) } returns listOf()
    }
    override val searchPlan: PlanSearchFunction = searchPlanMock
}