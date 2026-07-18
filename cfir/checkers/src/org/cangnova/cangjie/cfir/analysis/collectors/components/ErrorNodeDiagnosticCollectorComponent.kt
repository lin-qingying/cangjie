package org.cangnova.cangjie.cfir.analysis.collectors.components

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemantics
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.hasUninferredOmittedLambdaParameterType
import org.cangnova.cangjie.cfir.analysis.checkers.isTypeParameterWithInvalidDeclaredUpperBoundsInCurrentContext
import org.cangnova.cangjie.cfir.analysis.checkers.lambdaExpectedFunctionType
import org.cangnova.cangjie.cfir.analysis.checkers.expression.isInvalidPrimitiveCompoundAssignmentCall
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.analysis.diagnostics.toCfirDiagnostics
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirSuperReceiverExpression
import org.cangnova.cangjie.cfir.expressions.CfirWrappedExpression
import org.cangnova.cangjie.cfir.references.CfirErrorReference
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirSuperReference
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElementOffsetStrategy
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.fakeElement
import org.cangnova.cangjie.source.realElement
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.diagnostic.ConeAmbiguityError
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferValueParameterType
import org.cangnova.cangjie.cfir.diagnostic.ConeConstraintSystemHasContradiction
import org.cangnova.cangjie.cfir.diagnostic.ConeDiagnosticWithSingleCandidate
import org.cangnova.cangjie.cfir.diagnostic.ConeGenericTypeArgumentNotMatchConstraintError
import org.cangnova.cangjie.cfir.diagnostic.ConeNoMatchingInvokeOperatorError
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.diagnostic.ConeInapplicableCandidateError
import org.cangnova.cangjie.cfir.diagnostic.ConeTypeMismatchError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedNameError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedReferenceError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedSymbolError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedTypeQualifierError
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.cfir.types.renderForDebugging
import org.cangnova.cangjie.cfir.session.annotationMetadataRegistryOrNull
import org.cangnova.cangjie.cfir.session.importBindingStoreOrNull
import org.cangnova.cangjie.psi.CjNodeTypes

/**
 * 错误节点诊断收集组件（对应 Kotlin FIR 的 FirErrorNodeDiagnosticCollectorComponent）。
 *
 * 编译器前端的诊断报告被分为两个阶段：
 *   1. 解析阶段（resolve）：仅计算类型、绑定引用，将错误信息以 [ConeErrorType]
 *      的形式附着在 CFIR 节点的类型字段上，不直接输出诊断。
 *   2. 检查阶段（checker pass）：本组件在此阶段遍历 CFIR 树，从错误节点上
 *      "摘取"错误信息并转换为最终诊断报告。
 *
 * 具体职责：
 *   - 处理错误类型引用（[CfirErrorTypeRef]）
 *   - 处理错误表达式（[CfirErrorExpression]）
 *   - 处理错误引用（[CfirErrorReference]）
 *   - 处理节点上携带的 [ConeErrorType] 诊断
 *   - 对合成/假源码节点（fake source kind）做抑制，避免与专项 checker 重复报告
 *   - 通过 [ReportedConeDiagnosticKey] 去重，防止同一错误被多个重叠节点重复报告
 */
class ErrorNodeDiagnosticCollectorComponent(
    session: CfirSession,
    reporter: PendingDiagnosticReporter,
) : AbstractDiagnosticCollectorComponent(session, reporter) {

    /**
     * 已报告过的 [ConeDiagnostic] 的去重集合。
     *
     * 同一段源码上的同一类型错误可能被多个重叠 CFIR 节点（如函数调用、接收者、
     * 参数等）各自携带，使用源码偏移量 + 错误原因作为 key 保证每处错误只报告一次。
     */
    private val reportedConeDiagnostics = mutableSetOf<ReportedConeDiagnosticKey>()

    // ── visit 入口：针对各类可能携带错误的节点 ────────────────────────────────


    /** 访问未解析命名引用，并把其携带的诊断转换为普通 CFIR 诊断。 */
    override fun visitErrorNamedReference(errorNamedReference: CfirErrorNamedReference, data: CheckerContext) {
        processErrorReference(errorNamedReference, errorNamedReference.diagnostic, data)
    }

    /** 访问带候选的命名引用基类，并在其同时作为诊断 holder 时处理候选诊断。 */
    override fun visitNamedReferenceWithCandidateBase(
        namedReferenceWithCandidateBase: CfirNamedReferenceWithCandidateBase,
        data: CheckerContext,
    ) {
        val diagnosticHolder = namedReferenceWithCandidateBase as? CfirDiagnosticHolder
        if (diagnosticHolder != null) {
            processErrorReference(
                namedReferenceWithCandidateBase,
                diagnosticHolder.diagnostic,
                data,
            )
        }
    }





    /**
     * 访问已解析的类型引用（[CfirResolvedTypeRef]）。
     *
     * 即使类型引用"已解析"，其内部的 [ConeErrorType] 也可能携带诊断信息
     * （例如类型参数不合法），需要在此一并处理。
     */
    override fun visitResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef, data: CheckerContext) {
        assert(resolvedTypeRef.coneType !is ConeErrorType) {
            "Instead use CfirErrorTypeRef for ${resolvedTypeRef.coneType.renderForDebugging()}"
        }
    }

    /** 访问已解析但仍表示错误的引用，并处理其保留的解析诊断。 */
    override fun visitResolvedErrorReference(resolvedErrorReference: CfirResolvedErrorReference, data: CheckerContext) {
        processErrorReference(resolvedErrorReference, resolvedErrorReference.diagnostic, data)
    }

    /**
     * 访问错误类型引用（[CfirErrorTypeRef]）。
     *
     * [CfirErrorTypeRef] 表示类型解析完全失败（连 [ConeErrorType] 都未能生成），
     * 此处直接跳过——其错误由上层 visitResolvedTypeRef 或专项 checker 处理，
     * 在本组件中无需额外报告。
     */

    override fun visitErrorTypeRef(errorTypeRef: CfirErrorTypeRef, data: CheckerContext) {
//        if (errorTypeRef.isLambdaReturnTypeRefThatDoesntNeedReporting(data)) return
//        if (errorTypeRef.hasExpandedTypeAliasDeclarationSiteError()) return
        if (errorTypeRef.isNestedTypeAliasDeclarationSiteCascade(data)) return
        if (errorTypeRef.diagnostic.isUnresolvedCascadeAfterFailedImport(data)) return
        if (errorTypeRef.isMacroAnnotationTypeRef(data)) return

        val source = errorTypeRef.source
        if (source != null) {
            val key = ReportedConeDiagnosticKey(
                reason = errorTypeRef.diagnostic.reason,
                sourceStart = source.startOffset,
                sourceEnd = source.endOffset,
                callStart = null,
                callEnd = null,
            )
            if (!reportedConeDiagnostics.add(key)) return
        }

        reportCfirDiagnostic(
            errorTypeRef.diagnostic, source, data,
            // We provide a value parameter in case errorTypeRef is a type of this parameter
            valueParameter = data.containingElements.getOrNull(data.containingElements.lastIndex - 1) as? CfirValueParameter
        )
    }

    /**
     * declaration macro annotation 的 callee 类型引用不属于普通类型解析域。
     *
     * Raw builder 会把 `@pkg.Macro decl` 记录到 annotation metadata；即使 degraded
     * construction 后 annotation 仍留在 final CFIR，也不能再把 `pkg` 当作类型限定符报错。
     */
    private fun CfirErrorTypeRef.isMacroAnnotationTypeRef(context: CheckerContext): Boolean {
        val annotation = (
                context.callsOrAssignments.asReversed().asSequence() +
                        context.containingElements.asReversed().asSequence()
                )
            .filterIsInstance<CfirAnnotationCall>()
            .firstOrNull { it.typeRef.containsTypeRef(this) }
            ?: return false
        return context.session.annotationMetadataRegistryOrNull
            ?.snapshot(annotation)
            ?.qualifiedName != null
    }

    /**
     * annotation type resolve 会把原始 typeRef 保存在 delegated 链上；
     * macro annotation 过滤必须沿链识别当前错误 typeRef，不能只比较顶层对象身份。
     */
    private fun CfirTypeRef.containsTypeRef(target: CfirTypeRef): Boolean {
        var current: CfirTypeRef? = this
        while (current != null) {
            if (current === target) return true
            current = (current as? CfirResolvedTypeRef)?.delegatedTypeRef
        }
        return false
    }

    /**
     * typealias 展开类型的根 classifier 未解析时，嵌套类型实参上的未声明类型属于级联错误。
     *
     * 官方 cjc 在 `type x = List<T>` 中只报告 `List` 未声明；Kotlin FIR 也会通过
     * declaration-site typealias error 过滤避免使用点或嵌套节点重复报告。
     * 但父节点若只是 `ConeUnreportedDuplicateDiagnostic(T)`，真实错误所有者仍是内层 `T`，
     * 不能把这个重复包装当作根 classifier 错误继续抑制。
     */
    private fun CfirErrorTypeRef.isNestedTypeAliasDeclarationSiteCascade(context: CheckerContext): Boolean {
        if (diagnostic.unwrapUnreportedDuplicateDiagnostic() !is ConeUnresolvedTypeQualifierError) return false
        context.findClosestDeclaration<CfirTypeAlias>() ?: return false
        return context.containingElements
            .asReversed()
            .filterIsInstance<CfirErrorTypeRef>()
            .any { parent ->
                parent !== this &&
                        parent.diagnostic is ConeUnresolvedTypeQualifierError
            }
    }
    /**
     * 访问错误表达式节点（[CfirErrorExpression]）。
     *
     * 对齐 K2 `FirErrorNodeDiagnosticCollectorComponent.visitErrorExpression`：
     * 错误表达式自身的 [CfirErrorExpression.diagnostic] 是诊断源，不能通过
     * `coneTypeOrNull` 间接读取；无内层 expression 的错误表达式会把类型建模为
     * `ConeUnreportedDuplicateDiagnostic`，该诊断在映射层会被跳过。
     */
    override fun visitErrorExpression(errorExpression: CfirErrorExpression, data: CheckerContext) {
        if (errorExpression.diagnostic.isUnresolvedCascadeAfterFailedImport(data)) return
        val source = errorExpression.source as? CjSourceElement ?: return
        reportConeDiagnostic(errorExpression.diagnostic, source, data)
    }



    // ── 错误引用处理 ──────────────────────────────────────────────────────────

    /**
     * 对引用进行过滤，只处理 [CfirErrorReference]。
     *
     * 过滤条件：如果持有该引用的调用/赋值节点其接收者本身已经无法解析
     * （即接收者类型为 [ConeErrorType] 且诊断为 unresolved 系列），
     * 则跳过——接收者错误已被单独报告，此处若再报会产生重复的级联错误信息。
     */
    private fun processErrorReference(reference: CfirReference, diagnostic: ConeDiagnostic, context: CheckerContext) {
        var source = reference.source
        val callOrAssignment =
            context.callsOrAssignments.lastOrNull { it.toReferenceOrNull() == reference }
                ?: context.containingElements.asReversed().firstOrNull { it.toReferenceOrNull() == reference }
        if (isDominatedNestedAmbiguity(diagnostic, callOrAssignment, context)) return
        if (((reference is CfirNamedReference && reference.name.asString() == "<super>") || reference is CfirSuperReference) &&
            context.findClosestDeclaration<CfirExtend>() != null
        ) {
            with(context) {
                reporter.reportOn(
                    source = source ?: callOrAssignment?.source,
                    factory = CfirErrors.EXTEND_SUPER_NOT_ALLOWED,
                )
            }
            return
        }
        if (diagnostic.isUnresolvedCascadeAfterFailedImport(context)) return
        if (diagnostic.unwrapUnreportedDuplicateDiagnostic() is ConeTypeMismatchError &&
            context.isReturnResultElement(callOrAssignment)
        ) return
        // 注解项上的 unresolved 已由其类型引用报告，保持与 Kotlin FIR 的去重位置一致。
        if (source?.elementType == CjNodeTypes.ANNOTATION && diagnostic is ConeUnresolvedNameError) return
        // Already reported in FirConventionFunctionCallChecker
        if (source?.kind == CjFakeSourceElementKind.ArrayAccessNameReference &&
            diagnostic is ConeUnresolvedNameError
        ) return

        // If the receiver cannot be resolved, we skip reporting any further problems for this call.
        // If the receiver cannot be resolved, we skip reporting any further problems for this call.
        if (callOrAssignment is CfirQualifiedAccessExpression) {
            if (diagnostic is ConeUnresolvedNameError &&
                callOrAssignment.explicitReceiver is CfirSuperReceiverExpression &&
                context.isInsideConstructorValueParameterDefaultValue()
            ) return
            if (callOrAssignment.explicitReceiver is CfirSuperReceiverExpression &&
                context.isInsideIllegalSuperReceiverOwner()
            ) return
            if (callOrAssignment.dispatchReceiver.cannotBeResolved() ||
//                callOrAssignment.extensionReceiver.cannotBeResolved() ||
                callOrAssignment.explicitReceiver.cannotBeResolved()
            ) return
        }

        if (callOrAssignment is CfirFunctionCall &&
            callOrAssignment.isInvalidPrimitiveCompoundAssignmentCall(context)
        ) return

//        with(context) {
//            source = source?.delegatedPropertySourceOrThis()
//        }

        reportCfirDiagnostic(
            diagnostic,
            source,
            context,
            callOrAssignment.qualifiedAmbiguitySource(reference, diagnostic),
        )
    }

    /** 外层结构化调用歧义已携带该内层诊断时，避免重复报告内层错误。 */
    private fun isDominatedNestedAmbiguity(
        diagnostic: ConeDiagnostic,
        currentOwner: CfirElement?,
        context: CheckerContext,
    ): Boolean {
        val ancestors = buildList<CfirElement> {
            addAll(context.callsOrAssignments)
            addAll(context.containingElements)
        }
        return ancestors.asReversed().any { ancestor ->
            if (ancestor === currentOwner) return@any false
            val call = ancestor as? CfirFunctionCall ?: return@any false
            val outerDiagnostic = (call.calleeReference as? CfirDiagnosticHolder)?.diagnostic
                ?: return@any false
            val ambiguity = outerDiagnostic.unwrapUnreportedDuplicateDiagnostic() as? ConeAmbiguityError
                ?: return@any false
            ambiguity.dominatedNestedDiagnostics.any { it === diagnostic }
        }
    }

    /**
     * qualified access 的歧义诊断应标完整访问表达式，例如 `Int64.test`，而不是只标 selector。
     */
    private fun CfirElement?.qualifiedAmbiguitySource(
        reference: CfirReference,
        diagnostic: ConeDiagnostic,
    ): CjSourceElement? {
        val defaultSource = this?.source as? CjSourceElement
        if (diagnostic !is ConeAmbiguityError) return defaultSource
        val access = this as? CfirQualifiedAccessExpression ?: return defaultSource
        val referenceSource = reference.source ?: return defaultSource
        val receiverSource = access.explicitReceiver?.source as? CjSourceElement
            ?: access.dispatchReceiver?.source as? CjSourceElement
            ?: return defaultSource
        if (receiverSource.startOffset >= referenceSource.startOffset) return defaultSource
        return referenceSource.realElement().fakeElement(
            CjFakeSourceElementKind.ReferenceInAtomicQualifiedAccess,
            CjSourceElementOffsetStrategy.Custom.Delegated(
                startOffsetAnchor = receiverSource,
                endOffsetAnchor = referenceSource,
            ),
        )
    }

    // ── ConeErrorType 诊断处理 ────────────────────────────────────────────────

    /**
     * 从节点类型字段中提取 [ConeErrorType] 所携带的诊断并报告。
     *
     * @param owner  携带错误类型的 CFIR 节点（用于定位宿主调用/赋值）
     * @param coneType 节点的 cone 类型，若为 [ConeErrorType] 则提取其诊断
     * @param source 对应的源码位置
     * @param context 当前 checker 上下文
     */
    private fun processConeTypeDiagnostic(
        owner: CfirElement,
        coneType: ConeCangJieType?,
        source: AbstractCjSourceElement?,
        context: CheckerContext,
    ) {
        val sourceElement = source as? CjSourceElement ?: return
        // 只处理 ConeErrorType，普通类型直接跳过。
        val diagnostic = (coneType as? ConeErrorType)?.diagnostic ?: return

        // 找到宿主调用/赋值节点，其源码位置用于去重 key 的计算。
        val callOrAssignment = findOwningCallOrAssignment(owner, context)
        val callOrAssignmentSource = callOrAssignment?.source as? CjSourceElement

        reportConeDiagnostic(diagnostic, sourceElement, context, callOrAssignmentSource)
    }

    /**
     * 确定错误节点所归属的"宿主"调用或赋值节点。
     *
     * 对于调用/赋值节点自身携带的错误，直接以自身为宿主；
     * 对于其他节点（如类型引用、子表达式），从上下文调用栈中取最近的宿主。
     * 宿主信息用于计算去重 key，将同一调用位置产生的重复诊断合并。
     */
    private fun findOwningCallOrAssignment(owner: CfirElement, context: CheckerContext): CfirElement? {
        return when (owner) {
            is CfirFunctionCall,
            is CfirAssignment -> owner   // 节点本身就是调用/赋值，直接作为宿主
            is CfirNamedAccessExpression,
            is CfirQualifiedAccessExpression -> {
                // callee 自身携带错误类型时，宿主仍应是外层调用；否则调用语境诊断会退化为普通引用诊断。
                val ownerReference = owner.toReferenceOrNull()
                context.callsOrAssignments.asReversed().firstOrNull { candidate ->
                    candidate is CfirFunctionCall && candidate.toReferenceOrNull() == ownerReference
                } ?: owner
            }
            else -> context.callsOrAssignments.lastOrNull()  // 从上下文栈中取最近宿主
        }
    }

    /**
     * 对诊断进行去重检查，通过后转发给 [reportCfirDiagnostic]。
     *
     * 去重依据：错误原因 + 源码起止偏移 + 宿主调用起止偏移，
     * 任意一组合唯一地标识"同一处同一类型"的错误。
     *
     * 额外抑制规则：
     *   - 数组下标名称引用上的 [ConeUnresolvedNameError] 由下标专项 checker 处理，跳过。
     */
    private fun reportConeDiagnostic(
        diagnostic: ConeDiagnostic,
        source: CjSourceElement?,
        context: CheckerContext,
        callOrAssignmentSource: CjSourceElement? = null,
    ) {
        if (source == null) return

        // 数组访问名称引用的 unresolved 错误交由 ArrayAccessChecker 负责。
        if (source.kind == CjFakeSourceElementKind.ArrayAccessNameReference && diagnostic is ConeUnresolvedNameError) return
        if (diagnostic.isUnresolvedCascadeAfterFailedImport(context)) return
        if (diagnostic.unwrapUnreportedDuplicateDiagnostic() is ConeTypeMismatchError &&
            (context.isReturnResultSource(source) || context.isReturnResultSource(callOrAssignmentSource))
        ) return
        if (diagnostic is ConeGenericTypeArgumentNotMatchConstraintError) {
            val containingExtend = context.findClosestDeclaration<CfirExtend>()
            if (
                CfirExtendSemantics.isSourceInsideImmutableMutInterfaceExtendHeader(context, source) ||
                (
                    containingExtend != null &&
                        CfirExtendSemantics.isSourceInsideImmutableMutInterfaceSupertype(context, containingExtend, source)
                    )
            ) {
                return
            }
        }

        // 构造去重 key：同一源码范围 + 同一宿主范围 + 相同错误原因 → 视为同一诊断。
        val key = ReportedConeDiagnosticKey(
            reason = diagnostic.reason,
            sourceStart = source.startOffset,
            sourceEnd = source.endOffset,
            callStart = callOrAssignmentSource?.startOffset,
            callEnd = callOrAssignmentSource?.endOffset,
        )
        // add() 返回 false 说明已报告过，直接跳过。
        if (!reportedConeDiagnostics.add(key)) return

        reportCfirDiagnostic(diagnostic, source, context, callOrAssignmentSource)
    }

    // ── 辅助扩展 ──────────────────────────────────────────────────────────────

    /**
     * 从调用/赋值节点中提取其 callee 引用，用于在上下文调用栈中定位宿主。
     * 赋值的左值可能本身也是一个调用/访问节点，因此递归提取。
     */
    private fun CfirElement.toReferenceOrNull(): CfirReference? = when (this) {
        is CfirFunctionCall   -> calleeReference
        is CfirNamedAccessExpression -> calleeReference
        is CfirQualifiedAccessExpression -> calleeReference
        is CfirAssignment     -> lValue.toReferenceOrNull()
        else                  -> null
    }

    /**
     * `super.member` 的 receiver 若已由非法声明上下文检查器报告，selector 上的未解析错误就是级联噪声。
     *
     * class 内的字段初始化限制不属于这里；那些场景仍由专门的初始化 checker 决定是否保留成员未解析诊断。
     */
    private fun CheckerContext.isInsideIllegalSuperReceiverOwner(): Boolean {
        if (findClosestDeclaration<CfirExtend>() != null) return true
        val owner = findClosestDeclaration<CfirClassLikeDeclaration>() ?: return true
        return owner is CfirStruct || owner is CfirEnum || owner is CfirInterface
    }

    /**
     * 判断表达式的显式接收者是否已不可解析。
     *
     * 当接收者类型为 [ConeErrorType] 且诊断为 unresolved 系列时返回 true，
     * 表示接收者错误已被单独报告，下游依赖它的引用不必再重复报告。
     */
    private fun CfirExpression.hasUnresolvedReceiver(): Boolean {
        val receiver = when (this) {
            is CfirFunctionCall   -> explicitReceiver
            is CfirNamedAccessExpression -> explicitReceiver
            is CfirQualifiedAccessExpression -> explicitReceiver
            else                  -> null
        }
        return receiver.cannotBeResolved()
    }

    /**
     * 判断一个表达式是否因为错误而无法解析。
     *
     * 对齐 K2 FirErrorNodeDiagnosticCollectorComponent.cannotBeResolved()。
     * 当接收者类型为 [ConeErrorType] 且诊断属于以下类别时返回 true：
     * - unresolved 系列：名字/引用/符号无法解析
     * - super 相关：super 不可用（如 extend 体内）
     * null 表达式（无接收者）视为"可以解析"，返回 false。
     */
    private fun CfirExpression?.cannotBeResolved(): Boolean {
        val diagnostic = this?.coneTypeOrNull?.expandedErrorDiagnosticOrNull()
            ?: (this?.toReferenceOrNull() as? CfirDiagnosticHolder)?.diagnostic
        return when (val unwrappedDiagnostic = diagnostic.unwrapUnreportedDuplicateDiagnostic()) {
            is ConeUnresolvedNameError,
            is ConeUnresolvedReferenceError,
            is ConeUnresolvedSymbolError,
            is ConeUnresolvedTypeQualifierError -> true
            is org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic ->
                unwrappedDiagnostic.kind == org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind.SuperNotAllowed ||
                        unwrappedDiagnostic.kind == org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind.GenericTypeWithoutTypeArgument ||
                        unwrappedDiagnostic.kind == org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind.EmptyArrayLiteralTypeUndefined
            else -> false
        }
    }

    /** 解开用于去重占位的诊断包装，返回真正需要比较和分类的原始诊断。 */
    private fun ConeDiagnostic?.unwrapUnreportedDuplicateDiagnostic(): ConeDiagnostic? =
        (this as? ConeUnreportedDuplicateDiagnostic)?.original ?: this

    /**
     * 官方编译器在包导入失败后只报告 import 诊断，不继续把缺失包里的类型/引用扩散成
     * 使用点 unresolved 噪声。导入诊断本身由 [CfirImportsChecker] 报告，这里只过滤
     * 错误节点收集阶段从同一文件继续摘取到的 unresolved 级联。
     */
    private fun ConeDiagnostic.isUnresolvedCascadeAfterFailedImport(context: CheckerContext): Boolean {
        if (!isUnresolvedCascadeDiagnostic()) return false
        val file = context.containingFileSymbol?.takeIf { it.isBound }?.cfir ?: return false
        val imports = context.session.importBindingStoreOrNull?.getBindings(file)?.imports ?: return false
        return imports.any { binding ->
            binding.targets.isEmpty() &&
                binding.importDirective.source?.kind?.shouldSkipErrorTypeReporting != true
        }
    }

    /** 判断诊断是否属于 unresolved 级联类别。 */
    private fun ConeDiagnostic.isUnresolvedCascadeDiagnostic(): Boolean =
        when (unwrapUnreportedDuplicateDiagnostic()) {
            is ConeUnresolvedNameError,
            is ConeUnresolvedReferenceError,
            is ConeUnresolvedSymbolError,
            is ConeUnresolvedTypeQualifierError -> true
            else -> false
        }

    /**
     * typealias 声明侧已经携带 unresolved 错误时，使用点不能继续扩散成员/调用错误。
     */
    private fun ConeCangJieType.expandedErrorDiagnosticOrNull(): ConeDiagnostic? {
        var current = this
        val visitedAliases = linkedSetOf<ConeTypeAliasType>()
        while (current is ConeTypeAliasType && visitedAliases.add(current)) {
            current = current.expandedType ?: return null
        }
        return (current as? ConeErrorType)?.diagnostic
    }

    /** 判断当前遍历位置是否处于构造器值参数默认值表达式内部。 */
    private fun CheckerContext.isInsideConstructorValueParameterDefaultValue(): Boolean {
        val valueParameter = findClosestDeclaration<CfirValueParameter>() ?: return false
        val constructor = findClosestDeclaration<CfirConstructor>() ?: return false
        return valueParameter in constructor.valueParameters && valueParameter.defaultValue != null
    }

    /**
     * lambda 参数类型推断失败若发生在未映射到形参的调用实参上，
     * 主错误属于外层调用参数映射；这里不再把该 lambda 参数作为独立错误类型重复报告。
     */
    private fun ConeDiagnostic.isUnmappedCallArgumentLambdaParameterDiagnostic(
        source: CjSourceElement?,
        context: CheckerContext,
    ): Boolean {
        if (this !is ConeCannotInferValueParameterType || source == null) return false
        val containingCall = context.callsOrAssignments
            .asReversed()
            .filterIsInstance<CfirFunctionCall>()
            .firstOrNull { call ->
                call.argumentList.arguments.any { argument ->
                    argument is CfirAnonymousFunctionExpression &&
                        argument.anonymousFunction.valueParameters.any { parameter ->
                            parameter.source?.contains(source) == true
                        }
                }
            }
            ?: return false

        val candidateDiagnostic = (containingCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic
        val candidate = (candidateDiagnostic as? ConeDiagnosticWithSingleCandidate)?.candidate ?: return false
        if (!candidate.argumentMappingInitialized) return false

        return candidate.argumentMapping.keys.none { atom ->
            val expression = atom.expression as? CfirAnonymousFunctionExpression ?: return@none false
            expression.anonymousFunction.valueParameters.any { parameter ->
                parameter.source?.contains(source) == true
            }
        }
    }

    /** 判断一个 source 范围是否完整包含另一个 source 范围。 */
    private fun CjSourceElement.contains(other: CjSourceElement): Boolean =
        startOffset <= other.startOffset && other.endOffset <= endOffset

    /**
     * 判断当前 source 是否对应 `return expr` 的返回值根表达式。
     *
     * 根表达式上的通用类型不匹配由 `CfirReturnTypeMismatchChecker` 统一分类为
     * RETURN_TYPE_MISMATCH。构造器/调用的错误引用 source 可能只覆盖 callee，
     * 因此允许 source 从返回值根起点开始且不越过根表达式；return 内部更深层的
     * 实参/接收者错误不能被这里吞掉。
     */
    private fun CheckerContext.isReturnResultSource(source: CjSourceElement?): Boolean {
        if (source == null) return false
        val returnExpression = closestReturnExpressionOrNull() ?: return false
        val resultSource = returnExpression.result.source as? CjSourceElement ?: return false
        return source.startOffset == resultSource.startOffset &&
            source.endOffset <= resultSource.endOffset
    }

    /**
     * LightTree 路径没有可靠 PSI 父链，类型不匹配映射需要由 checker 栈显式提供
     * `return expr` 的 expr source，才能把根表达式 mismatch 分类为 RETURN_TYPE_MISMATCH。
     */
    private fun CheckerContext.returnExpressionSourceForTypeMismatch(
        source: CjSourceElement?,
        callOrAssignmentSource: CjSourceElement?,
    ): AbstractCjSourceElement? {
        if (!isReturnResultSource(source) && !isReturnResultSource(callOrAssignmentSource)) return null
        val returnExpression = closestReturnExpressionOrNull() ?: return null
        return returnExpression.result.source as? AbstractCjSourceElement
    }

    /**
     * 判断错误引用所属的调用/访问节点是否正是 `return expr` 的返回值根表达式。
     *
     * 构造器调用等场景中错误引用 source 只覆盖 callee（例如 `B<Int64>`），而返回值
     * 根表达式 source 覆盖整个调用（例如 `B<Int64>()`）。因此不能只比较引用 source，
     * 必须按宿主 CFIR 节点判断。
     */
    private fun CheckerContext.isReturnResultElement(element: CfirElement?): Boolean {
        val expression = element as? CfirExpression ?: return false
        val returnExpression = closestReturnExpressionOrNull() ?: return false
        val returnResult = returnExpression.result.unwrapWrappedExpression()
        val owner = expression.unwrapWrappedExpression()
        if (owner === returnResult) return true

        val ownerSource = owner.source as? CjSourceElement ?: return false
        val resultSource = returnResult.source as? CjSourceElement ?: return false
        return ownerSource.startOffset == resultSource.startOffset &&
            ownerSource.endOffset == resultSource.endOffset
    }

    /** 从当前 checker 遍历栈中取得最近的 return 表达式。 */
    private fun CheckerContext.closestReturnExpressionOrNull(): CfirReturnExpression? =
        containingStatements.asReversed().filterIsInstance<CfirReturnExpression>().firstOrNull()
            ?: containingElements.asReversed().filterIsInstance<CfirReturnExpression>().firstOrNull()

    /** 去掉 CFIR wrapped expression，取得实际表达式根。 */
    private tailrec fun CfirExpression.unwrapWrappedExpression(): CfirExpression = when (this) {
        is CfirWrappedExpression -> expression.unwrapWrappedExpression()
        else -> this
    }

    // ── 内部数据类 ────────────────────────────────────────────────────────────

    /**
     * 用于 [reportedConeDiagnostics] 的去重 key。
     *
     * 五元组：(错误原因, 源码起始偏移, 源码结束偏移, 宿主调用起始偏移, 宿主调用结束偏移)。
     * 宿主调用位置为可选，缺失时（即错误不在任何调用内）以 null 参与比较。
     */
    private data class ReportedConeDiagnosticKey(
        /** Cone diagnostic 的原因文本。 */
        val reason: String,
        /** 诊断 source 起始偏移。 */
        val sourceStart: Int,
        /** 诊断 source 结束偏移。 */
        val sourceEnd: Int,
        /** 宿主调用 source 起始偏移；没有宿主调用时为 null。 */
        val callStart: Int?,
        /** 宿主调用 source 结束偏移；没有宿主调用时为 null。 */
        val callEnd: Int?,
    )

    // ── 实例级转发 ────────────────────────────────────────────────────────────

    /**
     * 将 [ConeDiagnostic] 实际报告给 reporter 的实例方法，
     * 内部委托给 companion object 的静态实现，绑定当前 session 和 reporter。
     */
    private fun reportCfirDiagnostic(
        diagnostic: ConeDiagnostic,
        source: CjSourceElement?,
        context: CheckerContext,
        callOrAssignmentSource: CjSourceElement? = null,
        valueParameter: CfirValueParameter? = null

    ) {
        if (diagnostic.isLambdaParameterInferenceCoveredByShapeDiagnostic(source, context)) return
        if (diagnostic.isUnmappedCallArgumentLambdaParameterDiagnostic(source, context)) return
        if (diagnostic.isLambdaBodyCascadeFromUninferredLambdaParameter(source, context)) return
        if (diagnostic.unwrapUnreportedDuplicateDiagnostic() is ConeTypeMismatchError && context.isReturnResultSource(source)) return
        reportCfirDiagnostic(
            diagnostic = diagnostic,
            source = source,
            context = context,
            session = session,
            reporter = reporter,
            callOrAssignmentSource = callOrAssignmentSource,
            valueParameter = valueParameter,
            returnExpressionSource = context.returnExpressionSourceForTypeMismatch(source, callOrAssignmentSource),
        )
    }

    // ── 静态核心报告逻辑 ──────────────────────────────────────────────────────

    companion object {

        /**
         * 将单个 [ConeDiagnostic] 转换为 CFIR 诊断并提交给 [reporter]。
         *
         * 此方法也供外部（如其他 checker 组件）直接调用，因此放在 companion object 中。
         *
         * 抑制规则（以下情况直接返回，不报告）：
         *
         * 1. **委托属性访问器**（[CjFakeSourceElementKind.DelegatedPropertyAccessor]）：
         *    `getValue`/`setValue` 等委托调用由 DelegatedPropertyChecker 专项处理，
         *    此处产生的 unresolved / ambiguous / inapplicable 错误不重复报告。
         *
         * 2. **隐式构造器**（[CjFakeSourceElementKind.ImplicitConstructor]）：
         *    编译器自动生成的默认构造器调用不对用户可见，错误由 for-loop checker 处理。
         *
         * 3. **语法糖 for-in 循环**（[CjFakeSourceElementKind.DesugaredForLoop]）：
         *    for-in 循环被脱糖为 iterator()/hasNext()/next() 调用，
         *    错误由 ForLoopChecker 统一报告。
         *
         * 4. **前缀自增/自减的第二个 get**（[CjFakeSourceElementKind.DesugaredPrefixSecondGetReference]）：
         *    数组下标前缀运算（`++arr[i]`）会产生两个 `.get(...)` 调用，
         *    第二个是编译器内部重用，错误只在第一个上报告。
         *
         * 5. **when 条件主语**（[CjFakeSourceElementKind.UnresolvedWhenConditionSubject]）：
         *    when 主语访问失败时，错误已在 when 主语本身上报告过，
         *    条件分支里对该主语的引用不再重复报告。
         *
         * @param diagnostic           要报告的 cone 诊断
         * @param source               对应的源码节点（null 则跳过）
         * @param context              checker 上下文
         * @param session              编译会话
         * @param reporter             诊断接收器
         * @param callOrAssignmentSource 宿主调用/赋值的源码节点（用于错误定位）
         * @param valueParameter       若诊断与某个值参数相关，传入以提供更精准的错误信息
         */
        internal fun reportCfirDiagnostic(
            diagnostic: ConeDiagnostic,
            source: CjSourceElement?,
            context: CheckerContext,
            session: CfirSession = context.session,
            reporter: DiagnosticReporter,
            callOrAssignmentSource: CjSourceElement? = null,
            valueParameter: CfirValueParameter? = null,
            returnExpressionSource: AbstractCjSourceElement? = null,
        ) {
            // 抑制规则 1：委托属性访问器的 unresolved/ambiguous/inapplicable 错误
            // 由 DelegatedPropertyChecker 处理。
            if (source?.kind == CjFakeSourceElementKind.DelegatedPropertyAccessor &&
                (diagnostic is ConeUnresolvedNameError ||
                        diagnostic is ConeAmbiguityError      ||
                        diagnostic is ConeInapplicableCandidateError)
            ) {
                return
            }

            // 抑制规则 2 & 3：隐式构造器和脱糖 for-in 由专项 checker 处理。
            if (source?.kind == CjFakeSourceElementKind.ImplicitConstructor ||
                source?.kind == CjFakeSourceElementKind.DesugaredForLoop
            ) {
                return
            }

            // 抑制规则 4：前缀自增/自减第二个 get 调用不重复报告。
            if (source?.kind is CjFakeSourceElementKind.DesugaredPrefixSecondGetReference) {
                return
            }

            // 抑制规则 5：when 条件主语的引用错误已在 when 主语上报告过。
            if (source?.kind is CjFakeSourceElementKind.UnresolvedWhenConditionSubject) {
                return
            }

            if (diagnostic is ConeNoMatchingInvokeOperatorError &&
                with(context) {
                    diagnostic.receiverType.isTypeParameterWithInvalidDeclaredUpperBoundsInCurrentContext()
                }
            ) {
                return
            }

            if (diagnostic.isLambdaBodyCascadeFromUninferredLambdaParameter(source, context)) {
                return
            }
            if (diagnostic.isLambdaParameterInferenceCoveredByShapeDiagnostic(source, context)) {
                return
            }

            // 将 ConeDiagnostic 转换为具体的 CFIR 诊断列表并逐一提交。
            for (coneDiagnostic in diagnostic.toCfirDiagnostics(
                session,
                source,
                callOrAssignmentSource,
                valueParameter,
                returnExpressionSource,
            )) {
                if (
                    coneDiagnostic.factoryName == "CFIR_UNABLE_TO_INFER_GENERIC_FUNC" &&
                    (
                        context.hasGenericInstantiationMemberConflict(source) ||
                            context.hasGenericInstantiationMemberConflict(callOrAssignmentSource)
                        )
                ) {
                    continue
                }
                reporter.report(coneDiagnostic, context)
            }
        }
    }
}

/**
 * 官方 lambda 参数推断失败时只报告首个省略参数，body 内由该 placeholder
 * 派生的调用/运算符候选错误不继续扩散。
 */
private fun ConeDiagnostic.isLambdaBodyCascadeFromUninferredLambdaParameter(
    source: CjSourceElement?,
    context: CheckerContext,
): Boolean {
    if (source == null) return false
    if (this is ConeCannotInferValueParameterType) return false
    if (!isLambdaBodyCascadeDiagnostic()) return false

    val lambda = context.findClosestDeclaration<CfirAnonymousFunction>() ?: return false
    if (!lambda.isLambda || !lambda.hasExplicitParameterList) return false
    if (!lambda.hasUninferredOmittedLambdaParameterType()) return false
    if (lambda.valueParameters.any { parameter -> parameter.source?.containsSource(source) == true }) return false

    return true
}

/**
 * Lambda 头部已经按目标函数类型产生形状错误时，省略参数的推断失败不再单独报告。
 */
private fun ConeDiagnostic.isLambdaParameterInferenceCoveredByShapeDiagnostic(
    source: CjSourceElement?,
    context: CheckerContext,
): Boolean {
    if (this !is ConeCannotInferValueParameterType || source == null) return false
    val lambda = context.findClosestDeclaration<CfirAnonymousFunction>() ?: return false
    if (!lambda.isLambda || !lambda.hasExplicitParameterList) return false
    if (lambda.valueParameters.none { parameter -> parameter.source?.containsSource(source) == true }) return false
    if (context.hasLambdaParameterShapeDiagnostic(lambda)) return true

    val expectedFunctionType = lambda.lambdaExpectedFunctionType(context)
        ?: return false
    return lambda.valueParameters.size != expectedFunctionType.parameterTypes.size
}

private fun ConeDiagnostic.isLambdaBodyCascadeDiagnostic(): Boolean =
    when (unwrapUnreportedDuplicateDiagnosticForLambdaCascade()) {
        is ConeAmbiguityError,
        is ConeConstraintSystemHasContradiction,
        is ConeInapplicableCandidateError,
        is ConeNoMatchingInvokeOperatorError,
        is ConeTypeMismatchError,
        is ConeUnresolvedNameError,
        is ConeUnresolvedReferenceError,
        is ConeUnresolvedSymbolError -> true
        else -> false
    }

private fun ConeDiagnostic.unwrapUnreportedDuplicateDiagnosticForLambdaCascade(): ConeDiagnostic =
    (this as? ConeUnreportedDuplicateDiagnostic)?.original ?: this

private fun CjSourceElement.containsSource(other: CjSourceElement): Boolean =
    startOffset <= other.startOffset && other.endOffset <= endOffset
