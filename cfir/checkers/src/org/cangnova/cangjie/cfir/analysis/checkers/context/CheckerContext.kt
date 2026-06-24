package org.cangnova.cangjie.cfir.analysis.checkers.context

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirAnnotationContainer
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator

/** checker 执行期间可读取的诊断上下文，暴露当前 session、作用域和遍历栈信息。 */
abstract class CheckerContext : DiagnosticContext, SessionAndScopeSessionHolder {
    /** 当前 checker 使用的诊断 reporter。 */
    abstract val reporter: DiagnosticReporter

    /** 提供 session 与 scopeSession 的持有者。 */
    abstract val sessionHolder: SessionAndScopeSessionHolder

    /** 当前诊断流程使用的函数返回类型计算器。 */
    abstract val returnTypeCalculator: ReturnTypeCalculator

    /** 从外到内记录当前遍历位置所在的声明栈。 */
    abstract val containingDeclarations: List<CfirDeclaration>

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

    /** 根据 suppress 名称和 suppress-all 标记判断诊断是否应被抑制。 */
    override fun isDiagnosticSuppressed(diagnostic: CjDiagnostic): Boolean {
        val suppressedByAll = when (diagnostic.severity) {
            Severity.INFO -> allInfosSuppressed
            Severity.WARNING, Severity.STRONG_WARNING, Severity.FIXED_WARNING -> allWarningsSuppressed
            Severity.ERROR -> allErrorsSuppressed
        }
        return suppressedByAll || diagnostic.factoryName in suppressedDiagnostics
    }
}

/** 诊断收集 visitor 使用的可变 checker context 实现。 */
class MutableCheckerContext(
    /** 提供 session 与 scopeSession 的持有者。 */
    override val sessionHolder: SessionAndScopeSessionHolder,
    /** 当前诊断流程使用的函数返回类型计算器。 */
    override val returnTypeCalculator: ReturnTypeCalculator,
    /** 当前 checker 使用的诊断 reporter。 */
    override val reporter: DiagnosticReporter,
    /** 当前正在遍历的文件 symbol。 */
    override var containingFileSymbol: CfirFileSymbol?,
    /** 可变声明栈。 */
    private val mutableDeclarations: MutableList<CfirDeclaration> = mutableListOf(),
    /** 可变语句栈。 */
    private val mutableStatements: MutableList<CfirStatement> = mutableListOf(),
    /** 可变元素栈。 */
    private val mutableElements: MutableList<CfirElement> = mutableListOf(),
    /** 可变调用或赋值节点栈。 */
    private val mutableCallsOrAssignments: MutableList<CfirElement> = mutableListOf(),
    /** 可变注解容器栈。 */
    private val mutableAnnotationContainers: MutableList<CfirAnnotationContainer> = mutableListOf(),
    /** 当前作用域中被显式 suppress 的诊断名称集合。 */
    override val suppressedDiagnostics: Set<String> = emptySet(),
    /** 当前作用域是否 suppress 所有 info 级别诊断。 */
    override val allInfosSuppressed: Boolean = false,
    /** 当前作用域是否 suppress 所有 warning 级别诊断。 */
    override val allWarningsSuppressed: Boolean = false,
    /** 当前作用域是否 suppress 所有 error 级别诊断。 */
    override val allErrorsSuppressed: Boolean = false,


) : CheckerContextForProvider(
    sessionHolder = sessionHolder,
    returnTypeCalculator = returnTypeCalculator,
    allInfosSuppressed = allInfosSuppressed,
    allWarningsSuppressed = allWarningsSuppressed,
    allErrorsSuppressed = allErrorsSuppressed,
) {
    /** 当前声明栈的只读视图。 */
    override val containingDeclarations: List<CfirDeclaration>
        get() = mutableDeclarations

    /** 当前语句栈的只读视图。 */
    override val containingStatements: List<CfirStatement>
        get() = mutableStatements

    /** 当前元素栈的只读视图。 */
    override val containingElements: List<CfirElement>
        get() = mutableElements

    /** 当前调用或赋值节点栈的只读视图。 */
    override val callsOrAssignments: List<CfirElement>
        get() = mutableCallsOrAssignments

    /** 当前注解容器栈的只读视图。 */
    override val annotationContainers: List<CfirAnnotationContainer>
        get() = mutableAnnotationContainers

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
            reporter = reporter,
            mutableDeclarations = mutableDeclarations,
            mutableStatements = mutableStatements,
            mutableElements = mutableElements,
            mutableCallsOrAssignments = mutableCallsOrAssignments,
            mutableAnnotationContainers = mutableAnnotationContainers,
            suppressedDiagnostics = suppressedDiagnostics + diagnosticNames,
            allInfosSuppressed = this.allInfosSuppressed || allInfosSuppressed,
            allWarningsSuppressed = this.allWarningsSuppressed || allWarningsSuppressed,
            allErrorsSuppressed = this.allErrorsSuppressed || allErrorsSuppressed,
        )
    }

    /** 将声明压入声明栈。 */
    override fun addDeclaration(declaration: CfirDeclaration): CheckerContextForProvider {
        mutableDeclarations += declaration
        return this
    }

    /** 弹出最近压入的声明。 */
    override fun dropDeclaration() {
        if (mutableDeclarations.isNotEmpty()) {
            mutableDeclarations.removeLast()
        }
    }

    /** 将语句压入语句栈。 */
    override fun addStatement(statement: CfirStatement): CheckerContextForProvider {
        mutableStatements += statement
        return this
    }

    /** 弹出最近压入的语句。 */
    override fun dropStatement() {
        if (mutableStatements.isNotEmpty()) {
            mutableStatements.removeLast()
        }
    }

    /** 将调用或赋值节点压入对应上下文栈。 */
    override fun addCallOrAssignment(qualifiedAccessOrAnnotationCall: CfirStatement): CheckerContextForProvider {
        mutableCallsOrAssignments += qualifiedAccessOrAnnotationCall
        return this
    }

    /** 弹出最近压入的调用或赋值节点。 */
    override fun dropCallOrAssignment() {
        if (mutableCallsOrAssignments.isNotEmpty()) {
            mutableCallsOrAssignments.removeLast()
        }
    }

    /** 将注解容器压入注解容器栈。 */
    override fun addAnnotationContainer(annotationContainer: CfirAnnotationContainer): CheckerContextForProvider {
        mutableAnnotationContainers += annotationContainer
        return this
    }

    /** 弹出最近压入的注解容器。 */
    override fun dropAnnotationContainer() {
        if (mutableAnnotationContainers.isNotEmpty()) {
            mutableAnnotationContainers.removeLast()
        }
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
        if (mutableElements.lastOrNull() !== element) {
            mutableElements += element
        }
        return this
    }

    /** 弹出最近压入的元素。 */
    override fun dropElement() {
        if (mutableElements.isNotEmpty()) {
            mutableElements.removeLast()
        }
    }
}

/** 从当前声明栈由内向外查找第一个满足谓词的声明。 */
inline fun <reified T : CfirDeclaration> CheckerContext.findClosestDeclaration(noinline check: (T) -> Boolean = { true }): T? {
    for (declaration in containingDeclarations.asReversed()) {
        val typed = declaration as? T ?: continue
        if (check(typed)) {
            return typed
        }
    }
    return null
}
