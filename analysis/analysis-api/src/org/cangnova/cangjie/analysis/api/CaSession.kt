package org.cangnova.cangjie.analysis.api

import org.cangnova.cangjie.analysis.api.components.*
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner

/**
 * 仓颉分析会话（对齐 Kotlin 的 KaSession）。
 *
 * 所有分析操作的入口点。通过 [analyze] 函数获取，不能在 analyze 块外泄漏。
 *
 * 设计要点（对齐 Kotlin 约定）：
 * - 不能将 CaSession 存储为属性，只能作为 receiver 或参数传递
 * - 所有从 CaSession 获取的 lifetime owner 不能泄漏到 analyze() 块外
 * - 跨 analyze() 块传递需使用 Pointer 模式
 */
interface CaSession : CaLifetimeOwner,
    CaResolver,
    CaSymbolRelationProvider,
    CaDiagnosticProvider,
    CaScopeProvider,
    CaCompletionCandidateChecker,
    CaExpressionTypeProvider,
    CaTypeProvider,
    CaTypeInformationProvider,
    CaSymbolProvider,
    CaCInteropComponent,
    CaSymbolInformationProvider,
    CaTypeRelationChecker,
    CaExpressionInformationProvider,
    CaEvaluator,
    CaReferenceShortener,
    CaImportOptimizer,
    CaRenderer,
    CaVisibilityChecker,
    CaOriginalPsiProvider,
    CaTypeCreator,
    CaAnalysisScopeProvider,
    CaSignatureSubstitutor,
    CaSubstitutorProvider,
    CaDataFlowProvider,
    CaSourceProvider,
    CaDocProvider {

    /** 分析执行的用途模块 */
    val useSiteModule: CaModule

    /** 当前分析上下文的 CaSession */
    val useSiteSession: CaSession
        get() = this
}
