

package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve


import com.intellij.psi.PsiElement
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.containingClass
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
    is CjParameter -> elementCanBeLazilyResolved(element.ownerDeclaration)
    // 仓颉 low-level 主干不承载 Kotlin FIR 的 enum entry / dangling modifier list 形态。
    // 这里仅保留真实存在的可调用声明入口，避免把不存在的 declaration shape 带入 designation/file-structure 主流程。
    is CjCallableDeclaration -> {
        // 仓颉注解在 PSI 上会引入 CjMacroInput 包装层，lazy-resolve 需要把这层视为透明容器。
        val parentToCheck = when (val parent = unwrapMacroInputParent(element.parent)) {
            is CjTypeStatement, is CjFile -> parent
            is CjAbstractClassBody -> parent.containingClass
            else -> null
        }

        elementCanBeLazilyResolved(parentToCheck)
    }

    is CjPropertyAccessor -> elementCanBeLazilyResolved(element.property)
    is CjTypeStatement -> element.getClassId() != null
    is CjTypeAlias -> element.getClassId() != null
    !is CjNamedDeclaration -> false
    else -> errorWithAttachment("Unexpected ${element::class}") {
        withPsiEntry("declaration", element)
    }
}

/**
 * 如果父节点是宏输入包装层，则返回包装层的真实父节点。
 *
 * 仓颉注解和宏输入在 PSI 中可能额外包一层 [CjMacroInput]；lazy resolve 判断声明容器时需要把它视为透明节点。
 */
private fun unwrapMacroInputParent(parent: PsiElement?): PsiElement? {
    return when (parent) {
        is CjMacroInput -> parent.parent
        else -> parent
    }
}
