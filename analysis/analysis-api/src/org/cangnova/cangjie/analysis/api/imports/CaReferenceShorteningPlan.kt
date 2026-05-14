package org.cangnova.cangjie.analysis.api.imports

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.psi.CjFile

/**
 * 引用缩短计划。
 *
 * 与 [CaReferenceShorteningCommand] 的区别:
 * - 计划覆盖整个文件,不绑定具体区间;
 * - 不携带最终需要新增的 import 集合,需由调用方在落地阶段再聚合。
 *
 * 调用方可据此渲染 quick fix 预览,或转换为命令执行。
 */
interface CaReferenceShorteningPlan : CaLifetimeOwner {
    /** 计划作用的源文件。 */
    val file: CjFile

    /** 文件级别可执行的缩短操作列表。 */
    val operations: List<CaReferenceShorteningOperation>
}
