package org.cangnova.cangjie.analysis.api.cfir

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateDecision
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirAnalysisScopeProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirAnnotationProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirCInteropComponent
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirCallableSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirCompletionCandidateChecker
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirDataFlowProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirDefaultImportProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirDiagnosticProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirDocProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirEvaluator
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirExpressionInformationProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirExpressionTypeProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirImportOptimizer
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirOriginalPsiProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirPublicSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirReferenceShortener
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirRenderer
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirResolver
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirScopeProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirSignatureProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirSignatureSubstitutor
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirSourceProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirSubstitutedSignatureCacheKey
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirSubstitutorProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirSymbolInformationProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirSymbolProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirSymbolRelationProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirTypeCreator
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirTypeInformationProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirTypeProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirTypeRelationChecker
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirTypeSubstitutorCacheKey
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirVisibilityChecker
import org.cangnova.cangjie.analysis.api.cfir.components.completionDecisionKey
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirCallInfoSnapshot
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirResolutionFacade
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirScopeSnapshot
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirTopLevelSymbolQueryResult
import org.cangnova.cangjie.analysis.api.cfir.resolve.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.dataFlow.CaDataFlowInfo
import org.cangnova.cangjie.analysis.api.impl.base.CaBaseSession
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImports
import org.cangnova.cangjie.analysis.api.imports.CaImportOptimizationPlan
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningPlan
import org.cangnova.cangjie.analysis.api.interop.CaInteropInfo
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.substitution.CaSubstitutedSignature
import org.cangnova.cangjie.analysis.api.substitution.CaTypeSubstitutor
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjReferenceExpression

/**
 * CFIR 后端分析会话。
 *
 * 这个类只承担会话编排职责：
 * 1. 组装公开 Analysis API 组件；
 * 2. 暴露统一的 session 内部协议；
 * 3. 将缓存实现与 low-level 查询分别委托给独立服务。
 *
 * 因此 `analysis-api-cfir` 的组件层只依赖会话协议，
 * 而不再直接操作 low-level facade 或私有缓存结构。
 */
internal class CaCfirSession private constructor(
    val project: Project,
    val resolutionFacade: CaCfirResolutionFacade,
    token: CaLifetimeToken,
    analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSession(
    token = token,
    resolver = CaCfirResolver(analysisSessionProvider),
    symbolRelationProvider = CaCfirSymbolRelationProvider(analysisSessionProvider),
    symbolProvider = CaCfirSymbolProvider(analysisSessionProvider),
    symbolInformationProvider = CaCfirSymbolInformationProvider(analysisSessionProvider),
    annotationProvider = CaCfirAnnotationProvider(analysisSessionProvider),
    signatureProvider = CaCfirSignatureProvider(analysisSessionProvider),
    diagnosticProvider = CaCfirDiagnosticProvider(analysisSessionProvider),
    scopeProvider = CaCfirScopeProvider(analysisSessionProvider),
    analysisScopeProvider = CaCfirAnalysisScopeProvider(analysisSessionProvider),
    defaultImportProvider = CaCfirDefaultImportProvider(analysisSessionProvider),
    completionCandidateChecker = CaCfirCompletionCandidateChecker(analysisSessionProvider),
    expressionTypeProvider = CaCfirExpressionTypeProvider(analysisSessionProvider),
    expressionInformationProvider = CaCfirExpressionInformationProvider(analysisSessionProvider),
    evaluator = CaCfirEvaluator(analysisSessionProvider),
    dataFlowProvider = CaCfirDataFlowProvider(analysisSessionProvider),
    typeProvider = CaCfirTypeProvider(analysisSessionProvider),
    typeInformationProvider = CaCfirTypeInformationProvider(analysisSessionProvider),
    typeRelationChecker = CaCfirTypeRelationChecker(analysisSessionProvider),
    typeCreator = CaCfirTypeCreator(analysisSessionProvider),
    substitutorProvider = CaCfirSubstitutorProvider(analysisSessionProvider),
    signatureSubstitutor = CaCfirSignatureSubstitutor(analysisSessionProvider),
    referenceShortener = CaCfirReferenceShortener(analysisSessionProvider),
    importOptimizer = CaCfirImportOptimizer(analysisSessionProvider),
    renderer = CaCfirRenderer(analysisSessionProvider),
    visibilityChecker = CaCfirVisibilityChecker(analysisSessionProvider),
    originalPsiProvider = CaCfirOriginalPsiProvider(analysisSessionProvider),
    sourceProvider = CaCfirSourceProvider(analysisSessionProvider),
    cInteropComponent = CaCfirCInteropComponent(analysisSessionProvider),
    docProvider = CaCfirDocProvider(analysisSessionProvider),
) {
    override val useSiteModule: CaModule
        get() = resolutionFacade.useSiteModule

    internal val cfirSession: CfirSession
        get() = resolutionFacade.useSiteFirSession

    private val cacheStore = CaCfirSessionCacheStore()
    private val diagnosticQueryService = CaCfirSessionDiagnosticQueryService(
        resolutionFacade = resolutionFacade,
        cacheStore = cacheStore,
    )
    private val scopeQueryService = CaCfirSessionScopeQueryService(
        resolutionFacade = resolutionFacade,
        cacheStore = cacheStore,
    )
    private val symbolQueryService = CaCfirSessionSymbolQueryService(
        resolutionFacade = resolutionFacade,
        cacheStore = cacheStore,
    )
    private val typeQueryService = CaCfirSessionTypeQueryService(
        resolutionFacade = resolutionFacade,
        cacheStore = cacheStore,
    )

    /**
     * 以下入口构成 `analysis-api-cfir` 面向组件层的统一内部协议。
     *
     * 组件层不再直接依赖 low-level facade 或私有缓存结构，
     * 所有公开符号缓存、派生快照与底层语义查询都通过会话协议访问。
     */
    internal fun <S : CaSymbol> getOrCreatePublicSymbol(
        key: CaCfirPublicSymbolCacheKey,
        create: () -> S,
    ): S = cacheStore.getOrCreatePublicSymbol(key, create)

    internal fun getOrCreateTopLevelSymbolQuery(
        packageFqName: FqName,
        name: Name,
        create: () -> CaCfirTopLevelPublicSymbolQueryValue,
    ): CaCfirTopLevelPublicSymbolQueryValue =
        cacheStore.getOrCreateTopLevelSymbolQuery(packageFqName, name, create)

    internal fun getOrCreateDocumentation(
        key: CaCfirPublicSymbolCacheKey,
        create: () -> String?,
    ): String? = cacheStore.getOrCreateDocumentation(key, create)

    internal fun getOrCreateDeclarationAnnotations(
        owner: PsiElement,
        create: () -> List<CaAnnotation>,
    ): List<CaAnnotation> = cacheStore.getOrCreateDeclarationAnnotations(owner, create)

    internal fun getOrCreateDeclarationSignature(
        declaration: CjCallableDeclaration,
        create: () -> CaSignature,
    ): CaSignature = cacheStore.getOrCreateDeclarationSignature(declaration, create)

    internal fun getOrCreateCallableSignature(
        key: CaCfirCallableSymbolCacheKey,
        create: () -> CaSignature?,
    ): CaSignature? = cacheStore.getOrCreateCallableSignature(key, create)

    internal fun getOrCreateSubstitutedSignature(
        key: CaCfirSubstitutedSignatureCacheKey,
        create: () -> CaSubstitutedSignature,
    ): CaSubstitutedSignature = cacheStore.getOrCreateSubstitutedSignature(key, create)

    internal fun getOrCreateTypeSubstitutor(
        key: CaCfirTypeSubstitutorCacheKey,
        create: () -> CaTypeSubstitutor,
    ): CaTypeSubstitutor = cacheStore.getOrCreateTypeSubstitutor(key, create)

    internal fun getOrCreateDefaultImports(
        create: () -> CaDefaultImports,
    ): CaDefaultImports = cacheStore.getOrCreateDefaultImports(create)

    internal fun getOrCreateDataFlowInfo(
        expression: CjExpression,
        create: () -> CaDataFlowInfo,
    ): CaDataFlowInfo = cacheStore.getOrCreateDataFlowInfo(expression, create)

    internal fun getOrCreateCompletionDecision(
        symbol: CaSymbol,
        position: PsiElement,
        create: () -> CaCompletionCandidateDecision,
    ): CaCompletionCandidateDecision = cacheStore.getOrCreateCompletionDecision(
        symbolKey = symbol.completionDecisionKey(),
        position = position,
        create = create,
    )

    internal fun getOrCreateReferenceShorteningPlan(
        file: CjFile,
        create: () -> CaReferenceShorteningPlan,
    ): CaReferenceShorteningPlan = cacheStore.getOrCreateReferenceShorteningPlan(file, create)

    internal fun getOrCreateImportOptimizationPlan(
        file: CjFile,
        create: () -> CaImportOptimizationPlan,
    ): CaImportOptimizationPlan = cacheStore.getOrCreateImportOptimizationPlan(file, create)

    internal fun getOrCreateInteropInfo(
        element: PsiElement,
        create: () -> CaInteropInfo?,
    ): CaInteropInfo? = cacheStore.getOrCreateInteropInfo(element, create)

    internal fun getOrCreateSymbolInteropInfo(
        key: CaCfirPublicSymbolCacheKey,
        create: () -> CaInteropInfo?,
    ): CaInteropInfo? = cacheStore.getOrCreateSymbolInteropInfo(key, create)

    internal fun resolveSymbols(reference: CjReferenceExpression): Collection<CfirSymbol<*>> =
        symbolQueryService.resolveSymbols(reference)

    internal fun queryCallInfo(element: PsiElement): CaCfirCallInfoSnapshot? =
        diagnosticQueryService.queryCallInfo(element)

    internal fun queryDiagnostics(
        element: PsiElement,
        filter: DiagnosticCheckerFilter,
    ): List<org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic> =
        diagnosticQueryService.queryDiagnostics(element, filter)

    internal fun queryFileDiagnostics(
        file: CjFile,
        filter: DiagnosticCheckerFilter,
    ): Collection<org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic> =
        diagnosticQueryService.queryFileDiagnostics(file, filter)

    internal fun queryFileScope(file: CjFile): CaCfirScopeSnapshot =
        scopeQueryService.queryFileScope(file)

    internal fun queryPackageScope(packageFqName: FqName): CaCfirScopeSnapshot? =
        scopeQueryService.queryPackageScope(packageFqName)

    internal fun queryDeclaredMemberScope(classId: ClassId): CaCfirScopeSnapshot? =
        scopeQueryService.queryDeclaredMemberScope(classId)

    internal fun queryMemberScope(classId: ClassId): CaCfirScopeSnapshot? =
        scopeQueryService.queryMemberScope(classId)

    internal fun queryTypeScope(type: ConeCangJieType): CaCfirScopeSnapshot? =
        scopeQueryService.queryTypeScope(type)

    internal fun hasVisiblePackage(packageFqName: FqName): Boolean =
        scopeQueryService.hasVisiblePackage(packageFqName)

    internal fun lookupClassLikeSymbol(classId: ClassId): CfirClassLikeSymbol<*>? =
        symbolQueryService.lookupClassLikeSymbol(classId)

    internal fun queryTopLevelSymbols(
        packageFqName: FqName,
        name: Name,
    ): CaCfirTopLevelSymbolQueryResult = symbolQueryService.queryTopLevelSymbols(packageFqName, name)

    internal fun lookupFileSymbol(file: CjFile): CfirFileSymbol? =
        symbolQueryService.lookupFileSymbol(file)

    internal fun lookupSourcePsi(symbol: CfirSymbol<*>): PsiElement? =
        symbolQueryService.lookupSourcePsi(symbol)

    internal fun lookupSymbolsByPsi(psi: PsiElement): List<CfirSymbol<*>> =
        symbolQueryService.lookupSymbolsByPsi(psi)

    internal fun lookupContainingFile(symbol: CfirSymbol<*>): CjFile? =
        symbolQueryService.lookupContainingFile(symbol)

    internal fun queryExpressionType(expression: CjExpression): ConeCangJieType? =
        typeQueryService.queryExpressionType(expression)

    internal fun queryDeclarationReturnType(declaration: CjCallableDeclaration): ConeCangJieType? =
        typeQueryService.queryDeclarationReturnType(declaration)

    internal fun queryValueParameterType(parameter: CjParameter): ConeCangJieType? =
        typeQueryService.queryValueParameterType(parameter)

    internal fun queryCallableReturnType(symbol: CfirCallableSymbol<*>): ConeCangJieType? =
        typeQueryService.queryCallableReturnType(symbol)

    internal fun queryClassLikeDefaultType(symbol: CfirClassLikeSymbol<*>): ConeCangJieType? =
        typeQueryService.queryClassLikeDefaultType(symbol)

    internal fun queryTypeClassLikeSymbol(type: ConeCangJieType): CfirClassLikeSymbol<*>? =
        typeQueryService.queryTypeClassLikeSymbol(type)

    internal fun queryClassLikeSuperTypes(symbol: CfirClassLikeSymbol<*>): List<ConeCangJieType> =
        typeQueryService.queryClassLikeSuperTypes(symbol)

    internal fun isSubTypeOf(
        subType: ConeCangJieType,
        superType: ConeCangJieType,
    ): Boolean = typeQueryService.isSubTypeOf(subType, superType)

    internal fun areTypesEqual(
        left: ConeCangJieType,
        right: ConeCangJieType,
    ): Boolean = typeQueryService.areTypesEqual(left, right)

    companion object {
        fun create(
            project: Project,
            resolutionFacade: CaCfirResolutionFacade,
            token: CaLifetimeToken,
        ): CaCfirSession {
            lateinit var session: CaCfirSession
            val sessionProvider: () -> CaCfirSession = { session }
            session = CaCfirSession(
                project = project,
                resolutionFacade = resolutionFacade,
                token = token,
                analysisSessionProvider = sessionProvider,
            )
            return session
        }
    }
}
