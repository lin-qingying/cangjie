package org.cangnova.cangjie.analysis.api.imports

import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImportInfo

/**
 * import 优化计划。
 *
 * Analysis API 对单个源文件 import 重排/去重/补全的稳定建议视图:
 * - 调用方据此分类生成 `Apply Import Optimization` 类型的修复;
 * - 不直接修改 PSI,具体重写仍由 IDE intent/inspection 完成。
 *
 * 对齐 Kotlin Analysis API 的 `KaImportOptimizer.analyseImports` 结果。
 */
interface CaImportOptimizationPlan : CaLifetimeOwner {
    /** 当前计划针对的源文件。 */
    val file: CjFile

    /** 应保留的 import 列表(按建议顺序)。 */
    val retainedImports: List<CjImportInfo>

    /** 重复的 import 列表,可安全删除。 */
    val duplicateImports: List<CjImportInfo>

    /** 文件中存在但未被使用的 import,通常会被删除。 */
    val unusedImports: List<CjImportInfo>

    /** 文件中存在引用但缺失对应 import 的路径,通常会被添加。 */
    val missingImports: List<ImportPath>
}
