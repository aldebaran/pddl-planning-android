package com.softbankrobotics.pddlplanning.test

import com.softbankrobotics.pddlplanning.PlanSearchFunction
import io.mockk.every
import io.mockk.mockk

/**
 * This unit test checks the test helpers that do not require a context to work.
 * It is an example for testing third-party planner implementations.
 */
class ExamplePlanningUnitTest : PlanningUnitTest {
    private val searchPlanMock = mockk<PlanSearchFunction>().also {
        every { it.invoke(any(), any(), any()) } returns listOf()
    }
    override val searchPlan: PlanSearchFunction = searchPlanMock
}