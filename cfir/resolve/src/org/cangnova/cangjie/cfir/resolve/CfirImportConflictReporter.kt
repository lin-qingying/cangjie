package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportBinding
import org.cangnova.cangjie.name.Name

/** import 解析后的冲突分析结果。 */
internal data class CfirImportConflicts(
    /** 没有解析到任何目标的 import binding。 */
    val unresolvedTargets: List<CfirResolvedImportBinding>,
    /** 有效名称相同但稳定目标签名不同的 import 组。 */
    val conflictingNames: Map<Name, List<CfirResolvedImportBinding>>,
    /** alias 相同但稳定目标签名不同的 import 组。 */
    val conflictingAliases: Map<Name, List<CfirResolvedImportBinding>>,
)

/** import binding 冲突分析与诊断报告入口。 */
internal class CfirImportConflictReporter(
    @Suppress("unused")
    /** 预留的诊断报告器，后续用于把冲突分析结果落到诊断管线。 */
    private val diagnosticReporter: CfirDiagnosticReporter? = null,
) {
    /** 分析 import binding 中的未解析目标、同名冲突和 alias 冲突。 */
    fun analyze(resolvedImports: List<CfirResolvedImportBinding>): CfirImportConflicts {
        val unresolvedTargets = resolvedImports.filter { it.targets.isEmpty() }
        val conflictingNames = resolvedImports
            .groupBy { it.effectiveName }
            .filterValues { sameNameBindings ->
                sameNameBindings.size >= 2 &&
                    sameNameBindings.map { it.stableTargetSignature() }.toSet().size > 1
            }
        val conflictingAliases = resolvedImports
            .filter { it.importDirective.aliasName != null }
            .groupBy { it.importDirective.aliasName!! }
            .filterValues { sameAliasBindings ->
                sameAliasBindings.size >= 2 &&
                    sameAliasBindings.map { it.stableTargetSignature() }.toSet().size > 1
            }
        return CfirImportConflicts(
            unresolvedTargets = unresolvedTargets,
            conflictingNames = conflictingNames,
            conflictingAliases = conflictingAliases,
        )
    }

    /** 返回未解析 import target 集合。 */
    fun reportUnresolvedTargets(resolvedImports: List<CfirResolvedImportBinding>) = analyze(resolvedImports).unresolvedTargets

    /** 返回完整 import 冲突分析结果。 */
    fun reportConflicts(resolvedImports: List<CfirResolvedImportBinding>) = analyze(resolvedImports)
}
