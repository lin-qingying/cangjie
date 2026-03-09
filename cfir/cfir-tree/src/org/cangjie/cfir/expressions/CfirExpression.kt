package org.cangjie.cfir.expressions

import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.types.ConeCangjieType
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * 表达式基类。所有 CFIR 表达式节点的抽象基类。
 *
 * 每个表达式都有一个可选的解析后类型（在 BODY_RESOLVE 阶段填充）。
 */
abstract class CfirExpression : CfirElement, CfirStatement {
    /** 表达式的解析后类型，在类型检查之后设置 */
    abstract var coneTypeOrNull: ConeCangjieType?
}

/**
 * 语句接口。表达式和声明都可以作为语句。
 */
interface CfirStatement : CfirElement
