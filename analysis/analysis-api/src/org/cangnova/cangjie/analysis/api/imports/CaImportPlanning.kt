package org.cangnova.cangjie.analysis.api.imports

import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateDecision
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImportInfo

/**
 * 单个引用缩短动作的公开规划结果。
 *
 * 这里表达的是“某个表达式可否被更短的名字替代，以及是否需要补导入”，
 * 不直接执行 PSI 改写。
 */
interface CaReferenceShorteningOperation : CaLifetimeOwner {
    /**
     * 可被缩短的原始表达式。
     */
    val expression: CjExpression

    /**
     * 该表达式语义上指向的目标符号。
     */
    val target: CaSymbol

    /**
     * 缩短后应使用的短名。
     */
    val shortName: Name

    /**
     * 该缩短动作对应的补全/可见性判定。
     */
    val decision: CaCompletionCandidateDecision
}

/**
 * 单文件的引用缩短计划。
 */
interface CaReferenceShorteningPlan : CaLifetimeOwner {
    val file: CjFile
    val operations: List<CaReferenceShorteningOperation>
}

/**
 * 面向指定选择范围的引用缩短命令。
 *
 * 与 [CaReferenceShorteningPlan] 的区别在于：
 * 1. plan 描述“文件内所有可缩短操作”的全量快照；
 * 2. command 描述“当前选择范围内实际要处理的操作集合”。
 *
 * 这对应 Kotlin analysis 中 `shortenRange / shortenWholeFile` 的公共结果形态，
 * 也是后续真正执行 PSI 改写时的统一输入。
 */
interface CaReferenceShorteningCommand : CaLifetimeOwner {
    val file: CjFile
    val selection: TextRange
    val operations: List<CaReferenceShorteningOperation>
    val importsToAdd: Set<ImportPath>

    val isEmpty: Boolean
        get() = operations.isEmpty()
}

/**
 * 单文件的导入优化计划。
 *
 * 该计划不直接删除或新增 import，而是把当前文件的导入状态拆成：
 * - 已保留
 * - 重复
 * - 未使用
 * - 建议新增
 */
interface CaImportOptimizationPlan : CaLifetimeOwner {
    val file: CjFile
    val retainedImports: List<CjImportInfo>
    val duplicateImports: List<CjImportInfo>
    val unusedImports: List<CjImportInfo>
    val missingImports: List<ImportPath>
}
