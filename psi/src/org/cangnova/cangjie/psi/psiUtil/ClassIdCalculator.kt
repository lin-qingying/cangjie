/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.psi.psiUtil

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjFile

internal object ClassIdCalculator {
    /**
     * 仅为顶层 class-like 声明计算 `ClassId`。
     *
     * 一旦声明外层仍存在其他 class-like 容器，就直接返回 `null`，
     * 防止 PSI 回退路径重新构造出不应进入公开类型标识体系的声明。
     */
    fun calculateClassId(declaration: CjClassLikeDeclaration): ClassId? {
        var cjFile: CjFile? = null
        var seenSelf = false

        for (element in declaration.parentsWithSelf) {
            when (element) {
                is CjClassLikeDeclaration -> {
                    if (!seenSelf) {
                        seenSelf = true
                    } else {
                        return null
                    }
                }

                is CjFile -> {
                    cjFile = element
                    break
                }
            }
        }

        val file = cjFile ?: return null
        val className = declaration.nameAsSafeName
        return ClassId(file.packageFqName, className)
    }
}
