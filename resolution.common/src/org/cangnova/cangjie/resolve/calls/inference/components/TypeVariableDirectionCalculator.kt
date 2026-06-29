/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.inference.components

/**
 * 计算类型变量在约束系统中解析方向的工具入口。
 */
object TypeVariableDirectionCalculator {
    /**
     * 类型变量可被固定到的方向。
     */
    enum class ResolveDirection {
        /** 固定为当前约束的子类型方向。 */
        TO_SUBTYPE,
        /** 固定为当前约束的父类型方向。 */
        TO_SUPERTYPE,
        /** 当前约束不足以确定固定方向。 */
        UNKNOWN
    }
}
