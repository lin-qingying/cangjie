/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.codeinsight.refactoring.rename

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjProperty

/**
 * 仓颉属性与字段声明的 rename processor。
 */
class RenameCangJiePropertyProcessor : RenameCangJiePsiProcessor() {
    /**
     * 处理仓颉属性声明和字段变量声明。
     */
    override fun canProcessElement(element: PsiElement): Boolean =
        element is CjProperty || element is CjFieldVariable
}
