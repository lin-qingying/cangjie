@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProvider
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjEnum
import org.cangnova.cangjie.psi.CjEnumConstructor

/**
 * 已编译枚举项既是其 owner enum 的成员，也参与仓颉裸枚举项的包级名称查找。
 *
 * 这个索引投影只从平台声明索引读取顶层 enum PSI，不创建 CFIR 符号；符号身份仍由所属 enum 的
 * stub 反序列化结果提供，避免在名称查询和符号查询间产生两份 enum constructor。
 */
internal fun CangJieDeclarationProvider.getTopLevelEnumConstructorNamesInPackage(packageFqName: FqName): Set<Name> =
    topLevelEnums(packageFqName).flatMapTo(linkedSetOf()) { enum ->
        enum.constructor.mapNotNull { constructor -> constructor.name?.let(Name::identifier) }
    }

/** 返回包内匹配 [callableId] 的顶层枚举项 PSI，用于已编译库 callable 名称查询。 */
internal fun CangJieDeclarationProvider.getTopLevelEnumConstructors(callableId: CallableId): List<CjEnumConstructor> =
    topLevelEnums(callableId.packageName)
        .flatMap(CjEnum::constructor)
        .filter { constructor -> constructor.name == callableId.callableName.asString() }
        .distinct()

/**
 * 仅枚举包级类型声明，避免把嵌套类型中的 enum constructor 错误提升为包级 callable。
 */
private fun CangJieDeclarationProvider.topLevelEnums(packageFqName: FqName): List<CjEnum> =
    getTopLevelCangJieClassLikeDeclarationNamesInPackage(packageFqName)
        .mapNotNull { className ->
            getClassLikeDeclarationByClassId(ClassId(packageFqName, className)) as? CjEnum
        }
        .distinct()
