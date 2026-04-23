package org.cangnova.cangjie.analysis.api.cfir

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirAnalysisScopeProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirCInteropComponent
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirCompletionCandidateChecker
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirDataFlowProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirDefaultImportsProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirDiagnosticProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirCDocProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirEvaluator
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirExpressionInformationProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirExpressionTypeProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirImportOptimizer
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirInternalCacheStorage
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirOriginalPsiProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirReferenceShortener
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirRenderer
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirResolver
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirScopeProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirSignatureSubstitutor
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirSourceProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirSubstitutorProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirSymbolInformationProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirSymbolRelationProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirTypeCreator
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirTypeInformationProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirTypeProvider
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirTypeRelationChecker
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirVisibilityChecker
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbolProvider
import org.cangnova.cangjie.analysis.api.impl.base.CaBaseSession
import org.cangnova.cangjie.analysis.api.impl.base.util.createSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.assertIsValid
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieCompositeDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.declarations.createDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.packages.CangJieCompositePackageProvider
import org.cangnova.cangjie.analysis.api.platform.packages.CangJiePackageProvider
import org.cangnova.cangjie.analysis.api.platform.packages.createPackageProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaResolutionScope
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession

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
@OptIn(CaPlatformInterface::class, CaImplementationDetail::class)
internal class CaCfirSession private constructor(
    val project: Project,
    val resolutionFacade: LLResolutionFacade,
    token: CaLifetimeToken,
    analysisSessionProvider: () -> CaCfirSession,
    useSiteScope: CaResolutionScope
) : CaBaseSession(
    token = token,
    resolver = CaCfirResolver(analysisSessionProvider),
    symbolRelationProvider = CaCfirSymbolRelationProvider(analysisSessionProvider),
    symbolProvider = CaCfirSymbolProvider(analysisSessionProvider),
    symbolInformationProvider = CaCfirSymbolInformationProvider(analysisSessionProvider),
    diagnosticProvider = CaCfirDiagnosticProvider(analysisSessionProvider),
    scopeProvider = CaCfirScopeProvider(analysisSessionProvider),
    analysisScopeProvider = CaCfirAnalysisScopeProvider(analysisSessionProvider,useSiteScope),
    defaultImportProvider = CaCfirDefaultImportsProvider( ),
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
    cDocProvider = CaCfirCDocProvider(analysisSessionProvider),
) {
    override val useSiteModule: CaModule
        get() = resolutionFacade.useSiteModule

    internal val cfirSession: CfirSession
        get() = resolutionFacade.useSiteCfirSession



    val useSiteScopeDeclarationProvider: CangJieDeclarationProvider
    val useSitePackageProvider: CangJiePackageProvider


    init {
        useSiteScopeDeclarationProvider = CangJieCompositeDeclarationProvider.create(
            buildList {
                add(project.createDeclarationProvider(useSiteScope, useSiteModule))
//                extensionTools.mapTo(this) { it.declarationProvider }
            }
        )

        useSitePackageProvider = CangJieCompositePackageProvider.create(
            buildList {
                add(project.createPackageProvider(useSiteScope))
//                extensionTools.mapTo(this) { it.packageProvider }
            }
        )
    }
    /**
     * public symbol 的构造统一委托给 CangJie 风格的 builder。
     *
     * session 只负责缓存、查询与组件编排，不再把具体 symbol 组装逻辑直接堆在 factory 文件里。
     */
    internal val cfirSymbolBuilder: CaSymbolByCfirBuilder by lazy {
        CaSymbolByCfirBuilder(project, this, token)
    }
    fun getScopeSessionFor(session: CfirSession): ScopeSession = withValidityAssertion { resolutionFacade.getScopeSessionFor(session) }

    /**
     * 会话级缓存存储。
     *
     * 对齐 CangJie `CaCfirSession.cacheStorage` 的角色：session 只持有缓存基础设施，
     * 不再自己暴露一大批逐项转发方法。
     */
    internal val cacheStorage by lazy {
        CaCfirInternalCacheStorage(this)
    }


    companion object {
        @OptIn(CaImplementationDetail::class)
        internal fun createAnalysisSessionByResolutionFacade(
            resolutionFacade: LLResolutionFacade,
            token: CaLifetimeToken,
        ): CaCfirSession {
            token.assertIsValid()
            val useSiteModule = resolutionFacade.useSiteModule
            val useSiteSession = resolutionFacade.useSiteCfirSession

//            val extensionTools = buildList {
//                addIfNotNull(useSiteSession.llResolveExtensionTool)
//                useSiteModule.allDirectDependencies.mapNotNullTo(this) { dependency ->
//                    resolutionFacade.getDependencySessionFor(dependency)?.llResolveExtensionTool
//                }
//            }

            val resolutionScope = CaResolutionScope.forModule(useSiteModule)

            return createSession {
                CaCfirSession(
                    resolutionFacade.project,
                    resolutionFacade,
//                    extensionTools,
                    token,
                    analysisSessionProvider,
                    resolutionScope
                )
            }
        }
    }
}
