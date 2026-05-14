package org.cangnova.cangjie.analysis.api.imports

import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.psi.CjFile

/**
 * 引用缩短命令。
 *
 * 是 [CaReferenceShorteningPlan] 的可执行视图:
 * - 锁定一个文件 + 文本区间;
 * - 列出该区间内可执行的所有缩短操作以及附带需要新增的 import;
 * - 调用方按 [operations] 顺序应用,即可把完整限定名替换为短名。
 */
interface CaReferenceShorteningCommand : CaLifetimeOwner {
    /** 命令作用的源文件。 */
    val file: CjFile

    /** 命令的覆盖区间,仅处理落在区间内的引用。 */
    val selection: TextRange

    /** 该区间内可执行的缩短操作列表。 */
    val operations: List<CaReferenceShorteningOperation>

    /** 执行操作时需要追加到文件 import 区的路径集合(去重)。 */
    val importsToAdd: Set<ImportPath>

    /** 命令是否为空(无任何操作时为 `true`)。 */
    val isEmpty: Boolean
        get() = operations.isEmpty()
}
