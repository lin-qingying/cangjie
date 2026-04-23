package org.cangnova.cangjie.analysis.api.impl.base

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaNonPublicApi
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.CaCInteropComponent
import org.cangnova.cangjie.analysis.api.components.CaCDocProvider
import org.cangnova.cangjie.analysis.api.components.CaCompletionCandidateChecker
import org.cangnova.cangjie.analysis.api.components.CaDataFlowProvider
import org.cangnova.cangjie.analysis.api.components.CaDefaultImportsProvider
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticProvider
import org.cangnova.cangjie.analysis.api.components.CaEvaluator
import org.cangnova.cangjie.analysis.api.components.CaExpressionInformationProvider
import org.cangnova.cangjie.analysis.api.components.CaExpressionTypeProvider
import org.cangnova.cangjie.analysis.api.components.CaImportOptimizer
import org.cangnova.cangjie.analysis.api.components.CaOriginalPsiProvider
import org.cangnova.cangjie.analysis.api.components.CaReferenceShortener
import org.cangnova.cangjie.analysis.api.components.CaRenderer
import org.cangnova.cangjie.analysis.api.components.CaResolver
import org.cangnova.cangjie.analysis.api.components.CaScopeProvider

import org.cangnova.cangjie.analysis.api.components.CaSignatureSubstitutor
import org.cangnova.cangjie.analysis.api.components.CaSourceProvider
import org.cangnova.cangjie.analysis.api.components.CaSubstitutorProvider
import org.cangnova.cangjie.analysis.api.components.CaSymbolInformationProvider
import org.cangnova.cangjie.analysis.api.components.CaSymbolRelationProvider
import org.cangnova.cangjie.analysis.api.components.CaTypeCreator
import org.cangnova.cangjie.analysis.api.components.CaTypeInformationProvider
import org.cangnova.cangjie.analysis.api.components.CaTypeProvider
import org.cangnova.cangjie.analysis.api.components.CaTypeRelationChecker
import org.cangnova.cangjie.analysis.api.components.CaVisibilityChecker
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseAnalysisScopeProviderEx
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.CaSessionComponentImplementationDetail
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolProvider

/**
 * `CaSession` 的基础委托实现。
 *
 * `analysis-api-impl-base` 的职责是稳定会话分层和组件装配边界：
 * 公开 session 只表达“有哪些协议”，各具体后端通过构造函数注入实现。
 */
@OptIn(CaImplementationDetail::class, CaNonPublicApi::class, CaSessionComponentImplementationDetail::class)
abstract class CaBaseSession(
    final override val token: CaLifetimeToken,
    resolver: CaResolver,
    symbolRelationProvider: CaSymbolRelationProvider,
    symbolProvider: CaSymbolProvider,
    symbolInformationProvider: CaSymbolInformationProvider,

    diagnosticProvider: CaDiagnosticProvider,
    scopeProvider: CaScopeProvider,
    defaultImportProvider: CaDefaultImportsProvider,
    completionCandidateChecker: CaCompletionCandidateChecker,
    expressionTypeProvider: CaExpressionTypeProvider,
    expressionInformationProvider: CaExpressionInformationProvider,
    evaluator: CaEvaluator,
    analysisScopeProvider: CaBaseAnalysisScopeProviderEx,


    dataFlowProvider: CaDataFlowProvider,
    typeProvider: CaTypeProvider,
    typeInformationProvider: CaTypeInformationProvider,
    typeRelationChecker: CaTypeRelationChecker,
    typeCreator: CaTypeCreator,
    substitutorProvider: CaSubstitutorProvider,
    signatureSubstitutor: CaSignatureSubstitutor,
    referenceShortener: CaReferenceShortener,
    importOptimizer: CaImportOptimizer,
    renderer: CaRenderer,
    visibilityChecker: CaVisibilityChecker,
    originalPsiProvider: CaOriginalPsiProvider,
    sourceProvider: CaSourceProvider,
    cInteropComponent: CaCInteropComponent,
    cDocProvider: CaCDocProvider,
) : CaSession,
    CaResolver by resolver,
    CaSymbolRelationProvider by symbolRelationProvider,
    CaSymbolProvider by symbolProvider,
    CaSymbolInformationProvider by symbolInformationProvider,

    CaDiagnosticProvider by diagnosticProvider,
    CaScopeProvider by scopeProvider,
    CaBaseAnalysisScopeProviderEx by analysisScopeProvider,
    CaDefaultImportsProvider by defaultImportProvider,
    CaCompletionCandidateChecker by completionCandidateChecker,
    CaExpressionTypeProvider by expressionTypeProvider,
    CaExpressionInformationProvider by expressionInformationProvider,
    CaEvaluator by evaluator,
    CaDataFlowProvider by dataFlowProvider,
    CaTypeProvider by typeProvider,
    CaTypeInformationProvider by typeInformationProvider,
    CaTypeRelationChecker by typeRelationChecker,
    CaTypeCreator by typeCreator,
    CaSubstitutorProvider by substitutorProvider,
    CaSignatureSubstitutor by signatureSubstitutor,
    CaReferenceShortener by referenceShortener,
    CaImportOptimizer by importOptimizer,
    CaRenderer by renderer,
    CaVisibilityChecker by visibilityChecker,
    CaOriginalPsiProvider by originalPsiProvider,
    CaSourceProvider by sourceProvider,
    CaCInteropComponent by cInteropComponent,
    CaCDocProvider by cDocProvider
