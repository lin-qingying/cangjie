package org.cangnova.cangjie.analysis.api.stubs

import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.stubs.CangJieFileStubKind

/**
 * 单文件 stub 信息提供器。
 *
 * 把"文件维度"上能通过 stub 树快速获取的元信息(类型、顶级名)集中暴露,
 * 供索引、补全、引用解析在不解析正文的前提下查询。
 */
interface CaStubFileProvider {
    /** 返回 [file] 对应的 stub 种类;无 stub 时为 `null`。 */
    fun getFileStubKind(file: CjFile): CangJieFileStubKind?

    /** 文件内的顶级 classifier(class/interface/struct/enum/type alias)名称集合。 */
    fun getTopLevelClassifierNames(file: CjFile): Set<Name>

    /** 文件内的顶级 callable(function/property/main)名称集合。 */
    fun getTopLevelCallableNames(file: CjFile): Set<Name>
}
