/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.components

/**
 * 调用解析中是否允许 suspend 函数类型转换的策略。
 */
enum class SuspendConversionStrategy {
    /** 允许在普通/挂起函数类型之间执行 suspend conversion。 */
    SUSPEND_CONVERSION,
    /** 禁止 suspend conversion，候选按原始函数类型匹配。 */
    NO_CONVERSION,
}
