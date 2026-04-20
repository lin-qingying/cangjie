package org.cangnova.cangjie.analysis.api.platform.utils

import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.utils.addIfNotNull
import org.cangnova.cangjie.utils.partitionIsInstance

@CaPlatformInterface
class SublistMerger<A : Any>(
    initialElements: List<A>,
    val destination: MutableList<A>,
) {
    var remainingElements: List<A> = initialElements

    inline fun <reified R : A> merge(create: (List<R>) -> A?) {
        val (specificElements, remainingElements) = this.remainingElements.partitionIsInstance<_, R>()
        destination.addIfNotNull(create(specificElements))
        this.remainingElements = remainingElements
    }

    fun finish() {
        destination.addAll(remainingElements)
        remainingElements = emptyList()
    }
}

@CaPlatformInterface
fun <A : Any> List<A>.mergeInto(destination: MutableList<A>, f: SublistMerger<A>.() -> Unit) {
    SublistMerger(this, destination).apply {
        f()
        finish()
    }
}

@CaPlatformInterface
fun <A : Any> List<A>.mergeWith(f: SublistMerger<A>.() -> Unit): List<A> =
    mutableListOf<A>().also { destination -> mergeInto(destination, f) }

@CaPlatformInterface
inline fun <A : Any, reified R : A> List<A>.mergeOnly(crossinline create: (List<R>) -> A?): List<A> =
    mergeWith { merge<R>(create) }
