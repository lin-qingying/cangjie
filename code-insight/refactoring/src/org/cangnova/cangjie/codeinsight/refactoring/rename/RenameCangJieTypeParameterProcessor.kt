/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.codeinsight.refactoring.rename

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.psi.CjTypeParameter

/**
 * 仓颉类型参数的 rename processor。
 */
class RenameCangJieTypeParameterProcessor : RenameCangJiePsiProcessor() {
    /**
     * 只处理仓颉类型参数声明。
     */
    override fun canProcessElement(element: PsiElement): Boolean = element is CjTypeParameter
}
