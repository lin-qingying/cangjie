package org.cangnova.cangjie.analysis.api

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
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

fun <S : CaSymbol> CaSession.restoreSymbol(pointer: CaSymbolPointer<S>): S? =
    pointer.restoreSymbol(this)

@OptIn(CaImplementationDetail::class)
fun <T : CaType> CaSession.restoreType(pointer: CaTypePointer<T>): T? =
    pointer.restore(this)

fun <S : CaSymbol> CaSession.restoreSymbols(
    pointers: Collection<CaSymbolPointer<S>>,
): List<S?> = pointers.map { pointer -> pointer.restoreSymbol(this) }

@OptIn(CaImplementationDetail::class)
fun <T : CaType> CaSession.restoreTypes(
    pointers: Collection<CaTypePointer<T>>,
): List<T?> = pointers.map { pointer -> pointer.restore(this) }
/**
 * Returns a [CaModule] for a given [element] in the context of the session's use-site module.
 *
 * @see CaModuleProvider.getModule
 */
public fun CaSession.getModule(element: PsiElement): CaModule =
    CaModuleProvider.getModule(useSiteModule.project, element, useSiteModule)

@SubclassOptInRequired(CaImplementationDetail::class)
public interface CaModuleProvider {
    /**
     * Returns a [CaModule] for a given [element] in the context of the [useSiteModule].
     *
     * The resulting [CaModule] is guaranteed to be [resolvable][CaModule.isResolvable].
     *
     * ### Use-site Modules
     *
     * The use-site module is the [CaModule] from which [getModule] is called. This concept is the same as the use-site module accepted by
     * [analyze][org.jetbrains.kotlin.analysis.api.analyze], and closely related to the concept of a use-site element. In essence, when we
     * are performing analysis, most of the time we do so from the point of view of a particular [CaModule] or [PsiElement]. If this module
     * is already known, it should be passed as the [useSiteModule] to [getModule].
     *
     * Here, the use-site module is a way to disambiguate the [CaModule] of [element]s with whom multiple modules might be associated:
     *
     * 1. It allows replacing the original [CaModule] of [element] with another module, e.g. for supporting outsider files (see below).
     * 2. It helps to distinguish between multiple possible [CaModule]s for library elements.
     *
     * If you have a use-site module in hand, please pass it as an argument to stay consistent. In the future, we may utilize the use-site
     * module for additional purposes not listed above.
     *
     * #### Outsider Modules
     *
     * Normally, every Kotlin source file either belongs to some module (e.g. a source module, or a library module), or is self-contained
     * (a script file, or a file outside content roots). However, in certain cases there might be special modules that include both
     * existing source files, and also some additional files.
     *
     * An example of such a module is one that owns an 'outsider' source file. Outsiders are used in IntelliJ for displaying files that
     * technically belong to some module, but are not included in the module's content roots (e.g. a file from a previous VCS revision).
     * As there might be cross-references between the outsider file and other files in the module, they need to be analyzed as a single
     * synthetic module. Inside an analysis session for such a module (which would be the [useSiteModule]), sources that originally
     * belong to a source module should be treated rather as a part of the synthetic one.
     */
    public fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule

    public companion object {
        public fun getInstance(project: Project): CaModuleProvider = project.service()

        public fun getModule(project: Project, element: PsiElement, useSiteModule: CaModule?): CaModule =
            getInstance(project).getModule(element, useSiteModule)
    }
}
