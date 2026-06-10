package org.cangnova.cangjie.cfir.semantics

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.name.Name

abstract class AbstractCallInfo {
    abstract val callSite: CfirElement
    abstract val name: Name
    abstract val isImplicitInvoke: Boolean
    /**
     * 当前调用语法上是否携带显式类型实参。
     *
     * 诊断映射层只能依赖语义层抽象，不能反向读取 resolve 模块的具体
     * `CallInfo.typeArguments`。该标记用于区分显式泛型调用中的参数错误
     * 和真正的隐式泛型推断失败，避免把具体参数类型错误泛化为推断错误。
     */
    abstract val hasExplicitTypeArguments: Boolean
    abstract val explicitReceiver: CfirExpression?
    abstract val arguments: List<CfirExpression>
}
