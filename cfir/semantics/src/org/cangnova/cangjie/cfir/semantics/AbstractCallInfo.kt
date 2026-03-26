package org.cangnova.cangjie.cfir.semantics

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.name.Name

abstract class AbstractCallInfo {
    abstract val callSite: CfirElement
    abstract val name: Name
    abstract val isImplicitInvoke: Boolean
    abstract val explicitReceiver: CfirExpression?
    abstract val arguments: List<CfirExpression>
}

