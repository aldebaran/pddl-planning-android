package com.softbankrobotics.pddlplanning

import android.os.Parcel
import android.os.Parcelable
import com.softbankrobotics.pddlplanning.utils.Index
import com.softbankrobotics.pddlplanning.utils.Named
import com.softbankrobotics.pddlplanning.utils.mutableIndexOf
import java.io.Serializable

/** Top class to perform logical manipulation. */
sealed class LogicalExpression

/** Represents a logical value: a boolean! */
data class LogicalValue(val value: Boolean): LogicalExpression()

/**
 * A simple PDDL expression.
 * It can be empty, but in this case, args must be empty too.
 */
open class Expression(val word: String = "", vararg val args: Expression) : LogicalExpression(), Serializable {

    init {
        if (isEmpty()) assert(args.isEmpty())
    }

    fun isEmpty(): Boolean {
        return word.isEmpty()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Expression) return false
        return word == other.word
                && args.contentEquals(other.args)
    }

    override fun hashCode(): Int {
        return word.hashCode() + args.sumOf { it.hashCode() }
    }

    override fun toString(): String {
        return args.joinToString(
            prefix = "($word",
            transform = { " $it" },
            postfix = ")",
            separator = ""
        )
    }

    fun toDeclarationString(): String {
        val thisAsInstance = this as? Instance
        if (thisAsInstance != null)
            return thisAsInstance.declaration()
        return args.joinToString(
            prefix = "($word",
            transform = { " ${it.toDeclarationString()}" },
            postfix = ")",
            separator = ""
        )
    }

    infix fun isNegationOf(other: Expression): Boolean = equals(
        negationOf(other)
    )
}

fun negationOf(fact: Expression): Expression {
    return if (fact.word == not_operator_name) {
        assert(fact.args.size == 1)
        fact.args[0]
    } else {
        not(fact)
    }
}

// Types / Instances
/**
 * Represents a type. Can have a parent.
 * A factory to create instances of the type is required
 * so that the JVM type system can also be used to check the JVM type of the instances,
 * an thus leverage build-time type checking.
 * Every type ever constructed can be found back by their name, in the index of types.
 * Note that this index must remain unique to be consistent with the JVM type system.
 * @param createInstance A factory to create typed instances of this type.
 * @param name PDDL name of the type. Only ASCII alphanumerical characters and "_" are supported.
 * @param parent The parent type, as in a class hierarchy.
 * @throws IllegalStateException If a different type with the same name exists in the type system.
 */
data class Type(
        override val name: String,
        val parent: Type?,
        val createInstance: (String) -> Instance) : Named {

    init {
        mutableIndex.ensure(this)
    }

    override fun toString(): String {
        return if (parent != null) "$parent > $name" else name
    }

    companion object {
        private val mutableIndex = mutableIndexOf<Type>()
        val index: Index<Type> = mutableIndex
    }
}

interface Typed {
    val type: Type
}

/**
 * Represents an instance. Can have a type.
 * Only the name matters for comparisons and hash.
 */
open class Instance(override val name: String) : Named, Typed, Expression(name) {
    override val type: Type = Companion.type
    override fun toString(): String = name

    /**
     * Equals comparing instances by their names.
     * @throws IllegalStateException if they have the same name but are different.
     */
    override fun equals(other: Any?): Boolean {
        return if (other is Instance) {
            if (name == other.name && type != other.type) {
                error("mismatching types ($type vs. ${other.type}) " +
                        "for two instances with the same name \"$name\"")
            }
            name == other.name
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    /** PDDL representation of the declaration of the instance. */
    fun declaration(): String {
        // When type is omitted in PDDL, it already means "object"
        val typeSpecifier = if (type != Companion.type) " - ${type.name}" else " - object"
        return "$this$typeSpecifier"
    }

    companion object : Typed {

        override val type: Type = Type("object", null) { Instance(it) }

        /**
         * Creates an instance from a text representation.
         */
        fun create(name: String, typeName: String): Instance {
            val type = Type.index[typeName] ?: error("no such type \"$typeName\"")
            return type.createInstance(name)
        }
    }
}

// Operators
const val imply_operator_name = "imply"
fun imply(antecedent: Expression, consequent: Expression) =
    Expression(imply_operator_name, antecedent, consequent)

const val when_operator_name = "when"
fun `when`(antecedent: Expression, consequent: Expression) =
    Expression(when_operator_name, antecedent, consequent)

const val forall_operator_name = "forall"
fun forall(instance: Instance, predicate: Expression) =
    Expression(
        forall_operator_name,
        Expression(instance.declaration()),
        predicate
    ) //TODO: how to parse instance correctly?

const val exists_operator_name = "exists"
fun exists(instance: Instance, predicate: Expression) =
    Expression(
        exists_operator_name,
        Expression(instance.declaration()),
        predicate
    )

// Operators
// TODO: rename name constants with capital letters.
const val not_operator_name = "not"
fun not(arg: Expression) =
    Expression(
        not_operator_name,
        arg
    )

const val and_operator_name = "and"
fun and(vararg args: Expression) =
    Expression(
        and_operator_name,
        *args
    )

const val or_operator_name = "or"
fun or(vararg args: Expression) =
    Expression(
        or_operator_name,
        *args
    )

// Manipulating costs
/**
 * total_cost is a PDDL function usually found in various planners, as a unique measure of optimality.
 */
val total_cost = Expression("total-cost")

const val assignment_operator_name = "="
const val increase_operator_name = "increase"
fun assign(numericFluent: Expression, amount: Int) =
    Fact(assignment_operator_name, numericFluent, Instance("$amount"))
val initialCostIsZero = assign(total_cost, 0)
fun increase(numericFluent: Expression, amount: Int) =
    Expression(increase_operator_name, numericFluent, Instance("$amount"))
fun increaseCost(amount: Int) = increase(total_cost, amount)
fun increaseCostToAmout(exp: Expression): Int {
    if (exp.args.size == 2) {
        val number = exp.args[1].word
        return number.toInt()
    }
    throw RuntimeException("Increase cost expression should have 2 arguments")
}

typealias Fact = Expression
typealias Facts = Collection<Fact>

/**
 * Simplified creation of generic expressions only separated with spaces.
 * Most useful to declare simple, non-negative, facts.
 */
fun createFact(vararg args: String): Expression {
    assert(args.isNotEmpty())
    val nextArgs = args.slice(IntRange(1, args.size - 1)).map {
        Instance(
            it
        )
    }.toTypedArray()
    return Expression(
        args.first(),
        *nextArgs
    )
}

// Action
data class Action(
    val name: String,
    val parameters: List<Instance>,
    val precondition: Expression,
    val effect: Expression
) {
    override fun toString(): String {
        return "(:action $name\n" +
                "    :parameters (${parameters.joinToString(" ") { it.declaration() }})\n" +
                "    :precondition $precondition\n" +
                "    :effect $effect\n" +
                ")"
    }
}

// Goal
typealias Goal = Expression
typealias Goals = List<Goal>

/**
 * A task is a fully-determined action to perform.
 * In other words, it is an action plus the instances to use as parameters.
 */
// Note that "data" is a bit like "regular", leading to adequate comparison operators.
data class Task(val action: String, val parameters: List<String> = listOf()) : Parcelable {
    override fun toString(): String {
        return if (parameters.isEmpty()) action
        else "$action ${parameters.joinToString(" ")}"
    }

    constructor(source: Parcel) : this(
        source.readString()!!,
        source.createStringArrayList() as List<String>
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(action)
        writeStringList(parameters)
    }

    companion object {
        fun create(vararg args: String): Task {
            assert(args.isNotEmpty())
            //assertion needs to be that the size is at least 2, not at least 1 otherwise slice will throw
            return Task(
                args.first(),
                args.slice(IntRange(1, args.size - 1))
            )
        }

        @JvmField
        val CREATOR: Parcelable.Creator<Task> = object : Parcelable.Creator<Task> {
            override fun createFromParcel(source: Parcel): Task =
                Task(source)
            override fun newArray(size: Int): Array<Task?> = arrayOfNulls(size)
        }
    }
}
typealias Tasks = List<Task>
