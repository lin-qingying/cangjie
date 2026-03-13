package org.cangjie.analysis.api.impl.base

import org.cangjie.analysis.api.CaSession
import org.cangjie.analysis.api.components.*
import org.cangjie.analysis.api.lifetime.CaLifetimeToken

/**
 * CaSession 的基础实现（对齐 Kotlin 的 KaBaseSession）。
 *
 * 通过委托模式将所有 session component 的实现注入。
 */
abstract class CaBaseSession(
    final override val token: CaLifetimeToken,
    resolver: CaResolver,
    symbolRelationProvider: CaSymbolRelationProvider,
    diagnosticProvider: CaDiagnosticProvider,
    scopeProvider: CaScopeProvider,
    completionCandidateChecker: CaCompletionCandidateChecker,
    expressionTypeProvider: CaExpressionTypeProvider,
    typeProvider: CaTypeProvider,
    typeInformationProvider: CaTypeInformationProvider,
    symbolProvider: CaSymbolProvider,
    cInteropComponent: CaCInteropComponent,
    symbolInformationProvider: CaSymbolInformationProvider,
    typeRelationChecker: CaTypeRelationChecker,
    expressionInformationProvider: CaExpressionInformationProvider,
    evaluator: CaEvaluator,
    referenceShortener: CaReferenceShortener,
    importOptimizer: CaImportOptimizer,
    renderer: CaRenderer,
    visibilityChecker: CaVisibilityChecker,
    originalPsiProvider: CaOriginalPsiProvider,
    typeCreator: CaTypeCreator,
    analysisScopeProvider: CaAnalysisScopeProvider,
    signatureSubstitutor: CaSignatureSubstitutor,
    substitutorProvider: CaSubstitutorProvider,
    dataFlowProvider: CaDataFlowProvider,
    sourceProvider: CaSourceProvider,
    docProvider: CaDocProvider,
) : CaSession,
    CaResolver by resolver,
    CaSymbolRelationProvider by symbolRelationProvider,
    CaDiagnosticProvider by diagnosticProvider,
    CaScopeProvider by scopeProvider,
    CaCompletionCandidateChecker by completionCandidateChecker,
    CaExpressionTypeProvider by expressionTypeProvider,
    CaTypeProvider by typeProvider,
    CaTypeInformationProvider by typeInformationProvider,
    CaSymbolProvider by symbolProvider,
    CaCInteropComponent by cInteropComponent,
    CaSymbolInformationProvider by symbolInformationProvider,
    CaTypeRelationChecker by typeRelationChecker,
    CaExpressionInformationProvider by expressionInformationProvider,
    CaEvaluator by evaluator,
    CaReferenceShortener by referenceShortener,
    CaImportOptimizer by importOptimizer,
    CaRenderer by renderer,
    CaVisibilityChecker by visibilityChecker,
    CaOriginalPsiProvider by originalPsiProvider,
    CaTypeCreator by typeCreator,
    CaAnalysisScopeProvider by analysisScopeProvider,
    CaSignatureSubstitutor by signatureSubstitutor,
    CaSubstitutorProvider by substitutorProvider,
    CaDataFlowProvider by dataFlowProvider,
    CaSourceProvider by sourceProvider,
    CaDocProvider by docProvider
