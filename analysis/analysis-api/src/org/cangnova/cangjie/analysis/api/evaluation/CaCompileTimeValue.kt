package org.cangnova.cangjie.analysis.api.evaluation

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner

/**
 * 编译期常量值的根接口(密封)。
 *
 * - 把表达式经常量折叠/求值后的结果包装为稳定视图;
 * - 子接口区分标量、集合、tuple 等结构,供调用方按 sealed when 安全 narrow;
 * - 受 [CaLifetimeOwner] 约束,值对象不能逃逸 analyze 块。
 *
 * 对齐 Kotlin Analysis API 的 `KaCompileTimeValue`。
 */
sealed interface CaCompileTimeValue : CaLifetimeOwner {
    /** 该常量按仓颉源码语法渲染后的字符串形式(如 `1i8`、`"foo"`、`true`)。 */
    val renderedText: String
}
