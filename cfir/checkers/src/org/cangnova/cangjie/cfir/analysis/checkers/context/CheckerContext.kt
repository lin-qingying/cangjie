package org.cangnova.cangjie.cfir.analysis.checkers.context

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.cfir.CfirAnnotationContainer
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostic.ConeDiagnosticWithSingleCandidate
import org.cangnova.cangjie.cfir.diagnostics.*
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.source.CjSourceElement
import java.util.*

/** checker 执行期间可读取的诊断上下文，暴露当前 session、作用域和遍历栈信息。 */
abstract class CheckerContext : DiagnosticContext, SessionAndScopeSessionHolder {

    /** 提供 session 与 scopeSession 的持有者。 */
    abstract val sessionHolder: SessionAndScopeSessionHolder

    /** 当前诊断流程使用的函数返回类型计算器。 */
    abstract val returnTypeCalculator: ReturnTypeCalculator

    /** 从外到内记录当前遍历位置所在的声明栈。 */
    abstract val containingDeclarations: List<CfirBasedSymbol<*>>

    /** 从外到内记录当前遍历位置所在的语句栈。 */
    abstract val containingStatements: List<CfirStatement>

    /** 从外到内记录当前遍历路径上的 CFIR 元素栈。 */
    abstract val containingElements: List<CfirElement>

    /** 从外到内记录当前遍历位置所在的调用或赋值节点栈。 */
    abstract val callsOrAssignments: List<CfirElement>

    /** 从外到内记录当前遍历位置所在的注解容器栈。 */
    abstract val annotationContainers: List<CfirAnnotationContainer>

    /** 当前作用域中被显式 suppress 的诊断名称集合。 */
    abstract val suppressedDiagnostics: Set<String>

    /** 当前作用域是否 suppress 所有 info 级别诊断。 */
    abstract val allInfosSuppressed: Boolean

    /** 当前作用域是否 suppress 所有 warning 级别诊断。 */
    abstract val allWarningsSuppressed: Boolean

    /** 当前作用域是否 suppress 所有 error 级别诊断。 */
    abstract val allErrorsSuppressed: Boolean

    /** 当前 checker context 绑定的 CFIR session。 */
    override val session
        get() = sessionHolder.session

    /** 当前 checker context 绑定的作用域 session。 */
    override val scopeSession
        get() = sessionHolder.scopeSession

    /** 当前 session 的语言版本设置。 */
    override val languageVersionSettings: LanguageVersionSettings
        get() = session.languageVersionSettings

    /** 当前正在遍历的文件 symbol；未进入文件时为空。 */
    abstract val containingFileSymbol: CfirFileSymbol?

    /** 当前文件路径，用于 diagnostic context。 */
    override val containingFilePath: String?
        get() = containingFileSymbol?.sourceFile?.path

    /** 当前诊断收集轮次中，指定 lambda 是否已经产生参数形状诊断。 */
    abstract fun hasLambdaParameterShapeDiagnostic(lambda: CfirAnonymousFunction): Boolean

    /** 记录当前诊断收集轮次中指定 lambda 已经产生参数形状诊断。 */
    abstract fun recordLambdaParameterShapeDiagnostic(lambda: CfirAnonymousFunction)

    /** 记录当前调用范围已经由泛型实例化成员冲突拥有根诊断。 */
    abstract fun recordGenericInstantiationMemberConflict(source: CjSourceElement)

    /** 判断指定 source 或其宿主调用是否属于已确认的泛型实例化成员冲突。 */
    abstract fun hasGenericInstantiationMemberConflict(source: CjSourceElement?): Boolean

    /** 记录当前 source 已经由 static 泛型参数依赖拥有根诊断。 */
    abstract fun recordStaticGenericDependency(source: CjSourceElement)

    /** 判断指定 source 或其宿主调用是否包含已确认的 static 泛型参数依赖。 */
    abstract fun hasStaticGenericDependency(source: CjSourceElement?): Boolean

    /** 根据 suppress 名称和 suppress-all 标记判断诊断是否应被抑制。 */
    override fun isDiagnosticSuppressed(diagnostic: CjDiagnostic): Boolean {
        val suppressedByAll = when (diagnostic.severity) {
            Severity.INFO -> allInfosSuppressed
            Severity.WARNING, Severity.STRONG_WARNING, Severity.FIXED_WARNING -> allWarningsSuppressed
            Severity.ERROR -> allErrorsSuppressed
        }
        return suppressedByAll ||
                diagnostic.factoryName in suppressedDiagnostics ||
                diagnostic.isDerivedFromRecursiveImplicitReturn()
    }

    /**
     * 隐式返回类型递归是函数签名层的根错误。
     *
     * 官方编译器在引用仍含 `?` 返回类型的函数时报告函数名级
     * `sema_unable_to_infer_return_type`，并阻断同一返回表达式里由该未知返回类型派生的
     * operator / match / pattern 等后续错误。这里在 checker context 层统一过滤这些派生诊断，
     * 避免各 expression checker 分别硬编码同一规则。
     */
    private fun CjDiagnostic.isDerivedFromRecursiveImplicitReturn(): Boolean {
        if (factoryName == "CFIR_UNABLE_TO_INFER_RETURN_TYPE") return false
        if (factoryName == "CFIR_NO_MATCH_FUNCTION_DECLARATION_FOR_REF") return false

        return (containingStatements.asSequence() + callsOrAssignments.asSequence() + containingElements.asSequence())
            .filterIsInstance<CfirExpression>()
            .any { expression -> expression.dependsOnRecursiveImplicitReturnType() }
    }
}

/**
 * 判断表达式类型或引用目标是否依赖“隐式返回类型递归”的 callable。
 */
private fun CfirExpression.dependsOnRecursiveImplicitReturnType(): Boolean {
    if (coneTypeOrNull.hasRecursiveImplicitTypeError()) return true
    if ((this as? CfirResolvable)?.recursiveImplicitReturnCallableOrNull() != null) return true

    var found = false
    acceptChildren(object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            if (found) return
            if (element is CfirAnonymousFunctionExpression) return
            when {
                element is CfirExpression && element.coneTypeOrNull.hasRecursiveImplicitTypeError() -> {
                    found = true
                    return
                }

                element is CfirResolvable && element.recursiveImplicitReturnCallableOrNull() != null -> {
                    found = true
                    return
                }
            }
            element.acceptChildren(this, null)
        }
    }, null)
    return found
}

/**
 * 若当前可解析表达式引用了返回类型递归失败的 callable，返回该 callable 声明。
 */
private fun CfirResolvable.recursiveImplicitReturnCallableOrNull(): CfirCallableDeclaration? {
    val symbol = when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol
        is CfirErrorNamedReference ->
            (reference.diagnostic as? ConeDiagnosticWithSingleCandidate)?.candidateSymbol

        else -> null
    } as? CfirCallableSymbol<*> ?: return null

    return symbol.cfir.takeIf { it.returnTypeRef.hasRecursiveImplicitTypeError() }
}

/**
 * 判断类型引用是否承载隐式返回类型递归错误。
 */
private fun CfirTypeRef.hasRecursiveImplicitTypeError(): Boolean = when (this) {
    is CfirErrorTypeRef -> diagnostic.isRecursiveImplicitTypeDiagnostic()
    is CfirResolvedTypeRef -> coneType.hasRecursiveImplicitTypeError()
    else -> false
}

/**
 * 判断类型树内部是否包含隐式返回类型递归错误。
 */
private fun org.cangnova.cangjie.cfir.types.ConeCangJieType?.hasRecursiveImplicitTypeError(): Boolean {
    if (this == null) return false
    return contains { type ->
        type is ConeErrorType && type.diagnostic.isRecursiveImplicitTypeDiagnostic()
    }
}

/**
 * 判断 Cone diagnostic 是否是隐式返回类型递归错误。
 */
private fun ConeDiagnostic.isRecursiveImplicitTypeDiagnostic(): Boolean {
    val simpleDiagnostic = unwrapUnreportedDuplicateDiagnostic() as? ConeSimpleDiagnostic ?: return false
    return simpleDiagnostic.kind == DiagnosticKind.RecursionInImplicitTypes
}

private fun ConeDiagnostic.unwrapUnreportedDuplicateDiagnostic(): ConeDiagnostic =
    (this as? ConeUnreportedDuplicateDiagnostic)?.original ?: this

/**
 * 诊断收集 visitor 使用的可变 checker context 实现，对齐 Kotlin `MutableCheckerContext`。
 *
 * 主构造私有，公开副构造只占 `sessionHolder`/`returnTypeCalculator` 两个必填参数，
 * 其余栈字段在 `: this(...)` 调主构造时填默认值——避免公开主构造带默认参数后调用方误传mutableDeclarations=` mutableListOf()` 走错分支。
 */
class MutableCheckerContext private constructor(
    /** 提供 session 与 scopeSession 的持有者。 */
    override val sessionHolder: SessionAndScopeSessionHolder,
    /** 当前诊断流程使用的函数返回类型计算器。 */
    override val returnTypeCalculator: ReturnTypeCalculator,
    /** 当前正在遍历的文件 symbol。 */
    override var containingFileSymbol: CfirFileSymbol?,
    /** 可变声明符号栈（对齐 Kotlin `containingDeclarations: MutableList<FirBasedSymbol<*>>`，压 `declaration.symbol` 而非声明节点本身）。 */
    override val containingDeclarations: MutableList<CfirBasedSymbol<*>>,
    /** 可变语句栈。 */
    override val containingStatements: MutableList<CfirStatement>,
    /** 可变元素栈。 */
    override val containingElements: MutableList<CfirElement>,
    /** 可变调用或赋值节点栈。 */
    override val callsOrAssignments: MutableList<CfirElement>,
    /** 可变注解容器栈。 */
    override val annotationContainers: MutableList<CfirAnnotationContainer>,
    /** 当前诊断收集轮次内已经产生 lambda 参数形状诊断的函数集合。 */
    private val lambdaParameterShapeDiagnostics: MutableSet<CfirAnonymousFunction>,
    /** 当前诊断轮次中已由泛型实例化成员冲突拥有的调用范围。 */
    private val genericInstantiationMemberConflictRanges: MutableSet<Pair<Int, Int>>,
    /** 当前诊断轮次中已由 static 泛型参数依赖拥有的 source 范围。 */
    private val staticGenericDependencyRanges: MutableSet<Pair<Int, Int>>,
    /** 当前作用域中被显式 suppress 的诊断名称集合。 */
    override val suppressedDiagnostics: Set<String>,
    /** 当前作用域是否 suppress 所有 info 级别诊断。 */
    override val allInfosSuppressed: Boolean,
    /** 当前作用域是否 suppress 所有 warning 级别诊断。 */
    override val allWarningsSuppressed: Boolean,
    /** 当前作用域是否 suppress 所有 error 级别诊断。 */
    override val allErrorsSuppressed: Boolean,
) : CheckerContextForProvider(
    sessionHolder = sessionHolder,
    returnTypeCalculator = returnTypeCalculator,
    allInfosSuppressed = allInfosSuppressed,
    allWarningsSuppressed = allWarningsSuppressed,
    allErrorsSuppressed = allErrorsSuppressed,
) {
    /** 创建空的 mutable context——所有栈初始化为空、suppress 标志为 false。 */
    constructor(
        sessionHolder: SessionAndScopeSessionHolder,
        returnTypeCalculator: ReturnTypeCalculator,
    ) : this(
        sessionHolder = sessionHolder,
        returnTypeCalculator = returnTypeCalculator,
        containingFileSymbol = null,
        containingDeclarations = mutableListOf(),
        containingStatements = mutableListOf(),
        containingElements = mutableListOf(),
        callsOrAssignments = mutableListOf(),
        annotationContainers = mutableListOf(),
        lambdaParameterShapeDiagnostics = Collections.newSetFromMap(IdentityHashMap()),
        genericInstantiationMemberConflictRanges = linkedSetOf(),
        staticGenericDependencyRanges = linkedSetOf(),
        suppressedDiagnostics = emptySet(),
        allInfosSuppressed = false,
        allWarningsSuppressed = false,
        allErrorsSuppressed = false,
    )

    override fun hasLambdaParameterShapeDiagnostic(lambda: CfirAnonymousFunction): Boolean =
        lambda in lambdaParameterShapeDiagnostics

    override fun recordLambdaParameterShapeDiagnostic(lambda: CfirAnonymousFunction) {
        lambdaParameterShapeDiagnostics += lambda
    }

    override fun recordGenericInstantiationMemberConflict(source: CjSourceElement) {
        genericInstantiationMemberConflictRanges += source.startOffset to source.endOffset
    }

    override fun hasGenericInstantiationMemberConflict(source: CjSourceElement?): Boolean {
        source ?: return false
        return genericInstantiationMemberConflictRanges.any { (start, end) ->
            source.startOffset >= start && source.endOffset <= end
        }
    }

    override fun recordStaticGenericDependency(source: CjSourceElement) {
        staticGenericDependencyRanges += source.startOffset to source.endOffset
    }

    override fun hasStaticGenericDependency(source: CjSourceElement?): Boolean {
        source ?: return false
        return staticGenericDependencyRanges.any { (start, end) ->
            start >= source.startOffset && end <= source.endOffset ||
                    source.startOffset >= start && source.endOffset <= end
        }
    }

    /** 创建带有新增 suppress 信息的 context。 */
    override fun addSuppressedDiagnostics(
        diagnosticNames: Collection<String>,
        allInfosSuppressed: Boolean,
        allWarningsSuppressed: Boolean,
        allErrorsSuppressed: Boolean,
    ): CheckerContextForProvider {
        if (diagnosticNames.isEmpty()) return this
        return MutableCheckerContext(
            sessionHolder = sessionHolder,
            returnTypeCalculator = returnTypeCalculator,
            containingFileSymbol = containingFileSymbol,
            containingDeclarations = containingDeclarations,
            containingStatements = containingStatements,
            containingElements = containingElements,
            callsOrAssignments = callsOrAssignments,
            annotationContainers = annotationContainers,
            lambdaParameterShapeDiagnostics = lambdaParameterShapeDiagnostics,
            genericInstantiationMemberConflictRanges = genericInstantiationMemberConflictRanges,
            staticGenericDependencyRanges = staticGenericDependencyRanges,
            suppressedDiagnostics = suppressedDiagnostics + diagnosticNames,
            allInfosSuppressed = this.allInfosSuppressed || allInfosSuppressed,
            allWarningsSuppressed = this.allWarningsSuppressed || allWarningsSuppressed,
            allErrorsSuppressed = this.allErrorsSuppressed || allErrorsSuppressed,
        )
    }

    /** 将声明压入声明栈。 */
    override fun addDeclaration(declaration: CfirDeclaration): CheckerContextForProvider {
        containingDeclarations.add(declaration.symbol)
        return this
    }

    /** 弹出最近压入的声明。 */
    override fun dropDeclaration() {
        containingDeclarations.removeLast()

    }

    /** 将语句压入语句栈。 */
    override fun addStatement(statement: CfirStatement): CheckerContextForProvider {
        containingStatements += statement
        return this
    }

    /** 弹出最近压入的语句。 */
    override fun dropStatement() {
        containingStatements.removeLast()

    }

    /** 将调用或赋值节点压入对应上下文栈。 */
    override fun addCallOrAssignment(qualifiedAccessOrAnnotationCall: CfirStatement): CheckerContextForProvider {
        callsOrAssignments += qualifiedAccessOrAnnotationCall
        return this
    }

    /** 弹出最近压入的调用或赋值节点。 */
    override fun dropCallOrAssignment() {
        callsOrAssignments.removeLast()
    }

    /** 将注解容器压入注解容器栈。 */
    override fun addAnnotationContainer(annotationContainer: CfirAnnotationContainer): CheckerContextForProvider {
        annotationContainers += annotationContainer
        return this
    }

    /** 弹出最近压入的注解容器。 */
    override fun dropAnnotationContainer() {
        annotationContainers.removeLast()
    }

    /** 进入 contract body；当前上下文无需额外状态。 */
    override fun enterContractBody(): CheckerContextForProvider = this

    /** 退出 contract body；当前上下文无需额外状态。 */
    override fun exitContractBody(): CheckerContextForProvider = this

    /** 进入文件并记录当前文件 symbol。 */
    override fun enterFile(file: CfirFile): CheckerContextForProvider {
        containingFileSymbol = file.symbol
        return this
    }

    /** 退出文件并恢复当前文件 symbol 状态。 */
    override fun exitFile(file: CfirFile): CheckerContextForProvider {
        containingFileSymbol = file.symbol
        return this
    }

    /** 将元素压入元素栈；避免同一元素连续重复入栈。 */
    override fun addElement(element: CfirElement): CheckerContextForProvider {
        if (containingElements.lastOrNull() !== element) {
            containingElements += element
        }
        return this
    }

    /** 弹出最近压入的元素。 */
    override fun dropElement() {
        if (containingElements.isNotEmpty()) {
            containingElements.removeLast()
        }
    }
}

/**
 * Returns the closest to the end of context.containingDeclarations instance of type [T] or null if no such item could be found.
 * By specifying [check] you can filter which exact declaration should be found
 * E.g., property accessor is either getter or setter, but a type-based search could return, say,
 *   the closest setter, while we want to keep searching for a getter.
 */
inline fun <reified T : CfirBasedSymbol<*>> CheckerContext.findClosest(check: (T) -> Boolean = { true }): T? {
    for (it in containingDeclarations.asReversed()) {
        return (it as? T)?.takeIf(check) ?: continue
    }

    return null
}

/** 从当前声明栈由内向外查找第一个满足谓词的声明。 */
inline fun <reified T : CfirDeclaration> CheckerContext.findClosestDeclaration(noinline check: (T) -> Boolean = { true }): T? {
    for (declaration in containingDeclarations.asReversed()) {
        val typed = declaration.cfir as? T ?: continue
        if (check(typed)) {
            return typed
        }
    }
    return null
}
