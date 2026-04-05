package org.cangnova.cangjie.analysis.api

import org.cangnova.cangjie.analysis.api.components.CaAnalysisScopeProvider
import org.cangnova.cangjie.analysis.api.components.CaAnnotationProvider
import org.cangnova.cangjie.analysis.api.components.CaCInteropComponent
import org.cangnova.cangjie.analysis.api.components.CaCompletionCandidateChecker
import org.cangnova.cangjie.analysis.api.components.CaDataFlowProvider
import org.cangnova.cangjie.analysis.api.components.CaDefaultImportProvider
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticProvider
import org.cangnova.cangjie.analysis.api.components.CaDocProvider
import org.cangnova.cangjie.analysis.api.components.CaEvaluator
import org.cangnova.cangjie.analysis.api.components.CaExpressionInformationProvider
import org.cangnova.cangjie.analysis.api.components.CaExpressionTypeProvider
import org.cangnova.cangjie.analysis.api.components.CaImportOptimizer
import org.cangnova.cangjie.analysis.api.components.CaOriginalPsiProvider
import org.cangnova.cangjie.analysis.api.components.CaReferenceShortener
import org.cangnova.cangjie.analysis.api.components.CaRenderer
import org.cangnova.cangjie.analysis.api.components.CaResolver
import org.cangnova.cangjie.analysis.api.components.CaScopeProvider
import org.cangnova.cangjie.analysis.api.components.CaSignatureProvider
import org.cangnova.cangjie.analysis.api.components.CaSignatureSubstitutor
import org.cangnova.cangjie.analysis.api.components.CaSourceProvider
import org.cangnova.cangjie.analysis.api.components.CaSubstitutorProvider
import org.cangnova.cangjie.analysis.api.components.CaSymbolInformationProvider
import org.cangnova.cangjie.analysis.api.components.CaSymbolProvider
import org.cangnova.cangjie.analysis.api.components.CaSymbolRelationProvider
import org.cangnova.cangjie.analysis.api.components.CaTypeCreator
import org.cangnova.cangjie.analysis.api.components.CaTypeInformationProvider
import org.cangnova.cangjie.analysis.api.components.CaTypeProvider
import org.cangnova.cangjie.analysis.api.components.CaTypeRelationChecker
import org.cangnova.cangjie.analysis.api.components.CaVisibilityChecker
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner

/**
 * 仓颉 Analysis API 的分析会话。
 *
 * 所有分析操作都必须经由 [analyze] 获取的 session 完成，
 * 且 `CaSession` 以及它产出的 lifetime owner 都不能越过当前 analyze 块泄漏。
 *
 * 设计约束：
 * 1. `CaSession` 只能作为 analyze receiver 或临时参数使用；
 * 2. 从 session 得到的 symbol、type、scope、signature、annotation 等对象都受同一生命周期约束；
 * 3. 跨 analyze 传递必须使用 pointer，而不是直接持有原对象。
 */
interface CaSession : CaLifetimeOwner,
    CaResolver,
    CaSymbolRelationProvider,
    CaSymbolProvider,
    CaSymbolInformationProvider,
    CaAnnotationProvider,
    CaSignatureProvider,
    CaDiagnosticProvider,
    CaScopeProvider,
    CaAnalysisScopeProvider,
    CaDefaultImportProvider,
    CaCompletionCandidateChecker,
    CaExpressionTypeProvider,
    CaExpressionInformationProvider,
    CaEvaluator,
    CaDataFlowProvider,
    CaTypeProvider,
    CaTypeInformationProvider,
    CaTypeRelationChecker,
    CaTypeCreator,
    CaSubstitutorProvider,
    CaSignatureSubstitutor,
    CaReferenceShortener,
    CaImportOptimizer,
    CaRenderer,
    CaVisibilityChecker,
    CaOriginalPsiProvider,
    CaSourceProvider,
    CaCInteropComponent,
    CaDocProvider {

    /**
     * 当前分析执行的 use-site 模块。
     */
    val useSiteModule: CaModule

    /**
     * 当前分析上下文对应的会话对象。
     *
     * 该属性与 Kotlin Analysis API 中的 `useSiteSession` 对齐，
     * 便于统一表达“从任意组件回到当前 use-site 会话”的语义。
     */
    val useSiteSession: CaSession
        get() = this
}
