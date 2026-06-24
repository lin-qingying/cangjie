package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.resolve.DefaultImportsProvider

/**
 * 仓颉前端 session 使用的默认导入 provider。
 *
 * 语言级默认导入定义在 [DefaultImportsProvider]；当前统一仓颉流水线没有平台特定默认导入。
 */
object CfirDefaultImportsProvider : DefaultImportsProvider() {
    /**
     * 平台特定默认导入列表；仓颉统一前端当前为空。
     */
    override val platformSpecificDefaultImports: List<ImportPath> = emptyList()
}
