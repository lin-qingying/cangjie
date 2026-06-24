package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.name.Name

/**
 * 返回 CFIR 函数在 callable 索引中使用的名称。
 *
 * 普通具名函数直接使用声明名，main 函数归一化为 `main`，其他函数形态不进入具名 callable 索引。
 */
fun CfirFunction.callableNameOrNull(): Name? = when (this) {
    is CfirNamedFunction -> name
    is CfirMainFunction -> Name.identifier("main")
    else -> null
}
