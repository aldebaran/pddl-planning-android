package com.softbankrobotics.pddlplanning.test

import android.content.Context
import android.util.Log
import com.example.pddl_planning_test.R
import com.softbankrobotics.pddlplanning.ontology.Expression
import com.softbankrobotics.pddlplanning.ontology.Task
import com.softbankrobotics.pddlplanning.ontology.Tasks
import com.softbankrobotics.pddlplanning.ontology.createFact
import org.junit.Assert.assertEquals
import org.junit.Test

typealias ExpressionToTask = Pair<Collection<Expression>, Tasks>
typealias ExpressionsToTasks = List<Pair<Collection<Expression>, Tasks>>

/**
 * Provides a set of planning tests to add to your class of Android instrumented tests.
 * Extend this interface to inherit exposed tests that will check whether your planner works.
 */
interface PlanningInstrumentedTest : PlanningUnitTest {

    /**
     * A tag to use for the logs produced by the planning tests.
     */
    val logTag: String

    /**
     * The target context, usually provided by InstrumentationRegistry.
     */
    val context: Context

    override fun logDebug(message: String) { Log.d(logTag, message) }

    @Test
    fun plannerSanityCheck() {
        val (domain, problem) = domainAndProblemFromRaw(context, R.raw.sample_1)
        val expectedPlan = listOf(Task("ask"))
        val plan = searchPlan(domain, problem) { Log.d(logTag, it) }
        assertEquals(expectedPlan, plan)
    }

    @Test
    fun simpleServiceDomainInstrumented() {
        val (domain, problem) = domainAndProblemFromRaw(context, R.raw.sample_2)
        val initsToPlans = mutableListOf<ExpressionToTask>()

        // With PDDL4J, only AStar + SUM works.
        initsToPlans.add(
            listOf<Expression>() // Nothing to say about user.
                    to listOf() // Robot has nothing to do.
        )

        // Did manage to make it not work in PDDL4J.
        initsToPlans.add(
            listOf( // A user entered interaction.
                createFact("interacting_with", "user")
            ) to listOf( // Robots checks them in.
                Task.create("greet", "user"),
                Task.create("check_in", "user")
            )
        )

        // Fast-downward works fine for all these problems.
        checkPlansForInits(domain, problem, initsToPlans, searchPlan) { Log.d(logTag, it) }
    }

    @Test
    fun baseExample() {
        searchPlanAndPrint(context, "example_base", searchPlan, ::logDebug)
    }

    @Test
    fun aliceIsAlreadyHappy() {
        searchPlanAndPrint(context, "example_alice_happy", searchPlan, ::logDebug)
    }

    @Test
    fun everybodyIsHappy() {
        searchPlanAndPrint(context, "example_alice_happy", searchPlan, ::logDebug)
    }

    @Test
    fun nobodyAroundError() {
        searchPlanAndPrint(context, "example_nobody_around_error", searchPlan, ::logDebug)
    }

    @Test
    fun nobodyAroundConditionalGoal() {
        searchPlanAndPrint(
            context,
            "example_nobody_around_conditional_goal",
            searchPlan,
            ::logDebug
        )
    }

    @Test
    fun aliceIsAroundConditionalGoal() {
        searchPlanAndPrint(context, "example_alice_around_conditional_goal", searchPlan, ::logDebug)
    }

    @Test
    fun intrinsicAbsence() {
        searchPlanAndPrint(context, "example_intrinsic_absence", searchPlan, ::logDebug)
    }

    @Test
    fun nonDeterministic() {
        searchPlanAndPrint(context, "example_non_deterministic", searchPlan, ::logDebug)
    }

    @Test
    fun partialObservability() {
        searchPlanAndPrint(context, "example_partial_observability", searchPlan, ::logDebug)
    }

    @Test
    fun partialObservabilityNext() {
        searchPlanAndPrint(context, "example_partial_observability_next", searchPlan, ::logDebug)
    }

    @Test
    fun objectDiscoveryNobody() {
        searchPlanAndPrint(context, "example_object_discovery_nobody", searchPlan, ::logDebug)
    }

    @Test
    fun objectDiscovery() {
        searchPlanAndPrint(context, "example_object_discovery", searchPlan, ::logDebug)
    }

    @Test
    fun objectDiscoveryNext() {
        searchPlanAndPrint(context, "example_object_discovery_next", searchPlan, ::logDebug)
    }
}
