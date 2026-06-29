/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package org.cangnova.cangjie.generators.tree

/**
 * 类型参数的型变标注。
 * 移植自 Kotlin 编译器 org.jetbrains.kotlin.types.Variance，
 * 仅供 tree generator 基础设施使用。
 */
enum class Variance(val label: String, val allowsInPosition: Boolean, val allowsOutPosition: Boolean) {
    INVARIANT("", true, true),
    IN_VARIANCE("in", true, false),
    OUT_VARIANCE("out", false, true);

    /**
     * 返回相反方向的型变；不变型保持不变。
     */
    fun opposite(): Variance = when (this) {
        INVARIANT -> INVARIANT
        IN_VARIANCE -> OUT_VARIANCE
        OUT_VARIANCE -> IN_VARIANCE
    }
}
