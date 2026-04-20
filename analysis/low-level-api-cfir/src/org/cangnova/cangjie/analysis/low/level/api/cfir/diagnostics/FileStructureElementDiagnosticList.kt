/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic

internal class FileStructureElementDiagnosticList(
    private val map: Map<PsiElement, List<CjPsiDiagnostic>>
) {
    fun diagnosticsFor(element: PsiElement): List<CjPsiDiagnostic> = map[element] ?: emptyList()

    inline fun forEach(action: (List<CjPsiDiagnostic>) -> Unit) = map.values.forEach(action)
}
