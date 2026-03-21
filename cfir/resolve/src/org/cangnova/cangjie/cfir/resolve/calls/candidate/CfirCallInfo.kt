package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.semantics.AbstractCallInfo
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.Name

/**
 * 调用信息，封装一次函数调用、构造器调用或变量访问所需的上下文。
 * 它会在 tower 遍历前构建完成，并传递给候选收集和验证管线。
 * 对齐 K2 `CallInfo`，去掉 `invoke`、`SAM`、callable reference 等 Kotlin 特有字段。
 */
class CfirCallInfo(
    /** 调用站点对应的 AST 节点。 */
    override val callSite: CfirElement,
    /** 调用种类，如函数、变量访问或构造器调用。 */
    val callKind: CfirCallKind,
    /** 被调用的名称。 */
    override val name: Name,
    /** 显式接收者表达式，可为空。 */
    override val explicitReceiver: CfirExpression?,
    /** 实参列表。 */
    override val arguments: List<CfirExpression>,
    /** 显式类型实参列表。 */
    val typeArguments: List<CfirTypeRef>,
    /** 编译器会话。 */
    val session: CfirSession,
) : AbstractCallInfo() {
    override val isImplicitInvoke: Boolean = false
}

