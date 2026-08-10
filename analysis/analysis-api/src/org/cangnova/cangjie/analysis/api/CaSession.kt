package org.cangnova.cangjie.analysis.api

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.components.CaAnalysisScopeProvider
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
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.CaSessionComponentImplementationDetail
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolProvider
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypePointer

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
@OptIn(CaNonPublicApi::class, CaExperimentalApi::class, CaIdeApi::class, CaSessionComponentImplementationDetail::class)
@SubclassOptInRequired(CaImplementationDetail::class)
interface CaSession : CaLifetimeOwner,
    CaResolver,
    CaSymbolRelationProvider,
    CaSymbolProvider,
    CaSymbolInformationProvider,

    CaSignatureSubstitutor,

    CaDiagnosticProvider,
    CaScopeProvider,
    CaAnalysisScopeProvider,
    CaDefaultImportsProvider,
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

    CaReferenceShortener,
    CaImportOptimizer,
    CaRenderer,
    CaVisibilityChecker,
    CaOriginalPsiProvider,
    CaSourceProvider,
    CaCInteropComponent,
    CaCDocProvider {

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

/**
 * The [CaSession] of the current analysis context.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!

context(session: CaSession)
val useSiteSession: CaSession
    get() = with(session) { useSiteSession }

/**
 * 在当前会话中按 [pointer] 恢复单个 symbol。
 *
 * - 若指针所指对象在当前 session 不可见(如来自其他模块、声明已被移除)则返回 `null`;
 * - 调用方负责持有指针并跨 analyze 块传递,实际恢复必须在 session 内完成。
 *
 * 对齐 Kotlin Analysis API 的 `KaSession.restoreSymbol`。
 */
fun <S : CaSymbol> CaSession.restoreSymbol(pointer: CaSymbolPointer<S>): S? =
    pointer.restoreSymbol(this)

/**
 * 在当前会话中按 [pointer] 恢复单个 type。
 *
 * - 若指针所指类型在当前 session 不可解析则返回 `null`;
 * - 与 [restoreSymbol] 一样,是跨 analyze 块复用类型对象的官方入口。
 *
 * 对齐 Kotlin Analysis API 的 `KaSession.restoreType`。
 */
@OptIn(CaImplementationDetail::class)
fun <T : CaType> CaSession.restoreType(pointer: CaTypePointer<T>): T? =
    pointer.restore(this)

/**
 * 批量恢复 symbol 指针,结果按输入顺序返回,无法恢复的位置填 `null`。
 *
 * 适用于补全、引用解析等需要一次性处理多个候选的场景;
 * 调用方可据 `null` 位置剔除已失效条目。
 */
fun <S : CaSymbol> CaSession.restoreSymbols(
    pointers: Collection<CaSymbolPointer<S>>,
): List<S?> = pointers.map { pointer -> pointer.restoreSymbol(this) }

/**
 * 批量恢复 type 指针,结果按输入顺序返回,无法恢复的位置填 `null`。
 *
 * 语义与 [restoreSymbols] 对应,仅作用对象换成 [CaTypePointer]。
 */
@OptIn(CaImplementationDetail::class)
fun <T : CaType> CaSession.restoreTypes(
    pointers: Collection<CaTypePointer<T>>,
): List<T?> = pointers.map { pointer -> pointer.restore(this) }

/**
 * 在当前会话的 use-site 模块上下文中,查询 [element] 所属的 [CaModule]。
 *
 * 直接委托给 [CaModuleProvider.getModule],把 session 的 project 与 use-site 模块作为
 * 模糊归属(如 dangling file)时的回退依据。
 *
 * @see CaModuleProvider.getModule
 */
fun CaSession.getModule(element: PsiElement): CaModule =
    CaModuleProvider.getModule(useSiteModule.project, element, useSiteModule)
