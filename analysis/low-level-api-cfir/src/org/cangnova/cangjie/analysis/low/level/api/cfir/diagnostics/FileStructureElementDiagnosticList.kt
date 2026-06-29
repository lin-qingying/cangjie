/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic

/**
 * 单个文件结构元素收集到的 diagnostics 索引。
 */
internal class FileStructureElementDiagnosticList(
    /**
     * PSI 元素到其 diagnostics 列表的映射。
     */
    private val map: Map<PsiElement, List<CjPsiDiagnostic>>
) {
    /**
     * 返回指定 PSI 元素直接关联的 diagnostics。
     */
    fun diagnosticsFor(element: PsiElement): List<CjPsiDiagnostic> = map[element] ?: emptyList()

    /**
     * 遍历该结构元素下所有 PSI 位置的 diagnostics 列表。
     */
    inline fun forEach(action: (List<CjPsiDiagnostic>) -> Unit) = map.values.forEach(action)
}
