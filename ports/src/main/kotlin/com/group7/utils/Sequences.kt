package com.group7.utils

internal fun <A, B> Sequence<A>.zipCompletely(other: Sequence<B>): Sequence<Pair<A, B>> = sequence {
    val it1 = this@zipCompletely.iterator()
    val it2 = other.iterator()

    while (it1.hasNext() && it2.hasNext()) {
        yield(it1.next() to it2.next())
    }

    check(!it1.hasNext()) { "Elements were left unzipped" }
}
