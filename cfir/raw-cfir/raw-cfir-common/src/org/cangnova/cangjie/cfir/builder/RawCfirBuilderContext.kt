package org.cangjie.cfir.builder

import org.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * Shared state for raw CFIR building.
 */
class Context<T> {
    var packageFqName: FqName = FqName.ROOT
    var inLocalContext: Boolean = false

    val arraySetArgument: MutableMap<T, CfirExpression> = mutableMapOf()

    private val localContextStack: ArrayDeque<Boolean> = ArrayDeque()
    private val functionTargets: ArrayDeque<T> = ArrayDeque()
    private val loopTargets: ArrayDeque<T> = ArrayDeque()
    private val labelNames: ArrayDeque<Name> = ArrayDeque()

    fun <R> withPackage(fqName: FqName, block: () -> R): R {
        val previous = packageFqName
        packageFqName = fqName
        return try {
            block()
        } finally {
            packageFqName = previous
        }
    }

    fun <R> withLocalContext(block: () -> R): R {
        localContextStack.addLast(inLocalContext)
        inLocalContext = true
        return try {
            block()
        } finally {
            inLocalContext = localContextStack.removeLast()
        }
    }

    fun enterFunction(target: T) {
        functionTargets.addLast(target)
    }

    fun exitFunction() {
        if (functionTargets.isNotEmpty()) {
            functionTargets.removeLast()
        }
    }

    fun currentFunctionTarget(): T? = functionTargets.lastOrNull()

    fun enterLoop(target: T) {
        loopTargets.addLast(target)
    }

    fun exitLoop() {
        if (loopTargets.isNotEmpty()) {
            loopTargets.removeLast()
        }
    }

    fun currentLoopTarget(): T? = loopTargets.lastOrNull()

    fun pushLabel(name: Name) {
        labelNames.addLast(name)
    }

    fun popLabel() {
        if (labelNames.isNotEmpty()) {
            labelNames.removeLast()
        }
    }

    fun currentLabel(): Name? = labelNames.lastOrNull()
}

