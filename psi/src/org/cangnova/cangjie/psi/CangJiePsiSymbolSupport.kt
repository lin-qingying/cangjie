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

import com.intellij.model.Symbol
import com.intellij.model.psi.PsiSymbolDeclaration
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.model.psi.PsiSymbolService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

/**
 * 仓颉 PSI 对 IntelliJ Symbol API 的直接接入。
 *
 * Kotlin 当前依赖的平台 target extraction 会直接消费 PSI 自身暴露的 declaration/reference，
 * 因此仓颉 PSI 也必须在这里补齐同一套入口，而不是把“光标命中了谁”的判断继续留给上层功能。
 */
internal fun PsiElement.cangJieOwnReferences(): Collection<PsiSymbolReference> {
    val references = references
    if (references.isEmpty()) {
        return emptyList()
    }

    val symbolService = PsiSymbolService.getInstance()
    return references.map(symbolService::asSymbolReference)
}

internal fun PsiElement.cangJieOwnDeclarations(): Collection<PsiSymbolDeclaration> {
    val declaration = this as? PsiNameIdentifierOwner ?: return emptyList()
    val nameIdentifier = declaration.nameIdentifier ?: return emptyList()
    return listOf(CangJiePsiSymbolDeclaration(declaration, nameIdentifier))
}

/**
 * 声明 target 的范围必须精确锚定到名字 token。
 *
 * 这样平台在按 offset 抽取 declaration target 时，只有真的命中声明名才会返回结果，
 * 不会在函数体空白、关键字或块边界上错误地把整个外层声明当成 target。
 */
private class CangJiePsiSymbolDeclaration(
    private val declaration: PsiNameIdentifierOwner,
    private val nameIdentifier: PsiElement,
) : PsiSymbolDeclaration {
    override fun getDeclaringElement(): PsiElement = declaration

    override fun getRangeInDeclaringElement(): TextRange {
        return nameIdentifier.textRange.shiftRight(-declaration.textRange.startOffset)
    }

    override fun getSymbol(): Symbol {
        return PsiSymbolService.getInstance().asSymbol(declaration)
    }
}
