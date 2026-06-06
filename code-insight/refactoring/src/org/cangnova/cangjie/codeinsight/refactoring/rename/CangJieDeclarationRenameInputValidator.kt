/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.codeinsight.refactoring.rename

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenameInputValidator
import com.intellij.util.ProcessingContext
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.psiUtil.isIdentifier
import org.cangnova.cangjie.psi.psiUtil.quoteIfNeeded

/**
 * 仓颉命名声明的 rename 输入校验器。
 *
 * 对齐 Kotlin `KotlinDeclarationRenameInputValidator`：允许普通标识符，也允许可被原始
 * 标识符语法包裹后成立的名称，具体合法性仍交给 PSI lexer 判断。
 */
class CangJieDeclarationRenameInputValidator : RenameInputValidator {
    override fun getPattern() = PlatformPatterns.psiElement(CjNamedDeclaration::class.java)

    override fun isInputValid(newName: String, element: PsiElement, context: ProcessingContext): Boolean =
        newName.quoteIfNeeded().isIdentifier()
}
