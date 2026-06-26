/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.utils.printer

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.psi.psiUtil.parents
import org.cangnova.cangjie.psi.psiUtil.parentsWithSelf

/**
 * 查找当前 PSI 元素最近的指定类型父节点。
 */
public inline fun <reified T : PsiElement> PsiElement.parentOfType(withSelf: Boolean = false): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, !withSelf)
}

/**
 * 按从近到远的顺序遍历当前 PSI 元素的指定类型父节点。
 */
public fun <T : PsiElement> PsiElement.parentsOfType(clazz: Class<out T>, withSelf: Boolean = true): Sequence<T> {
    return (if (withSelf) parentsWithSelf else parents).filterIsInstance(clazz)
}

/**
 * 使用 reified 类型参数遍历当前 PSI 元素的指定类型父节点。
 */
public inline fun <reified T : PsiElement> PsiElement.parentsOfType(withSelf: Boolean = true): Sequence<T> =
    parentsOfType(T::class.java, withSelf)
