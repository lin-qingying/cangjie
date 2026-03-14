package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.Name

/**
 * 调用信息，封装一次函数调用/构造器调用/变量访问的所有上下文信息。
 *
 * 在 Tower 遍历前构建，传递给候选收集和验证管线。
 *
 * 对齐 K2 CallInfo，去掉 invoke/SAM/callable-reference 等 Kotlin 特有字段。
 */
class CfirCallInfo(
    /** 调用站点 AST 节点 */
    val callSite: CfirElement,
    /** 调用种类（函数/变量访问/构造器） */
    val callKind: CfirCallKind,
    /** 被调用名称 */
    val name: Name,
    /** 显式接收者表达式（可选） */
    val explicitReceiver: CfirExpression?,
    /** 实参列表 */
    val arguments: List<CfirExpression>,
    /** 显式类型实参列表 */
    val typeArguments: List<CfirTypeRef>,
    /** 编译器 session */
    val session: CfirSession,
)
