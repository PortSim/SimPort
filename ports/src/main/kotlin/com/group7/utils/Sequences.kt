package com.group7.utils

/**
 * Zips two sequences together. Requires `other` to be longer than `this`.
 *
 * @param other the other sequence to be zipped with
 */
internal fun <A, B> Sequence<A>.zipCompletely(other: Sequence<B>): Sequence<Pair<A, B>> = sequence {
    val it1 = this@zipCompletely.iterator()
    val it2 = other.iterator()

    while (it1.hasNext() && it2.hasNext()) {
        yield(it1.next() to it2.next())
    }

    check(!it1.hasNext()) { "Elements were left unzipped" }
}
