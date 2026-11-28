package org.cangnova.cangjie.cfir.declarations.utils

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationDataRegistry
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.builder.CfirTypeParameterBuilder
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.symbols.toLookupTag

internal object ImplicitDefaultUpperBoundKey : CfirDeclarationDataKey()

private var CfirDeclarationAttributes.hasImplicitDefaultUpperBound: Boolean? by
    CfirDeclarationDataRegistry.attributesAccessor(ImplicitDefaultUpperBoundKey)

/**
 * 记录类型参数的 Any 上界是否由 CFIR 语义模型补入，而不是来自声明/CJO。
 * 该事实随 attributes 副本传播，供序列化边界只写显式约束。
 */
var CfirTypeParameter.hasImplicitDefaultUpperBound: Boolean
    get() = attributes.hasImplicitDefaultUpperBound == true
    set(value) {
        attributes.hasImplicitDefaultUpperBound = value
    }

/**
 * 返回声明或 CJO 显式携带的上界。
 * 默认 Any 的 source 为空；CFIR 可能在显式 where 上界之后补入它，因此不能靠列表位置识别。
 */
val CfirTypeParameter.explicitUpperBounds
    get(): List<CfirTypeRef> {
        if (!hasImplicitDefaultUpperBound && origin != CfirDeclarationOrigin.Source) return bounds
        val implicitDefaultBoundIndices = bounds.indices.filter { index ->
            bounds[index].isImplicitDefaultAnyBound()
        }
        if (implicitDefaultBoundIndices.isEmpty()) {
            check(!hasImplicitDefaultUpperBound) {
                "A type parameter marked with an implicit default upper bound must contain an implicit std.core.Any ref"
            }
            return bounds
        }
        check(implicitDefaultBoundIndices.size == 1) {
            "A type parameter must contain at most one implicit std.core.Any upper bound"
        }
        val implicitDefaultBoundIndex = implicitDefaultBoundIndices.single()
        return bounds.filterIndexed { index, _ -> index != implicitDefaultBoundIndex }
    }

private fun isDefaultAnyBound(bound: CfirTypeRef): Boolean =
    (bound as? CfirResolvedTypeRef)?.coneType.let { coneType ->
        (coneType as? ConeClassLikeType)?.classId == StdlibClassIds.Any
    }

private fun CfirTypeRef.isImplicitDefaultAnyBound(): Boolean =
    source == null && isDefaultAnyBound(this)

/**
 * 对齐 Kotlin `FirTypeParameterBuilder.addDefaultBoundIfNecessary` 的职责：
 * 当类型参数未声明任何显式上界时，补上仓颉默认顶层约束 `std.core.Any`，
 * 确保 RAW_CFIR 构建结束后 `bounds` 至少包含一个 `CfirResolvedTypeRef`。
 *
 * 该工具同时服务于源码 raw-cfir 构建（PSI / LightTree）与反序列化路径，
 * 避免 TYPES 阶段或下游 checker 撞上 `CfirImplicitTypeRefImpl` cast 失败。
 */
fun CfirTypeParameterBuilder.addDefaultBoundIfNecessary() {
    if (bounds.isNotEmpty()) {
        attributes.hasImplicitDefaultUpperBound = false
        return
    }

    val defaultBound = ConeClassLikeType(StdlibClassIds.Any.toLookupTag(), isInterface = true)
    bounds += defaultBound.toCfirResolvedTypeRef()
    attributes.hasImplicitDefaultUpperBound = true
}

/**
 * 将 CJO 中显式约束反序列化后的类型参数补齐到 CFIR 统一语义形状。
 * 空约束列表表示官方 CJO 未写上界，必须在语义层恢复默认 Any。
 */
fun CfirTypeParameter.addDefaultBoundIfNecessary() {
    if (hasImplicitDefaultUpperBound) {
        check(bounds.any(::isDefaultAnyBound)) {
            "A type parameter marked with an implicit default upper bound must contain std.core.Any"
        }
        return
    }
    if (bounds.isNotEmpty()) {
        hasImplicitDefaultUpperBound = false
        return
    }

    replaceBounds(listOf(ConeClassLikeType(StdlibClassIds.Any.toLookupTag(), isInterface = true).toCfirResolvedTypeRef()))
    hasImplicitDefaultUpperBound = true
}
