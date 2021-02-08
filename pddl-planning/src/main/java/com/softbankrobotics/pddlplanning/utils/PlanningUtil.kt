package com.softbankrobotics.pddlplanning.utils

import com.softbankrobotics.pddlplanning.LogFunction
import com.softbankrobotics.pddlplanning.PlanSearchFunction
import com.softbankrobotics.pddlplanning.ontology.*
import com.softbankrobotics.pddlplanning.replaceInit

fun createDomain(
    types: Iterable<Type>,
    constants: Iterable<Instance>,
    predicates: Iterable<Expression>,
    actions: Iterable<Action>
): String {

    var domain = "(define (domain generated_domain)\n" +
            "(:requirements :adl :negative-preconditions :universal-preconditions)\n\n"

    // Fill the types
    domain += "(:types\n"
    val typesByParent = types.groupBy { it.parent }
    val baseTypes = typesByParent[null] ?: listOf()
    for (parentToTypes in typesByParent) {
        if (parentToTypes.key == null)
            continue // base types are put after all other types
        domain += "    ${parentToTypes.value.joinToString(" ") { it.name }} - ${parentToTypes.key!!.name}\n"
    }
    domain += "    ${baseTypes.joinToString(" ") { it.name }}\n"
    domain += ")\n\n"

    // Fill the constants.
    val constantTypeToNames = mutableMapOf<Type, MutableList<String>>()
    val constantNamesWithNoType = mutableListOf<String>()
    for (constant in constants) {
        if (constant.type != null)
            constantTypeToNames.getOrPut(constant.type!!, { mutableListOf() }).add(constant.name)
        else
            constantNamesWithNoType.add(constant.name)
    }
    domain += "(:constants\n"
    for (instanceType in constantTypeToNames) {
        domain += "   "
        for (instanceName in instanceType.value)
            domain += " $instanceName"
        domain += " - ${instanceType.key.name}\n"
    }
    for (instanceName in constantNamesWithNoType)
        domain += " $instanceName\n"
    domain += ")\n\n"

    // Fill the predicates
    domain += "(:predicates"
    domain += predicates.joinToString("\n    ", "\n    ", "\n") {
        it.toDeclarationString()
    }
    domain += ")\n\n"

    // Fill the functions
    domain += "(:functions\n    $total_cost\n    "
    domain += ")\n\n"

    // Fill the actions
    actions.forEach { domain += "$it\n\n" }

    domain += ")"
    return domain
}

fun createProblem(
    objects: Collection<Instance>,
    init: Collection<Fact>,
    goals: Collection<Goal>
): String {
    return "(define (problem sandbox_problem)\n" +
            "(:domain generated_domain)\n" +
            "(:requirements :adl :negative-preconditions :universal-preconditions)\n\n" +
            "(:objects${objects.joinToString(
                "\n  ",
                "\n  ",
                postfix = "\n"
            ) { it.toDeclarationString() }})\n\n" +
            "(:init${init.joinToString("\n  ", "\n  ", postfix = "\n")})\n\n" +
            "(:goal" + when {
        goals.size > 1 -> "\n  (and\n    ${goals.joinToString("\n    ")}\n  )\n)"
        goals.size == 1 -> "\n    ${goals.joinToString("\n    ")}\n  )"
        else -> ")"
    } + "\n(:metric minimize (total-cost)))"
}

fun searchPlanForInit(
    domain: String,
    problem: String,
    init: Facts,
    searchPlan: PlanSearchFunction,
    logFunction: LogFunction
): Tasks {
    val problemWithInit = replaceInit(problem, init)
    println("Searching plan for init:\n${init.joinToString("\n")}")
    val plan = searchPlan(domain, problemWithInit, logFunction)
    println("Found plan:\n${plan.joinToString("\n")}")
    return plan
}
