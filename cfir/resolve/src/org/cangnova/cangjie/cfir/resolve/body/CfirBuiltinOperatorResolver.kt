package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperatorMatch
import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperators
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.Name

object CfirBuiltinOperatorResolver {
    fun tryResolveBuiltinOperator(
        name: Name,
        receiverType: ConeCangJieType?,
        argumentTypes: List<ConeCangJieType>,
    ): BuiltinPrimitiveOperatorMatch? =
        BuiltinPrimitiveOperators.resolve(name, receiverType, argumentTypes)
}
