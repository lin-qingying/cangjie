package org.cangnova.cangjie.cfir.builder

import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRef
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroParameterSignature
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroRawConstructedClassScope
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroRawOwnerSnapshot
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroRawTypeParameterScope
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirStructSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CjMemberParsingContext
import org.cangnova.cangjie.parsing.CjParameterGrammar

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

    /** 当前解析入口的语言模块；普通宏新 token 的 parser 没有 module 上下文。 */
    var sourceModuleName: String = ""
        private set

    /** 完整源码按 package 前缀进入，结束后恢复片段原本的解析来源。 */
    fun <R> withSourceModuleName(moduleName: String, block: () -> R): R {
        val previous = sourceModuleName
        sourceModuleName = moduleName
        return try {
            block()
        } finally {
            sourceModuleName = previous
        }
    }

    /** 当前是否处于局部声明上下文。 */
    var inLocalContext: Boolean = false

    /** array set 语法在 raw 构建期需要暂存的下标或实参表达式。 */
    val arraySetArgument: MutableMap<T, CfirExpression> = mutableMapOf()

    /**
     * 对位 Kotlin Context.capturedTypeParameters：只保存词法捕获符号，不拥有参数声明。
     * 空参数层也保留作用域边界；仓颉没有 inner/local nominal，不添加 outer-class header 引用。
     */
    val capturedTypeParameters: MutableList<StatusCfirTypeParameterSymbolList> = mutableListOf()

    /** 进入声明的捕获环境；参数声明仍由 fresh header 独立拥有。 */
    fun pushCfirTypeParameters(isLocal: Boolean, parameters: List<CfirTypeParameterRef>) {
        capturedTypeParameters.add(StatusCfirTypeParameterSymbolList(isLocal, parameters.map { it.symbol }))
    }

    /** 与进入捕获环境严格成对，异常路径也必须退出该层。 */
    fun popCfirTypeParameters() {
        capturedTypeParameters.removeAt(capturedTypeParameters.lastIndex)
    }

    /**
     * 可构造名义类型帧。Kotlin 把 ownerTypeParameters 沿 class 转换显式下传；仓颉成员经统一声明分派
     * 进入，因此以实际容器 symbol 标识的帧保存同一事实。extend/interface 不构造实例，没有此帧。
     */
    private val constructedClasses: ArrayDeque<MacroRawConstructedClassScope> = ArrayDeque()

    /** 进入 class/struct/enum 成员区；[owner] 必须是当前实际容器帧的 symbol。 */
    fun pushConstructedClass(owner: CfirBasedSymbol<*>, parameters: List<CfirTypeParameterRef>) {
        check(containerSymbolIfAny === owner) { "A constructed class frame must belong to the actual container frame." }
        constructedClasses.addLast(MacroRawConstructedClassScope(owner, parameters.map { it.symbol }))
    }

    /** 与进入严格成对。 */
    fun popConstructedClass(owner: CfirBasedSymbol<*>) {
        check(constructedClasses.lastOrNull()?.owner === owner) { "Inconsistent constructed class frame." }
        constructedClasses.removeLast()
    }

    /**
     * 当前直接容器若是可构造名义类型，返回其 header 参数 symbol；否则为空。
     * 必须在构造器自身容器帧入栈之前读取，错误位置（extend、函数体内）的构造器不借用任何参数。
     */
    fun constructedClassTypeParameterSymbols(): List<CfirTypeParameterSymbol> {
        val frame = constructedClasses.lastOrNull() ?: return emptyList()
        return if (frame.owner === containerSymbolIfAny) frame.symbols else emptyList()
    }

    /** 局部上下文进入前状态栈，用于嵌套恢复。 */
    private val localContextStack: ArrayDeque<Boolean> = ArrayDeque()
    /** 当前可绑定 return 的函数 target 栈。 */
    // null 是仓颉非正文区域的词法边界，必须阻断更外层的 return target。
    private val functionTargets: ArrayDeque<CfirFunctionTarget?> = ArrayDeque()
    /** 当前可见 label 名称栈。 */
    private val labelNames: ArrayDeque<Name> = ArrayDeque()
    /** 当前声明容器符号栈。 */
    /** enclosing 保存已有 owner；declaration 才参与 Kotlin 的 forced target 替换。 */
    private enum class ContainerFrameKind { ENCLOSING, DECLARATION }
    private data class ContainerFrame(
        val requestedSymbol: CfirBasedSymbol<*>,
        val symbol: CfirBasedSymbol<*>,
        val kind: ContainerFrameKind,
    )
    private val containerSymbolStack: ArrayDeque<ContainerFrame> = ArrayDeque()
    private var declarationFrameCount: Int = 0
    /** 每次进入词法容器时屏蔽签名语境，退出时恢复；不是把当前函数的参数误当外层参数。 */
    private val enclosingSignatures: ArrayDeque<MacroParameterSignature?> = ArrayDeque()
    private var parameterSignature: MacroParameterSignature? = null
    private var nominalSyntax: (() -> CjMemberParsingContext)? = null
    /** 当前 class-like dispatch receiver self type 栈。 */
    private val dispatchReceiverTypesStack: ArrayDeque<ConeSimpleCangJieType> = ArrayDeque()

    /** 可强制替换最外层容器符号的入口，供特殊 raw build 场景复用上下文。 */
    var forcedContainerSymbol: CfirBasedSymbol<*>? = null
        set(value) {
            check(field == null) { "A forced declaration symbol cannot be reassigned." }
            check(declarationFrameCount == 0) { "A forced symbol must be configured before declaration construction." }
            field = value
        }

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
        val actual = if (declarationFrameCount == 0) forcedContainerSymbol ?: symbol else symbol
        pushContainerFrame(ContainerFrame(symbol, actual, ContainerFrameKind.DECLARATION))
        declarationFrameCount++
    }

    /** 指定声明复活沿真实 designation 恢复父路径，不把父 symbol 当作新建 target。 */
    fun pushEnclosingContainerSymbol(symbol: CfirBasedSymbol<*>) {
        check(declarationFrameCount == 0) { "An enclosing owner must be restored before declaration construction." }
        pushContainerFrame(ContainerFrame(symbol, symbol, ContainerFrameKind.ENCLOSING))
    }

    private fun pushContainerFrame(frame: ContainerFrame) {
        enclosingSignatures.addLast(parameterSignature)
        parameterSignature = null
        containerSymbolStack.addLast(frame)
    }

    /**
     * 弹出当前容器符号，并校验 push/pop 的对象身份严格配对。
     *
     * Kotlin FIR 的 raw-builder context 将 container symbol 当作 annotation
     * owner 的栈不变量；空栈或错误 symbol 不能静默恢复，否则临时 fragment
     * 会把 annotation 绑定到错误声明上。
     */
    fun popContainerSymbol(symbol: CfirBasedSymbol<*>) {
        popContainerFrame(symbol, ContainerFrameKind.DECLARATION)
        declarationFrameCount--
    }

    /** 与 enclosing 进入严格成对，不撤销或重新配置 forced target。 */
    fun popEnclosingContainerSymbol(symbol: CfirBasedSymbol<*>) {
        check(declarationFrameCount == 0) { "The designated declaration must be closed before its enclosing owner." }
        popContainerFrame(symbol, ContainerFrameKind.ENCLOSING)
    }

    private fun popContainerFrame(symbol: CfirBasedSymbol<*>, kind: ContainerFrameKind) {
        check(containerSymbolStack.isNotEmpty()) {
            "Cannot pop a container symbol from an empty raw CFIR builder stack."
        }
        val frame = containerSymbolStack.last()
        check(frame.kind == kind && frame.requestedSymbol === symbol) {
            "Inconsistent raw CFIR declaration container frame: expected=$kind/$symbol, actual=$frame."
        }
        containerSymbolStack.removeLast()
        parameterSignature = enclosingSignatures.removeLast()
    }

    /** 默认值和参数 annotation 共享实际 callable 的签名帧，结束后恢复外层签名。 */
    fun <R> withParameterSignature(signature: MacroParameterSignature?, block: () -> R): R {
        val previous = parameterSignature
        parameterSignature = signature
        return try { block() } finally { parameterSignature = previous }
    }

    /** primary 文法由实际名义容器的类型确定，不按构造器名字推断。 */
    fun constructorParameterSignature(owner: CfirBasedSymbol<*>, isPrimary: Boolean): MacroParameterSignature.Declaration {
        val grammar = if (!isPrimary) CjParameterGrammar.REGULAR_TYPED else when (containingClassSymbolIfAny) {
            is CfirClassSymbol -> CjParameterGrammar.CLASS_PRIMARY
            is CfirStructSymbol -> CjParameterGrammar.STRUCT_PRIMARY
            else -> error("A primary constructor parameter must belong to an actual class or struct container.")
        }
        return MacroParameterSignature.Declaration(owner, grammar)
    }

    /** 名义类型/extend 的语法事实与实际容器 symbol 一起进入 raw 构建。 */
    fun <R> withNominalSyntax(syntax: () -> CjMemberParsingContext, block: () -> R): R {
        val previous = nominalSyntax
        // 只有实际采集 macro surface 才读取原始标识符，普通 stub raw 构建不因此强制加载 AST。
        val value by lazy(LazyThreadSafetyMode.NONE, syntax)
        nominalSyntax = { value }
        return try { block() } finally { nominalSyntax = previous }
    }

    /** 只复制路径集合；其中符号和 target 保留真实身份，不提前解引用尚未绑定的声明。 */
    fun snapshotMacroOwner(): MacroRawOwnerSnapshot = MacroRawOwnerSnapshot(
        packageFqName = packageFqName,
        containerSymbols = containerSymbolStack.map { it.symbol },
        dispatchReceiverType = currentDispatchReceiverType(),
        inLocalContext = inLocalContext,
        functionTarget = currentFunctionTarget(),
        signature = parameterSignature,
        nominalSyntax = nominalSyntax?.invoke(),
        capturedTypeParameters = capturedTypeParameters.map { MacroRawTypeParameterScope(it.isLocal, it.list) },
        constructedClasses = constructedClasses.toList(),
    )

    /**
     * 在新 fragment builder 上安装宿主的实际词法环境。新 token 的 source module 不继承宿主。
     * 先恢复容器/receiver/target，再安装签名帧，防止容器入栈把该帧屏蔽。
     */
    fun <R> withMacroOwner(snapshot: MacroRawOwnerSnapshot, block: () -> R): R {
        check(containerSymbolStack.isEmpty() && enclosingSignatures.isEmpty() && functionTargets.isEmpty() && dispatchReceiverTypesStack.isEmpty() &&
            capturedTypeParameters.isEmpty() && constructedClasses.isEmpty() && parameterSignature == null && nominalSyntax == null && !inLocalContext && forcedContainerSymbol == null
        ) { "Macro fragments require a fresh raw builder lexical context." }
        return withPackage(snapshot.packageFqName) {
            inLocalContext = snapshot.inLocalContext
            nominalSyntax = snapshot.nominalSyntax?.let { value -> { value } }
            snapshot.containerSymbols.forEach(::pushContainerSymbol)
            snapshot.capturedTypeParameters.forEach {
                capturedTypeParameters.add(StatusCfirTypeParameterSymbolList(it.isLocal, it.symbols))
            }
            constructedClasses.addAll(snapshot.constructedClasses)
            snapshot.dispatchReceiverType?.let(::pushDispatchReceiverType)
            snapshot.functionTarget?.let(::enterFunction)
            try {
                withSourceModuleName("") { withParameterSignature(snapshot.signature, block) }
            } finally {
                if (snapshot.functionTarget != null) exitFunction()
                if (snapshot.dispatchReceiverType != null) popDispatchReceiverType()
                constructedClasses.clear()
                repeat(snapshot.capturedTypeParameters.size) { popCfirTypeParameters() }
                snapshot.containerSymbols.asReversed().forEach(::popContainerSymbol)
                nominalSyntax = null
                inLocalContext = false
            }
        }
    }

    /** 当前容器符号；不存在时返回 null。 */
    val containerSymbolIfAny: CfirBasedSymbol<*>?
        get() = containerSymbolStack.lastOrNull()?.symbol

    /** 当前容器符号；调用方必须保证容器栈非空。 */
    val containerSymbol: CfirBasedSymbol<*>
        get() = containerSymbolStack.last().symbol

    /** 声明/参数 annotation owner 与名义父容器独立；从真实帧查最近 class-like，不按名字推导。 */
    val containingClassSymbolIfAny: CfirClassLikeSymbol<*>?
        get() = containerSymbolStack.asReversed().firstNotNullOfOrNull { it.symbol as? CfirClassLikeSymbol<*> }

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

    /** 进入函数构建上下文。 */
    fun enterFunction(target: CfirFunctionTarget) {
        functionTargets.addLast(target)
    }

    /** 参数默认值不属于本函数或外层函数的正文；其内嵌函数正文另行进入自身 target。 */
    fun enterNonFunctionContext() {
        functionTargets.addLast(null)
    }

    /** 退出函数构建上下文。 */
    fun exitFunction() {
        check(functionTargets.isNotEmpty()) { "A return context must be entered before it is exited." }
        functionTargets.removeLast()
    }

    /** 当前可绑定 return 的函数 target。 */
    fun currentFunctionTarget(): CfirFunctionTarget? = functionTargets.lastOrNull()

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

/** 捕获层只持有真实参数 symbol；局部层标记与 Kotlin status list 对位。 */
data class StatusCfirTypeParameterSymbolList(
    val isLocal: Boolean,
    val list: List<CfirTypeParameterSymbol>,
)
