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

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiNameIdentifierOwner
import org.cangnova.cangjie.name.FqName

/**
 * 表示 `CangJieReferenceProvidersService`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
open class CangJieReferenceProvidersService {
    /**
     * 提供 `getReferences` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun getReferences(psiElement: PsiElement): Array<PsiReference> = PsiReference.EMPTY_ARRAY

    /**
     * 返回声明名称的规范目标。模式名称需要语义核验，PSI 模块本身不发布未确认的 binder。
     * 实现不得调用 references 或 getOwnDeclarations，以免声明发布与引用分派递归。
     */
    open fun getNamedDeclarationTarget(psiElement: PsiElement): PsiNameIdentifierOwner? =
        if (psiElement is CjNamedPattern || psiElement is CjCatchParameter ||
            psiElement is CjCommandTypePattern || psiElement is CjTryResource
        ) null else psiElement as? PsiNameIdentifierOwner

    /** 解析声明的正式全限定身份；局部或没有成功声明身份的节点返回 null。 */
    open fun getDeclarationFqName(declaration: PsiNameIdentifierOwner): FqName? = null

    companion object {
        private val NO_REFERENCES_SERVICE = CangJieReferenceProvidersService()

        @JvmStatic
        fun getInstance(project: Project): CangJieReferenceProvidersService {
            return project.getService(CangJieReferenceProvidersService::class.java) ?: NO_REFERENCES_SERVICE
        }

        @JvmStatic
        fun getReferencesFromProviders(psiElement: PsiElement): Array<PsiReference> {
            return getInstance(psiElement.project).getReferences(psiElement)
        }
    }
}
