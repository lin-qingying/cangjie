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
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.providers.createExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
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
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.createTypeSubstitutorByTypeConstructor
import org.cangnova.cangjie.cfir.types.expandedExtendTargetKey
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.source.text
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import java.util.ArrayDeque
import java.util.IdentityHashMap

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
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFile) {
        GenericInstantiationAnalyzer(context, reporter).checkFile(declaration)
    }
}

private class GenericInstantiationAnalyzer(
    private val checkerContext: CheckerContext,
    private val reporter: DiagnosticReporter,
) {
    private val triggersByDeclaration = IdentityHashMap<CfirDeclaration, List<InstantiationTrigger>>()
    private val checkedInstantiations = linkedSetOf<DeclarationInstantiationKey>()
    private val reportedSources = linkedSetOf<SourceKey>()
    private val reportedMemberInstantiationSources = linkedSetOf<MemberInstantiationReportKey>()
    private val activeInstantiations = mutableListOf<DeclarationInstantiationFrame>()
    private var infiniteInstantiationOccurred: Boolean = false

    fun checkFile(file: CfirFile) {
        for (declaration in file.declarations) {
            if (infiniteInstantiationOccurred) return
            processDeclaration(declaration, emptyMap(), instantiationContext = null)
        }
    }

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

        if (targetDeclaration == null) return
        if (hasExpandingActiveInstantiation(targetDeclaration, expandedArguments)) {
            reportInfiniteInstantiation(memberInstantiationContext?.source)
            return
        }
        val nestedSubstitutions = typeParameters
            .zip(expandedArguments)
            .associate { (typeParameter, argument) -> typeParameter.symbol to argument }
        if (targetDeclaration is CfirClassLikeDeclaration) {
            checkInstantiatedDuplicateSupertypes(
                declaration = targetDeclaration,
                substitutions = nestedSubstitutions,
                triggerSource = memberInstantiationContext?.source,
                checkExtendInterfaces = trigger.checkExtendInterfaces,
                duplicateSuperInterfaceSource = trigger.duplicateSuperInterfaceSource,
            )
            checkInstantiatedMemberSignatures(targetDeclaration, nestedSubstitutions, memberInstantiationContext?.source)
        }
        processDeclaration(targetDeclaration, nestedSubstitutions, memberInstantiationContext)
    }

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
            collectTypeTriggers(resolvedTypeRef.coneType, source, isNestedTypeArgument = false)
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
            if (expression.resolvedQualifierClassifier(checkerContext.session) != null) {
                expression.coneTypeOrNull?.let { qualifierType ->
                    collectTypeTriggers(qualifierType, expression.source, isNestedTypeArgument = false)
                }
            }

            val callableSymbol = expression.resolvedCallableSymbolOrNull() ?: return
            val isConstructorLikeInstantiationCall = callableSymbol.isConstructorLikeInstantiationCall()
            val source = if (isConstructorLikeInstantiationCall && expression.typeArguments.isEmpty()) {
                expression.source ?: expression.calleeReference.source
            } else {
                expression.calleeReference.source ?: expression.source
            }
            if (isConstructorLikeInstantiationCall) {
                expression.coneTypeOrNull?.let { constructedType ->
                    collectTypeTriggers(
                        constructedType,
                        source,
                        isNestedTypeArgument = false,
                        checkExtendInterfaces = true,
                        duplicateSuperInterfaceSource = expression.calleeReference.source ?: source,
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
                source = source,
                isNestedTypeArgument = false,
            )
            typeArguments.forEach { collectTypeTriggers(it, source, isNestedTypeArgument = true) }
        }

        private fun collectTypeTriggers(
            type: ConeCangJieType,
            source: CjSourceElement?,
            isNestedTypeArgument: Boolean,
            propagateSourceAsRoot: Boolean = true,
            checkExtendInterfaces: Boolean = false,
            duplicateSuperInterfaceSource: CjSourceElement? = source,
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
                            )
                        }
                    }
                    type.typeArguments.forEach {
                        collectTypeTriggers(
                            it.type,
                            source,
                            isNestedTypeArgument = true,
                            propagateSourceAsRoot = propagateSourceAsRoot,
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
                        )
                    }
                    collectTypeTriggers(
                        type.returnType,
                        source,
                        isNestedTypeArgument = true,
                        propagateSourceAsRoot = propagateSourceAsRoot,
                    )
                }

                is ConeTupleType -> type.elementTypes.forEach {
                    collectTypeTriggers(
                        it,
                        source,
                        isNestedTypeArgument = true,
                        propagateSourceAsRoot = propagateSourceAsRoot,
                    )
                }

                is ConeVArrayType -> collectTypeTriggers(
                    type.elementType,
                    source,
                    isNestedTypeArgument = true,
                    propagateSourceAsRoot = propagateSourceAsRoot,
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
                    )
                }
                is ConeIntersectionType -> type.intersectedTypes.forEach {
                    collectTypeTriggers(
                        it,
                        source,
                        isNestedTypeArgument = true,
                        propagateSourceAsRoot = propagateSourceAsRoot,
                    )
                }

                is ConeUnionType -> type.unionTypes.forEach {
                    collectTypeTriggers(
                        it,
                        source,
                        isNestedTypeArgument = true,
                        propagateSourceAsRoot = propagateSourceAsRoot,
                    )
                }

                else -> type.typeArguments.forEach {
                    collectTypeTriggers(
                        it.type,
                        source,
                        isNestedTypeArgument = true,
                        propagateSourceAsRoot = propagateSourceAsRoot,
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
                source = source,
                isNestedTypeArgument = false,
            )
            typeArguments.forEach { collectTypeTriggers(it, source, isNestedTypeArgument = true) }
        }
    }

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

    private fun checkInstantiatedMemberSignatures(
        declaration: CfirClassLikeDeclaration,
        substitutions: Map<CfirTypeParameterSymbol, ConeCangJieType>,
        triggerSource: CjSourceElement?,
    ) {
        if (substitutions.isEmpty()) return
        if (substitutions.values.any { it.containsTypeParameterSymbol() }) return
        val membersByName = declaration.collectInstantiatedMemberSignatures(substitutions)
        reportInstantiatedMemberSignatureConflicts(
            membersByName = membersByName,
            triggerSource = triggerSource,
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
            instantiationName = { Name.identifier(instantiatedType.renderForInstantiationDiagnostic()) },
        )
    }

    private fun reportInstantiatedMemberSignatureConflicts(
        membersByName: Map<Name, List<InstantiatedMemberSignature>>,
        triggerSource: CjSourceElement?,
        instantiationName: () -> Name,
    ) {
        val sourceKey = triggerSource.sourceKeyOrNull() ?: return

        for ((name, members) in membersByName) {
            val stableMembers = mutableListOf<InstantiatedMemberSignature>()
            val genericMembers = members.filter { it.hasGenericTypes }
            stableMembers += members.filterNot { it.hasGenericTypes }

            for (genericMember in genericMembers) {
                for (stableMember in stableMembers) {
                    if (!stableMember.conflictsWith(genericMember)) continue
                    if (stableMember.isInheritedDefaultInterfaceConflictWith(genericMember)) continue
                    reportGenericInstantiationCausesAmbiguousFunctions(
                        source = triggerSource,
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

    private fun CfirExtend.collectInheritedDefaultFunctionSignatures(
        ownerExtend: CfirExtend,
        substitutor: ConeSubstitutor,
    ): List<InstantiatedMemberSignature> {
        val signatures = mutableListOf<InstantiatedMemberSignature>()
        val visitedInterfaces = linkedSetOf<org.cangnova.cangjie.name.ClassId>()
        for (superTypeRef in superTypeRefs) {
            val supertype = superTypeRef.coneTypeOrNull ?: continue
            collectDefaultInterfaceFunctionSignatures(
                ownerExtend = ownerExtend,
                interfaceType = substitutor.substituteOrSelf(supertype),
                destination = signatures,
                visitedInterfaces = visitedInterfaces,
            )
        }
        return signatures
    }

    private fun collectDefaultInterfaceFunctionSignatures(
        ownerExtend: CfirExtend?,
        interfaceType: ConeCangJieType,
        destination: MutableList<InstantiatedMemberSignature>,
        visitedInterfaces: MutableSet<org.cangnova.cangjie.name.ClassId>,
    ) {
        val classifierType = interfaceType as? ConeClassifierType ?: return
        val symbol = classifierType.toSymbol(checkerContext.session) as? CfirClassLikeSymbol<*> ?: return
        if (!visitedInterfaces.add(symbol.classId)) return
        val declaration = symbol.cfir as? CfirClassLikeDeclaration ?: return
        val substitutor = declaration.createDeclarationTypeSubstitutor(interfaceType)

        for (member in declaration.declarations) {
            val function = member as? CfirFunction ?: continue
            if (function.body == null || function.status.isAbstract) continue
            destination += function.toInstantiatedMemberSignature(
                ownerName = declaration.name,
                substitutor = substitutor,
                inheritedDefaultOwnerExtend = ownerExtend,
            ) ?: continue
        }

        for (superTypeRef in declaration.superTypeRefs) {
            val supertype = superTypeRef.coneTypeOrNull ?: continue
            collectDefaultInterfaceFunctionSignatures(
                ownerExtend = ownerExtend,
                interfaceType = substitutor.substituteOrSelf(supertype),
                destination = destination,
                visitedInterfaces = visitedInterfaces,
            )
        }
    }

    private fun CfirClassLikeDeclaration.collectInstantiatedMemberSignatures(
        substitutions: Map<CfirTypeParameterSymbol, ConeCangJieType>,
    ): Map<Name, List<InstantiatedMemberSignature>> {
        val functions = linkedMapOf<CfirFunctionSymbol<*>, CfirFunction>()
        for (member in declarations) {
            val function = member as? CfirFunction ?: continue
            functions[function.symbol] = function
        }

        val substitutor = substitutions.toConeSubstitutor()
        val ownSignatures = functions.values
            .asSequence()
            .mapNotNull { it.toInstantiatedMemberSignature(name, substitutor) }
            .toList()
        val signatures = ownSignatures.toMutableList()
        signatures += collectInheritedDefaultFunctionSignatures(
            superTypeRefs = superTypeRefs,
            ownerExtend = null,
            substitutor = substitutor,
        )
        signatures += collectInheritedInterfaceFunctionSignatures(
            superTypeRefs = superTypeRefs,
            substitutor = substitutor,
            implementedBy = if (this is CfirInterface) emptyList() else ownSignatures,
        )
        instantiatedSelfType(substitutor)?.let { instantiatedType ->
            signatures += collectClassLikeExtendMemberSignatures(instantiatedType)
        }

        return signatures.groupBy { it.name }
    }

    private fun collectClassLikeExtendMemberSignatures(
        instantiatedType: ConeCangJieType,
    ): List<InstantiatedMemberSignature> {
        val targetKey = instantiatedType.expandedExtendTargetKey ?: return emptyList()
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
        return signatures
    }

    private fun collectInheritedInterfaceFunctionSignatures(
        superTypeRefs: List<CfirTypeRef>,
        substitutor: ConeSubstitutor,
        implementedBy: List<InstantiatedMemberSignature> = emptyList(),
    ): List<InstantiatedMemberSignature> {
        val signatures = mutableListOf<InstantiatedMemberSignature>()
        val visitedInterfaces = linkedSetOf<String>()
        for (superTypeRef in superTypeRefs) {
            val supertype = superTypeRef.coneTypeOrNull ?: continue
            collectInterfaceFunctionSignatures(
                interfaceType = substitutor.substituteOrSelf(supertype),
                genericInterfaceType = supertype,
                destination = signatures,
                visitedInterfaces = visitedInterfaces,
                implementedBy = implementedBy,
            )
        }
        return signatures
    }

    private fun collectInterfaceFunctionSignatures(
        interfaceType: ConeCangJieType,
        genericInterfaceType: ConeCangJieType,
        destination: MutableList<InstantiatedMemberSignature>,
        visitedInterfaces: MutableSet<String>,
        implementedBy: List<InstantiatedMemberSignature> = emptyList(),
    ) {
        val classifierType = interfaceType as? ConeClassifierType ?: return
        val symbol = classifierType.toSymbol(checkerContext.session) as? CfirClassLikeSymbol<*> ?: return
        val declaration = symbol.cfir as? CfirClassLikeDeclaration ?: return
        val interfaceKey = context(checkerContext) {
            classifierType.instantiatedInterfaceKey()
        } ?: symbol.classId.asString()
        if (!visitedInterfaces.add(interfaceKey)) return

        val substitutor = declaration.createDeclarationTypeSubstitutor(classifierType)
        val genericSubstitutor = declaration.createDeclarationTypeSubstitutor(genericInterfaceType)
        for (member in declaration.declarations) {
            val function = member as? CfirFunction ?: continue
            val signature = function.toInstantiatedMemberSignature(
                ownerName = declaration.name,
                substitutor = substitutor,
                genericSubstitutor = genericSubstitutor,
            ) ?: continue
            if (implementedBy.any { implementation -> implementation.conflictsWith(signature) }) continue
            destination += signature
        }

        for (superTypeRef in declaration.superTypeRefs) {
            val supertype = superTypeRef.coneTypeOrNull ?: continue
            collectInterfaceFunctionSignatures(
                interfaceType = substitutor.substituteOrSelf(supertype),
                genericInterfaceType = genericSubstitutor.substituteOrSelf(supertype),
                destination = destination,
                visitedInterfaces = visitedInterfaces,
                implementedBy = implementedBy,
            )
        }
    }

    private fun collectInheritedDefaultFunctionSignatures(
        superTypeRefs: List<CfirTypeRef>,
        ownerExtend: CfirExtend?,
        substitutor: ConeSubstitutor,
    ): List<InstantiatedMemberSignature> {
        val signatures = mutableListOf<InstantiatedMemberSignature>()
        val visitedInterfaces = linkedSetOf<org.cangnova.cangjie.name.ClassId>()
        for (superTypeRef in superTypeRefs) {
            val supertype = superTypeRef.coneTypeOrNull ?: continue
            collectDefaultInterfaceFunctionSignatures(
                ownerExtend = ownerExtend,
                interfaceType = substitutor.substituteOrSelf(supertype),
                destination = signatures,
                visitedInterfaces = visitedInterfaces,
            )
        }
        return signatures
    }

    private fun CfirClassLikeDeclaration.instantiatedSelfType(
        substitutor: ConeSubstitutor,
    ): ConeCangJieType? {
        val selfType = declarationSelfTypeForInstantiation() ?: return null
        return substitutor.substituteOrSelf(selfType)
    }

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

    private fun CfirFunction.toInstantiatedMemberSignature(
        ownerName: Name,
        substitutor: ConeSubstitutor,
        inheritedDefaultOwnerExtend: CfirExtend? = null,
        genericSubstitutor: ConeSubstitutor = ConeSubstitutor.Empty,
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
        )
    }

    private fun Map<CfirTypeParameterSymbol, ConeCangJieType>.toConeSubstitutor(): ConeSubstitutor {
        if (isEmpty()) return ConeSubstitutor.Empty
        return createTypeSubstitutorByTypeConstructor(
            map = mapKeys { (symbol, _) -> symbol.toLookupTag() as TypeConstructorMarker },
            context = checkerContext.session.typeContext,
            approximateIntegerLiterals = false,
        )
    }

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

    private fun CfirFunction.instantiationMemberName(ownerName: Name): Name =
        if (this is CfirConstructor) ownerName else symbol.name

    private fun InstantiatedMemberSignature.conflictsWith(other: InstantiatedMemberSignature): Boolean {
        if (function === other.function) return false
        if (isStatic != other.isStatic) return false
        if (parameterTypes.size != other.parameterTypes.size) return false
        return parameterTypes.zip(other.parameterTypes).all { (left, right) ->
            AbstractTypeChecker.equalTypes(checkerContext.session.typeContext, left, right)
        }
    }

    private fun InstantiatedMemberSignature.isInheritedDefaultInterfaceConflictWith(
        other: InstantiatedMemberSignature,
    ): Boolean =
        inheritedDefaultOwnerExtend != null && other.inheritedDefaultOwnerExtend != null

    private fun CfirClassLikeDeclaration.renderInstantiationName(
        substitutions: Map<CfirTypeParameterSymbol, ConeCangJieType>,
    ): Name {
        val arguments = typeParameters.joinToString(", ") { typeParameter ->
            substitutions[typeParameter.symbol]?.renderForInstantiationDiagnostic()
                ?: typeParameter.symbol.name.asString()
        }
        return Name.identifier("${name.asString()}<$arguments>")
    }

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

    private fun List<ConeCangJieType>.structurallyEqual(other: List<ConeCangJieType>): Boolean {
        if (size != other.size) return false
        val visited = mutableSetOf<TypePairKey>()
        return zip(other).all { (left, right) ->
            left.structurallyEqual(right, visited, depth = 0)
        }
    }

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

    private fun ConeCangJieType.isExactlyTypeParameter(symbol: CfirTypeParameterSymbol): Boolean =
        (this as? ConeTypeParameterType)?.lookupTag?.typeParameterSymbol == symbol

    private fun reportInfiniteInstantiation(source: CjSourceElement?) {
        val diagnosticSource = source?.firstCharacterDiagnosticSource() ?: return
        val sourceKey = diagnosticSource.sourceKeyOrNull() ?: return
        if (!reportedSources.add(sourceKey)) return
        infiniteInstantiationOccurred = true
        context(checkerContext) {
            reporter.reportOn(diagnosticSource, CfirErrors.GENERIC_INFINITE_INSTANTIATION)
        }
    }

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

    private fun reportGenericInstantiationCausesAmbiguousFunctions(
        source: CjSourceElement?,
        sourceKey: SourceKey,
        instantiationName: Name,
        functionName: Name,
    ) {
        val diagnosticSource = source ?: return
        val reportKey = MemberInstantiationReportKey(sourceKey, functionName)
        if (!reportedMemberInstantiationSources.add(reportKey)) return
        context(checkerContext) {
            reporter.reportOn(
                diagnosticSource,
                CfirErrors.GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS,
                instantiationName,
                functionName,
            )
        }
    }

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

    private fun ConeCangJieType.instantiationKey(): String =
        when (this) {
            is ConeTypeParameterType -> "P@${System.identityHashCode(lookupTag.typeParameterSymbol)}"
            else -> "${javaClass.name}@${System.identityHashCode(this)}"
        }

    private fun CfirQualifiedAccessExpression.resolvedCallableSymbolOrNull(): CfirCallableSymbol<*>? =
        when (val reference = calleeReference) {
            is CfirResolvedErrorReference -> reference.resolvedSymbol as? CfirCallableSymbol<*>
            is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirCallableSymbol<*>
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirCallableSymbol<*>
            else -> null
        }

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

    private fun InstantiationTrigger.infiniteInstantiationSource(): CjSourceElement? =
        source.takeIf { propagateSourceAsRoot }

    private fun InstantiationTrigger.cyclicInstantiationSource(
        instantiationContext: InstantiationContext?,
        targetDeclaration: CfirDeclaration?,
    ): CjSourceElement? {
        if (
            instantiationContext?.targetDeclaration != null &&
            instantiationContext.targetDeclaration !== targetDeclaration
        ) {
            return instantiationContext.source
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
        if (instantiationContext != null) return instantiationContext
        val source = source.takeIf { propagateSourceAsRoot } ?: return null
        return InstantiationContext(source, targetDeclaration)
    }
}

private data class InstantiationTrigger(
    val symbol: CfirBasedSymbol<*>?,
    val declaration: CfirTypeParameterRefsOwner?,
    val typeArguments: List<ConeCangJieType>,
    val source: CjSourceElement?,
    val isNestedTypeArgument: Boolean,
    val propagateSourceAsRoot: Boolean = true,
    val checkExtendInterfaces: Boolean = false,
    val duplicateSuperInterfaceSource: CjSourceElement? = source,
    val builtinTargetKey: CfirExtendTargetKey? = null,
    val builtinInstantiatedType: ConeCangJieType? = null,
) {
    val targetDeclaration: CfirDeclaration?
        get() = (declaration as? CfirDeclaration)
            ?.takeIf { it.origin == CfirDeclarationOrigin.Source }
}

private data class TypeParameterEdge(
    val to: CfirTypeParameterSymbol,
    val expansive: Boolean,
)

private data class TypeTraversalItem(
    val type: ConeCangJieType,
    val depth: Int,
)

private data class DeclarationInstantiationFrame(
    val declaration: CfirDeclaration,
    val typeArguments: List<ConeCangJieType>,
)

private data class InstantiationContext(
    val source: CjSourceElement,
    val targetDeclaration: CfirDeclaration?,
)

private class TypePairKey(
    private val left: ConeCangJieType,
    private val right: ConeCangJieType,
) {
    override fun equals(other: Any?): Boolean =
        other is TypePairKey && left === other.left && right === other.right

    override fun hashCode(): Int =
        31 * System.identityHashCode(left) + System.identityHashCode(right)
}

private data class InstantiatedMemberSignature(
    val function: CfirFunction,
    val name: Name,
    val isStatic: Boolean,
    val parameterTypes: List<ConeCangJieType>,
    val hasGenericTypes: Boolean,
    val inheritedDefaultOwnerExtend: CfirExtend?,
)

private data class DeclarationInstantiationKey(
    val symbol: CfirBasedSymbol<*>,
    val arguments: List<String>,
)

private data class MemberInstantiationReportKey(
    val sourceKey: SourceKey,
    val functionName: Name,
)

private data class SourceKey(
    val startOffset: Int,
    val endOffset: Int,
)

private fun CjSourceElement?.sourceKeyOrNull(): SourceKey? {
    val source = this ?: return null
    return SourceKey(source.startOffset, source.endOffset)
}
