/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.codeinsight.refactoring.rename

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.psi.CjCatchParameter
import org.cangnova.cangjie.psi.CjParameter

/**
 * 仓颉值参数与 catch 参数的 rename processor。
 */
class RenameCangJieParameterProcessor : RenameCangJiePsiProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean =
        element.originalElement is CjParameter || element.originalElement is CjCatchParameter

    override fun substituteElementToRename(element: PsiElement, editor: com.intellij.openapi.editor.Editor?): PsiElement? =
        element.originalElement
}
