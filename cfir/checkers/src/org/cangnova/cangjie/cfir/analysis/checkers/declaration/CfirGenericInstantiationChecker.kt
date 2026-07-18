package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.firstCharacterDiagnosticSource
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.calls.resolvedQualifierClassifier
import org.cangnova.cangjie.cfir.diagnostics.SourceElementPositioningStrategies
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRef
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.diagnostic.ConeDiagnosticWithSingleCandidate
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.providers.createExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassSubstitutionScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirStructSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeCStringType
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeQuestType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.asCone
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.createTypeSubstitutorByTypeConstructor
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.source.text
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import java.util.ArrayDeque
import java.util.IdentityHashMap

/**
 * 类型结构递归遍历的最大深度，防止病态递归类型导致无限遍历。
 */
private const val TYPE_KEY_MAX_DEPTH = 64

/**
 * 泛型实例化无限展开检查。
 *
 * 官方 Cangjie 在 `StructInheritanceChecker::CheckInstDupFuncsRecursively` 中维护
 * `(Decl, instTys)` 的已检查集合，并在 `WillCauseInfiniteInstantiation` 中对声明
 * 类型参数映射执行 occurs-check。CFIR 这里采用同一语义：先按声明预收集实例化触发点，
 * 再以有限的声明实例化状态驱动检查，避免在每个 typeRef 上重复递归遍历整棵声明树。
 *
 * Kotlin 没有同名仓颉语义；框架形态参考
 * `FirNonExpansiveInheritanceRestrictionChecker`：构造有限图/边集合后用 DFS 判断带扩张
 * 边的环，诊断归属仍由 Cangjie 官方实例化触发点规则决定。
 */
object CfirGenericInstantiationChecker : CfirFileChecker() {
    /**
     * 对单个文件执行泛型实例化无限展开和实例化成员冲突检查。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFile) {
        GenericInstantiationAnalyzer(context, reporter).checkFile(declaration)
    }
}

/**
 * 文件级泛型实例化分析器。
 */
private class GenericInstantiationAnalyzer(
    /**
     * 当前 checker 上下文。
     */
    private val checkerContext: CheckerContext,

    /**
     * 诊断报告器。
     */
    private val reporter: DiagnosticReporter,
) {
    /**
     * 每个声明预收集的实例化触发点缓存。
     */
    private val triggersByDeclaration = IdentityHashMap<CfirDeclaration, List<InstantiationTrigger>>()

    /**
     * 已经处理过的声明实例化 key 集合。
     */
    private val checkedInstantiations = linkedSetOf<DeclarationInstantiationKey>()

    /**
     * 已经报告过无限实例化的源码范围集合。
     */
    private val reportedSources = linkedSetOf<SourceKey>()

    /**
     * 已经报告过成员实例化冲突的源码和函数名集合。
     */
    private val reportedMemberInstantiationSources = linkedSetOf<MemberInstantiationReportKey>()

    /**
     * 已报告 static 成员不完整类型实参的源码范围。
     */
    private val reportedIncompleteTypeArgumentSources = linkedSetOf<SourceKey>()

    /**
     * 当前递归处理中的声明实例化栈。
     */
    private val activeInstantiations = mutableListOf<DeclarationInstantiationFrame>()

    /**
     * 是否已经报告过无限实例化诊断。
     */
    private var infiniteInstantiationOccurred: Boolean = false

    /**
     * 从文件顶层声明开始执行实例化分析。
     */
    fun checkFile(file: CfirFile) {
        for (declaration in file.declarations) {
            if (infiniteInstantiationOccurred) return
            processDeclaration(declaration, emptyMap(), instantiationContext = null)
        }
    }

    /**
     * 在给定类型实参替换下处理一个声明。
     */
    private fun processDeclaration(
        declaration: CfirDeclaration,
        substitutions: Map<CfirTypeParameterSymbol, ConeCangJieType>,
        instantiationContext: InstantiationContext?,
    ) {
        if (infiniteInstantiationOccurred) return
        if (declaration.origin != CfirDeclarationOrigin.Source) return
        if (!checkedInstantiations.add(declaration.instantiationKey(substitutions))) return

        val frame = declaration.instantiationFrameOrNull(substitutions)
        if (frame != null) {
            activeInstantiations += frame
        }
        try {
            for (trigger in triggersFor(declaration)) {
                if (infiniteInstantiationOccurred) return
                processTrigger(trigger, substitutions, instantiationContext)
            }
        } finally {
            if (frame != null) {
                activeInstantiations.removeAt(activeInstantiations.lastIndex)
            }
        }
    }

    /**
     * 处理单个实例化触发点。
     */
    private fun processTrigger(
        trigger: InstantiationTrigger,
        substitutions: Map<CfirTypeParameterSymbol, ConeCangJieType>,
        instantiationContext: InstantiationContext?,
    ) {
        if (infiniteInstantiationOccurred) return
        // 官方实例化诊断以真实 AST 引用节点作为触发点；synthetic typeRef 不能抢先占用实例化缓存。
        if (trigger.source == null) return
        if (trigger.isNestedTypeArgument && instantiationContext == null) return
        if (trigger.builtinTargetKey != null) {
            processBuiltinExtendTrigger(trigger, substitutions, instantiationContext)
            return
        }

        val declaration = trigger.declaration ?: return
        val typeParameters = declaration.typeParameters
        if (typeParameters.isEmpty() || trigger.typeArguments.isEmpty()) return
        if (typeParameters.size != trigger.typeArguments.size) return

        val targetDeclaration = trigger.targetDeclaration
        if (targetDeclaration != null && trigger.isNestedTypeArgument && trigger.symbol is CfirClassLikeSymbol<*>) {
            processDeclaration(targetDeclaration, emptyMap(), instantiationContext = null)
            if (infiniteInstantiationOccurred) return
        }

        val cyclicInstantiationSource = trigger.cyclicInstantiationSource(instantiationContext, targetDeclaration)
        val memberInstantiationContext = trigger.memberInstantiationContext(instantiationContext, targetDeclaration)
        if (hasCyclicInstantiation(typeParameters, trigger.typeArguments)) {
            reportInfiniteInstantiation(cyclicInstantiationSource)
            return
        }

        val expandedArguments = substitute(trigger.typeArguments, substitutions)
        if (hasCyclicInstantiation(typeParameters, expandedArguments)) {
            reportInfiniteInstantiation(cyclicInstantiationSource)
            return
        }

        val nestedSubstitutions = typeParameters
            .zip(expandedArguments)
            .associate { (typeParameter, argument) -> typeParameter.symbol to argument }
        if (trigger.checkStaticCompleteness) {
            checkStaticMemberCompleteness(trigger, typeParameters, expandedArguments, nestedSubstitutions)
        }

        if (targetDeclaration == null) return
        if (hasExpandingActiveInstantiation(targetDeclaration, expandedArguments)) {
            // 扩张边的诊断归属当前实例化声明的入边；member context 可能已切换到
            // 当前 trigger，不能覆盖真正形成闭包的上一条成员签名 source。
            reportInfiniteInstantiation(instantiationContext?.source ?: memberInstantiationContext?.source)
            return
        }
        if (targetDeclaration is CfirClassLikeDeclaration) {
            checkInstantiatedDuplicateSupertypes(
                declaration = targetDeclaration,
                substitutions = nestedSubstitutions,
                triggerSource = memberInstantiationContext?.source,
                checkExtendInterfaces = trigger.checkExtendInterfaces,
                duplicateSuperInterfaceSource = trigger.duplicateSuperInterfaceSource,
            )
            if (trigger.checkMemberSignatures) {
                checkInstantiatedMemberSignatures(
                    targetDeclaration,
                    nestedSubstitutions,
                    memberInstantiationContext?.source,
                    memberInstantiationContext?.ownMemberConflictSource,
                )
            }
        }
        processDeclaration(targetDeclaration, nestedSubstitutions, memberInstantiationContext)
    }

    /**
     * 处理内建 extend 目标产生的实例化成员签名检查。
     */
    private fun processBuiltinExtendTrigger(
        trigger: InstantiationTrigger,
        substitutions: Map<CfirTypeParameterSymbol, ConeCangJieType>,
        instantiationContext: InstantiationContext?,
    ) {
        val targetKey = trigger.builtinTargetKey ?: return
        val instantiatedType = trigger.builtinInstantiatedType ?: return
        val reportSource = trigger.memberInstantiationContext(instantiationContext, targetDeclaration = null)?.source
        val concreteType = substitute(instantiatedType, substitutions)
        checkInstantiatedBuiltinExtendMemberSignatures(targetKey, concreteType, reportSource)
    }

    /**
     * 取得声明内部的实例化触发点。
     */
    private fun triggersFor(declaration: CfirDeclaration): List<InstantiationTrigger> =
        triggersByDeclaration.getOrPut(declaration) {
            val collector = InstantiationTriggerCollector()
            declaration.accept(collector)
            collector.triggers
        }

    private inner class InstantiationTriggerCollector : CfirDefaultVisitorVoid() {
        val triggers = mutableListOf<InstantiationTrigger>()

        override fun visitElement(element: CfirElement) {
            element.acceptChildren(this)
        }

        override fun visitTypeParameter(typeParameter: CfirTypeParameter) {
            // 官方实例化检查会忽略泛型参数上界，避免与 GenericDeep 上界递归检查重复。
        }

        override fun visitResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef) {
            if (resolvedTypeRef is CfirErrorTypeRef || resolvedTypeRef.coneType is ConeErrorType) return
            val source = resolvedTypeRef.source ?: resolvedTypeRef.delegatedTypeRef?.source
            collectTypeTriggers(
                resolvedTypeRef.coneType,
                source,
                isNestedTypeArgument = false,
                checkMemberSignatures = true,
            )
            collectExplicitUserTypeTrigger(resolvedTypeRef, source)
            super.visitResolvedTypeRef(resolvedTypeRef)
        }

        override fun visitQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression) {
            collectQualifiedAccessTrigger(qualifiedAccessExpression)
            super.visitQualifiedAccessExpression(qualifiedAccessExpression)
        }

        override fun visitFunctionCall(functionCall: CfirFunctionCall) {
            collectQualifiedAccessTrigger(functionCall)
            super.visitFunctionCall(functionCall)
        }

        private fun collectQualifiedAccessTrigger(expression: CfirQualifiedAccessExpression) {
            val qualifier = expression.explicitReceiver
            if (qualifier?.resolvedQualifierClassifier(checkerContext.session) != null) {
                qualifier.coneTypeOrNull?.let { qualifierType ->
                    collectTypeTriggers(
                        qualifierType,
                        qualifier.source,
                        isNestedTypeArgument = false,
                        checkMemberSignatures = true,
                        ownMemberConflictSource = qualifier.source,
                    )
                }
            }

            val callableSymbol = expression.resolvedCallableSymbolOrNull() ?: return
            val isConstructorLikeInstantiationCall = callableSymbol.isConstructorLikeInstantiationCall()
            val source = expression.calleeReference.source ?: expression.source
            val ownMemberConflictSource = if (isConstructorLikeInstantiationCall && expression.typeArguments.isEmpty()) {
                expression.source ?: source
            } else {
                source
            }
            if (isConstructorLikeInstantiationCall) {
                val constructedType = expression.coneTypeOrNull
                    ?.takeUnless { it is ConeErrorType }
                    ?: expression.inferredConstructedTypeFromCandidate()
                constructedType?.let {
                    collectTypeTriggers(
                        it,
                        source,
                        isNestedTypeArgument = false,
                        checkExtendInterfaces = true,
                        duplicateSuperInterfaceSource = expression.calleeReference.source ?: source,
                        checkMemberSignatures = true,
                        ownMemberConflictSource = ownMemberConflictSource,
                    )
                }
            }
            if (expression.typeArguments.isEmpty()) return
            val declaration = callableSymbol.cfir as? CfirTypeParameterRefsOwner ?: return
            val typeArguments = expression.typeArguments.map { it.coneTypeOrNull ?: return }
            triggers += InstantiationTrigger(
                symbol = callableSymbol,
                declaration = declaration,
                typeArguments = typeArguments,
                typeArgumentSources = expression.typeArguments.map { typeArgument -> typeArgument.source ?: source },
                source = source,
                isNestedTypeArgument = false,
                checkStaticCompleteness = true,
                checkMemberSignatures = true,
                ownMemberConflictSource = ownMemberConflictSource,
            )
            typeArguments.forEach {
                collectTypeTriggers(
                    it,
                    source,
                    isNestedTypeArgument = true,
                    checkMemberSignatures = true,
                    ownMemberConflictSource = ownMemberConflictSource,
                )
            }
        }

        /**
         * 从已完成候选中恢复构造器 owner 的最终实例化类型。
         *
         * 泛型实例化成员冲突是实例化后的独立检查，不参与重载候选淘汰；即使调用引用因该
         * 冲突暂时保留错误类型，resolver 已完成的声明替换和约束系统结果仍是唯一权威来源。
         */
        private fun CfirQualifiedAccessExpression.inferredConstructedTypeFromCandidate(): ConeCangJieType? {
            val diagnostic = (calleeReference as? CfirDiagnosticHolder)?.diagnostic
                as? ConeDiagnosticWithSingleCandidate
                ?: return null
            val candidate = diagnostic.candidate
            val constructor = candidate.symbol.cfir as? CfirConstructor ?: return null
            val declaredReturnType = constructor.returnTypeRef.coneTypeOrNull ?: return null
            val candidateSubstitutor = candidate.typeParameterSubstitutorOrNull ?: return null
            val candidateReturnType = candidateSubstitutor.substituteOrSelf(declaredReturnType)
            val resultingSubstitutor = candidate.system.asReadOnlyStorage()
                .buildAbstractResultingSubstitutor(checkerContext.session.typeContext)
                .asCone()
            return resultingSubstitutor.substituteOrSelf(candidateReturnType)
                .takeUnless { it is ConeErrorType || it.containsTypeParameterSymbol() }
        }

        private fun collectTypeTriggers(
            type: ConeCangJieType,
            source: CjSourceElement?,
            isNestedTypeArgument: Boolean,
            propagateSourceAsRoot: Boolean = true,
            checkExtendInterfaces: Boolean = false,
            duplicateSuperInterfaceSource: CjSourceElement? = source,
            checkMemberSignatures: Boolean = false,
            ownMemberConflictSource: CjSourceElement? = source,
        ) {
            when (type) {
                is ConeClassifierType -> {
                    if (type.typeArguments.isNotEmpty()) {
                        val symbol = type.toSymbol(checkerContext.session) as? CfirClassLikeSymbol<*>
                        if (symbol != null) {
                            triggers += InstantiationTrigger(
                                symbol = symbol,
                                declaration = symbol.cfir,
                                typeArguments = type.typeArguments.map { it.type },
                                source = source,
                                isNestedTypeArgument = isNestedTypeArgument,
                                propagateSourceAsRoot = propagateSourceAsRoot,
                                checkExtendInterfaces = checkExtendInterfaces,
                                duplicateSuperInterfaceSource = duplicateSuperInterfaceSource,
                                checkMemberSignatures = checkMemberSignatures,
                                ownMemberConflictSource = ownMemberConflictSource,
                            )
                        }
                    }
                    type.typeArguments.forEach {
                        collectTypeTriggers(
                            it.type,
                            source,
                            isNestedTypeArgument = true,
                            propagateSourceAsRoot = propagateSourceAsRoot,
                            checkMemberSignatures = checkMemberSignatures,
                            ownMemberConflictSource = ownMemberConflictSource,
                        )
                    }
                }

                is ConeFunctionType -> {
                    type.parameterTypes.forEach {
                        collectTypeTriggers(
                            it,
                            source,
                            isNestedTypeArgument = true,
                            propagateSourceAsRoot = propagateSourceAsRoot,
                            checkMemberSignatures = checkMemberSignatures,
                            ownMemberConflictSource = ownMemberConflictSource,
                        )
                    }
                    collectTypeTriggers(
                        type.returnType,
                        source,
                        isNestedTypeArgument = true,
                        propagateSourceAsRoot = propagateSourceAsRoot,
                        checkMemberSignatures = checkMemberSignatures,
                        ownMemberConflictSource = ownMemberConflictSource,
                    )
                }

                is ConeTupleType -> type.elementTypes.forEach {
                    collectTypeTriggers(
                        it,
                        source,
                        isNestedTypeArgument = true,
                        propagateSourceAsRoot = propagateSourceAsRoot,
                        checkMemberSignatures = checkMemberSignatures,
                        ownMemberConflictSource = ownMemberConflictSource,
                    )
                }

                is ConeVArrayType -> collectTypeTriggers(
                    type.elementType,
                    source,
                    isNestedTypeArgument = true,
                    propagateSourceAsRoot = propagateSourceAsRoot,
                    checkMemberSignatures = checkMemberSignatures,
                    ownMemberConflictSource = ownMemberConflictSource,
                )
                is ConePointerType -> {
                    triggers += InstantiationTrigger(
                        symbol = null,
                        declaration = null,
                        typeArguments = listOf(type.pointeeType),
                        source = source,
                        isNestedTypeArgument = isNestedTypeArgument,
                        propagateSourceAsRoot = propagateSourceAsRoot,
                        builtinTargetKey = CfirExtendTargetKey.CPointer,
                        builtinInstantiatedType = type,
                    )
                    collectTypeTriggers(
                        type.pointeeType,
                        source,
                        isNestedTypeArgument = true,
                        propagateSourceAsRoot = propagateSourceAsRoot,
                        checkMemberSignatures = checkMemberSignatures,
                        ownMemberConflictSource = ownMemberConflictSource,
                    )
                }
                is ConeIntersectionType -> type.intersectedTypes.forEach {
                    collectTypeTriggers(
                        it,
                        source,
                        isNestedTypeArgument = true,
                        propagateSourceAsRoot = propagateSourceAsRoot,
                        checkMemberSignatures = checkMemberSignatures,
                        ownMemberConflictSource = ownMemberConflictSource,
                    )
                }

                is ConeUnionType -> type.unionTypes.forEach {
                    collectTypeTriggers(
                        it,
                        source,
                        isNestedTypeArgument = true,
                        propagateSourceAsRoot = propagateSourceAsRoot,
                        checkMemberSignatures = checkMemberSignatures,
                        ownMemberConflictSource = ownMemberConflictSource,
                    )
                }

                else -> type.typeArguments.forEach {
                    collectTypeTriggers(
                        it.type,
                        source,
                        isNestedTypeArgument = true,
                        propagateSourceAsRoot = propagateSourceAsRoot,
                        checkMemberSignatures = checkMemberSignatures,
                        ownMemberConflictSource = ownMemberConflictSource,
                    )
                }
            }
        }

        private fun collectExplicitUserTypeTrigger(
            resolvedTypeRef: CfirResolvedTypeRef,
            source: CjSourceElement?,
        ) {
            val classifierType = resolvedTypeRef.coneType as? ConeClassifierType ?: return
            if (classifierType.typeArguments.isNotEmpty()) return
            val userTypeRef = resolvedTypeRef.delegatedTypeRef as? CfirUserTypeRef ?: return
            val qualifierPart = userTypeRef.qualifier.lastOrNull() ?: return
            if (qualifierPart.typeArguments.isEmpty()) return
            val typeArguments = qualifierPart.typeArguments.map { it.coneTypeOrNull ?: return }
            val symbol = classifierType.toSymbol(checkerContext.session) as? CfirClassLikeSymbol<*> ?: return
            triggers += InstantiationTrigger(
                symbol = symbol,
                declaration = symbol.cfir,
                typeArguments = typeArguments,
                typeArgumentSources = qualifierPart.typeArguments.map { typeArgument -> typeArgument.source ?: source },
                source = source,
                isNestedTypeArgument = false,
                checkStaticCompleteness = true,
                checkMemberSignatures = true,
            )
            typeArguments.forEach {
                collectTypeTriggers(
                    it,
                    source,
                    isNestedTypeArgument = true,
                    checkMemberSignatures = true,
                )
            }
        }
        }

    /**
     * 检查一次泛型实例化中每个具体类型实参的 static 成员完整性。
     *
     * 官方检查只在对应类型参数的上界声明了 static 成员且实参已经满足上界时触发；
     * 诊断必须落在各自的类型实参 source，不能退化到整个调用表达式。
     */
    private fun checkStaticMemberCompleteness(
        trigger: InstantiationTrigger,
        typeParameters: List<CfirTypeParameterRef>,
        typeArguments: List<ConeCangJieType>,
        substitutions: Map<CfirTypeParameterSymbol, ConeCangJieType>,
    ) {
        for ((index, pair) in typeParameters.zip(typeArguments).withIndex()) {
            val (typeParameter, typeArgument) = pair
            val upperBounds = typeParameter.symbol.cfir.bounds
                .mapNotNull { bound -> bound.coneTypeOrNull }
                .map { bound -> substitute(bound, substitutions) }
            if (upperBounds.isEmpty()) continue
            if (upperBounds.any { upperBound ->
                    !AbstractTypeChecker.isSubtypeOfWithoutOptionBoxing(
                        checkerContext.session.typeContext,
                        typeArgument,
                        upperBound,
                    )
                }
            ) {
                continue
            }

            val hasStaticUpperBoundMember = context(checkerContext) {
                CfirStaticMemberCompleteness.hasStaticMembers(upperBounds)
            }
            if (!hasStaticUpperBoundMember) continue

            val isIncomplete = typeArgument.isNothing || context(checkerContext) {
                CfirStaticMemberCompleteness.unimplementedStaticRequirements(typeArgument).isNotEmpty()
            }
            if (!isIncomplete) continue

            val diagnosticSource = trigger.typeArgumentSources.getOrNull(index) ?: trigger.source ?: continue
            val sourceKey = diagnosticSource.sourceKeyOrNull() ?: continue
            if (!reportedIncompleteTypeArgumentSources.add(sourceKey)) continue
            context(checkerContext) {
                reporter.reportOn(
                    source = diagnosticSource,
                    factory = CfirErrors.CANNOT_INSTANTIATED_BY_INCOMPLETE_TYPE,
                    a = typeParameter.symbol.cfir.name,
                    b = typeArgument,
                )
            }
        }
    }

    /**
     * 使用当前声明实例化替换批量替换类型实参。
     */
    private fun substitute(
        types: List<ConeCangJieType>,
        substitutions: Map<CfirTypeParameterSymbol, ConeCangJieType>,
    ): List<ConeCangJieType> {
        if (substitutions.isEmpty()) return types
        val substitutor = createTypeSubstitutorByTypeConstructor(
            map = substitutions.mapKeys { (symbol, _) -> symbol.toLookupTag() as TypeConstructorMarker },
            context = checkerContext.session.typeContext,
            approximateIntegerLiterals = false,
        )
        return types.map { substitutor.substituteOrSelf(it) }
    }

    /**
     * 使用当前声明实例化替换单个类型。
     */
    private fun substitute(
        type: ConeCangJieType,
        substitutions: Map<CfirTypeParameterSymbol, ConeCangJieType>,
    ): ConeCangJieType {
        if (substitutions.isEmpty()) return type
        val substitutor = createTypeSubstitutorByTypeConstructor(
            map = substitutions.mapKeys { (symbol, _) -> symbol.toLookupTag() as TypeConstructorMarker },
            context = checkerContext.session.typeContext,
            approximateIntegerLiterals = false,
        )
        return substitutor.substituteOrSelf(type)
    }

    /**
     * 判断类型参数到类型实参映射是否形成带扩张边的循环。
     */
    private fun hasCyclicInstantiation(
        typeParameters: List<CfirTypeParameterRef>,
        typeArguments: List<ConeCangJieType>,
    ): Boolean {
        val parameterSymbols = typeParameters.mapTo(linkedSetOf()) { it.symbol }
        val graph = linkedMapOf<CfirTypeParameterSymbol, MutableList<TypeParameterEdge>>()
        for ((typeParameter, typeArgument) in typeParameters.zip(typeArguments)) {
            val from = typeParameter.symbol
            for (to in typeArgument.collectTypeParameterSymbols(parameterSymbols)) {
                val expansive = !typeArgument.isExactlyTypeParameter(to)
                if (from == to && !expansive) continue
                graph.getOrPut(from) { mutableListOf() } += TypeParameterEdge(to, expansive)
                graph.getOrPut(to) { mutableListOf() }
            }
        }
        return graph.hasExpansiveCycle()
    }

    /**
     * 检查实例化后的成员函数签名是否产生冲突。
     */
    private fun checkInstantiatedMemberSignatures(
        declaration: CfirClassLikeDeclaration,
        substitutions: Map<CfirTypeParameterSymbol, ConeCangJieType>,
        triggerSource: CjSourceElement?,
        ownMemberConflictSource: CjSourceElement?,
    ) {
        if (substitutions.isEmpty()) return
        val membersByName = declaration.collectInstantiatedMemberSignatures(substitutions)
        reportInstantiatedMemberSignatureConflicts(
            membersByName = membersByName,
            triggerSource = triggerSource,
            ownMemberConflictSource = ownMemberConflictSource,
            instantiationName = { declaration.renderInstantiationName(substitutions) },
        )
    }

    /**
     * 官方 `CheckInstDupSuperInterfaces` 会在类型引用/构造表达式实例化目标声明时，
     * 对目标声明的父接口表做一次带类型实参的重复接口检查。这里和声明级父类型检查互补，
     * 诊断归属实例化触发引用。
     */
    private fun checkInstantiatedDuplicateSupertypes(
        declaration: CfirClassLikeDeclaration,
        substitutions: Map<CfirTypeParameterSymbol, ConeCangJieType>,
        triggerSource: CjSourceElement?,
        checkExtendInterfaces: Boolean,
        duplicateSuperInterfaceSource: CjSourceElement?,
    ) {
        if (substitutions.isEmpty()) return
        if (substitutions.values.any { it.containsTypeParameterSymbol() }) return
        val source = duplicateSuperInterfaceSource ?: triggerSource ?: return
        if (source.usesDeclarationTypeParameterArgument(declaration.typeParameters)) return
        val substitutor = substitutions.toConeSubstitutor()
        val instantiatedSelfType = declaration.instantiatedSelfType(substitutor)
        val duplicatedInterface = context(checkerContext) {
            declaration.findInstantiatedDuplicateSuperInterface(
                substitutor = substitutor,
                passedDeclarations = linkedSetOf(),
                checkExtendInterfaces = checkExtendInterfaces,
                instantiatedSelfType = instantiatedSelfType,
            )
        } ?: return

        context(checkerContext) {
            reporter.reportOn(
                source = source.typeConstructorNameDiagnosticSource(declaration.name),
                factory = CfirErrors.SUPER_TYPES_DUPLICATE,
                a = duplicatedInterface,
                positioningStrategy = SourceElementPositioningStrategies.DEFAULT,
            )
        }
    }

    /**
     * 检查内建目标 extend 在具体目标类型上的成员签名冲突。
     */
    private fun checkInstantiatedBuiltinExtendMemberSignatures(
        targetKey: CfirExtendTargetKey,
        instantiatedType: ConeCangJieType,
        triggerSource: CjSourceElement?,
    ) {
        val membersByName = collectBuiltinExtendMemberSignatures(targetKey, instantiatedType)
        if (membersByName.isEmpty()) return
        reportInstantiatedMemberSignatureConflicts(
            membersByName = membersByName,
            triggerSource = triggerSource,
            ownMemberConflictSource = triggerSource,
            instantiationName = { Name.identifier(instantiatedType.renderForInstantiationDiagnostic()) },
        )
    }

    /**
     * 报告实例化成员签名冲突。
     */
    private fun reportInstantiatedMemberSignatureConflicts(
        membersByName: Map<Name, List<InstantiatedMemberSignature>>,
        triggerSource: CjSourceElement?,
        ownMemberConflictSource: CjSourceElement?,
        instantiationName: () -> Name,
    ) {
        for ((name, members) in membersByName) {
            val stableMembers = mutableListOf<InstantiatedMemberSignature>()
            val genericMembers = members.filter { it.hasGenericTypes }
            stableMembers += members.filterNot { it.hasGenericTypes }

            for (genericMember in genericMembers) {
                for (stableMember in stableMembers) {
                    if (!stableMember.conflictsWith(genericMember)) continue
                    if (stableMember.isInheritedDefaultInterfaceConflictWith(genericMember)) continue
                    val diagnosticSource = if (stableMember.isOwnMember || genericMember.isOwnMember) {
                        ownMemberConflictSource ?: triggerSource
                    } else {
                        triggerSource
                    }
                    val sourceKey = diagnosticSource.sourceKeyOrNull() ?: break
                    reportGenericInstantiationCausesAmbiguousFunctions(
                        source = diagnosticSource,
                        sourceKey = sourceKey,
                        instantiationName = instantiationName(),
                        functionName = name,
                    )
                    break
                }
                // 官方会把已实例化的泛型成员放回成员表，后续泛型成员需要继续与它比较。
                stableMembers += genericMember
            }
        }
    }

    /**
     * 收集指定内建 extend 目标在具体实例化类型上的成员函数签名。
     */
    private fun collectBuiltinExtendMemberSignatures(
        targetKey: CfirExtendTargetKey,
        instantiatedType: ConeCangJieType,
    ): Map<Name, List<InstantiatedMemberSignature>> {
        val signatures = mutableListOf<InstantiatedMemberSignature>()
        val extendProvider = checkerContext.session.extendProvider
        for (extend in extendProvider.getExtendsForTarget(targetKey)) {
            if (!extendProvider.isExtendAccessible(extend)) continue
            val targetPattern = extend.extendedTypeRef.coneTypeOrNull ?: continue
            val substitution = createExtendDeclarationSubstitution(
                session = checkerContext.session,
                extend = extend,
                targetPattern = targetPattern,
                concreteReceiverType = instantiatedType,
            ) ?: continue

            signatures += extend.collectOwnFunctionSignatures(substitution.substitutor)
            signatures += extend.collectInheritedDefaultFunctionSignatures(extend, substitution.substitutor)
        }
        return signatures.groupBy { it.name }
    }

    /**
     * 收集当前 extend 自身声明的函数签名。
     */
    private fun CfirExtend.collectOwnFunctionSignatures(
        substitutor: ConeSubstitutor,
    ): List<InstantiatedMemberSignature> {
        return declarations
            .asSequence()
            .filterIsInstance<CfirFunction>()
            .mapNotNull { function ->
                function.toInstantiatedMemberSignature(Name.identifier("extend"), substitutor)
            }
            .toList()
    }

    /**
     * 收集当前 extend 从接口继承的默认函数签名。
     */
    private fun CfirExtend.collectInheritedDefaultFunctionSignatures(
        ownerExtend: CfirExtend,
        substitutor: ConeSubstitutor,
    ): List<InstantiatedMemberSignature> {
        val signatures = mutableListOf<InstantiatedMemberSignature>()
        val visitedInterfaces = linkedSetOf<String>()
        for (superTypeRef in superTypeRefs) {
            val supertype = superTypeRef.coneTypeOrNull ?: continue
            collectDefaultInterfaceFunctionSignatures(
                ownerExtend = ownerExtend,
                interfaceType = substitutor.substituteOrSelf(supertype),
                genericInterfaceType = supertype,
                destination = signatures,
                visitedInterfaces = visitedInterfaces,
            )
        }
        return signatures
    }

    /**
     * 递归收集接口默认函数签名。
     */
    private fun collectDefaultInterfaceFunctionSignatures(
        ownerExtend: CfirExtend?,
        interfaceType: ConeCangJieType,
        genericInterfaceType: ConeCangJieType,
        destination: MutableList<InstantiatedMemberSignature>,
        visitedInterfaces: MutableSet<String>,
    ) {
        val classifierType = interfaceType as? ConeClassifierType ?: return
        val symbol = classifierType.toSymbol(checkerContext.session) as? CfirClassLikeSymbol<*> ?: return
        val genericClassifierType = genericInterfaceType as? ConeClassifierType ?: classifierType
        val interfaceKey = context(checkerContext) {
            genericClassifierType.instantiatedInterfaceKey()
        } ?: symbol.classId.asString()
        if (!visitedInterfaces.add(interfaceKey)) return
        val declaration = symbol.cfir as? CfirClassLikeDeclaration ?: return
        val substitutor = declaration.createDeclarationTypeSubstitutor(interfaceType)
        val genericSubstitutor = declaration.createDeclarationTypeSubstitutor(genericClassifierType)

        if (declaration is CfirInterface) {
            for (member in declaration.declarations) {
                val function = member as? CfirFunction ?: continue
                destination += function.toInstantiatedMemberSignature(
                    ownerName = declaration.name,
                    substitutor = substitutor,
                    inheritedDefaultOwnerExtend = ownerExtend,
                    genericSubstitutor = genericSubstitutor,
                    inheritedInterfaceKey = interfaceKey,
                    isOwnMember = false,
                ) ?: continue
            }
        }

        for (superTypeRef in declaration.superTypeRefs) {
            val supertype = superTypeRef.coneTypeOrNull ?: continue
            collectDefaultInterfaceFunctionSignatures(
                ownerExtend = ownerExtend,
                interfaceType = substitutor.substituteOrSelf(supertype),
                genericInterfaceType = genericSubstitutor.substituteOrSelf(supertype),
                destination = destination,
                visitedInterfaces = visitedInterfaces,
            )
        }
    }

    /**
     * 收集 class-like 声明实例化后的所有函数成员签名。
     */
    private fun CfirClassLikeDeclaration.collectInstantiatedMemberSignatures(
        substitutions: Map<CfirTypeParameterSymbol, ConeCangJieType>,
    ): Map<Name, List<InstantiatedMemberSignature>> {
        val substitutor = substitutions.toConeSubstitutor()
        val instantiatedType = instantiatedSelfType(substitutor) ?: return emptyMap()
        val concreteScopes = instantiatedUseSiteMemberScopes(instantiatedType) ?: return emptyMap()
        val genericType = declarationSelfTypeForInstantiation() ?: return emptyMap()
        val genericScope = instantiatedUseSiteMemberScopes(genericType)?.substituted ?: return emptyMap()
        val ownFunctions: Set<CfirFunctionSymbol<*>> = declarations.asSequence()
            .filterIsInstance<CfirFunction>()
            .map { it.symbol }
            .toCollection(linkedSetOf())
        val signatures = mutableListOf<InstantiatedMemberSignature>()

        for (callableName in concreteScopes.substituted.getCallableNames()) {
            val genericIndependentMembers = genericScope.originalFunctionSymbolsByName(callableName)
            concreteScopes.substituted.processFunctionsByName(callableName) { functionSymbol ->
                val originalSymbol = functionSymbol.unwrapSubstitutionOverrides()
                if (originalSymbol !in genericIndependentMembers) {
                    return@processFunctionsByName
                }
                functionSymbol.toInstantiatedMemberSignature(
                    ownerName = name,
                    ownFunctions = ownFunctions,
                )?.let(signatures::add)

                // 仅当前类型 own member 才可能在具体实例化后新覆盖一个原本独立的父 overload。
                // inherited requirement 的 override 链已经由 provider MergeInheritedMembers 归并，
                // 不能在 checker 中再次拆开。
                if (originalSymbol !in ownFunctions) return@processFunctionsByName
                concreteScopes.raw.processDirectOverriddenFunctionsWithBaseScope(originalSymbol) { overridden, _ ->
                    if (overridden.unwrapSubstitutionOverrides() in genericIndependentMembers) {
                        overridden.toInstantiatedMemberSignature(
                            ownerName = name,
                            ownFunctions = ownFunctions,
                        )?.let(signatures::add)
                    }
                    ProcessorAction.NEXT
                }
            }
        }
        concreteScopes.substituted.processDeclaredConstructors { constructorSymbol ->
            constructorSymbol.toInstantiatedMemberSignature(
                ownerName = name,
                ownFunctions = ownFunctions,
            )?.let(signatures::add)
        }

        return signatures.groupBy { it.name }
    }

    /**
     * 为具体实例化类型创建统一的 use-site substitution scope。
     *
     * 父类、接口与 extend 的替换和覆盖合并由 provider scope 统一完成；checker 只消费
     * 合并后的独立函数成员，避免再维护一套与调用解析分叉的继承遍历。
     */
    private fun CfirClassLikeDeclaration.instantiatedUseSiteMemberScopes(
        instantiatedType: ConeCangJieType,
    ): InstantiatedMemberScopes? {
        val classLikeSymbol = symbol as? CfirClassLikeSymbol<*> ?: return null
        val rawScope = CfirClassUseSiteMemberScope(
            session = checkerContext.session,
            classSymbol = classLikeSymbol,
            symbolProvider = checkerContext.session.symbolProvider,
            extendProvider = checkerContext.session.extendProvider,
            directSupertypeProvider = checkerContext.session.directSupertypeProviderOrNull,
            ownerType = instantiatedType,
            dispatchReceiverType = instantiatedType,
            scopeKind = CfirClassMemberScopeKind.USE_SITE,
        )
        val substitutedScope = CfirClassSubstitutionScope(
            session = checkerContext.session,
            useSiteMemberScope = rawScope,
            dispatchReceiverType = instantiatedType,
        )
        return InstantiatedMemberScopes(rawScope, substitutedScope)
    }

    /**
     * 收集泛型声明形态下仍然独立可见的原始函数成员。
     */
    private fun CfirTypeScope.originalFunctionSymbolsByName(name: Name): Set<CfirNamedFunctionSymbol> = buildSet {
        processFunctionsByName(name) { functionSymbol ->
            add(functionSymbol.unwrapSubstitutionOverrides())
        }
    }

    /**
     * 把 scope 已完成 owner 替换的函数符号转换为冲突签名。
     */
    private fun CfirFunctionSymbol<*>.toInstantiatedMemberSignature(
        ownerName: Name,
        ownFunctions: Set<CfirFunctionSymbol<*>>,
    ): InstantiatedMemberSignature? {
        val instantiatedFunction = cfir
        if (instantiatedFunction.typeParameters.isNotEmpty()) return null
        val originalSymbol = unwrapSubstitutionOverrides()
        val originalFunction = originalSymbol.cfir
        if (
            originalFunction.origin != CfirDeclarationOrigin.Source &&
            originalFunction.origin !is CfirDeclarationOrigin.SubstitutionOverride
        ) {
            return null
        }
        val parameterTypes = instantiatedFunction.valueParameters.map { parameter ->
            parameter.returnTypeRef.coneTypeOrNull ?: return null
        }
        val hasGenericParameterTypes = originalFunction.valueParameters.any { parameter ->
            parameter.returnTypeRef.coneTypeOrNull?.containsTypeParameterSymbol() == true
        }
        return InstantiatedMemberSignature(
            function = originalFunction,
            name = originalFunction.instantiationMemberName(ownerName),
            isStatic = instantiatedFunction.status.isStatic,
            parameterTypes = parameterTypes,
            hasGenericTypes = hasGenericParameterTypes,
            inheritedDefaultOwnerExtend = null,
            inheritedInterfaceKey = null,
            isOwnMember = originalSymbol in ownFunctions,
        )
    }

    /**
     * 构造当前 class-like 声明在替换器作用下的 self type。
     */
    private fun CfirClassLikeDeclaration.instantiatedSelfType(
        substitutor: ConeSubstitutor,
    ): ConeCangJieType? {
        val selfType = declarationSelfTypeForInstantiation() ?: return null
        return substitutor.substituteOrSelf(selfType)
    }

    /**
     * 构造当前 class-like 声明以自身类型参数为实参的 self type。
     */
    private fun CfirClassLikeDeclaration.declarationSelfTypeForInstantiation(): ConeCangJieType? {
        val classLikeSymbol = symbol as? CfirClassLikeSymbol<*> ?: return null
        val arguments = typeParameters.map { typeParameter ->
            ConeTypeParameterTypeImpl(typeParameter.symbol.toLookupTag())
        }
        return when (classLikeSymbol) {
            is CfirInterfaceSymbol -> ConeClassLikeType(
                lookupTag = classLikeSymbol.toLookupTag(),
                typeArguments = arguments,
                isInterface = true,
            )
            is CfirStructSymbol -> ConeStructType(
                lookupTag = classLikeSymbol.toLookupTag(),
                typeArguments = arguments,
            )
            is CfirEnumSymbol -> ConeEnumType(
                lookupTag = classLikeSymbol.toLookupTag(),
                typeArguments = arguments,
                isRefEnum = classLikeSymbol.isRefEnum,
            )
            else -> ConeClassLikeType(
                lookupTag = classLikeSymbol.toLookupTag(),
                typeArguments = arguments,
            )
        }
    }

    /**
     * 将函数声明转换为实例化成员签名。
     */
    private fun CfirFunction.toInstantiatedMemberSignature(
        ownerName: Name,
        substitutor: ConeSubstitutor,
        inheritedDefaultOwnerExtend: CfirExtend? = null,
        genericSubstitutor: ConeSubstitutor = ConeSubstitutor.Empty,
        inheritedInterfaceKey: String? = null,
        isOwnMember: Boolean = true,
    ): InstantiatedMemberSignature? {
        if (origin != CfirDeclarationOrigin.Source && origin !is CfirDeclarationOrigin.SubstitutionOverride) return null
        if (typeParameters.isNotEmpty()) return null
        val parameterTypes = valueParameters.map { parameter ->
            parameter.returnTypeRef.coneTypeOrNull ?: return null
        }
        val returnType = returnTypeRef.coneTypeOrNull
        val signatureTypes = if (returnType == null) parameterTypes else parameterTypes + returnType
        val genericSignatureTypes = signatureTypes.map { type -> genericSubstitutor.substituteOrSelf(type) }
        val hasGenericTypes = genericSignatureTypes.any { type ->
            type.containsTypeParameterSymbol()
        }
        val instantiatedParameterTypes = parameterTypes.map { substitutor.substituteOrSelf(it) }
        return InstantiatedMemberSignature(
            function = this,
            name = instantiationMemberName(ownerName),
            isStatic = status.isStatic,
            parameterTypes = instantiatedParameterTypes,
            hasGenericTypes = hasGenericTypes,
            inheritedDefaultOwnerExtend = inheritedDefaultOwnerExtend,
            inheritedInterfaceKey = inheritedInterfaceKey,
            isOwnMember = isOwnMember,
        )
    }

    /**
     * 把类型参数替换表转换为 cone substitutor。
     */
    private fun Map<CfirTypeParameterSymbol, ConeCangJieType>.toConeSubstitutor(): ConeSubstitutor {
        if (isEmpty()) return ConeSubstitutor.Empty
        return createTypeSubstitutorByTypeConstructor(
            map = mapKeys { (symbol, _) -> symbol.toLookupTag() as TypeConstructorMarker },
            context = checkerContext.session.typeContext,
            approximateIntegerLiterals = false,
        )
    }

    /**
     * 根据 class-like 实例化类型创建声明类型参数替换器。
     */
    private fun CfirTypeParameterRefsOwner.createDeclarationTypeSubstitutor(
        type: ConeCangJieType,
    ): ConeSubstitutor {
        val lookupType = type as? ConeLookupTagBasedType ?: return ConeSubstitutor.Empty
        if (typeParameters.isEmpty() || typeParameters.size != lookupType.typeArguments.size) {
            return ConeSubstitutor.Empty
        }
        val substitutions = typeParameters.zip(lookupType.typeArguments).associate { (typeParameter, argument) ->
            typeParameter.symbol.toLookupTag() as TypeConstructorMarker to argument.type
        }
        return createTypeSubstitutorByTypeConstructor(
            map = substitutions,
            context = checkerContext.session.typeContext,
            approximateIntegerLiterals = false,
        )
    }

    /**
     * 取得实例化成员冲突诊断使用的成员名称。
     */
    private fun CfirFunction.instantiationMemberName(ownerName: Name): Name =
        if (this is CfirConstructor) ownerName else symbol.name

    /**
     * 判断两个实例化成员签名是否冲突。
     */
    private fun InstantiatedMemberSignature.conflictsWith(other: InstantiatedMemberSignature): Boolean {
        if (function === other.function && inheritedInterfaceKey == other.inheritedInterfaceKey) return false
        if (isStatic != other.isStatic) return false
        if (parameterTypes.size != other.parameterTypes.size) return false
        return parameterTypes.zip(other.parameterTypes).all { (left, right) ->
            AbstractTypeChecker.equalTypes(checkerContext.session.typeContext, left, right)
        }
    }

    /**
     * 判断冲突双方是否都是继承来的接口默认实现。
     */
    private fun InstantiatedMemberSignature.isInheritedDefaultInterfaceConflictWith(
        other: InstantiatedMemberSignature,
    ): Boolean =
        inheritedDefaultOwnerExtend != null && other.inheritedDefaultOwnerExtend != null

    /**
     * 渲染 class-like 声明的实例化名称。
     */
    private fun CfirClassLikeDeclaration.renderInstantiationName(
        substitutions: Map<CfirTypeParameterSymbol, ConeCangJieType>,
    ): Name {
        val arguments = typeParameters.joinToString(", ") { typeParameter ->
            substitutions[typeParameter.symbol]?.renderForInstantiationDiagnostic()
                ?: typeParameter.symbol.name.asString()
        }
        return Name.identifier("${name.asString()}<$arguments>")
    }

    /**
     * 构造当前声明的活动实例化栈帧。
     */
    private fun CfirDeclaration.instantiationFrameOrNull(
        substitutions: Map<CfirTypeParameterSymbol, ConeCangJieType>,
    ): DeclarationInstantiationFrame? {
        val owner = this as? CfirTypeParameterRefsOwner ?: return null
        if (owner.typeParameters.isEmpty()) return null
        val arguments = owner.typeParameters.map { typeParameter ->
            substitutions[typeParameter.symbol] ?: return null
        }
        return DeclarationInstantiationFrame(this, arguments)
    }

    /**
     * 判断目标声明当前是否已经在活动栈中以会扩张的类型实参出现。
     */
    private fun hasExpandingActiveInstantiation(
        declaration: CfirDeclaration,
        typeArguments: List<ConeCangJieType>,
    ): Boolean {
        for (frame in activeInstantiations.asReversed()) {
            if (frame.declaration !== declaration) continue
            if (typeArguments.structurallyEqual(frame.typeArguments)) continue
            if (typeArguments.any { type ->
                    type.containsInstantiationOf(
                        declaration = declaration,
                        typeArguments = frame.typeArguments,
                    )
                }
            ) {
                return true
            }
        }
        return false
    }

    /**
     * 判断类型参数依赖图中是否存在带扩张边的环。
     */
    private fun Map<CfirTypeParameterSymbol, List<TypeParameterEdge>>.hasExpansiveCycle(): Boolean {
        for ((from, edges) in this) {
            for (edge in edges) {
                if (reaches(
                        current = edge.to,
                        target = from,
                        hasExpansiveEdge = edge.expansive,
                        visited = linkedSetOf(),
                    )
                ) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * 在类型参数依赖图中判断 current 是否可达 target。
     */
    private fun Map<CfirTypeParameterSymbol, List<TypeParameterEdge>>.reaches(
        current: CfirTypeParameterSymbol,
        target: CfirTypeParameterSymbol,
        hasExpansiveEdge: Boolean,
        visited: MutableSet<Pair<CfirTypeParameterSymbol, Boolean>>,
    ): Boolean {
        if (current == target && hasExpansiveEdge) return true
        if (!visited.add(current to hasExpansiveEdge)) return false
        for (edge in this[current].orEmpty()) {
            if (reaches(edge.to, target, hasExpansiveEdge || edge.expansive, visited)) return true
        }
        return false
    }

    /**
     * 从类型结构中收集允许集合内出现的类型参数符号。
     */
    private fun ConeCangJieType.collectTypeParameterSymbols(
        allowed: Set<CfirTypeParameterSymbol>,
    ): Set<CfirTypeParameterSymbol> {
        val result = linkedSetOf<CfirTypeParameterSymbol>()
        val visited = IdentityHashMap<ConeCangJieType, Unit>()
        val workList = ArrayDeque<TypeTraversalItem>()
        workList.add(TypeTraversalItem(this, depth = 0))

        while (workList.isNotEmpty()) {
            val (type, depth) = workList.removeLast()
            if (depth >= TYPE_KEY_MAX_DEPTH) continue
            if (visited.put(type, Unit) != null) continue

            val typeParameter = (type as? ConeTypeParameterType)?.lookupTag?.typeParameterSymbol
            if (typeParameter != null && typeParameter in allowed) {
                result += typeParameter
            }

            for (nestedType in type.nestedTypesForTraversal()) {
                workList.add(TypeTraversalItem(nestedType, depth + 1))
            }
        }
        return result
    }

    /**
     * 判断类型结构中是否包含任意类型参数。
     */
    private fun ConeCangJieType.containsTypeParameterSymbol(): Boolean {
        val visited = IdentityHashMap<ConeCangJieType, Unit>()
        val workList = ArrayDeque<TypeTraversalItem>()
        workList.add(TypeTraversalItem(this, depth = 0))

        while (workList.isNotEmpty()) {
            val (type, depth) = workList.removeLast()
            if (depth >= TYPE_KEY_MAX_DEPTH) continue
            if (visited.put(type, Unit) != null) continue

            if (type is ConeTypeParameterType) return true

            for (nestedType in type.nestedTypesForTraversal()) {
                workList.add(TypeTraversalItem(nestedType, depth + 1))
            }
        }
        return false
    }

    /**
     * 取得类型结构遍历时需要继续下探的嵌套类型集合。
     */
    private fun ConeCangJieType.nestedTypesForTraversal(): Collection<ConeCangJieType> =
        when (this) {
            is ConeFunctionType -> parameterTypes + listOf(returnType)
            is ConeTupleType -> elementTypes
            is ConeVArrayType -> listOf(elementType)
            is ConePointerType -> listOf(pointeeType)
            is ConeIntersectionType -> intersectedTypes
            is ConeUnionType -> unionTypes
            else -> typeArguments.mapNotNull { it.type }
        }

    /**
     * 判断当前类型结构中是否包含指定声明的某个实例化形式。
     */
    private fun ConeCangJieType.containsInstantiationOf(
        declaration: CfirDeclaration,
        typeArguments: List<ConeCangJieType>,
        visited: MutableSet<ConeCangJieType> = java.util.Collections.newSetFromMap(IdentityHashMap()),
        depth: Int = 0,
    ): Boolean {
        if (depth >= TYPE_KEY_MAX_DEPTH) return false
        if (!visited.add(this)) return false
        val classifierType = this as? ConeClassifierType
        if (
            classifierType != null &&
            classifierType.toSymbol(checkerContext.session)?.cfir === declaration &&
            classifierType.typeArguments.map { it.type }.structurallyEqual(typeArguments)
        ) {
            return true
        }
        return nestedTypesForTraversal().any { nested ->
            nested.containsInstantiationOf(declaration, typeArguments, visited, depth + 1)
        }
    }

    /**
     * 判断两个类型列表是否结构相等。
     */
    private fun List<ConeCangJieType>.structurallyEqual(other: List<ConeCangJieType>): Boolean {
        if (size != other.size) return false
        val visited = mutableSetOf<TypePairKey>()
        return zip(other).all { (left, right) ->
            left.structurallyEqual(right, visited, depth = 0)
        }
    }

    /**
     * 判断两个类型结构是否相等。
     */
    private fun ConeCangJieType.structurallyEqual(
        other: ConeCangJieType,
        visited: MutableSet<TypePairKey>,
        depth: Int,
    ): Boolean {
        if (this === other) return true
        if (depth >= TYPE_KEY_MAX_DEPTH) return false
        if (!visited.add(TypePairKey(this, other))) return true
        return when (this) {
            is ConeLookupTagBasedType -> {
                val otherLookup = other as? ConeLookupTagBasedType ?: return false
                lookupTag == otherLookup.lookupTag &&
                    typeArguments.map { it.type }.structurallyEqual(
                        otherLookup.typeArguments.map { it.type },
                        visited,
                        depth + 1,
                    )
            }

            is ConeTypeParameterType -> {
                val otherParameter = other as? ConeTypeParameterType ?: return false
                lookupTag.typeParameterSymbol == otherParameter.lookupTag.typeParameterSymbol
            }

            is ConeTupleType -> {
                val otherTuple = other as? ConeTupleType ?: return false
                elementTypes.structurallyEqual(otherTuple.elementTypes, visited, depth + 1)
            }

            is ConeFunctionType -> {
                val otherFunction = other as? ConeFunctionType ?: return false
                isCFunc == otherFunction.isCFunc &&
                    isClosureType == otherFunction.isClosureType &&
                    hasVariableLenArg == otherFunction.hasVariableLenArg &&
                    parameterTypes.structurallyEqual(otherFunction.parameterTypes, visited, depth + 1) &&
                    returnType.structurallyEqual(otherFunction.returnType, visited, depth + 1)
            }

            is ConeVArrayType -> {
                val otherVArray = other as? ConeVArrayType ?: return false
                size == otherVArray.size &&
                    elementType.structurallyEqual(otherVArray.elementType, visited, depth + 1)
            }

            is ConePointerType -> {
                val otherPointer = other as? ConePointerType ?: return false
                pointeeType.structurallyEqual(otherPointer.pointeeType, visited, depth + 1)
            }

            is ConeIntersectionType -> {
                val otherIntersection = other as? ConeIntersectionType ?: return false
                intersectedTypes.toList().structurallyEqual(otherIntersection.intersectedTypes.toList(), visited, depth + 1)
            }

            is ConeUnionType -> {
                val otherUnion = other as? ConeUnionType ?: return false
                unionTypes.toList().structurallyEqual(otherUnion.unionTypes.toList(), visited, depth + 1)
            }

            is ConePrimitiveType -> other is ConePrimitiveType && kind == other.kind
            is ConeCStringType -> other is ConeCStringType
            is ConeQuestType -> other is ConeQuestType
            else -> false
        }
    }

    /**
     * 在共享 visited 集合下判断两个类型列表是否结构相等。
     */
    private fun List<ConeCangJieType>.structurallyEqual(
        other: List<ConeCangJieType>,
        visited: MutableSet<TypePairKey>,
        depth: Int,
    ): Boolean {
        if (size != other.size) return false
        return zip(other).all { (left, right) ->
            left.structurallyEqual(right, visited, depth)
        }
    }

    /**
     * 渲染实例化诊断中展示的类型文本。
     */
    private fun ConeCangJieType.renderForInstantiationDiagnostic(
        visited: MutableSet<ConeCangJieType> = java.util.Collections.newSetFromMap(IdentityHashMap()),
        depth: Int = 0,
    ): String {
        if (depth >= TYPE_KEY_MAX_DEPTH) return "..."
        if (!visited.add(this)) return "..."
        return when (this) {
            is ConeLookupTagBasedType -> buildString {
                append(lookupTag.name.asString())
                if (typeArguments.isNotEmpty()) {
                    append(typeArguments.joinToString(prefix = "<", postfix = ">") {
                        it.type.renderForInstantiationDiagnostic(visited, depth + 1)
                    })
                }
            }

            is ConeTypeParameterType -> lookupTag.typeParameterSymbol.name.asString()
            is ConeTupleType -> elementTypes.joinToString(prefix = "(", postfix = ")") {
                it.renderForInstantiationDiagnostic(visited, depth + 1)
            }
            is ConeFunctionType -> buildString {
                append(parameterTypes.joinToString(prefix = "(", postfix = ")") {
                    it.renderForInstantiationDiagnostic(visited, depth + 1)
                })
                append(" -> ")
                append(returnType.renderForInstantiationDiagnostic(visited, depth + 1))
            }
            is ConeVArrayType -> "VArray<${elementType.renderForInstantiationDiagnostic(visited, depth + 1)}, $size>"
            is ConePointerType -> "CPointer<${pointeeType.renderForInstantiationDiagnostic(visited, depth + 1)}>"
            is ConeIntersectionType -> intersectedTypes.joinToString(prefix = "(", postfix = ")", separator = " & ") {
                it.renderForInstantiationDiagnostic(visited, depth + 1)
            }
            is ConeUnionType -> unionTypes.joinToString(prefix = "(", postfix = ")", separator = " | ") {
                it.renderForInstantiationDiagnostic(visited, depth + 1)
            }
            is ConePrimitiveType -> kind.typeName
            is ConeCStringType -> "CString"
            is ConeQuestType -> "?"
            else -> javaClass.simpleName
        }
    }

    /**
     * 判断类型是否正好是指定类型参数。
     */
    private fun ConeCangJieType.isExactlyTypeParameter(symbol: CfirTypeParameterSymbol): Boolean =
        (this as? ConeTypeParameterType)?.lookupTag?.typeParameterSymbol == symbol

    /**
     * 报告泛型无限实例化诊断。
     */
    private fun reportInfiniteInstantiation(source: CjSourceElement?) {
        val diagnosticSource = source?.firstCharacterDiagnosticSource() ?: return
        val sourceKey = diagnosticSource.sourceKeyOrNull() ?: return
        if (!reportedSources.add(sourceKey)) return
        infiniteInstantiationOccurred = true
        context(checkerContext) {
            reporter.reportOn(diagnosticSource, CfirErrors.GENERIC_INFINITE_INSTANTIATION)
        }
    }

    /**
     * 把类型构造器 source 收窄到构造器名称。
     */
    private fun CjSourceElement.typeConstructorNameDiagnosticSource(name: Name): CjOffsetsOnlySourceElement {
        val sourceText = text?.toString()
        val nameText = name.asString()
        val nameStartInSource = sourceText
            ?.substringBefore('<')
            ?.lastIndexOf(nameText)
            ?.takeIf { it >= 0 }
            ?: 0
        return CjOffsetsOnlySourceElement(
            startOffset = startOffset + nameStartInSource,
            endOffset = startOffset + nameStartInSource + nameText.length,
        )
    }

    /**
     * 重复父接口实例化诊断只面向已经固定的实例化引用。
     *
     * `A<X, Y>` 这类仍直接携带目标声明类型参数名的引用，要等真实调用/类型使用
     * 固定类型实参后，再由成员实例化检查或具体重复父接口检查报告。
     */
    private fun CjSourceElement.usesDeclarationTypeParameterArgument(
        typeParameters: List<CfirTypeParameterRef>,
    ): Boolean {
        if (typeParameters.isEmpty()) return false
        val sourceText = text?.toString() ?: return false
        val argumentText = sourceText.substringAfter('<', missingDelimiterValue = "")
            .substringBeforeLast('>', missingDelimiterValue = "")
        if (argumentText.isBlank()) return false
        return typeParameters.any { typeParameter ->
            val parameterName = Regex.escape(typeParameter.symbol.name.asString())
            Regex("""(?<![\p{L}\p{N}_])$parameterName(?![\p{L}\p{N}_])""").containsMatchIn(argumentText)
        }
    }

    /**
     * 报告泛型实例化导致成员函数调用歧义的诊断。
     */
    private fun reportGenericInstantiationCausesAmbiguousFunctions(
        source: CjSourceElement?,
        sourceKey: SourceKey,
        instantiationName: Name,
        functionName: Name,
    ) {
        val diagnosticSource = source ?: return
        val reportKey = MemberInstantiationReportKey(sourceKey, functionName)
        if (!reportedMemberInstantiationSources.add(reportKey)) return
        checkerContext.recordGenericInstantiationMemberConflict(diagnosticSource)
        context(checkerContext) {
            reporter.reportOn(
                diagnosticSource,
                CfirErrors.GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS,
                instantiationName,
                functionName,
            )
        }
    }

    /**
     * 构造声明实例化去重 key。
     */
    private fun CfirDeclaration.instantiationKey(
        substitutions: Map<CfirTypeParameterSymbol, ConeCangJieType>,
    ): DeclarationInstantiationKey {
        if (substitutions.isEmpty()) return DeclarationInstantiationKey(symbol, emptyList())
        val arguments = (this as? CfirTypeParameterRefsOwner)
            ?.typeParameters
            ?.map { typeParameter ->
                substitutions[typeParameter.symbol]?.instantiationKey()
                    ?: "P@${System.identityHashCode(typeParameter.symbol)}"
            }
            .orEmpty()
        return DeclarationInstantiationKey(symbol, arguments)
    }

    /**
     * 构造单个类型实参的实例化 key。
     */
    private fun ConeCangJieType.instantiationKey(): String =
        when (this) {
            is ConeTypeParameterType -> "P@${System.identityHashCode(lookupTag.typeParameterSymbol)}"
            else -> "${javaClass.name}@${System.identityHashCode(this)}"
        }

    /**
     * 从 qualified access 解析 callable 符号。
     */
    private fun CfirQualifiedAccessExpression.resolvedCallableSymbolOrNull(): CfirCallableSymbol<*>? =
        when (val reference = calleeReference) {
            is CfirResolvedErrorReference -> reference.resolvedSymbol as? CfirCallableSymbol<*>
            is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirCallableSymbol<*>
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirCallableSymbol<*>
            else -> null
        }

    /**
     * 判断 callable 符号是否表示构造器形式的实例化调用。
     */
    private fun CfirCallableSymbol<*>.isConstructorLikeInstantiationCall(): Boolean {
        if (this is CfirConstructorSymbol) return true
        return when (cfir.origin) {
            CfirDeclarationOrigin.Synthetic.BuiltinArrayConstructor,
            CfirDeclarationOrigin.Synthetic.BuiltinPointerConstructor,
            CfirDeclarationOrigin.Synthetic.BuiltinCStringConstructor,
                -> true

            else -> false
        }
    }

    /**
     * 取得无限实例化诊断的根触发 source。
     */
    private fun InstantiationTrigger.infiniteInstantiationSource(): CjSourceElement? =
        source.takeIf { propagateSourceAsRoot }

    /**
     * 取得循环实例化诊断应使用的 source。
     */
    private fun InstantiationTrigger.cyclicInstantiationSource(
        instantiationContext: InstantiationContext?,
        targetDeclaration: CfirDeclaration?,
    ): CjSourceElement? {
        if (
            instantiationContext?.targetDeclaration != null &&
            instantiationContext.targetDeclaration !== targetDeclaration
        ) {
            // 仅从具体构造调用建立的根 context 需要把诊断归属切换到当前扩张边。
            // 声明图遍历必须保留最外层声明入口 source，官方闭环诊断依赖该入口。
            return if (instantiationContext.preferCurrentTriggerSource) {
                source ?: instantiationContext.source
            } else {
                instantiationContext.source
            }
        }
        return infiniteInstantiationSource()
    }

    /**
     * 官方 `instTriggerInfos` 记录触发实例化的 `(node, target, instTys)`。
     * CFIR 只需要 source 和 target：source 用于成员实例化诊断，target 用于区分
     * “同一声明内部的直接环”和“由外层声明触发的间接展开”。
     */
    private fun InstantiationTrigger.memberInstantiationContext(
        instantiationContext: InstantiationContext?,
        targetDeclaration: CfirDeclaration?,
    ): InstantiationContext? {
        if (instantiationContext != null) {
            if (targetDeclaration == null || instantiationContext.targetDeclaration === targetDeclaration) {
                return instantiationContext
            }
            if (!instantiationContext.preferCurrentTriggerSource) return instantiationContext
            return InstantiationContext(
                source = source ?: instantiationContext.source,
                targetDeclaration = targetDeclaration,
                ownMemberConflictSource = instantiationContext.ownMemberConflictSource,
                preferCurrentTriggerSource = true,
            )
        }
        val source = source.takeIf { propagateSourceAsRoot } ?: return null
        return InstantiationContext(
            source = source,
            targetDeclaration = targetDeclaration,
            ownMemberConflictSource = ownMemberConflictSource ?: source,
            preferCurrentTriggerSource = checkExtendInterfaces,
        )
    }
}

/**
 * 泛型实例化触发点。
 */
private data class InstantiationTrigger(
    /**
     * 触发点解析到的符号。
     */
    val symbol: CfirBasedSymbol<*>?,

    /**
     * 被实例化的类型参数拥有者。
     */
    val declaration: CfirTypeParameterRefsOwner?,

    /**
     * 本次实例化使用的类型实参。
     */
    val typeArguments: List<ConeCangJieType>,

    /**
     * 与 [typeArguments] 按索引对应的精确源码位置。
     */
    val typeArgumentSources: List<CjSourceElement?> = emptyList(),

    /**
     * 诊断归属的源码位置。
     */
    val source: CjSourceElement?,

    /**
     * 触发点是否位于嵌套类型实参中。
     */
    val isNestedTypeArgument: Boolean,

    /**
     * 当前触发点是否是官方实例化完整性检查认可的真实泛型引用。
     */
    val checkStaticCompleteness: Boolean = false,

    /**
     * 是否允许把 source 作为根实例化触发点传播。
     */
    val propagateSourceAsRoot: Boolean = true,

    /**
     * 实例化重复父类型检查是否需要纳入 extend 接口。
     */
    val checkExtendInterfaces: Boolean = false,

    /**
     * 重复父接口诊断优先使用的 source。
     */
    val duplicateSuperInterfaceSource: CjSourceElement? = source,

    /**
     * 是否对该触发点执行实例化成员签名冲突检查。
     */
    val checkMemberSignatures: Boolean = false,

    /**
     * 直接成员参与冲突时使用的诊断 source。
     */
    val ownMemberConflictSource: CjSourceElement? = source,

    /**
     * 内建 extend 目标 key。
     */
    val builtinTargetKey: CfirExtendTargetKey? = null,

    /**
     * 内建 extend 的具体实例化类型。
     */
    val builtinInstantiatedType: ConeCangJieType? = null,
) {
    /**
     * 当前触发点对应的源码声明目标。
     */
    val targetDeclaration: CfirDeclaration?
        get() = (declaration as? CfirDeclaration)
            ?.takeIf { it.origin == CfirDeclarationOrigin.Source }
}

/**
 * 类型参数依赖图中的边。
 */
private data class TypeParameterEdge(
    /**
     * 边指向的类型参数。
     */
    val to: CfirTypeParameterSymbol,

    /**
     * 该边是否表示扩张依赖。
     */
    val expansive: Boolean,
)

/**
 * 类型遍历 worklist 条目。
 */
private data class TypeTraversalItem(
    /**
     * 待遍历类型。
     */
    val type: ConeCangJieType,

    /**
     * 当前递归深度。
     */
    val depth: Int,
)

/**
 * 活动声明实例化栈帧。
 */
private data class DeclarationInstantiationFrame(
    /**
     * 当前正在处理的声明。
     */
    val declaration: CfirDeclaration,

    /**
     * 当前声明实例化类型实参。
     */
    val typeArguments: List<ConeCangJieType>,
)

/**
 * 成员实例化诊断上下文。
 */
private data class InstantiationContext(
    /**
     * 触发实例化的源码位置。
     */
    val source: CjSourceElement,

    /**
     * 当前实例化目标声明。
     */
    val targetDeclaration: CfirDeclaration?,

    /**
     * 直接成员参与冲突时使用的诊断 source。
     */
    val ownMemberConflictSource: CjSourceElement,

    /**
     * 是否由具体构造调用建立，跨声明时诊断 source 应跟随当前扩张边。
     */
    val preferCurrentTriggerSource: Boolean,
)

/** 当前实例化 owner 的原始 use-site scope 与最终 substitution scope。 */
private data class InstantiatedMemberScopes(
    /** 用于查询 own member 在具体实例化后新形成的 direct-overridden provenance。 */
    val raw: CfirTypeScope,

    /** 提供 resolver 实际消费的最终成员集合与 substituted 参数签名。 */
    val substituted: CfirTypeScope,
)

/**
 * 用对象身份比较的一对类型 key。
 */
private class TypePairKey(
    /**
     * 左侧类型对象。
     */
    private val left: ConeCangJieType,

    /**
     * 右侧类型对象。
     */
    private val right: ConeCangJieType,
) {
    /**
     * 基于左右类型对象身份比较 key。
     */
    override fun equals(other: Any?): Boolean =
        other is TypePairKey && left === other.left && right === other.right

    /**
     * 基于左右类型对象身份计算 hash。
     */
    override fun hashCode(): Int =
        31 * System.identityHashCode(left) + System.identityHashCode(right)
}

/**
 * 实例化后的成员函数签名。
 */
private data class InstantiatedMemberSignature(
    /**
     * 原始函数声明。
     */
    val function: CfirFunction,

    /**
     * 签名名称，构造器使用 owner 名称。
     */
    val name: Name,

    /**
     * 函数是否为 static。
     */
    val isStatic: Boolean,

    /**
     * 替换后的参数类型列表。
     */
    val parameterTypes: List<ConeCangJieType>,

    /**
     * 签名中是否仍含有泛型类型。
     */
    val hasGenericTypes: Boolean,

    /**
     * 该签名是否来自某个 extend 继承的接口默认实现。
     */
    val inheritedDefaultOwnerExtend: CfirExtend?,

    /**
     * 继承接口实例的泛型来源 key。
     */
    val inheritedInterfaceKey: String?,

    /**
     * 是否是当前 class-like/extend 自身声明的直接成员。
     */
    val isOwnMember: Boolean,
)

/**
 * 声明实例化去重 key。
 */
private data class DeclarationInstantiationKey(
    /**
     * 声明符号。
     */
    val symbol: CfirBasedSymbol<*>,

    /**
     * 类型实参 key 列表。
     */
    val arguments: List<String>,
)

/**
 * 成员实例化冲突诊断去重 key。
 */
private data class MemberInstantiationReportKey(
    /**
     * 触发 source key。
     */
    val sourceKey: SourceKey,

    /**
     * 冲突函数名。
     */
    val functionName: Name,
)

/**
 * 源码范围去重 key。
 */
private data class SourceKey(
    /**
     * 起始偏移。
     */
    val startOffset: Int,

    /**
     * 结束偏移。
     */
    val endOffset: Int,
)

/**
 * 将 source 转换为范围 key。
 */
private fun CjSourceElement?.sourceKeyOrNull(): SourceKey? {
    val source = this ?: return null
    return SourceKey(source.startOffset, source.endOffset)
}
