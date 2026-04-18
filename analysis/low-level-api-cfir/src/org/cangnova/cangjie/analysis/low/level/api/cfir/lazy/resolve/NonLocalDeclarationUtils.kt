/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import org.cangnova.cangjie.analysis.utils.printer.parentOfType
import org.cangnova.cangjie.psi.*
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
    is CjParameter -> elementCanBeLazilyResolved(element.ownerDeclaration)
    is CjCallableDeclaration, is CjEnumEntry -> {
        val parentToCheck = when (val parent = element.parent) {
            is CjClassOrObject, is CjFile -> parent
            is CjClassBody -> parent.parent as? CjClassOrObject
            else -> null
        }

        elementCanBeLazilyResolved(parentToCheck.takeUnless { it is CjEnumEntry })
    }

    is CjPropertyAccessor -> elementCanBeLazilyResolved(element.property)
    is CjClassOrObject -> element.isTopLevel() || element.getClassId() != null
    is CjTypeAlias -> element.isTopLevel() || element.getClassId() != null
    is CjModifierList -> element.isNonLocalDanglingModifierList()
    !is CjNamedDeclaration -> false
    else -> errorWithAttachment("Unexpected ${element::class}") {
        withPsiEntry("declaration", element)
    }
}

/**
 * Detects a common pattern of invalid code where a modifier list (e.g., annotation)
 * is dangling—unattached to a valid declaration—or left unclosed and followed by another declaration.
 *
 * ### Examples
 *
 * ```kotlin
 * class C1 {
 *     @Ann1 @Ann2
 * }
 *
 * class C2 {
 *     @Ann(
 *     fun foo() {}
 * }
 *
 * @Ann("argument"
 * fun foo() {}
 * ```
 * @see org.cangnova.cangjie.cfir.declarations.CfirDanglingModifierList
 * @see org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder.Visitor.buildErrorNonLocalDeclarationForDanglingModifierList
 */
private fun CjModifierList.isNonLocalDanglingModifierList(): Boolean {
    val parentToCheck = when (val parent = parent) {
        is CjFile -> parent
        is CjClassBody -> (parent.parent as? CjClassOrObject).takeUnless { it is CjEnumEntry }
        else -> null
    }

    return elementCanBeLazilyResolved(parentToCheck)
}
