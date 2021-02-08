package com.softbankrobotics.pddlplanning.utils

import java.util.concurrent.ConcurrentHashMap

/**
 * Regroups named objects with unique string identifiers (aka names).
 */
typealias Index<T> = Map<String, T>

/**
 * Creates an index from a name mapping and a list of objects.
 */
fun <T> indexOf(nameOf: (T) -> String, vararg elements: T): Index<T> = elements.associateBy(nameOf)

/**
 * Helper to create an index from named objects.
 */
fun <T : Named> indexOf(vararg elements: T): Index<T> = elements.associateBy { it.name }

/**
 * Helper to quickly make an index from a set of named objects.
 */
inline fun <reified T> Set<T>.toIndex(noinline nameOf: (T) -> String): Index<T> {
    return indexOf(nameOf, *this.toTypedArray())
}

/**
 * Helper to quickly make an index from a set of named objects.
 */
inline fun <reified T : Named> Set<T>.toIndex(): Index<T> {
    return indexOf(*this.toTypedArray())
}

/**
 * A mutable index, thread-safe.
 * Allows only operations that guarantees that both values and names are unique.
 */
class MutableIndex<T>(private val nameOf: (T) -> String) : Index<T> {

    // Map-related
    private val data = ConcurrentHashMap<String, T>()
    override val entries: Set<Map.Entry<String, T>> get() = data.entries
    override val keys: Set<String> get() = data.keys
    override val size: Int get() = data.size
    override val values: Collection<T> = data.values
    override fun containsKey(key: String): Boolean = data.containsKey(key)
    override fun containsValue(value: T): Boolean = data.containsValue(value)
    override fun get(key: String): T? = data[key]
    override fun isEmpty(): Boolean = data.isEmpty()

    /**
     * Adds the object to the index if not already found.
     * @throws IllegalStateException If a different object is found under the same name.
     */
    fun ensure(incoming: T) = synchronized(this) {
        val existing = data[nameOf(incoming)]
        if (existing != null) {
            assertNoConflict(incoming, existing, nameOf)
        } else {
            data[nameOf(incoming)] = incoming
        }
    }

    /**
     * Removes the object from the index if not already absent.
     * @throws IllegalStateException If a different object is found under the same name.
     */
    fun remove(incoming: T) = synchronized(this) {
        val existing = data[nameOf(incoming)]
        if (existing != null) {
            assertNoConflict(existing, incoming, nameOf)
            data.remove(nameOf(incoming))
        }
    }

    companion object {
        /**
         * Checks that the two types do not have the same name while being different.
         * @throws IllegalStateException If the types are different but have the same name.
         */
        private fun <T> assertNoConflict(existing: T, incoming: T, nameOf: (T) -> String) {
            if (nameOf(existing) == nameOf(incoming)) {
                if (existing != incoming) {
                    error(
                        "\"${nameOf(existing)}\" already exists but differs\n" +
                                "incoming: $incoming\nexisting: $existing"
                    )
                }
            }
        }
    }
}

/**
 * Creates a mutable index from a name mapping and elements.
 */
fun <T> mutableIndexOf(nameOf: (T) -> String, vararg elements: T): MutableIndex<T> {
    val index = MutableIndex(nameOf)
    elements.forEach { index.ensure(it) }
    return index
}

/**
 * Helper to create a mutable index from named objects.
 */
fun <T : Named> mutableIndexOf(vararg elements: T): MutableIndex<T> =
    mutableIndexOf<T>({ it.name }, *elements)