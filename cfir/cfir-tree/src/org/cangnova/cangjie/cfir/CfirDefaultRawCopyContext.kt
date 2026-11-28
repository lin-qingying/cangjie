@file:OptIn(
    org.cangnova.cangjie.cfir.declarations.ResolveStateAccess::class,
    org.cangnova.cangjie.cfir.CfirImplementationDetail::class,
)

package org.cangnova.cangjie.cfir

import java.util.Collections
import java.util.IdentityHashMap
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.annotations.CangjieAnnotationIdentity
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirResolvedDeclarationStatusImpl
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.impl.CfirAnnotationArgumentMappingImpl
import org.cangnova.cangjie.cfir.expressions.impl.CfirEmptyAnnotationArgumentMapping
import org.cangnova.cangjie.cfir.patterns.CfirPatternTreeOwner
import org.cangnova.cangjie.cfir.patterns.CfirTypePatternMatchingKind
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.session.CfirConditionalCompilationFailure
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.impl.ResolvedImplicitTypeRef
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjSourceFileLinesMapping

/**
 * RAW 的完整 runtime metadata policy。所有 AST 复制均回到 state 中安装的 generated copier。
 * borrowedHandles 是调用者明确提供的 module/session/scope 与 active-parent 推断构造器身份集合。
 * 推断构造器须在 beforeLambda 获取并显式加入集合；不扫描外部声明图，也不分配新的类型变量。
 */
class CfirDefaultRawCopyContext(
    override val state: CfirRawCopyState,
    borrowedHandles: Collection<Any>,
) : CfirRawCopyContext {
    private val borrowedHandles: Set<Any> = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>()).apply { addAll(borrowedHandles) }
    private val copiedAttributes = IdentityHashMap<CfirDeclarationAttributes, CfirDeclarationAttributes>()
    private val types = CfirRawTypeCopier(state, ::diagnostic, ::borrowedHandle)

    override fun validateRawNode(source: CfirElement) {
        if (source is CfirElementWithResolveState) {
            check(source.resolveState is CfirResolvedToPhaseState && source.resolveState.resolvePhase == CfirResolvePhase.RAW_CFIR) {
                "RAW copier cannot import resolved/in-progress declaration state: ${source.resolveState}"
            }
        }
        if (source is CfirControlFlowGraphOwner) check(source.controlFlowGraphReference == null) { "RAW cannot import a CFG" }
        if (source is CfirProperty) propertyBodyState(source.bodyResolveState)
        if (source is CfirFunctionCall) {
            check(source.calleeReference !is org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference &&
                    source.calleeReference !is org.cangnova.cangjie.cfir.references.CfirErrorNamedReference &&
                    source.calleeReference !is org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase &&
                    source.argumentList !is CfirResolvedArgumentList
            ) { "RAW cannot import ordinary call-resolution results" }
        }
        if (source is CfirIfExpression) {
            check(source.calleeReference === org.cangnova.cangjie.cfir.references.CfirStubReference) {
                "RAW cannot import if select resolution results"
            }
        }
        if (source is CfirSubscriptExpression) {
            check(source.resolvedGetCall == null && source.resolvedSetCall == null) {
                "RAW cannot import resolved subscript calls"
            }
        }
        if (source is CfirAnonymousFunction) {
            check(source.matchingParameterFunctionType == null) { "RAW cannot import an analyzed lambda matching shape" }
        }
    }

    override fun validateCopiedNode(source: CfirElement, target: CfirElement) {
        validateRawNode(target)
        if (source is CfirDeclaration) {
            check(target is CfirDeclaration && target.attributes !== source.attributes) { "A RAW declaration shared its attributes container" }
            check(target.symbol === state.declaredSymbol(source.symbol)) { "Declaration was not bound to its first-pass symbol" }
        }
    }

    /** 29 个现存 concrete symbol 使用真实构造器；open symbol 的未知子类由 state 的 exact-class 契约拒绝。 */
    override fun registerDeclaredSymbol(owner: CfirDeclaration, symbol: CfirBasedSymbol<*>) {
        val fresh = when (symbol) {
            is CfirErrorEnumConstructorSymbol -> CfirErrorEnumConstructorSymbol(symbol.callableId, diagnostic(symbol.diagnostic))
            is CfirErrorNamedValueSymbol -> CfirErrorNamedValueSymbol(symbol.callableId, diagnostic(symbol.diagnostic))
            is CfirErrorFunctionSymbol -> CfirErrorFunctionSymbol()
            is CfirPrimitiveTypeSymbol -> CfirPrimitiveTypeSymbol(symbol.classId, symbol.kind)
            is CfirBuiltInTypeSymbol -> CfirBuiltInTypeSymbol(symbol.classId, symbol.kind)
            is CfirClassSymbol -> CfirClassSymbol(symbol.classId)
            is CfirInterfaceSymbol -> CfirInterfaceSymbol(symbol.classId)
            is CfirStructSymbol -> CfirStructSymbol(symbol.classId)
            is CfirEnumSymbol -> CfirEnumSymbol(symbol.classId)
            is CfirTypeAliasSymbol -> CfirTypeAliasSymbol(symbol.classId)
            is CfirTypeParameterSymbol -> CfirTypeParameterSymbol()
            is CfirNamedFunctionSymbol -> CfirNamedFunctionSymbol(symbol.callableId)
            is CfirAnonymousFunctionSymbol -> CfirAnonymousFunctionSymbol()
            is CfirMainFunctionSymbol -> CfirMainFunctionSymbol(symbol.callableId)
            is CfirMacroDeclarationSymbol -> CfirMacroDeclarationSymbol(symbol.callableId)
            is CfirFinalizerSymbol -> CfirFinalizerSymbol(symbol.callableId)
            is CfirConstructorSymbol -> CfirConstructorSymbol(symbol.callableId)
            is CfirPropertySymbol -> CfirPropertySymbol(symbol.callableId)
            is CfirPropertyAccessorSymbol -> CfirPropertyAccessorSymbol()
            is CfirFieldVariableSymbol -> CfirFieldVariableSymbol(symbol.callableId)
            is CfirResourceVariableSymbol -> CfirResourceVariableSymbol(symbol.callableId)
            is CfirPatternVariableSymbol -> CfirPatternVariableSymbol(symbol.callableId)
            is CfirPatternBindingSymbol -> CfirPatternBindingSymbol(symbol.callableId)
            is CfirValueParameterSymbol -> CfirValueParameterSymbol(symbol.callableId)
            is CfirFileSymbol -> CfirFileSymbol()
            is CfirCodeFragmentSymbol -> CfirCodeFragmentSymbol()
            is CfirExtendSymbol -> CfirExtendSymbol()
            is CfirEnumConstructorSymbol -> CfirEnumConstructorSymbol(symbol.callableId)
            is CfirInvalidDeclarationSymbol -> CfirInvalidDeclarationSymbol()
        }
        state.registerDeclaredSymbol(owner, symbol, fresh)
        if (symbol is CfirClassLikeSymbol<*> && fresh is CfirClassLikeSymbol<*>) {
            types.registerClassifier(symbol, fresh, owner.moduleData.session)
        }
    }

    override fun resolvePhase(source: CfirElementWithResolveState): CfirResolvePhase {
        validateRawNode(source)
        return CfirResolvePhase.RAW_CFIR
    }

    override fun <V : Any> collectOwnedMetadata(kind: CfirRawCopyMetadataKind, value: V, visit: (CfirElement) -> Unit) {
        when (kind) {
            CfirRawCopyMetadataKind.SOURCE -> source(value as CjSourceElement)
            CfirRawCopyMetadataKind.MODULE_DATA -> moduleData(value as CfirModuleData)
            CfirRawCopyMetadataKind.SOURCE_FILE -> sourceFile(value as CjSourceFile)
            CfirRawCopyMetadataKind.LINE_MAPPING -> lineMapping(value as CjSourceFileLinesMapping)
            CfirRawCopyMetadataKind.NAME -> name(value as Name)
            CfirRawCopyMetadataKind.FQ_NAME -> fqName(value as FqName)
            CfirRawCopyMetadataKind.CLASS_ID -> classId(value as ClassId)
            CfirRawCopyMetadataKind.VISIBILITY -> visibility(value as Visibility)
            CfirRawCopyMetadataKind.MODALITY -> modality(value as Modality)
            CfirRawCopyMetadataKind.ORIGIN -> origin(value as CfirDeclarationOrigin)
            CfirRawCopyMetadataKind.STATUS -> validateStatus(value as CfirDeclarationStatus)
            CfirRawCopyMetadataKind.ATTRIBUTES -> CfirRawDeclarationDataRegistry.collectOwned(value as CfirDeclarationAttributes, this, visit)
            CfirRawCopyMetadataKind.DEPRECATIONS -> deprecationsProvider(value as DeprecationsProvider)
            CfirRawCopyMetadataKind.SCOPE_PROVIDER -> scopeProvider(value as CfirScopeProvider)
            CfirRawCopyMetadataKind.CONE_TYPE -> types.collect(value as ConeCangJieType)
            CfirRawCopyMetadataKind.DIAGNOSTIC -> diagnostic(value as ConeDiagnostic)
            CfirRawCopyMetadataKind.TARGET -> require(value is CfirFunctionTarget || value is CfirLoopTarget) { "Unsupported RAW target" }
            CfirRawCopyMetadataKind.PATTERN_OWNER -> require(value is CfirElement && value is CfirPatternTreeOwner)
            CfirRawCopyMetadataKind.ANNOTATION_MAPPING -> (value as CfirAnnotationArgumentMapping).mapping.values.forEach { collectExpression(it, visit) }
            CfirRawCopyMetadataKind.ANNOTATION_VIEW -> collectView(value as CfirAnnotationArgumentView, visit)
            CfirRawCopyMetadataKind.ANNOTATION_ARGUMENT_MAP -> {
                val mapping = value as Map<*, *>
                mapping.forEach { (name, expression) ->
                    require(name is Name && expression is CfirExpression)
                    collectExpression(expression, visit)
                }
            }
            CfirRawCopyMetadataKind.CONDITIONAL_COMPILATION_FAILURE -> conditionalCompilationFailure(value as CfirConditionalCompilationFailure)
            CfirRawCopyMetadataKind.LITERAL_VALUE -> literalValue(value)
            CfirRawCopyMetadataKind.MATCH_EXHAUSTIVENESS -> matchExhaustiveness(value as CfirMatchExhaustivenessStatus)
            CfirRawCopyMetadataKind.PROPERTY_BODY_STATE -> propertyBodyState(value as CfirPropertyBodyResolveState)
            CfirRawCopyMetadataKind.TYPE_PATTERN_KIND -> typePatternKind(value as CfirTypePatternMatchingKind)
            CfirRawCopyMetadataKind.ASSIGNMENT_MISMATCH -> error("RAW cannot import a resolved assignment mismatch")
            CfirRawCopyMetadataKind.ANNOTATION_IDENTITY -> annotationIdentity(value as CangjieAnnotationIdentity)
        }
    }

    private fun collectExpression(expression: CfirExpression, visit: (CfirElement) -> Unit) {
        if (!state.isBorrowedElement(expression)) visit(expression)
    }

    private fun copyExpression(expression: CfirExpression): CfirExpression =
        if (state.isBorrowedElement(expression)) state.referencedElement(expression) else state.copyOwned(expression)

    private fun collectView(view: CfirAnnotationArgumentView, visit: (CfirElement) -> Unit) {
        view.entries.forEach { entry ->
            entry.argument?.let { collectExpression(it, visit) }
            val constant = entry.constantExpression
            if (constant != null) {
                val parameter = entry.resolvedParameter
                if (entry.isDefaultOrigin && parameter != null && state.isBorrowedSymbol(parameter.symbol)) {
                    check(parameter.defaultValue === constant) { "Default view did not refer to its borrowed SDK parameter default" }
                    state.registerBorrowedElementReference(constant)
                } else collectExpression(constant, visit)
            }
        }
    }

    override fun source(value: CjSourceElement): CjSourceElement = value
    override fun moduleData(value: CfirModuleData): CfirModuleData = borrowedHandle(value)
    override fun sourceFile(value: CjSourceFile): CjSourceFile = value
    override fun lineMapping(value: CjSourceFileLinesMapping): CjSourceFileLinesMapping = value
    override fun name(value: Name): Name = value
    override fun fqName(value: FqName): FqName = value
    override fun classId(value: ClassId): ClassId = value
    override fun lookupTag(value: ConeClassLikeLookupTag): ConeClassLikeLookupTag = types.lookupTag(value)
    override fun <V : Any> borrowedHandle(value: V): V {
        check(value in borrowedHandles) { "RAW metadata environment handle was not explicitly borrowed" }
        return value
    }
    override fun visibility(value: Visibility): Visibility = value
    override fun modality(value: Modality): Modality = value
    override fun origin(value: CfirDeclarationOrigin): CfirDeclarationOrigin = value
    override fun scopeProvider(value: CfirScopeProvider): CfirScopeProvider = borrowedHandle(value)
    override fun <T : ConeCangJieType> type(value: T): T = types.copy(value)

    private fun validateStatus(value: CfirDeclarationStatus) {
        check(value is CfirDeclarationStatusImpl || value is CfirResolvedDeclarationStatusImpl) { "Unknown RAW declaration status implementation" }
        if (value is CfirDeclarationStatusImpl) {
            val knownBits = CfirDeclarationStatusImpl.Modifier.entries.fold(0) { result, modifier -> result or modifier.mask }
            check(value.rawFlags and knownBits.inv() == 0) { "Unclassified declaration status flag" }
        }
    }

    override fun status(value: CfirDeclarationStatus): CfirDeclarationStatus {
        validateStatus(value)
        return if (value is CfirResolvedDeclarationStatus) {
            CfirResolvedDeclarationStatusImpl(
                value.source, value.visibility, value.visibilityExplicitness, value.modalityExplicitness,
                value.bodyPresence, value.isDeclarationFile, value.isVisibilityExplicit, value.isModalityExplicit,
                value.isAbstractExplicit, value.isOverride, value.isOperator, value.isStatic, value.isConst,
                value.isMut, value.isUnsafe, value.isForeign, value.isC, value.isCommon, value.isFromCommonPart,
                value.isSpecific, value.isCommonWithDefault, value.isRedef, value.isDefault, value.isAbstract,
                value.isOpen, value.isSealed, value.modality,
            )
        } else CfirDeclarationStatusImpl(value.visibility, value.modality).apply {
            visibilityExplicitness = value.visibilityExplicitness
            modalityExplicitness = value.modalityExplicitness
            bodyPresence = value.bodyPresence
            isDeclarationFile = value.isDeclarationFile
            isAbstractExplicit = value.isAbstractExplicit
            isOverride = value.isOverride
            isOperator = value.isOperator
            isStatic = value.isStatic
            isConst = value.isConst
            isMut = value.isMut
            isUnsafe = value.isUnsafe
            isForeign = value.isForeign
            isC = value.isC
            isCommon = value.isCommon
            isFromCommonPart = value.isFromCommonPart
            isSpecific = value.isSpecific
            isCommonWithDefault = value.isCommonWithDefault
            isRedef = value.isRedef
            isDefault = value.isDefault
            isAbstract = value.isAbstract
            isOpen = value.isOpen
            isSealed = value.isSealed
        }
    }

    override fun attributes(value: CfirDeclarationAttributes): CfirDeclarationAttributes {
        copiedAttributes[value]?.let { return it }
        val target = CfirDeclarationAttributes()
        copiedAttributes[value] = target
        CfirRawDeclarationDataRegistry.copy(value, target, this)
        return target
    }

    override fun deprecationsProvider(value: DeprecationsProvider): DeprecationsProvider = when (value) {
        EmptyDeprecationsProvider, UnresolvedDeprecationProvider -> value
        else -> error("RAW cannot import a resolved/cached deprecation provider")
    }

    override fun diagnostic(value: ConeDiagnostic): ConeDiagnostic = when (value) {
        is ConeSimpleDiagnostic -> ConeSimpleDiagnostic(value.reason, value.kind)
        is ConeUnreportedDuplicateDiagnostic -> ConeUnreportedDuplicateDiagnostic(diagnostic(value.original))
        else -> error("Diagnostic has no RAW metadata policy")
    }

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirTargetElement> target(value: CfirTarget<E>): CfirTarget<E> = when (value) {
        is CfirFunctionTarget -> state.target(value) { CfirFunctionTarget(value.labelName, value.isLambda) }
        is CfirLoopTarget -> state.target(value) { CfirLoopTarget(value.labelName) }
        else -> error("Unknown RAW target implementation")
    } as CfirTarget<E>

    override fun patternOwner(value: CfirPatternTreeOwner): CfirPatternTreeOwner =
        state.referencedElement(value as CfirElement) as CfirPatternTreeOwner

    override fun annotationMapping(value: CfirAnnotationArgumentMapping): CfirAnnotationArgumentMapping = when (value) {
        CfirEmptyAnnotationArgumentMapping -> {
            state.allowSharedImmutable(value)
            value
        }
        is CfirAnnotationArgumentMappingImpl -> CfirAnnotationArgumentMappingImpl(value.source, annotationArgumentMap(value.mapping))
        else -> error("Annotation mapping has no RAW metadata policy")
    }

    override fun annotationArgumentMap(value: Map<Name, CfirExpression>): Map<Name, CfirExpression> =
        value.entries.associateTo(linkedMapOf()) { (name, expression) -> name to copyExpression(expression) }

    override fun annotationView(value: CfirAnnotationArgumentView): CfirAnnotationArgumentView {
        val entries = mutableListOf<CfirAnnotationArgumentViewEntry>()
        val prepared = value.entries.map { entry ->
            Triple(entry, entry.argument?.let(::copyExpression), entry.constantExpression?.let(::copyExpression))
        }
        state.deferReference {
            prepared.forEach { (entry, argument, constant) ->
                entries += CfirAnnotationArgumentViewEntry(
                    entry.sourceOrder, entry.explicitName, argument,
                    entry.resolvedParameter?.let { state.referencedElement(it) }, entry.status,
                    entry.isDefaultOrigin, constant, entry.source,
                )
            }
        }
        return CfirAnnotationArgumentView(entries)
    }

    override fun conditionalCompilationFailure(value: CfirConditionalCompilationFailure): CfirConditionalCompilationFailure =
        CfirConditionalCompilationFailure(value.reason, value.source, value.conditionName, value.rightValue, value.operator)

    override fun literalValue(value: Any): Any = when (value) {
        is String, is Boolean, is Byte, is Short, is Int, is Long, is Float, is Double, is Char,
        is UByte, is UShort, is UInt, is ULong, Unit -> value
        else -> error("Literal value has no RAW immutable-value policy")
    }

    override fun matchExhaustiveness(value: CfirMatchExhaustivenessStatus): CfirMatchExhaustivenessStatus {
        check(value === CfirMatchExhaustivenessStatus.Unknown) { "RAW cannot import completed match exhaustiveness" }
        return value
    }

    override fun propertyBodyState(value: CfirPropertyBodyResolveState): CfirPropertyBodyResolveState {
        check(value == CfirPropertyBodyResolveState.NOTHING_RESOLVED) { "RAW cannot import property body progress" }
        return value
    }

    override fun typePatternKind(value: CfirTypePatternMatchingKind): CfirTypePatternMatchingKind {
        check(value == CfirTypePatternMatchingKind.UNKNOWN) { "RAW cannot import resolved type-pattern matching" }
        return value
    }

    override fun assignmentMismatch(value: CfirAssignmentTypeMismatchOutcome): CfirAssignmentTypeMismatchOutcome =
        error("RAW cannot import assignment RHS resolution results")

    override fun annotationIdentity(value: CangjieAnnotationIdentity): CangjieAnnotationIdentity = when (value) {
        is CangjieAnnotationIdentity.LanguageBuiltIn -> CangjieAnnotationIdentity.LanguageBuiltIn(value.kind, value.sourceName)
        is CangjieAnnotationIdentity.PackageDirective -> CangjieAnnotationIdentity.PackageDirective(value.kind, value.sourceName)
        is CangjieAnnotationIdentity.Custom -> CangjieAnnotationIdentity.Custom(value.classFqName)
        is CangjieAnnotationIdentity.SystemMacro -> CangjieAnnotationIdentity.SystemMacro(value.classFqName, value.sourceName)
        is CangjieAnnotationIdentity.SpecialExpression -> CangjieAnnotationIdentity.SpecialExpression(value.sourceName)
        is CangjieAnnotationIdentity.PlatformDerived -> CangjieAnnotationIdentity.PlatformDerived(value.kind, value.classFqName, value.sourceName)
        CangjieAnnotationIdentity.Unknown -> value
    }

    override fun resolvedImplicitTypeRef(value: ResolvedImplicitTypeRef): ResolvedImplicitTypeRef =
        ResolvedImplicitTypeRef(state.copyOwned(value.typeRef))

    override fun emptyArgumentList(value: CfirEmptyArgumentList): CfirEmptyArgumentList {
        state.allowSharedImmutable(value)
        return value
    }

    override fun resolvedArgumentList(value: CfirResolvedArgumentList): CfirResolvedArgumentList {
        check(value is CfirResolvedArgumentListImpl) { "RAW cannot import an error-call/candidate argument-mapping result" }
        val mapping = linkedMapOf<CfirExpression, CfirValueParameter>()
        val allMapping = if (value.mapping === value.mappingIncludingContextArguments) mapping else linkedMapOf()
        val target = CfirResolvedArgumentListImpl(value.originalArgumentList?.let { state.copyOwned(it) }, mapping, allMapping)
        val prepared = value.mappingIncludingContextArguments.map { (argument, parameter) ->
            Triple(copyExpression(argument), parameter, value.mapping.containsKey(argument))
        }
        state.deferReference {
            prepared.forEach { (argument, parameter, isValue) ->
                val freshParameter = state.referencedElement(parameter)
                allMapping[argument] = freshParameter
                if (isValue) mapping[argument] = freshParameter
            }
        }
        return target
    }
}
