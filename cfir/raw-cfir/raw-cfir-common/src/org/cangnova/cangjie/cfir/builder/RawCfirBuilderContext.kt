package org.cangnova.cangjie.cfir.builder

import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * Shared state for raw CFIR building.
 */
class Context<T> {

    lateinit var packageFqName: FqName
    var className: FqName = FqName.ROOT
    var inLocalContext: Boolean = false
    var classNameBeforeLocalContext: FqName = FqName.ROOT

    val currentClassId: ClassId?
        get() = when {
            className == FqName.ROOT -> null
            else -> ClassId(packageFqName, className)
        }

    val arraySetArgument: MutableMap<T, CfirExpression> = mutableMapOf()

    private val localContextStack: ArrayDeque<Boolean> = ArrayDeque()
    private val classNameStack: ArrayDeque<FqName> = ArrayDeque()
    private val classNameBeforeLocalContextStack: ArrayDeque<FqName> = ArrayDeque()
    private val functionTargets: ArrayDeque<T> = ArrayDeque()
    private val loopTargets: ArrayDeque<T> = ArrayDeque()
    private val labelNames: ArrayDeque<Name> = ArrayDeque()
    private val containerSymbolStack: ArrayDeque<CfirSymbol<*>> = ArrayDeque()

    var forcedContainerSymbol: CfirSymbol<*>? = null

    fun <R> withPackage(fqName: FqName, block: () -> R): R {
        val hadPrevious = this::packageFqName.isInitialized
        val previous = if (hadPrevious) packageFqName else FqName.ROOT
        packageFqName = fqName
        return try {
            block()
        } finally {
            packageFqName = previous
        }
    }

    fun <R> withLocalContext(block: () -> R): R {
        localContextStack.addLast(inLocalContext)
        classNameBeforeLocalContextStack.addLast(classNameBeforeLocalContext)
        inLocalContext = true
        classNameBeforeLocalContext = className
        return try {
            block()
        } finally {
            classNameBeforeLocalContext = classNameBeforeLocalContextStack.removeLast()
            inLocalContext = localContextStack.removeLast()
        }
    }

    fun <R> withClassName(name: Name, block: () -> R): R {
        classNameStack.addLast(className)
        className = if (className == FqName.ROOT) FqName.topLevel(name) else className.child(name)
        return try {
            block()
        } finally {
            className = classNameStack.removeLast()
        }
    }

    fun pushContainerSymbol(symbol: CfirSymbol<*>) {
        val actual = if (containerSymbolStack.isEmpty() && forcedContainerSymbol != null) forcedContainerSymbol!! else symbol
        containerSymbolStack.addLast(actual)
    }

    fun popContainerSymbol(symbol: CfirSymbol<*>) {
        if (containerSymbolStack.isEmpty()) return
        containerSymbolStack.removeLast()
    }

    val containerSymbolIfAny: CfirSymbol<*>?
        get() = containerSymbolStack.lastOrNull()

    val containerSymbol: CfirSymbol<*>
        get() = containerSymbolStack.last()

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
