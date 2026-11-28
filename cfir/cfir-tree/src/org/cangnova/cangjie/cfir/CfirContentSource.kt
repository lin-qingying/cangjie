package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAnnotationInfo
import org.cangnova.cangjie.cfir.declarations.CfirInteropInfo
import org.cangnova.cangjie.cfir.declarations.frozenCopy
import org.cangnova.cangjie.cfir.declarations.DeprecationsProvider
import org.cangnova.cangjie.cfir.declarations.UnresolvedDeprecationProvider
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression

/**
 * 声明内容的数据源，在真实声明首次构造时固定。
 * Owned 使用声明自己的正常字段；Published 仅提供已完成快照，任何读取都不得解码或开启事务。
 */
sealed interface CfirContentSource {
    data object Owned : CfirContentSource

    /** 同一实际 Package 的稳定数据源；一次 snapshot 调用只观察一个完成代。 */
    interface Published : CfirContentSource {
        fun snapshot(): CfirPublishedContentSnapshot
    }
}

/** 被动完成视图；缺失只表示该快照没有选择并完成对应内容，不伪造 body 或 initializer。 */
interface CfirPublishedContentSnapshot {
    fun body(owner: CfirFunctionBodyOwner): CfirBlock?
    fun initializer(owner: CfirVariable): CfirExpression?
    fun defaultValue(owner: CfirValueParameter): CfirExpression?
    fun annotations(owner: CfirDeclaration): CfirPublishedAnnotations
    fun deprecations(owner: CfirDeclaration): DeprecationsProvider = annotations(owner).deprecationsProvider
}

/** 未选择与已完成空注解分开；被动列表视图不能被用来推断保存槽或完成资格。 */
sealed interface CfirPublishedAnnotations {
    val values: MutableOrEmptyList<CfirAnnotation>
    val deprecationsProvider: DeprecationsProvider

    data object Unselected : CfirPublishedAnnotations {
        override val values: MutableOrEmptyList<CfirAnnotation> = MutableOrEmptyList.empty()
        override val deprecationsProvider: DeprecationsProvider = UnresolvedDeprecationProvider
    }

    class Complete(
        annotations: List<CfirAnnotation>,
        override val deprecationsProvider: DeprecationsProvider = org.cangnova.cangjie.cfir.declarations.buildDeprecationsProvider(annotations),
        annotationInfo: CfirDeclarationAnnotationInfo? = null,
        interopInfo: CfirInteropInfo? = null,
    ) : CfirPublishedAnnotations {
        val annotationInfo: CfirDeclarationAnnotationInfo? = annotationInfo?.frozenCopy()
        val interopInfo: CfirInteropInfo? = interopInfo?.frozenCopy()
        override val values: MutableOrEmptyList<CfirAnnotation> = if (annotations.isEmpty()) MutableOrEmptyList.empty()
            else MutableOrEmptyList(java.util.Collections.unmodifiableList(ArrayList(annotations)))
    }
}
