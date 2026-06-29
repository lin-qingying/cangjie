package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.utils.asCaType
import org.cangnova.cangjie.analysis.api.impl.base.annotations.CaBaseEmptyAnnotationList
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.types.CaTypeProjection
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.renderForDebugging

/**
 * CFIR public type 叶子的最小共享协议。
 *
 * 这里对齐 Kotlin `KaFirType`：只暴露底层 `ConeCangJieType`
 * 与当前 `CaCfirSession`，不再引入额外的“通用实现基类”。
 */
internal interface CaCfirType : CaLifetimeOwner {

    /**
     * 该公开类型包装的底层 CFIR Cone 类型。
     */
    val coneType: ConeCangJieType


}

/**
 * 按底层 Cone 类型比较两个 CFIR 公开类型。
 */
internal fun CaCfirType.typeEquals(other: Any?): Boolean {
    if (other !is CaCfirType) return false
    return coneType == other.coneType
}

/**
 * 返回底层 Cone 类型的哈希码。
 */
internal fun CaCfirType.typeHashcode(): Int = coneType.hashCode()

/**
 * 已解析 class-like qualifier 片段。
 */
internal class CaCfirResolvedClassTypeQualifierImpl(
    /**
     * qualifier 片段名称。
     */
    override val name: org.cangnova.cangjie.name.Name,
    /**
     * qualifier 片段携带的公开类型实参。
     */
    override val typeArguments: List<CaTypeProjection>,
    /**
     * qualifier 片段解析到的公开 classifier 符号。
     */
    override val symbol: org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol,
    /**
     * qualifier 片段所属生命周期令牌。
     */
    override val token: CaLifetimeToken,
) : org.cangnova.cangjie.analysis.api.types.CaResolvedClassTypeQualifier

/**
 * 未解析 class-like qualifier 片段。
 */
internal class CaCfirUnresolvedClassTypeQualifierImpl(
    /**
     * qualifier 片段名称。
     */
    override val name: org.cangnova.cangjie.name.Name,
    /**
     * qualifier 片段携带的公开类型实参。
     */
    override val typeArguments: List<CaTypeProjection>,
    /**
     * qualifier 片段所属生命周期令牌。
     */
    override val token: CaLifetimeToken,
) : org.cangnova.cangjie.analysis.api.types.CaUnresolvedClassTypeQualifier



/**
 * 创建不含任何类型注解的公开注解列表。
 */
internal fun emptyTypeAnnotations(token: CaLifetimeToken): CaAnnotationList = CaBaseEmptyAnnotationList(token)
