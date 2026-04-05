package org.cangnova.cangjie.cfir.builder

import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * Raw CFIR 构建共享上下文。
 *
 * 这里仅维护 Raw CFIR 构建真实需要的语境：
 * - 包级语境
 * - 局部语境
 * - 当前容器符号栈
 *
 * 类型声明的稳定标识统一由顶层声明规则推导，
 * 此处不维护任何额外的类型层级状态。
 */
class Context<T> {

    lateinit var packageFqName: FqName
    var inLocalContext: Boolean = false

    val arraySetArgument: MutableMap<T, CfirExpression> = mutableMapOf()

    private val localContextStack: ArrayDeque<Boolean> = ArrayDeque()
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
        inLocalContext = true
        return try {
            block()
        } finally {
            inLocalContext = localContextStack.removeLast()
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
