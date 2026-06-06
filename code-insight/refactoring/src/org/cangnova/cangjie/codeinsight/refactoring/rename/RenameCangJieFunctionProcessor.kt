/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.codeinsight.refactoring.rename

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.psi.CjNamedFunction

/**
 * 仓颉函数声明的 rename processor。
 */
class RenameCangJieFunctionProcessor : RenameCangJiePsiProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean = element is CjNamedFunction
}
