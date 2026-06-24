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
 *
 * @param T raw builder 处理的语法节点类型。
 */
class Context<T> {

    /** 当前文件包名；由 raw builder 进入文件或 package directive 时设置。 */
    lateinit var packageFqName: FqName

    /** 当前是否处于局部声明上下文。 */
    var inLocalContext: Boolean = false

    /** array set 语法在 raw 构建期需要暂存的下标或实参表达式。 */
    val arraySetArgument: MutableMap<T, CfirExpression> = mutableMapOf()

    /** 局部上下文进入前状态栈，用于嵌套恢复。 */
    private val localContextStack: ArrayDeque<Boolean> = ArrayDeque()
    /** 当前可绑定 return 的函数 target 栈。 */
    private val functionTargets: ArrayDeque<CfirFunctionTarget> = ArrayDeque()
    /** 每个函数入口处 loop target 栈的基准大小。 */
    private val loopBaseSizeAtFunctionEntry: ArrayDeque<Int> = ArrayDeque()
    /** 当前可绑定 break/continue 的循环 target 栈。 */
    private val loopTargets: ArrayDeque<CfirLoopTarget> = ArrayDeque()
    /** 当前可见 label 名称栈。 */
    private val labelNames: ArrayDeque<Name> = ArrayDeque()
    /** 当前声明容器符号栈。 */
    private val containerSymbolStack: ArrayDeque<CfirBasedSymbol<*>> = ArrayDeque()
    /** 当前 class-like dispatch receiver self type 栈。 */
    private val dispatchReceiverTypesStack: ArrayDeque<ConeSimpleCangJieType> = ArrayDeque()

    /** 可强制替换最外层容器符号的入口，供特殊 raw build 场景复用上下文。 */
    var forcedContainerSymbol: CfirBasedSymbol<*>? = null

    /** 在 [fqName] 包上下文中执行 [block]，结束后恢复先前包名。 */
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

    /** 在局部声明上下文中执行 [block]，结束后恢复先前局部状态。 */
    fun <R> withLocalContext(block: () -> R): R {
        localContextStack.addLast(inLocalContext)
        inLocalContext = true
        return try {
            block()
        } finally {
            inLocalContext = localContextStack.removeLast()
        }
    }

    /** 将 [symbol] 压入容器栈；首层可被 [forcedContainerSymbol] 替换。 */
    fun pushContainerSymbol(symbol: CfirBasedSymbol<*>) {
        val actual = if (containerSymbolStack.isEmpty() && forcedContainerSymbol != null) forcedContainerSymbol!! else symbol
        containerSymbolStack.addLast(actual)
    }

    /** 弹出当前容器符号；[symbol] 用作调用点语义说明，不参与运行时匹配。 */
    fun popContainerSymbol(symbol: CfirBasedSymbol<*>) {
        if (containerSymbolStack.isEmpty()) return
        containerSymbolStack.removeLast()
    }

    /** 当前容器符号；不存在时返回 null。 */
    val containerSymbolIfAny: CfirBasedSymbol<*>?
        get() = containerSymbolStack.lastOrNull()

    /** 当前容器符号；调用方必须保证容器栈非空。 */
    val containerSymbol: CfirBasedSymbol<*>
        get() = containerSymbolStack.last()

    /** 压入当前 class-like dispatch receiver self type。 */
    fun pushDispatchReceiverType(type: ConeSimpleCangJieType) {
        dispatchReceiverTypesStack.addLast(type)
    }

    /** 弹出当前 dispatch receiver self type；空栈时保持幂等。 */
    fun popDispatchReceiverType() {
        if (dispatchReceiverTypesStack.isNotEmpty()) {
            dispatchReceiverTypesStack.removeLast()
        }
    }

    /** 返回当前 dispatch receiver self type；不存在时为 null。 */
    fun currentDispatchReceiverType(): ConeSimpleCangJieType? =
        dispatchReceiverTypesStack.lastOrNull()

    /** 进入函数构建上下文，并记录该函数入口处 loop 栈基准。 */
    fun enterFunction(target: CfirFunctionTarget) {
        functionTargets.addLast(target)
        loopBaseSizeAtFunctionEntry.addLast(loopTargets.size)
    }

    /** 退出函数构建上下文，并恢复函数入口记录。 */
    fun exitFunction() {
        if (functionTargets.isNotEmpty()) {
            functionTargets.removeLast()
        }
        if (loopBaseSizeAtFunctionEntry.isNotEmpty()) {
            loopBaseSizeAtFunctionEntry.removeLast()
        }
    }

    /** 当前可绑定 return 的函数 target。 */
    fun currentFunctionTarget(): CfirFunctionTarget? = functionTargets.lastOrNull()

    /** 进入循环构建上下文。 */
    fun enterLoop(target: CfirLoopTarget) {
        loopTargets.addLast(target)
    }

    /** 退出循环构建上下文；空栈时保持幂等。 */
    fun exitLoop() {
        if (loopTargets.isNotEmpty()) {
            loopTargets.removeLast()
        }
    }

    /** 当前最近的 loop target，不考虑函数边界。 */
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

    /** 压入当前 label 名称。 */
    fun pushLabel(name: Name) {
        labelNames.addLast(name)
    }

    /** 弹出当前 label；空栈时保持幂等。 */
    fun popLabel() {
        if (labelNames.isNotEmpty()) {
            labelNames.removeLast()
        }
    }

    /** 当前最近 label 名称；不存在时返回 null。 */
    fun currentLabel(): Name? = labelNames.lastOrNull()
}
