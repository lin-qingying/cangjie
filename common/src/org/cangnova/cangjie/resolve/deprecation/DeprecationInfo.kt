/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.deprecation

/**
 * 对齐 Kotlin 编译器的弃用级别枚举。
 *
 * 该枚举是 CFIR deprecation 子系统的公共依赖，必须保持与上游相同的比较顺序：
 * `WARNING < ERROR < HIDDEN`。
 */
enum class DeprecationLevelValue {
    WARNING,
    ERROR,
    HIDDEN,
}
