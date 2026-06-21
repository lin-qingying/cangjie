package org.cangnova.cangjie.cfir.builder

import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.CfirLoopTarget
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * Raw CFIR 构建共享上下文。
 *
 * 这里仅维护 Raw CFIR 构建真实需要的语境：
 * - 包级语境
 * - 局部语境
 * - 当前容器符号栈
 * - 当前 class-like self type 栈
 *
 * self type 与 Kotlin FIR `dispatchReceiverTypesStack` 对齐，必须携带当前
 * class-like 的类型参数，否则成员声明会退化成裸接收者类型。
 */
class Context<T> {

    lateinit var packageFqName: FqName
    var inLocalContext: Boolean = false

    val arraySetArgument: MutableMap<T, CfirExpression> = mutableMapOf()

    private val localContextStack: ArrayDeque<Boolean> = ArrayDeque()
    private val functionTargets: ArrayDeque<CfirFunctionTarget> = ArrayDeque()
    private val loopBaseSizeAtFunctionEntry: ArrayDeque<Int> = ArrayDeque()
    private val loopTargets: ArrayDeque<CfirLoopTarget> = ArrayDeque()
    private val labelNames: ArrayDeque<Name> = ArrayDeque()
    private val containerSymbolStack: ArrayDeque<CfirBasedSymbol<*>> = ArrayDeque()
    private val dispatchReceiverTypesStack: ArrayDeque<ConeSimpleCangJieType> = ArrayDeque()

    var forcedContainerSymbol: CfirBasedSymbol<*>? = null

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

    fun pushContainerSymbol(symbol: CfirBasedSymbol<*>) {
        val actual = if (containerSymbolStack.isEmpty() && forcedContainerSymbol != null) forcedContainerSymbol!! else symbol
        containerSymbolStack.addLast(actual)
    }

    fun popContainerSymbol(symbol: CfirBasedSymbol<*>) {
        if (containerSymbolStack.isEmpty()) return
        containerSymbolStack.removeLast()
    }

    val containerSymbolIfAny: CfirBasedSymbol<*>?
        get() = containerSymbolStack.lastOrNull()

    val containerSymbol: CfirBasedSymbol<*>
        get() = containerSymbolStack.last()

    fun pushDispatchReceiverType(type: ConeSimpleCangJieType) {
        dispatchReceiverTypesStack.addLast(type)
    }

    fun popDispatchReceiverType() {
        if (dispatchReceiverTypesStack.isNotEmpty()) {
            dispatchReceiverTypesStack.removeLast()
        }
    }

    fun currentDispatchReceiverType(): ConeSimpleCangJieType? =
        dispatchReceiverTypesStack.lastOrNull()

    fun enterFunction(target: CfirFunctionTarget) {
        functionTargets.addLast(target)
        loopBaseSizeAtFunctionEntry.addLast(loopTargets.size)
    }

    fun exitFunction() {
        if (functionTargets.isNotEmpty()) {
            functionTargets.removeLast()
        }
        if (loopBaseSizeAtFunctionEntry.isNotEmpty()) {
            loopBaseSizeAtFunctionEntry.removeLast()
        }
    }

    fun currentFunctionTarget(): CfirFunctionTarget? = functionTargets.lastOrNull()

    fun enterLoop(target: CfirLoopTarget) {
        loopTargets.addLast(target)
    }

    fun exitLoop() {
        if (loopTargets.isNotEmpty()) {
            loopTargets.removeLast()
        }
    }

    fun currentLoopTarget(): CfirLoopTarget? = loopTargets.lastOrNull()

    /**
     * 仅返回“当前函数边界以内”可见的最近循环目标。
     *
     * 这让 raw builder 可以像 Kotlin FIR 一样，在构建 jump 时区分：
     * - 同一函数体内的隐式最近循环
     * - 跨函数边界的外层循环（当前不允许 break/continue 穿透）
     */
    fun currentLoopTargetInCurrentFunction(): CfirLoopTarget? {
        val visibleLoopBase = loopBaseSizeAtFunctionEntry.lastOrNull() ?: 0
        return if (loopTargets.size > visibleLoopBase) loopTargets.lastOrNull() else null
    }

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
