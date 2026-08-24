/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * 对齐 Kotlin `PersistentCheckerContext`：不可变 checker context 快照，
 * 每次 `addDeclaration`/`addStatement`/... 返回新实例（copy-on-write），
 * `dropXxx` 为空实现（持久快照不弹栈，由调用方持有旧引用）。
 */

package org.cangnova.cangjie.cfir.analysis.checkers.context

import org.cangnova.cangjie.cfir.CfirAnnotationContainer
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.source.CjSourceElement
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

/**
 * 不可变 checker context 快照，对齐 Kotlin `PersistentCheckerContext`。
 *
 * 与 [MutableCheckerContext] 对偶：所有栈字段用 `PersistentList`/`PersistentSet`，
 * `addXxx` 返回新实例（copy-on-write），`dropXxx` 为空实现。
 *
 * 供 low-level analysis 那边持久化 checker context 用——快照不可变，
 * 即便上游 mutable context 继续压栈弹栈，已发出的快照引用不变。
 */
class PersistentCheckerContext private constructor(
    override val containingDeclarations: PersistentList<CfirBasedSymbol<*>>,
    override val containingStatements: PersistentList<CfirStatement>,
    override val containingElements: PersistentList<CfirElement>,
    override val callsOrAssignments: PersistentList<CfirElement>,
    override val annotationContainers: PersistentList<CfirAnnotationContainer>,
    override val containingFileSymbol: CfirFileSymbol?,
    sessionHolder: SessionAndScopeSessionHolder,
    returnTypeCalculator: ReturnTypeCalculator,
    override val suppressedDiagnostics: PersistentSet<String>,
    allInfosSuppressed: Boolean,
    allWarningsSuppressed: Boolean,
    allErrorsSuppressed: Boolean,
) : CheckerContextForProvider(
    sessionHolder = sessionHolder,
    returnTypeCalculator = returnTypeCalculator,
    allInfosSuppressed = allInfosSuppressed,
    allWarningsSuppressed = allWarningsSuppressed,
    allErrorsSuppressed = allErrorsSuppressed,
) {
    /** 创建空持久 context。 */
    constructor(
        sessionHolder: SessionAndScopeSessionHolder,
        returnTypeCalculator: ReturnTypeCalculator,
    ) : this(
        containingDeclarations = persistentListOf(),
        containingStatements = persistentListOf(),
        containingElements = persistentListOf(),
        callsOrAssignments = persistentListOf(),
        annotationContainers = persistentListOf(),
        containingFileSymbol = null,
        sessionHolder = sessionHolder,
        returnTypeCalculator = returnTypeCalculator,
        suppressedDiagnostics = persistentSetOf(),
        allInfosSuppressed = false,
        allWarningsSuppressed = false,
        allErrorsSuppressed = false,
    )

    override fun addDeclaration(declaration: CfirDeclaration): PersistentCheckerContext =
        copy(containingDeclarations = containingDeclarations.add(declaration.symbol))

    /** 持久快照不背 mutable 诊断记录状态——lambda 形状诊断由 mutable 收集轮管理，快照恒返回 false。 */
    override fun hasLambdaParameterShapeDiagnostic(lambda: CfirAnonymousFunction): Boolean = false

    /** 持久快照不记录诊断状态——由 mutable 收集轮承担。 */
    override fun recordLambdaParameterShapeDiagnostic(lambda: CfirAnonymousFunction) {}

    /** 持久快照不记录泛型实例化冲突范围——由 mutable 收集轮承担。 */
    override fun recordGenericInstantiationMemberConflict(source: CjSourceElement) {}

    override fun hasGenericInstantiationMemberConflict(source: CjSourceElement?): Boolean = false

    /** 持久快照不记录 static 泛型依赖范围——由 mutable 收集轮承担。 */
    override fun recordStaticGenericDependency(source: CjSourceElement) {}

    override fun hasStaticGenericDependency(source: CjSourceElement?): Boolean = false

    /** 持久快照不记录类型转换溢出范围——由 mutable 收集轮承担。 */
    override fun recordTypeConversionOverflow(source: CjSourceElement) {}

    override fun hasTypeConversionOverflow(source: CjSourceElement?): Boolean = false

    /** 持久快照不弹栈，由调用方持有旧引用实现"弹"。 */
    override fun dropDeclaration() {}

    override fun addStatement(statement: CfirStatement): PersistentCheckerContext =
        copy(containingStatements = containingStatements.add(statement))

    override fun dropStatement() {}

    override fun addCallOrAssignment(qualifiedAccessOrAnnotationCall: CfirStatement): PersistentCheckerContext =
        copy(callsOrAssignments = callsOrAssignments.add(qualifiedAccessOrAnnotationCall))

    override fun dropCallOrAssignment() {}

    override fun addAnnotationContainer(annotationContainer: CfirAnnotationContainer): PersistentCheckerContext =
        copy(annotationContainers = annotationContainers.add(annotationContainer))

    override fun dropAnnotationContainer() {}

    override fun addElement(element: CfirElement): PersistentCheckerContext =
        copy(containingElements = containingElements.add(element))

    override fun dropElement() {}

    override fun addSuppressedDiagnostics(
        diagnosticNames: Collection<String>,
        allInfosSuppressed: Boolean,
        allWarningsSuppressed: Boolean,
        allErrorsSuppressed: Boolean,
    ): PersistentCheckerContext {
        if (diagnosticNames.isEmpty()) return this
        return copy(
            suppressedDiagnostics = suppressedDiagnostics.addAll(diagnosticNames),
            allInfosSuppressed = this.allInfosSuppressed || allInfosSuppressed,
            allWarningsSuppressed = this.allWarningsSuppressed || allWarningsSuppressed,
            allErrorsSuppressed = this.allErrorsSuppressed || allErrorsSuppressed,
        )
    }

    /** 仓颉无 contract body 语义，enter/exit 为空实现并返回新实例。 */
    override fun enterContractBody(): PersistentCheckerContext = this

    override fun exitContractBody(): PersistentCheckerContext = this

    override fun enterFile(file: CfirFile): PersistentCheckerContext =
        copy(containingFileSymbol = file.symbol)

    override fun exitFile(file: CfirFile): PersistentCheckerContext =
        copy(containingFileSymbol = null)

    private fun copy(
        containingDeclarations: PersistentList<CfirBasedSymbol<*>> = this.containingDeclarations,
        containingStatements: PersistentList<CfirStatement> = this.containingStatements,
        containingElements: PersistentList<CfirElement> = this.containingElements,
        callsOrAssignments: PersistentList<CfirElement> = this.callsOrAssignments,
        annotationContainers: PersistentList<CfirAnnotationContainer> = this.annotationContainers,
        containingFileSymbol: CfirFileSymbol? = this.containingFileSymbol,
        suppressedDiagnostics: PersistentSet<String> = this.suppressedDiagnostics,
        allInfosSuppressed: Boolean = this.allInfosSuppressed,
        allWarningsSuppressed: Boolean = this.allWarningsSuppressed,
        allErrorsSuppressed: Boolean = this.allErrorsSuppressed,
    ): PersistentCheckerContext = PersistentCheckerContext(
        containingDeclarations = containingDeclarations,
        containingStatements = containingStatements,
        containingElements = containingElements,
        callsOrAssignments = callsOrAssignments,
        annotationContainers = annotationContainers,
        containingFileSymbol = containingFileSymbol,
        sessionHolder = sessionHolder,
        returnTypeCalculator = returnTypeCalculator,
        suppressedDiagnostics = suppressedDiagnostics,
        allInfosSuppressed = allInfosSuppressed,
        allWarningsSuppressed = allWarningsSuppressed,
        allErrorsSuppressed = allErrorsSuppressed,
    )
}
