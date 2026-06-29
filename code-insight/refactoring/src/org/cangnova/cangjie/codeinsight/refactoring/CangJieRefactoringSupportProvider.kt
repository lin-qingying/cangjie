/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.codeinsight.refactoring

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.codeinsight.refactoring.safeDelete.canDeleteElement

/**
 * 仓颉语言在 IntelliJ refactoring 框架中的能力入口。
 *
 * 该类属于共享 `code-insight:refactoring`，插件层只负责 XML 注册，LSP 层也只能复用
 * 同一套 code-insight 重构能力，避免在宿主侧各自实现一份语言语义。
 */
class CangJieRefactoringSupportProvider : RefactoringSupportProvider() {
    /**
     * 根据仓颉 PSI 语义判断元素是否支持 safe delete。
     */
    override fun isSafeDeleteAvailable(element: PsiElement): Boolean = element.canDeleteElement()

    /**
     * 与 Kotlin 一致：仓颉自己注册 rename handler/processor，禁用平台默认 inplace rename。
     */
    override fun isInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean = false

    /**
     * 与 Kotlin 一致：成员 rename 也交给语言级 processor，避免平台 Java member 路径介入。
     */
    override fun isMemberInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean = false
}
