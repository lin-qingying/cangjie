package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.name.Name

fun CfirFunction.callableNameOrNull(): Name? = when (this) {
    is CfirNamedFunction -> name
    is CfirMainFunction -> Name.identifier("main")
    else -> null
}
