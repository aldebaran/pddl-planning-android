package com.softbankrobotics.pddlplanning.test

import android.content.Context
import android.util.Log
import com.softbankrobotics.pddlplanning.Expression
import com.softbankrobotics.pddlplanning.Task
import com.softbankrobotics.pddlplanning.Tasks
import com.softbankrobotics.pddlplanning.createFact
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
        val plan = runBlocking { searchPlan(domain, problem) { Log.d(logTag, it) } }
        assertEquals(expectedPlan, plan)
    }

    @Test
    fun simpleServiceDomainInstrumented() {
        val (domain, problem) = domainAndProblemFromRaw(context, R.raw.sample_2)
        val initsToPlans = mutableListOf<ExpressionToTask>()

        initsToPlans.add(
            listOf<Expression>() // Nothing to say about user.
                    to listOf() // Robot has nothing to do.
        )

        initsToPlans.add(
            listOf( // A user entered interaction.
                createFact("interacting_with", "user")
            ) to listOf( // Robots checks them in.
                Task.create("greet", "user"),
                Task.create("check_in", "user")
            )
        )

        runBlocking { checkPlansForInits(domain, problem, initsToPlans, searchPlan) { Log.d(logTag, it) } }
    }

    @Test
    fun baseExample() {
        val plan = searchPlanFromResource(R.raw.example_base)
        assertEquals(
            plan,
            listOf(
                Task.create("joke_with", "alice"),
                Task.create("joke_with", "bob")
            )
        )
    }

    @Test
    fun aliceIsAlreadyHappy() {
        val plan = searchPlanFromResource(R.raw.example_alice_happy)
        assertEquals(
            plan,
            listOf(
                Task.create("joke_with", "bob")
            )
        )
    }

    @Test
    fun everybodyIsHappy() {
        val plan = searchPlanFromResource(R.raw.example_everybody_happy)
        assertEquals(
            plan,
            listOf<Task>()
        )
    }

    @Test
    fun nobodyAroundError() {
        assertThrows(Throwable::class.java) {
            searchPlanFromResource(R.raw.example_nobody_around_error)
        }
    }

    @Test
    fun nobodyAroundConditionalGoal() {
        val plan = searchPlanFromResource(R.raw.example_nobody_around_conditional_goal)
        assertEquals(
            plan,
            listOf<Task>()
        )
    }

    @Test
    fun aliceIsAroundConditionalGoal() {
        val plan = searchPlanFromResource(R.raw.example_alice_around_conditional_goal)
        assertEquals(
            plan,
            listOf(
                Task.create("joke_with", "alice")
            )
        )
    }

    @Test
    fun intrinsicAbsence() {
        val plan = searchPlanFromResource(R.raw.example_intrinsic_absence)
        assertEquals(plan, listOf(Task.create("joke_with", "alice")))
    }

    @Test
    fun nonDeterministic() {
        val plan = searchPlanFromResource(R.raw.example_non_deterministic)
        assertEquals(
            plan,
            listOf(
                Task.create("find_human", "alice"),
                Task.create("find_human", "bob"),
                Task.create("joke_with", "bob"),
                Task.create("joke_with", "alice")
            )
        )
    }

    @Test
    fun partialObservability() {
        val plan = searchPlanFromResource(R.raw.example_partial_observability)
        assertEquals(
            plan,
            listOf(
                Task.create("find_human", "alice"),
                Task.create("find_human", "bob"),
                Task.create("joke_with", "bob"),
                Task.create("joke_with", "alice")
            )
        )
    }

    @Test
    fun partialObservabilityNext() {
        val plan = searchPlanFromResource(R.raw.example_partial_observability_next)
        assertEquals(
            plan,
            listOf(
                Task.create("find_human", "bob"),
                Task.create("joke_with", "bob")
            )
        )
    }

    @Test
    fun objectDiscoveryNobody() {
        val plan = searchPlanFromResource(R.raw.example_object_discovery_nobody)
        assertEquals(
            plan,
            listOf(
                Task.create("find_human", "someone"),
                Task.create("joke_with", "someone")
            )
        )
    }

    @Test
    fun objectDiscovery() {
        val plan = searchPlanFromResource(R.raw.example_object_discovery)
        assertEquals(
            plan,
            listOf(
                Task.create("find_human", "alice"),
                Task.create("joke_with", "alice")
            )
        )
    }

    @Test
    fun objectDiscoveryNext() {
        val plan = searchPlanFromResource(R.raw.example_object_discovery_next)
        assertEquals(plan, listOf(Task.create("joke_with", "charles")))
    }

    private fun searchPlanFromResource(resource: Int): List<Task> {
        return searchPlanFromResource(context, resource, searchPlan, ::logDebug)
    }
}
