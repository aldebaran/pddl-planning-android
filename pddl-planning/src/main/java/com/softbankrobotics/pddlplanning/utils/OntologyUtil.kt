package com.softbankrobotics.pddlplanning.utils

import com.softbankrobotics.pddlplanning.*

/**
 * Check whether a fact involves the given instance.
 */
fun Fact.contains(instance: Instance): Boolean {
    return containsAnyOf(setOf(instance))
}

/**
 * Check whether a fact involves any of the given instances.
 */
fun Fact.containsAnyOf(instances: Iterable<Instance>): Boolean {
    if (word == not_operator_name) {
        return args.first().containsAnyOf(instances)
    } else {
        val instanceNames = instances.map { it.name }.toSet()
        for (arg in args) {
            if (instanceNames.contains(arg.word)) {
                return true
            }
        }
        return false
    }
}

/**
 * Whather the given instance is involved in a task.
 */
fun Task.contains(instance: Instance): Boolean {
    return parameters.any { it == instance.name }
}

/**
 * Apply parameter values.
 */
fun applyParameters(expression: Expression, parameters: Map<Instance, Instance>): Expression {
    val updatedArgs = when (expression.word) {
        not_operator_name, and_operator_name, or_operator_name, imply_operator_name, when_operator_name ->
            expression.args.map { applyParameters(it, parameters) }
        forall_operator_name, exists_operator_name ->
            listOf(expression.args[0], applyParameters(expression.args[1], parameters))
        else -> expression.args.map { parameters[it] ?: it }
    }
    return Expression(expression.word, *updatedArgs.toTypedArray())
}

/**
 * Parse instance from parameter expression.
 * Useful for forall and exists statements.
 */
private fun parseParameter(expression: Expression): Instance {
    val parameterDeclaration = expression.word
    val matches = Regex("([\\w?]+)\\s*?-?\\s*?(\\w+)?").matchEntire(parameterDeclaration)
        ?: throw RuntimeException("parameter declaration is badly formed: \"$parameterDeclaration\"")
    val stringMatches = matches.groupValues
    val name = stringMatches[1]
    val type = if (stringMatches.size > 2) stringMatches[2] else "object"
    return Instance.create(name, type)
}

/**
 * Expand a forall or an exists statement.
 */
private fun expandUniversalOperator(
    expression: Expression,
    objects: Collection<Instance>
): List<Expression> {
    val parameter = parseParameter(expression.args[0])
    return objects.mapNotNull {
        if (it.type == parameter.type)
            applyParameters(expression.args[1], mapOf(parameter to it))
        else null
    }
}

/**
 * Evaluates an expression.
 */
fun evaluateExpression(
    expression: Expression,
    objects: Collection<Instance>,
    init: Collection<Fact>,
    log: LogFunction? = null
): Boolean {

    val result = when (expression.word) {
        and_operator_name -> expression.args.all { evaluateExpression(it, objects, init, log) }
        or_operator_name -> expression.args.any { evaluateExpression(it, objects, init, log) }
        not_operator_name -> !evaluateExpression(expression.args[0], objects, init, log)
        forall_operator_name -> {
            val expressions = expandUniversalOperator(expression, objects)
            expressions.all { evaluateExpression(it, objects, init, log) }
        }
        exists_operator_name -> {
            val expressions = expandUniversalOperator(expression, objects)
            expressions.any { evaluateExpression(it, objects, init, log) }
        }
        imply_operator_name, when_operator_name -> {
            val premiseTruth = evaluateExpression(expression.args[0], objects, init, log)
            val conclusionTruth = evaluateExpression(expression.args[1], objects, init, log)
            !premiseTruth || conclusionTruth
        }

        "" -> throw RuntimeException("cannot evaluate empty expression")

        else -> init.contains(expression)
    }
    if (log != null)
        log("${if (result) 10003.toChar() else 10060.toChar()} $expression")
    return result
}

/**
 * A set of values that can be compared to a single value.
 */
data class Either<T>(val values: Collection<T>) {

    constructor(value: T) : this(setOf(value))

    override fun toString(): String {
        return if (values.size == 1)
            values.first().toString()
        else
            "{${values.joinToString(", ") { it.toString() }}}"
    }

    companion object {
        fun <T> ofEither(values: Collection<Either<T>>): Either<T> {
            return Either(values.flatMap { it.values })
        }
    }
}

/** Extract all the predicates involved in the consequences in the given expression. */
fun extractConsequentPredicatesFromExpression(expression: Expression): Set<Either<String>> {
    return when (expression.word) {
        assignment_operator_name, increase_operator_name -> setOf()
        not_operator_name -> extractConsequentPredicatesFromExpression(expression.args[0])
        and_operator_name ->
            expression.args.flatMap { extractConsequentPredicatesFromExpression(it) }.toSet()
        or_operator_name ->
            setOf(
                Either.ofEither(
                    expression.args.flatMap { extractConsequentPredicatesFromExpression(it) }
                )
            )
        imply_operator_name, when_operator_name -> // Ignoring the antecedent here
            extractConsequentPredicatesFromExpression(expression.args[1])
        forall_operator_name, exists_operator_name ->
            extractConsequentPredicatesFromExpression(expression.args[1])
        "" -> return setOf()
        else -> setOf(Either(expression.word))
    }
}

/**
 * Extract all the predicates involved in the given expression.
 */
fun extractPredicates(expression: Expression): Set<String> {
    return when (expression.word) {
        assignment_operator_name, increase_operator_name -> setOf()
        not_operator_name -> extractPredicates(expression.args[0])
        and_operator_name -> expression.args.flatMap { extractPredicates(it) }.toSet()
        or_operator_name -> expression.args.flatMap { extractPredicates(it) }.toSet()
        imply_operator_name, when_operator_name ->
            extractPredicates(expression.args[0]).plus(extractPredicates(expression.args[1]))
        forall_operator_name, exists_operator_name -> extractPredicates(expression.args[1])
        "" -> return setOf()
        else -> return setOf(expression.word)
    }
}
