package org.cangnova.cangjie.analysis.api.cfir.references

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.scopes.CaCfirScopeWithKind
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.cfir.components.createUseSiteMemberCallableVisibilityChecker
import org.cangnova.cangjie.analysis.api.cfir.components.declarationSiteMemberScope
import org.cangnova.cangjie.analysis.api.cfir.scopes.CaCfirBasedScope
import org.cangnova.cangjie.analysis.api.components.CaScopeKind
import org.cangnova.cangjie.analysis.api.components.correspondingProperty
import org.cangnova.cangjie.analysis.api.components.deprecationStatus
import org.cangnova.cangjie.analysis.api.components.memberScope
import org.cangnova.cangjie.analysis.api.components.containingDeclaration
import org.cangnova.cangjie.analysis.api.components.packageScope
import org.cangnova.cangjie.analysis.api.components.scope
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaAdditionalCDocResolutionProvider
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.idea.references.CDocReference
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.cfir.scopes.CfirPotentialDirectExtendCallableScope
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.processCallablesByNameWithLookupProvenance
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.calls.producesInapplicableCandidate
import org.cangnova.cangjie.cfir.resolve.calls.overloads.ConeSimpleConstraintSystemImpl
import org.cangnova.cangjie.cfir.resolve.inference.inferenceComponents
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.lexer.cdoc.psi.CDocElement
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocLink
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocName
import org.cangnova.cangjie.lexer.cdoc.parser.CDocKnownTag
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjConstructor
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjPrimaryConstructor
import org.cangnova.cangjie.psi.CjTypeParameterListOwner
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.psiUtil.isPropertyParameter
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.resolve.deprecation.DeprecationLevelValue
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.TypeParameterMarker
import org.cangnova.cangjie.type.model.safeSubstitute

/**
 * CFIR 实现的 K2 CDoc 名称解析器，对位 Kotlin `KDocReferenceResolver`。
 *
 * 解析顺序、限定名回退、可见作用域、K2 对应声明类别的优先级和附加解析扩展点均按 K2 实现；
 * 仅省略仓颉不存在的 Kotlin receiver parameter、context parameter、companion object、
 * Java synthetic property 和 Throwable 类型语义。仓颉 `@throws` / `@exception` 使用
 * `std.core.Exception` 作为异常上界。
 *
 * 仓颉 type-qualified `extend` 链接从 CFIR target index 取直接候选，用 K2 同构的 fresh-variable
 * subtype 约束检查 receiver 适用性，并继续由 CFIR provenance / accessibility checker 处理来源和可见性。
 * 普通成员 use-site scope 仍按仓颉完整实例匹配过滤候选。
 * 仓颉 enum constructor 是独立的 callable 符号类别；在作用域 callable 查找中与函数同级参与
 * 普通链接解析，保留同名函数 / enum constructor 的全部候选。
 */
@OptIn(CaExperimentalApi::class, CaImplementationDetail::class)
internal object CfirCDocReferenceResolver {
    private val ADDITIONAL_CDOC_RESOLUTION_PROVIDER_EP_NAME: ExtensionPointName<CaAdditionalCDocResolutionProvider> =
        ExtensionPointName("org.cangnova.cangjie.analysis.caAdditionalCDocResolutionProvider")

    private data class ResolveResult(
        val symbol: CaSymbol,
        val receiverClassReference: CaClassLikeSymbol? = null,
    )

    private data class VisibleScope(
        val scope: CaScope,
        val receiverType: ConeCangJieType? = null,
    )

    private enum class ContextDeclarationHandlingMode {
        PARAMETER_SYMBOLS_FOR_PROPERTY_PARAMETERS,
        PROPERTY_SYMBOLS_FOR_PROPERTY_PARAMETERS,
    }

    fun resolveCDocReference(reference: CDocReference, session: CaCfirSession): Collection<CaSymbol> {
        val nameElement = reference.element
        val fullFqName = generateSequence(nameElement) { it.parent as? CDocName }
            .last()
            .getQualifiedNameAsFqName()
        val selectedFqName = nameElement.getQualifiedNameAsFqName()
        val tag = nameElement.getStrictParentOfType<CDocLink>()?.getTagIfSubject()?.knownTag

        return resolveCDocFqName(
            session = session,
            selectedFqName = selectedFqName,
            fullFqName = fullFqName,
            contextElement = nameElement,
            subjectTag = tag,
        )
    }

    private fun resolveCDocFqName(
        session: CaSession,
        selectedFqName: FqName,
        fullFqName: FqName,
        contextElement: CjElement,
        subjectTag: CDocKnownTag?,
    ): Set<CaSymbol> = with(session) {
        val contextElementOrSelf = contextElement.getContextElementOrSelf()
        val resolved = resolveCDocFqName(fullFqName, contextElementOrSelf, subjectTag)

        if (selectedFqName == fullFqName) {
            return@with resolved.mapTo(mutableSetOf()) { it.symbol }
        }

        if (resolved.isEmpty()) {
            return@with resolveCDocFqName(
                session = session,
                selectedFqName = selectedFqName,
                fullFqName = fullFqName.parent(),
                contextElement = contextElementOrSelf,
                subjectTag = null,
            )
        }

        val goBackSteps = fullFqName.pathSegments().size - selectedFqName.pathSegments().size
        check(goBackSteps > 0) {
            "Selected CDoc name ($selectedFqName) must be shorter than the full link ($fullFqName)"
        }
        resolved.mapNotNullTo(mutableSetOf()) { result ->
            findParentSymbol(result, goBackSteps, selectedFqName)
        }
    }

    private fun CaSession.resolveCDocFqName(
        fqName: FqName,
        contextElement: CjElement,
        subjectTag: CDocKnownTag?,
    ): List<ResolveResult> {
        handleContextDeclarations(fqName, contextElement, subjectTag)?.let { return it }
        val visibleScopes = getVisibleScopes(contextElement)
        return findSymbolsInScopes(fqName, contextElement, visibleScopes, subjectTag)
    }

    private fun CaSession.handleContextDeclarations(
        fqName: FqName,
        contextElement: CjElement,
        subjectTag: CDocKnownTag?,
    ): List<ResolveResult>? {
        if (subjectTag.isExceptionExpected()) return null

        val shortName = fqName.shortName()
        if (!fqName.isOneSegment() && (subjectTag == CDocKnownTag.PARAM || subjectTag == CDocKnownTag.PROPERTY)) {
            return emptyList()
        }

        if (fqName.isOneSegment()) {
            if (subjectTag != null && shortName.asString() == "this") return emptyList()

            if (subjectTag == CDocKnownTag.PROPERTY) {
                val containingType = (contextElement as? CjTypeStatement)
                    ?.takeUnless { it.isExtend() }
                    ?: return emptyList()
                val cfirSession = this as CaCfirSession
                val property = cfirSession.declarationSiteMemberScope(containingType.classSymbol).callables(shortName)
                    .filterIsInstance<CaPropertySymbol>()
                    .let { nonHidden(it.toList()) }
                    .firstOrNull()
                return listOfNotNull(property?.toResolveResult())
            }

            val mode = if (subjectTag == CDocKnownTag.PARAM) {
                ContextDeclarationHandlingMode.PARAMETER_SYMBOLS_FOR_PROPERTY_PARAMETERS
            } else {
                ContextDeclarationHandlingMode.PROPERTY_SYMBOLS_FOR_PROPERTY_PARAMETERS
            }
            val contextSymbols = getSymbolsFromContextDeclaration(shortName, contextElement, mode)
                .let { nonHidden(it) }

            if (subjectTag == CDocKnownTag.PARAM) {
                return contextSymbols.filter { it is CaValueParameterSymbol || it is CaTypeParameterSymbol }
                    .map { it.toResolveResult() }
            }
            if (contextSymbols.isNotEmpty()) return contextSymbols.map { it.toResolveResult() }
        }

        return null
    }

    private fun CaSession.getSymbolsFromContextDeclaration(
        name: Name,
        contextElement: CjElement,
        mode: ContextDeclarationHandlingMode,
    ): List<CaSymbol> = buildList {
        val declaration = contextElement as? CjDeclaration ?: return@buildList

        if ((declaration as? CjNamedDeclaration)?.nameAsName == name) {
            val symbol = if (declaration is CjConstructor<*>) {
                declaration.getContainingTypeStatement().classSymbol
            } else {
                declaration.symbol
            }
            add(symbol)
        }

        if (declaration is CjEnumConstructor && declaration.name == name.asString()) {
            add(declaration.symbol)
        }

        fun collectParametersAndTypeParameters(parameterOwner: CjDeclaration) {
            if (parameterOwner is CjCallableDeclaration) {
                for (parameter in parameterOwner.valueParameters) {
                    if (parameter.nameAsName != name) continue

                    val parameterSymbol = parameter.symbol
                    if (
                        parameterOwner is CjPrimaryConstructor &&
                        parameter.isPropertyParameter() &&
                        mode == ContextDeclarationHandlingMode.PROPERTY_SYMBOLS_FOR_PROPERTY_PARAMETERS
                    ) {
                        val propertySymbol = (parameterSymbol as? CaValueParameterSymbol)?.correspondingProperty
                        if (propertySymbol != null) add(propertySymbol)
                    } else {
                        add(parameterSymbol)
                    }
                }
            }

            if (parameterOwner is CjTypeParameterListOwner) {
                for (typeParameter in parameterOwner.typeParameters) {
                    if (typeParameter.nameAsName == name) add(typeParameter.symbol)
                }
            }
        }

        collectParametersAndTypeParameters(declaration)
        if (declaration is CjTypeStatement) {
            declaration.primaryConstructor?.let(::collectParametersAndTypeParameters)
        }
    }

    private fun CaSession.getVisibleScopes(position: CjElement): List<VisibleScope> {
        val contextScopes = position.containingCjFile.scopeContext(position).scopes
        val localScopes = contextScopes.filter { entry ->
            entry.kind == CaScopeKind.LOCAL_SCOPE ||
                entry.kind == CaScopeKind.TYPE_SCOPE ||
                entry.kind == CaScopeKind.TYPE_PARAMETER_SCOPE ||
                entry.kind == CaScopeKind.STATIC_MEMBER_SCOPE
        }.map { entry ->
            val sourceTowerElement = (entry as? CaCfirScopeWithKind)?.sourceTowerElement
            val receiver = sourceTowerElement?.implicitReceiver
                ?.takeUnless { it.producesInapplicableCandidate() }
            VisibleScope(entry.scope, receiver?.type)
        }
        val globalScopes = contextScopes.filter { entry ->
            entry.kind == CaScopeKind.FILE_SCOPE ||
                entry.kind == CaScopeKind.PACKAGE_MEMBER_SCOPE ||
                entry.kind == CaScopeKind.BUILTIN_SCOPE ||
                entry.kind.isImporting
        }.map { VisibleScope(it.scope) }

        return localScopes + getOuterTypeScopesForPosition(position) + globalScopes
    }

    private fun CaSession.getOuterTypeScopesForPosition(position: CjElement): List<VisibleScope> {
        val session = this
        return buildList {
            var current: PsiElement? = position
            while (current != null) {
                if (current is CjTypeStatement && !current.isExtend()) {
                    val declarationContainer = current.classSymbol as CaDeclarationContainerSymbol
                    add(VisibleScope(with(session) { declarationContainer.memberScope }))
                }
                current = current.parent
            }
        }
    }

    private fun CaSession.findSymbolsInScopes(
        fqName: FqName,
        contextElement: CjElement,
        visibleScopes: List<VisibleScope>,
        subjectTag: CDocKnownTag?,
    ): List<ResolveResult> {
        val shortName = fqName.shortName()
        val scopes = sequence {
            for (visibleScope in visibleScopes) {
                for (scope in applyScopeReduction(fqName, listOf(visibleScope.scope))) {
                    yield(
                        VisibleScope(
                            scope = scope,
                            receiverType = visibleScope.receiverType.takeIf { fqName.isOneSegment() },
                        ),
                    )
                }
            }
            getLongestExistingPackageScope(fqName)?.let { yield(VisibleScope(it)) }
        }

        if (subjectTag.isExceptionExpected()) {
            for ((scope) in scopes) {
                scope.classifiers(shortName).toList()
                    .let { filterExceptionClasses(nonHidden(it)) }
                    .takeIf { it.isNotEmpty() }
                    ?.let { return it.map { symbol -> symbol.toResolveResult() } }

                filterExceptionClasses(resolveAdditionalCDocSymbols(fqName, contextElement))
                    .takeIf { it.isNotEmpty() }
                    ?.let { return it.map { symbol -> symbol.toResolveResult() } }
            }
            return emptyList()
        }

        for (visibleScope in scopes) {
            val scope = visibleScope.scope
            scope.classifiers(shortName).toList().let { nonHidden(it) }
                .takeIf { it.isNotEmpty() }
                ?.let { return it.map { symbol -> symbol.toResolveResult() } }

            val callables = nonHidden(getVisibleCallables(visibleScope, shortName, contextElement))
            callables.filter { it is CaFunctionSymbol || it is CaEnumConstructorSymbol }
                .takeIf { it.isNotEmpty() }
                ?.let { return it.map { symbol -> symbol.toResolveResult() } }

            callables.filterIsInstance<CaVariableSymbol>()
                .takeIf { it.isNotEmpty() }
                ?.let { return it.map { symbol -> symbol.toResolveResult() } }

        }

        getTypeQualifiedExtensions(fqName, contextElement, visibleScopes.map { it.scope })
            .takeIf { it.isNotEmpty() }
            ?.let { return it }

        getPackageSymbol(fqName)?.let { return listOf(it.toResolveResult()) }

        return resolveAdditionalCDocSymbols(fqName, contextElement).map { it.toResolveResult() }
    }

    /**
     * 从公开 scope 对应的 CFIR scope 读取 callable 来源；extend 成员须连同原始 tower receiver
     * 一起交给编译器可见性检查器，避免从最终 symbol 反推来源或丢失 private peer 信息。
     */
    private fun CaSession.getVisibleCallables(
        visibleScope: VisibleScope,
        name: Name,
        contextElement: CjElement,
    ): List<CaCallableSymbol> {
        val cfirBackedScope = visibleScope.scope as? CaCfirBasedScope<*>
            ?: return visibleScope.scope.callables(name)
                .filter { it.location != CaSymbolLocation.EXTEND }
                .toList()
        val cfirTypeScope = cfirBackedScope.cfirScope as? CfirTypeScope
            ?: return visibleScope.scope.callables(name)
                .filter { it.location != CaSymbolLocation.EXTEND }
                .toList()
        val cfirSession = this as? CaCfirSession
            ?: error("CFIR CDoc resolution requires a CFIR Analysis session")
        val visibilityChecker = visibleScope.receiverType?.let { receiverType ->
            val useSiteFile = contextElement.containingCjFile.getOrBuildCfirFile(cfirSession.resolutionFacade)
            cfirSession.createUseSiteMemberCallableVisibilityChecker(
                useSiteFile = useSiteFile,
                receiverType = receiverType,
                position = contextElement,
            )
        }
        val callables = linkedSetOf<CaCallableSymbol>()

        cfirTypeScope.processCallablesByNameWithLookupProvenance(name) { candidate ->
            if (candidate.provenance.sourceExtend != null && visibilityChecker?.isVisible(candidate) != true) {
                return@processCallablesByNameWithLookupProvenance
            }

            val callable = cfirSession.cfirSymbolBuilder.buildSymbol(candidate.symbol) as CaCallableSymbol
            if (candidate.provenance.sourceExtend == null && callable.location == CaSymbolLocation.EXTEND) {
                return@processCallablesByNameWithLookupProvenance
            }
            callables += callable
        }
        return callables.toList()
    }

    /**
     * 按 K2 `KDocReferenceResolver.getTypeQualifiedExtensions` 的顺序查找 receiver 限定的扩展成员。
     *
     * KDoc 先从位置作用域解析 receiver class，再查询该 receiver 上的扩展 callable。仓颉把
     * `extend` 成员建模为类型 use-site scope 的贡献者，因此这里通过该 scope 查询，并在 CFIR
     * provenance 仍完整时执行唯一的访问检查器。
     */
    private fun CaSession.getTypeQualifiedExtensions(
        fqName: FqName,
        contextElement: CjElement,
        visibleScopes: List<CaScope>,
    ): List<ResolveResult> {
        if (fqName.isRoot) return emptyList()
        val extensionName = fqName.shortName()
        val receiverTypeName = fqName.parent()
        if (receiverTypeName.isRoot) return emptyList()

        val scopesContainingPossibleReceivers = sequence {
            yieldAll(applyScopeReduction(receiverTypeName, visibleScopes))
            getLongestExistingPackageScope(receiverTypeName)?.let { yield(it) }
        }
        val possibleReceivers = nonHidden(
            scopesContainingPossibleReceivers
                .flatMap { it.classifiers(receiverTypeName.shortName()) }
                .filterIsInstance<CaClassLikeSymbol>()
                .toSet(),
        )
        if (possibleReceivers.isEmpty()) return emptyList()

        val cfirSession = this as? CaCfirSession
            ?: error("CFIR CDoc resolution requires a CFIR Analysis session")
        val useSiteFile = contextElement.containingCjFile.getOrBuildCfirFile(cfirSession.resolutionFacade)

        return possibleReceivers.flatMap { receiverClassSymbol ->
            val receiverType = with(this) { receiverClassSymbol.defaultType }
            val receiverScope = with(this) { receiverType.scope } ?: return@flatMap emptyList()
            val cfirScope = checkNotNull((receiverScope as? CaCfirBasedScope<*>)?.cfirScope as? CfirTypeScope) {
                "CFIR class-like type scope must retain its underlying CfirTypeScope"
            }
            val potentialCandidateScope = cfirScope as? CfirPotentialDirectExtendCallableScope
                ?: error("CFIR type-qualified extension scope must expose target-indexed direct extend candidates")
            val receiverConeType = (receiverType as? org.cangnova.cangjie.analysis.api.cfir.types.CaCfirType)?.coneType
                ?: error("CFIR type-qualified extension lookup requires a CFIR receiver type")
            val visibilityChecker = cfirSession.createUseSiteMemberCallableVisibilityChecker(
                useSiteFile = useSiteFile,
                receiverType = receiverConeType,
                position = contextElement,
            )
            val extensionSymbols = linkedSetOf<CaCallableSymbol>()

            potentialCandidateScope.processPotentialDirectExtendCallablesByName(extensionName) { candidate ->
                val extend = candidate.provenance.sourceExtend
                    ?: error("Potential direct extend candidate is missing its source extend")
                check(candidate.provenance.sourceSupertypeDescriptor == null) {
                    "Potential direct extend candidate must not originate from an inherited interface member"
                }
                if (!cfirSession.canBeResolvedAsTypeQualifiedExtend(extend, receiverClassSymbol, receiverConeType)) {
                    return@processPotentialDirectExtendCallablesByName
                }
                if (!visibilityChecker.isVisible(candidate)) return@processPotentialDirectExtendCallablesByName

                val callable = cfirSession.cfirSymbolBuilder.buildSymbol(candidate.symbol) as CaCallableSymbol
                if (nonHidden(listOf(callable)).isNotEmpty()) extensionSymbols += callable
            }

            extensionSymbols.map { it.toResolveResult(receiverClassSymbol) }
        }
    }

    /**
     * 对位 K2 `KaCallableSymbol.canBeCalledAsExtensionOn`：把 extend owner 与 receiver owner 类型参数
     * fresh 化，再约束 `receiver <: extend target`。仓颉类型系统没有 Kotlin CapturedType，因此无需
     * K2 的 capture-from-expression 预处理。
     */
    private fun CaCfirSession.canBeResolvedAsTypeQualifiedExtend(
        extend: CfirExtend,
        receiverClassSymbol: CaClassLikeSymbol,
        receiverType: ConeCangJieType,
    ): Boolean {
        extend.lazyResolveToPhase(CfirResolvePhase.TYPES)
        val expectedReceiverType = extend.extendedTypeRef.coneTypeOrNull ?: return false
        val receiverTypeParameters = receiverClassSymbol.typeParameters.map { parameter ->
            val cfirParameter = parameter as? CaCfirTypeParameterSymbol
                ?: error("CFIR class-like type parameter must retain its CFIR symbol")
            cfirParameter.cfirSymbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
            cfirParameter.cfirSymbol.toLookupTag()
        }
        val typeParameters: List<TypeParameterMarker> = buildList {
            extend.typeParameters.forEach { parameter ->
                parameter.symbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
                add(parameter.symbol.toLookupTag())
            }
            addAll(receiverTypeParameters)
        }.distinct()

        if (typeParameters.isEmpty()) {
            return AbstractTypeChecker.isSubtypeOf(cfirSession.typeContext, receiverType, expectedReceiverType)
        }

        val inferenceComponents = cfirSession.inferenceComponents
        val constraintSystem = ConeSimpleConstraintSystemImpl(
            inferenceComponents.createConstraintSystem(),
            cfirSession,
        )
        val substitutor = constraintSystem.registerTypeVariables(typeParameters)
        val actualReceiverType = with(constraintSystem.context) {
            substitutor.safeSubstitute(receiverType) as ConeCangJieType
        }
        val freshExpectedReceiverType = with(constraintSystem.context) {
            substitutor.safeSubstitute(expectedReceiverType) as ConeCangJieType
        }
        constraintSystem.addSubtypeConstraint(actualReceiverType, freshExpectedReceiverType)
        return !constraintSystem.hasContradiction()
    }

    private fun <T : CaSymbol> CaSession.nonHidden(symbols: Iterable<T>): List<T> = with(this) {
        symbols.filter { symbol -> symbol.deprecationStatus?.deprecationLevel != DeprecationLevelValue.HIDDEN }
    }

    private fun CaSession.filterExceptionClasses(symbols: Iterable<CaSymbol>): List<CaClassLikeSymbol> = with(this) {
        val exceptionType = checkNotNull(getClassLikeSymbol(StdlibClassIds.Exception)) {
            "The standard library class std.core.Exception must be available during CDoc resolution"
        }.defaultType
        symbols.filterIsInstance<CaClassLikeSymbol>()
            .filter { symbol -> symbol.defaultType.isSubTypeOf(exceptionType) }
    }

    private fun CaSession.resolveAdditionalCDocSymbols(fqName: FqName, contextElement: CjElement): List<CaSymbol> =
        ADDITIONAL_CDOC_RESOLUTION_PROVIDER_EP_NAME.getExtensionList(contextElement.project)
            .flatMap { it.resolveCDocFqName(this, fqName, contextElement) }

    private fun CaSession.getLongestExistingPackageScope(fqName: FqName): CaScope? {
        val segments = fqName.pathSegments()
        for (packageSegmentCount in segments.size - 1 downTo 1) {
            val packageName = FqName.fromSegments(segments.take(packageSegmentCount).map(Name::asString))
            val declarationName = FqName.fromSegments(segments.drop(packageSegmentCount).map(Name::asString))
            val packageScope = getPackageSymbol(packageName)?.packageScope ?: continue
            getNestedScopePossiblyContainingShortName(declarationName, packageScope)?.let { return it }
        }
        return null
    }

    private fun CaSession.getNestedScopePossiblyContainingShortName(fqName: FqName, scope: CaScope): CaScope? {
        val segments = fqName.pathSegments()
        if (segments.size == 1) return scope

        var currentScope = scope
        for (segment in segments.dropLast(1)) {
            val containers = currentScope.classifiers(segment)
                .filterIsInstance<CaDeclarationContainerSymbol>()
                .toList()
            if (containers.isEmpty()) return null
            val memberScopes = with(this) { containers.map { it.memberScope } }
            currentScope = with(this) { memberScopes.asCompositeScope() }
        }
        return currentScope
    }

    private fun CaSession.applyScopeReduction(fqName: FqName, scopes: List<CaScope>): List<CaScope> =
        scopes.mapNotNull { scope -> getNestedScopePossiblyContainingShortName(fqName, scope) }

    private fun CaSession.findParentSymbol(
        resolveResult: ResolveResult,
        goBackSteps: Int,
        selectedFqName: FqName,
    ): CaSymbol? {
        val receiverClassReference = resolveResult.receiverClassReference
        return if (receiverClassReference != null) {
            findParentSymbol(receiverClassReference, goBackSteps - 1, selectedFqName)
        } else {
            findParentSymbol(resolveResult.symbol, goBackSteps, selectedFqName)
        }
    }

    private fun CaSession.findParentSymbol(
        symbol: CaSymbol,
        goBackSteps: Int,
        selectedFqName: FqName,
    ): CaSymbol? {
        if (symbol !is CaDeclarationSymbol && symbol !is CaPackageSymbol) return null
        if (symbol is CaDeclarationSymbol) {
            goToNthParent(symbol, goBackSteps)?.let { return it }
        }
        return getPackageSymbol(selectedFqName)
    }

    private fun CaSession.goToNthParent(symbol: CaDeclarationSymbol, steps: Int): CaDeclarationSymbol? {
        var current = symbol
        repeat(steps) {
            current = current.containingDeclaration as? CaClassSymbol ?: return null
        }
        return current
    }

    private fun CjElement.getContextElementOrSelf(): CjElement =
        PsiTreeUtil.findFirstContext(this, false) { context ->
            context !is CDocElement && context !is CDocName && context !is CDocLink && context !is CjFile
        } as? CjElement ?: this

    private fun FqName.isOneSegment(): Boolean = !isRoot && parent().isRoot

    private fun CaSymbol.toResolveResult(receiverClassReference: CaClassLikeSymbol? = null): ResolveResult =
        ResolveResult(this, receiverClassReference)

    private fun CDocKnownTag?.isExceptionExpected(): Boolean =
        this == CDocKnownTag.THROWS || this == CDocKnownTag.EXCEPTION
}
