package com.softbankrobotics.pddlplanning.test

import android.content.Context
import com.softbankrobotics.pddlplanning.LogFunction
import com.softbankrobotics.pddlplanning.PlanSearchFunction
import com.softbankrobotics.pddlplanning.ontology.Expression
import com.softbankrobotics.pddlplanning.ontology.Tasks
import com.softbankrobotics.pddlplanning.splitDomainAndProblem
import com.softbankrobotics.pddlplanning.utils.searchPlanForInit
import org.junit.Assert


/**
 * Checks whether the right plan is found for a given init in a problem.
 * Note that it replaces the init of the problem.
 */
fun checkPlanForInit(
    domain: String,
    problem: String,
    init: Collection<Expression>,
    searchPlan: PlanSearchFunction,
    logFunction: LogFunction,
    expectedPlan: Tasks
) {
    val plan = searchPlanForInit(domain, problem, init, searchPlan, logFunction)
    Assert.assertEquals(expectedPlan, plan)
}

/**
 * Checks whether the right plans are found for given inits in a problem.
 * Note that it replaces the init of the problem all along.
 * The problem ends up with the last init.
 */
fun checkPlansForInits(
    domain: String, problem: String,
    initsToExpectedPlans: ExpressionsToTasks,
    searchPlan: PlanSearchFunction,
    logFunction: LogFunction
) {
    initsToExpectedPlans.forEach { initToExpectedPlan ->
        checkPlanForInit(
            domain,
            problem,
            initToExpectedPlan.first,
            searchPlan,
            logFunction,
            initToExpectedPlan.second
        )
    }
}

/**
 * Loads a domain & problem from a raw resources, and runs a planning function on it.
 * The result plan is printed to the debug output.
 */
fun searchPlanAndPrint(
    context: Context,
    resourceName: String,
    searchPlan: PlanSearchFunction,
    logFunction: LogFunction
) {
    val pddl = stringFromRawResourceName(context, resourceName)
    val (domain, problem) = splitDomainAndProblem(pddl)
    val plan = searchPlan(domain, problem, logFunction)
    logFunction("Plan: $plan")
}

/**
 * Reads PDDL domain and problem from a raw resource, given its identifier.
 */
fun domainAndProblemFromRaw(context: Context, id: Int): Pair<String, String> {
    val pddl = stringFromRawResource(context, id)
    println("Using base PDDL: $pddl")
    return splitDomainAndProblem(pddl)
}

fun stringFromRawResource(context: Context, id: Int): String {
    val input = context.resources.openRawResource(id)
    return String(input.readBytes(), Charsets.UTF_8)
}

fun stringFromRawResourceName(context: Context, resourceName: String): String {
    val resourceId =
        context.resources.getIdentifier(resourceName, "raw", context.packageName)
    return stringFromRawResource(context, resourceId)
}
