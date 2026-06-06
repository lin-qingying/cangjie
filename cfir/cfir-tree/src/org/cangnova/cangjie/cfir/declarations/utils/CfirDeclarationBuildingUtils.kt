package org.cangnova.cangjie.cfir.declarations.utils

import org.cangnova.cangjie.cfir.declarations.builder.CfirTypeParameterBuilder
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.symbols.toLookupTag

/**
 * 对齐 Kotlin `FirTypeParameterBuilder.addDefaultBoundIfNecessary` 的职责：
 * 当类型参数未声明任何显式上界时，补上仓颉默认顶层约束 `std.core.Any`，
 * 确保 RAW_CFIR 构建结束后 `bounds` 至少包含一个 `CfirResolvedTypeRef`。
 *
 * 该工具同时服务于源码 raw-cfir 构建（PSI / LightTree）与反序列化路径，
 * 避免 TYPES 阶段或下游 checker 撞上 `CfirImplicitTypeRefImpl` cast 失败。
 */
fun CfirTypeParameterBuilder.addDefaultBoundIfNecessary() {
    if (bounds.isNotEmpty()) return

    val defaultBound = ConeClassLikeType(StdlibClassIds.Any.toLookupTag(), isInterface = true)
    bounds += defaultBound.toCfirResolvedTypeRef()
}