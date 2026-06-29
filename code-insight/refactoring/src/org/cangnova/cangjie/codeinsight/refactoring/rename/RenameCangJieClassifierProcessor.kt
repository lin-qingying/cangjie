/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.codeinsight.refactoring.rename

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.psi.CjClassLikeDeclaration

/**
 * class / struct / interface / enum 等 classifier 声明的 rename processor。
 */
class RenameCangJieClassifierProcessor : RenameCangJiePsiProcessor() {
    /**
     * 只处理仓颉 class-like 声明。
     */
    override fun canProcessElement(element: PsiElement): Boolean = element is CjClassLikeDeclaration
}
