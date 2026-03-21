package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * 表达式解析模式，用于控制类型合成方向。
 * 参考仓颉/C++ 编译器里的 Synthesize / Check 双向类型检查模型，
 * 同时对齐 K2 的 `ResolutionMode`。
 * - [ContextIndependent]：自底向上推断（Synthesize），无期望类型
 * - [WithExpectedType]：自顶向下校验（Check），带期望类型
 */
sealed class CfirResolutionMode {

    /** 自底向上推断，不带期望类型，纯粹从表达式本身合成类型。 */
    object ContextIndependent : CfirResolutionMode()

    /** 自顶向下校验，带期望类型，用于类型检查和隐式转换。 */
    class WithExpectedType(val expectedType: ConeCangJieType) : CfirResolutionMode()
}

