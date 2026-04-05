package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * 组合多个 extend provider。
 *
 * owner extend 查询也必须透传，否则 substitution scope 无法在多来源场景
 * 下稳定定位 extend 成员的声明归属。
 */
class CfirCompositeExtendProvider(
    private val providers: List<CfirExtendProvider>,
) : CfirExtendProvider {
    override fun getExtendsForClass(classId: ClassId): List<CfirExtend> =
        providers.flatMap { it.getExtendsForClass(classId) }

    override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> =
        providers.flatMap { it.getExtendsInPackage(packageFqName) }

    override fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend> =
        providers.flatMap { it.getExtendsForBuiltinType(kind) }

    override fun getContainingExtend(symbol: CfirCallableSymbol<*>): CfirExtend? =
        providers.firstNotNullOfOrNull { it.getContainingExtend(symbol) }

    override fun isExtendAccessible(extend: CfirExtend): Boolean =
        providers.all { it.isExtendAccessible(extend) }
}
