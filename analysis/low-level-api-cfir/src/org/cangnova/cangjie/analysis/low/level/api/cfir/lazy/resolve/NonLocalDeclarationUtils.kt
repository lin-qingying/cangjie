/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve


import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.parentOfType
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry

/**
 * Note: The `CjCodeFragment` itself is technically lazy-resolvable, but the function doesn't support it yet.
 */
internal fun elementCanBeLazilyResolved(element: CjElement?): Boolean = when (element) {
    null -> false
    is CjFunctionLiteral -> false
    is CjTypeParameter -> elementCanBeLazilyResolved(element.parentOfType<CjNamedDeclaration>(withSelf = false))
    is CjFile -> element !is CjCodeFragment
    is CjParameter -> elementCanBeLazilyResolved(element.ownerFunction)
    // 仓颉 low-level 主干不承载 Kotlin FIR 的 enum entry / dangling modifier list 形态。
    // 这里仅保留真实存在的可调用声明入口，避免把不存在的 declaration shape 带入 designation/file-structure 主流程。
    is CjCallableDeclaration -> {
        val parentToCheck = when (val parent = element.parent) {
            is CjTypeStatement, is CjFile -> parent
            is CjClassBody -> parent.parent as? CjTypeStatement
            else -> null
        }

        elementCanBeLazilyResolved(parentToCheck)
    }

    is CjPropertyAccessor -> elementCanBeLazilyResolved(element.property)
    is CjTypeStatement -> element.parent is CjFile
    is CjTypeAlias -> element.parent is CjFile
    !is CjNamedDeclaration -> false
    else -> errorWithAttachment("Unexpected ${element::class}") {
        withPsiEntry("declaration", element)
    }
}
