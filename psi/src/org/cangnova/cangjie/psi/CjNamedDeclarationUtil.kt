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

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.FqNameUnsafe

/**
 * 提供 `CjNamedDeclarationUtil` 单例，集中承载仓颉 PSI的共享状态、工厂或工具行为。
 */
object CjNamedDeclarationUtil {
    /**
     * 提供 `getUnsafeFQName` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getUnsafeFQName(namedDeclaration: CjNamedDeclaration): FqNameUnsafe? {
        val fqName = namedDeclaration.fqName
        return fqName?.toUnsafe()
    }

    /**
     * 提供 `getFQName` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getFQName(namedDeclaration: CjNamedDeclaration): FqName? {
        val name = namedDeclaration.nameAsName ?: return null

        val parentFqName = getParentFqName(namedDeclaration) ?: return null

        return parentFqName.child(name)
    }

    /**
     * 提供 `getParentFqName` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getParentFqName(namedDeclaration: CjNamedDeclaration): FqName? {
        var parent = namedDeclaration.parent
        if (parent is CjAbstractClassBody) {
            parent = parent.getParent()
        }

        if (parent is CjFile) {
            return parent.packageFqName
        } else if (namedDeclaration is CjParameter) {
            val constructorClass = CjPsiUtil.getClassIfParameterIsProperty(namedDeclaration)
            if (constructorClass != null) {
                return getFQName(constructorClass)
            }
        } 
        else if (parent is CjExtend) {
            return getParentFqName(parent)
        }

        return null
    }
}
