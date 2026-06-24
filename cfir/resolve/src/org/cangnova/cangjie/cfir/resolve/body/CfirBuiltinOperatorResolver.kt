package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperatorMatch
import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperators
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.Name

/** 内建基础类型运算符的调用解析入口。 */
object CfirBuiltinOperatorResolver {
    /** 尝试按名称、接收者类型和参数类型匹配内建基础类型运算符。 */
    fun tryResolveBuiltinOperator(
        name: Name,
        receiverType: ConeCangJieType?,
        argumentTypes: List<ConeCangJieType>,
    ): BuiltinPrimitiveOperatorMatch? =
        BuiltinPrimitiveOperators.resolve(name, receiverType, argumentTypes)
}
