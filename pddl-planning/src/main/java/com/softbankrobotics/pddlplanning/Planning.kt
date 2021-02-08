package com.softbankrobotics.pddlplanning

import com.softbankrobotics.pddlplanning.ontology.*
import com.softbankrobotics.pddlplanning.utils.*

typealias LogFunction = (String) -> Unit
typealias PlanSearchFunction = (String, String, LogFunction?) -> Tasks

fun adaptProblemAndSearchPlan(
    domain: String,
    problemBase: String,
    objects: Iterable<Instance>,
    init: Iterable<Fact>,
    log: LogFunction? = null,
    searchPlan: PlanSearchFunction
): Tasks {

    if (log != null) log("Current objects:\n${objects.joinToString("\n")}")
    var problem = replaceObjects(problemBase, objects)
    if (log != null) log("Initial state:\n${init.joinToString("\n")}")
    problem = replaceInit(problem, init)
    val startTime = System.currentTimeMillis()
    val plan = searchPlan(domain, problem, log)
    val planTime = System.currentTimeMillis() - startTime
    if (log != null) log("Found plan in ${planTime}ms:\n${plan.joinToString(")\n(", "(", ")")}")
    return plan
}

fun adaptProblemAndSearchPlan(
    domain: String,
    problemBase: String,
    objects: Iterable<Instance>,
    init: Iterable<Fact>,
    goals: Goals,
    log: LogFunction? = null,
    searchPlan: PlanSearchFunction
): Tasks {

    if (log != null) log("Goals:\n${goals.joinToString("\n")}")
    val problem = replaceGoal(problemBase, goals)
    return adaptProblemAndSearchPlan(domain, problem, objects, init, log, searchPlan)
}

/**
 * Search plan using PDDL Ontology.
 */
fun searchPlan(
    types: Collection<Type>,
    constants: Collection<Instance>,
    predicates: Collection<Expression>,
    actions: Collection<Action>,
    objects: Collection<Instance>,
    init: Collection<Fact>,
    goals: Collection<Goal>,
    planSearchFunction: PlanSearchFunction
): Tasks {
    val domain = createDomain(types, constants, predicates, actions)
    // Sometimes constants may be repeated in the :init section.
    // Certain planners do not like this, so let us avoid that.
    val problem = createProblem(objects - constants, init, goals)
    return planSearchFunction(domain, problem, null)
}

/**
 * Check problems in PDDL before searching plan using PDDL Ontology.
 */
fun checkProblemAndSearchPlan(
    types: Collection<Type>,
    constants: Collection<Instance>,
    predicates: Collection<Expression>,
    actions: Collection<Action>,
    objects: Collection<Instance>,
    init: Collection<Fact>,
    goals: Collection<Goal>,
    planSearchFunction: PlanSearchFunction,
    log: LogFunction? = null
): Tasks {
    if (log != null)
        analyzeUnsatisfiedGoals(objects, init, goals, log)
    checkErrors(types, constants, predicates, actions, objects, init, goals)
    return searchPlan(
        types,
        constants,
        predicates,
        actions,
        objects,
        init,
        goals,
        planSearchFunction
    )
}

fun analyzeUnsatisfiedGoals(
    objects: Collection<Instance>,
    init: Collection<Fact>,
    goals: Collection<Goal>,
    log: LogFunction
) {
    log("Goal analysis for state:\n${init.joinToString("\n")}")
    val evaluatedGoals = goals.map { it to evaluateExpression(it, objects, init) }
    log("Evaluated goals:\n${evaluatedGoals.joinToString("\n") { 
        "${if (it.second) 10003.toChar() else 10060.toChar()} ${it.first}"
    }}")
}

fun checkErrors(
    types: Collection<Type>,
    constants: Collection<Instance>,
    predicates: Collection<Expression>,
    actions: Collection<Action>,
    objects: Collection<Instance>,
    init: Collection<Fact>,
    goals: Collection<Goal>
) {
    // Check that no unknown predicate is used...
    fun checkMissing(actual: Set<String>, expected: Set<String>, context: String) {
        val missing = actual - expected
        if (missing.isNotEmpty())
            throw IllegalArgumentException("$context use undefined predicates $missing")
    }
    val expectedPredicateNames = predicates.map { it.word }.toSet()
    // ... in actions...
    val involvedPredicateNamesInActions =
            actions.flatMap { extractPredicates(it.precondition)
                    .plus(extractPredicates(it.effect)) }
                    .toSet()
    checkMissing(involvedPredicateNamesInActions, expectedPredicateNames, "actions")
    // ... and in goals.
    val involvedPredicateNamesInGoals = goals.flatMap { extractPredicates(it) }.toSet()
    checkMissing(involvedPredicateNamesInGoals, expectedPredicateNames, "goals")

    // Check whether goals mention predicates that are not mentioned in the effects of actions.
    val goalPredicates =
        goals.flatMap { extractConsequentPredicatesFromExpression(it) }.distinct()
    val actionPredicates =
        actions.flatMap { extractConsequentPredicatesFromExpression(it.effect) }.distinct()
    val unmanagedPredicates = goalPredicates.filter { goal ->
        actionPredicates.none { action ->
            goal.values.any { it in action.values }
        }
    }
    if (unmanagedPredicates.isNotEmpty())
        throw RuntimeException("no action produces effect involving predicates required in goals $unmanagedPredicates")

    // TODO: check that instances are not unmanaged
    // TODO: check that all predicates are declared
    // TODO: check for explicitly contradictory goals
}
