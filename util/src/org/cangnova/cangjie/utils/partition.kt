package org.cangnova.cangjie.utils

inline fun <A, reified B : A> List<A>.partitionIsInstance(): Pair<List<B>, List<A>> {
    val matching = mutableListOf<B>()
    val remaining = mutableListOf<A>()

    for (element in this) {
        if (element is B) {
            matching += element
        } else {
            remaining += element
        }
    }

    return matching to remaining
}
