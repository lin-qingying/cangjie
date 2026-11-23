package org.cangnova.cangjie.idea.references

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocName
import org.cangnova.cangjie.psi.cangJieCanonicalTargets
import org.cangnova.cangjie.psi.CjBasicType
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjSuperTypeCallEntry
import org.cangnova.cangjie.psi.CjValueArgumentName
import org.cangnova.cangjie.utils.firstIsInstance
import org.cangnova.cangjie.utils.firstIsInstanceOrNull

/**
 * 对齐 Kotlin `idea.references.referenceUtils` 的统一引用入口。
 *
 * 上层 analysis / usages / rename / target extraction 不应自行猜“这个 PSI 应该取哪个 reference”，
 * 而应通过这里拿到当前元素的主引用。
 */
/**
 * simple-name 表达式上的主仓颉 simple-name reference。
 */
val CjSimpleNameExpression.mainReference: CjSimpleNameReference
    get() = references.firstIsInstance()

/**
 * 普通引用表达式上的主仓颉 reference。
 */
val CjReferenceExpression.mainReference: CjReference?
    get() = when (this) {
        // 声明名同样使用 simple-name PSI，但没有使用处 reference；可空入口必须保留这种正常缺失。
        is CjSimpleNameExpression -> references.firstIsInstanceOrNull<CjSimpleNameReference>()
        is CjCallExpression -> (calleeExpression as? CjReferenceExpression)?.mainReference
        else -> references.firstIsInstanceOrNull()
    }

/**
 * 基础类型节点上的主 PSI reference。
 */
val CjBasicType.mainReference: PsiReference?
    get() = references.firstOrNull()

/**
 * 值参数名称节点上的主 PSI reference。
 */
val CjValueArgumentName.mainReference: PsiReference?
    get() = references.firstOrNull()

/**
 * super type call entry 上的主 PSI reference。
 */
val CjSuperTypeCallEntry.mainReference: PsiReference?
    get() = references.firstOrNull()

/**
 * 任意仓颉 PSI 元素上的主仓颉 reference。
 */
val CjElement.mainReference: CjReference?
    get() = when (this) {
        is CjReferenceExpression -> mainReference
        is CDocName -> mainReference
        else -> references.firstIsInstanceOrNull()
    }

/**
 * CDoc 名称节点上的主 CDoc reference。
 */
val CDocName.mainReference: CDocReference
    get() = references.firstIsInstance()


/**
 * 解包 reference 的解析目标集合。
 */
val PsiReference.unwrappedTargets: Set<PsiElement>
    get() = cangJieCanonicalTargets()
