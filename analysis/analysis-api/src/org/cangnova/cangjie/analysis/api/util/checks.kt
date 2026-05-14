package org.cangnova.cangjie.analysis.api.util

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * 断言 [obj] 必须是 [T] 类型,否则抛出 [IllegalArgumentException]。
 *
 * 借助 Kotlin contracts 在返回后让编译器把 [obj] 自动 smart cast 为 [T];
 * 用于校验入参契约(典型场景:外部传入的 Any 经过校验后转交内部强类型逻辑)。
 */
@OptIn(ExperimentalContracts::class)
inline fun <reified T> requireIsInstance(obj: Any) {
    contract {
        returns() implies (obj is T)
    }
    require(obj is T) { "Expected ${T::class} instead of ${obj::class} for $obj" }
}

/**
 * 断言 [obj] 必须是 [T] 类型,否则抛出 [IllegalStateException]。
 *
 * 语义与 [requireIsInstance] 类似,但用于校验内部不变量(违例属于编程错误),
 * 异常类型也对应 `check` 的语义。
 */
@OptIn(ExperimentalContracts::class)
inline fun <reified T> checkIsInstance(obj: Any) {
    contract {
        returns() implies (obj is T)
    }
    check(obj is T) { "Expected ${T::class} instead of ${obj::class} for $obj" }
}