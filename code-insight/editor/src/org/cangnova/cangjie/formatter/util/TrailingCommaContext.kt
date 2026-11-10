/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */



package org.cangnova.cangjie.formatter.util

import org.cangnova.cangjie.formatter.CangJieCodeStyleSettings
import org.cangnova.cangjie.psi.CjElement
import com.intellij.psi.PsiElement


/**
 * 描述某个 PSI 元素的尾逗号适用状态。
 */
class TrailingCommaContext private constructor(
    /** 被检查的 PSI 元素。 */
    val element: PsiElement,
    /** 元素当前的尾逗号状态。 */
    val state: TrailingCommaState
) {

    /**
     * 当状态适用时返回仓颉 PSI 元素。
     */
    val cjElement: CjElement get() = element as? CjElement ?: error("State is NOT_APPLICABLE")

    companion object {
        /**
         * 根据 PSI 元素计算尾逗号上下文。
         */
        fun create(element: PsiElement): TrailingCommaContext = TrailingCommaContext(
            element,
            TrailingCommaState.stateForElement(element),
        )
    }
}

/**
 * 判断尾逗号已经存在，或根据当前代码风格允许补充。
 */
fun TrailingCommaContext.commaExistsOrMayExist(settings: CangJieCodeStyleSettings): Boolean = when (state) {
    TrailingCommaState.EXISTS -> true
    TrailingCommaState.MISSING -> settings.addTrailingCommaIsAllowedFor(element)
    else -> false
}
