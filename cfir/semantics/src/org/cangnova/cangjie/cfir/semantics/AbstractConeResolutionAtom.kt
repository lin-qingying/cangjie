package org.cangnova.cangjie.cfir.semantics

import org.cangnova.cangjie.cfir.expressions.CfirExpression

/**
 * 调用解析中的表达式原子抽象。
 *
 * 候选参数映射和接收者选择只需要拿到对应表达式，不依赖 resolve 模块内部的具体原子实现。
 */
abstract class AbstractConeResolutionAtom {
    /** 该解析原子对应的 CFIR 表达式。 */
    abstract val expression: CfirExpression
}
