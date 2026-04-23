package org.cangnova.cangjie.analysis.api.cfir

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateDecision
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirCompletionSymbolKey
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile

/**
 * 顶层公开符号查询值。
 *
 * `analysis-api-cfir` 对同一包名和短名下的顶层 class-like 与 callable
 * 采用统一查询和统一缓存，因此这里直接保存公开 Analysis API 视图。
 */
internal data class CaCfirTopLevelPublicSymbolQueryValue(
    val classLikeSymbols: List<CaClassLikeSymbol>,
    val callableSymbols: List<CaCallableSymbol>,
)

/**
 * 顶层 low-level 符号查询值。
 *
 * session query service 只负责恢复真实 CFIR 叶子符号；
 * public symbol 的构造和缓存仍然留在 `cfir.symbols` 层。
 */
internal data class CaCfirTopLevelSymbolQueryResult(
    val classLikeSymbols: List<CfirClassLikeSymbol<*>>,
    val callableSymbols: List<CfirCallableSymbol<*>>,
)

/**
 * 顶层公开符号查询键。
 *
 * 同一 use-site session 中，顶层公开符号查询仅由包名与短名决定，
 * 不能夹带任何兜底上下文或临时 PSI 状态。
 */
internal data class CaCfirTopLevelPublicSymbolQueryKey(
    val packageFqName: FqName,
    val name: Name,
)

/**
 * 元素级诊断查询键。
 *
 * 诊断快照同时受目标元素与诊断过滤器影响，因此两者必须共同参与缓存命中。
 */
internal data class CaCfirDiagnosticsQueryKey(
    val element: PsiElement,
    val filter: DiagnosticCheckerFilter,
)

/**
 * 文件级诊断查询键。
 *
 * 文件诊断会随着过滤器维度变化而变化，不能仅按文件缓存。
 */
internal data class CaCfirFileDiagnosticsQueryKey(
    val file: CjFile,
    val filter: DiagnosticCheckerFilter,
)

/**
 * 补全决策缓存键。
 *
 * 同一公开或临时符号在不同位置上的补全决策可能不同，因此必须同时记录
 * “补全中的符号身份”和“触发位置”两个维度。
 */
internal data class CaCfirCompletionDecisionKey(
    val symbolKey: CaCfirCompletionSymbolKey,
    val position: PsiElement,
)
