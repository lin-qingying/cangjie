/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.codeinsight.refactoring.safeDelete

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjImportAlias
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjPropertyAccessor

/**
 * Safe delete 的语言级可删除声明判定。
 *
 * 对齐 Kotlin `canDeleteElement()` 的 owner 边界：只有真正可独立命名并可被引用的
 * 声明才暴露 safe delete，属性访问器参数不作为独立删除目标。
 */
fun PsiElement.canDeleteElement(): Boolean {
    if (this is CjParameter) {
        val accessor = parent?.parent as? CjPropertyAccessor
        return accessor == null
    }

    return this is CjNamedDeclaration ||
        this is CjEnumConstructor ||
        this is CjImportAlias
}
