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
import com.intellij.psi.PsiReference

/**
 * 仓颉 PSI 对 IntelliJ Symbol API 的直接接入。
 *
 * Kotlin 当前依赖的平台 target extraction 会直接消费 PSI 自身暴露的 declaration/reference，
 * 因此仓颉 PSI 也必须在这里补齐同一套入口，而不是把“光标命中了谁”的判断继续留给上层功能。
 */
internal fun PsiElement.cangJieOwnReferences(): Collection<PsiSymbolReference> {
    val references = references
    if (references.isNotEmpty()) {
        return references.asCangJieSymbolReferences()
    }

    val bridgedReferenceOwners = cangJieReferenceBridgeOwners()
    if (bridgedReferenceOwners.isEmpty()) {
        return emptyList()
    }

    return bridgedReferenceOwners.flatMap { owner ->
        owner.references.map { reference ->
            CangJiePsiSymbolReferenceBridge(
                host = this,
                delegate = reference,
            )
        }
    }
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

/**
 * type PSI 只是包了一层语法外壳，真正可解析的引用仍然挂在内部 simple-name 上。
 *
 * 平台 target extraction 按 offset 命中 `CjUserType` / `CjTypeReference` 时，
 * 如果这里只暴露当前 PSI 自身的 `references`，就会把 `Box` / `String`
 * 这类类型位引用直接漏掉。桥接统一留在 PSI Symbol 暴露层，
 * 避免 documentation / navigation / declaration target 各自再做一套 parent 特判。
 */
private fun PsiElement.cangJieReferenceBridgeOwners(): List<PsiElement> {
    return when (this) {
        is CjUserType -> listOfNotNull(referenceExpression)
        is CjTypeReference -> listOfNotNull((typeElement as? CjUserType)?.referenceExpression)
        else -> emptyList()
    }
}

private fun Array<PsiReference>.asCangJieSymbolReferences(): List<PsiSymbolReference> {
    val symbolService = PsiSymbolService.getInstance()
    return map(symbolService::asSymbolReference)
}

/**
 * 把子 PSI 上的真实引用提升为父 PSI 的 Symbol API 引用。
 *
 * 这样 offset 命中 `CjTypeReference` / `CjUserType` 时，
 * 平台仍能按父节点坐标提取到内部 simple-name 的 target。
 */
private class CangJiePsiSymbolReferenceBridge(
    private val host: PsiElement,
    private val delegate: PsiReference,
) : PsiSymbolReference {
    private val delegateReference = PsiSymbolService.getInstance().asSymbolReference(delegate)

    override fun getElement(): PsiElement = host

    override fun getRangeInElement(): TextRange {
        val shift = delegate.element.textRange.startOffset - host.textRange.startOffset
        return delegate.rangeInElement.shiftRight(shift)
    }

    override fun resolveReference(): Collection<Symbol> {
        return delegateReference.resolveReference()
    }
}
