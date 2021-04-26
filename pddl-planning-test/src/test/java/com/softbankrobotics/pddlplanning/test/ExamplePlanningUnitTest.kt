package com.softbankrobotics.pddlplanning.test

import com.softbankrobotics.pddlplanning.PlanSearchFunction
import com.softbankrobotics.pddlplanning.Task
import com.softbankrobotics.pddlplanning.test.PlanningUnitTest.Companion.scratchDomain
import com.softbankrobotics.pddlplanning.test.PlanningUnitTest.Companion.scratchProblem
import com.softbankrobotics.pddlplanning.test.PlanningUnitTest.Companion.simpleServiceDomain
import com.softbankrobotics.pddlplanning.test.PlanningUnitTest.Companion.simpleServiceProblem
import io.mockk.coEvery
import io.mockk.mockk

/**
 * This unit test checks the test helpers that do not require a context to work.
 * It is an example for testing third-party planner implementations.
 */
class ExamplePlanningUnitTest : PlanningUnitTest {
    private val searchPlanMock = mockk<PlanSearchFunction>().also {
        coEvery {
            it.invoke(scratchDomain, scratchProblem, any())
        } returns listOf(Task.create("show_menu", "user"))

        coEvery {
            it.invoke(simpleServiceDomain, simpleServiceProblem, any())
        } returns listOf()
    }
    override val searchPlan: PlanSearchFunction = searchPlanMock
}