package com.softbankrobotics.pddlplanning

import com.softbankrobotics.pddlplanning.ontology.Fact
import com.softbankrobotics.pddlplanning.ontology.Goal
import com.softbankrobotics.pddlplanning.ontology.Instance


/** Checks that the character at the given position is '('. */
private fun assertOpenParenthesis(pddl: String, position: Int) {
    assert(pddl[position] == '(') {
        "parenthesis location $position does not point at a parenthesis, instead points at ${pddl[position]}"
    }
}

/** Find first child expression. */
private fun firstChildExpression(pddl: String, position: Int): Int? {
    assertOpenParenthesis(pddl, position)
    pddl.substring(position).forEachIndexed { i, c ->
        when (c) {
            '(' -> if (i != 0) return position + i
            ')' -> return null
        }
    }
    throw RuntimeException("malformed PDDL: parenthesis opened at $position is never closed")
}

/** Find next sibling expression. */
private fun nextSiblingExpression(pddl: String, position: Int): Int? {
    assertOpenParenthesis(pddl, position)
    var depth = 0
    pddl.substring(position).forEachIndexed { i, c ->
        when (c) {
            '(' -> if (++depth == 1 && i != 0) return position + i
            ')' -> if (--depth < 0) return null
        }
    }
    if (depth > 0)
        throw RuntimeException("malformed PDDL: parenthesis opened at $position is never closed")
    return null
}

/** Iterate using a breadth-first walk. */
private class BreadthFirstIterator(private val pddl: String, private var position: Int) :
    Iterator<Int> {
    private var next: Int? = null
    private val nextChildren = mutableListOf<Int>()

    init {
        assertOpenParenthesis(pddl, position)
        next = position
    }

    override fun hasNext(): Boolean {
        return next != null
    }

    override fun next(): Int {
        assert(hasNext())
        val result = next!!
        position = result

        // prepare next
        firstChildExpression(pddl, position)?.let { nextChildren.add(it) }
        next = nextSiblingExpression(pddl, position)
        if (next == null) {
            if (nextChildren.isNotEmpty()) {
                next = nextChildren.first()
                nextChildren.removeAt(0)
            }
        }

        return result
    }
}

fun wordAt(pddl: String, position: Int): String {
    assertOpenParenthesis(pddl, position)
    val ahead = pddl.substring(position + 1).trim()
    val endOfWord = ahead.indexOfFirst { c ->
        c.isWhitespace() || c in listOf('(', ')')
    }
    return ahead.substring(0, endOfWord)
}

private fun expressionRangeAt(pddl: String, position: Int): IntRange {
    assertOpenParenthesis(pddl, position)
    var depth = 0
    pddl.substring(position).forEachIndexed { i, c ->
        when (c) {
            '(' -> ++depth
            ')' -> if (--depth == 0) return IntRange(position, position + i)
        }
    }
    throw RuntimeException("malformed PDDL: parenthesis opened at $position is never closed")
}

/** Splits PDDL content into a domain and a problem. */
fun splitDomainAndProblem(pddlContent: String): Pair<String, String> {
    val domain = pddlContent.substringBeforeLast("(define ")
    val problem = pddlContent.substring(domain.length)
    return Pair(domain, problem)
}

/**
 * Looks for an expression starting with the given word and returns the range of the expression.
 */
fun findExpressionWithWord(problem: String, word: String): IntRange? {
    val trimmed = problem.trim()
    val iterator = BreadthFirstIterator(trimmed, 0)

    while (iterator.hasNext()) {
        val position = iterator.next()
        if (wordAt(trimmed, position) == word)
            return expressionRangeAt(trimmed, position)
    }
    return null
}

fun replaceObjects(problem: String, instances: Iterable<Instance>): String {
    val range = findExpressionWithWord(problem, ":objects")!!
    var newObjects = "(:objects\n    "
    for (instance in instances)
        newObjects += instance.declaration() + "\n    "
    newObjects += ")"
    return problem.replaceRange(range, newObjects)
}


/** Replaces the :init section of a problem with the given facts. */
fun replaceInit(problem: String, facts: Iterable<Fact>): String {
    val range = findExpressionWithWord(problem, ":init")!!
    val newInit = "(:init\n${facts.joinToString("\n  ")})"
    return problem.replaceRange(range, newInit)
}

/**
 * Replaces the :goal section of a problem with the given goals.
 */
fun replaceGoal(problem: String, goals: List<Goal>): String {
    val range = findExpressionWithWord(problem, ":goal")
    if (range == null) {
        throw RuntimeException("no goal section found in PDDL problem")
    } else {
        val newGoal = "(:goal" + when {
            goals.size > 1 -> "\n    (and\n    ${goals.joinToString("\n    ")}\n    ))"
            goals.size == 1 -> "\n    ${goals.joinToString("\n    ")}\n    )"
            else -> ")"
        }
        return problem.replaceRange(range, newGoal)
    }
}