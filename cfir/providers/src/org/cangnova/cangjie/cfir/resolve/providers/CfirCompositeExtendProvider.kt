package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
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
    /**
     * 按查询优先级排列的 extend provider。
     */
    private val providers: List<CfirExtendProvider>,
) : CfirExtendProvider {
    /**
     * 聚合所有 provider 中匹配目标 key 的 extend。
     */
    override fun getExtendsForTarget(targetKey: CfirExtendTargetKey): List<CfirExtend> =
        providers.flatMap { it.getExtendsForTarget(targetKey) }

    /**
     * 聚合所有 provider 中扩展指定 class 的 extend。
     */
    override fun getExtendsForClass(classId: ClassId): List<CfirExtend> =
        providers.flatMap { it.getExtendsForClass(classId) }

    /**
     * 聚合指定包内的 extend。
     */
    override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> =
        providers.flatMap { it.getExtendsInPackage(packageFqName) }

    /**
     * 聚合扩展指定 builtin primitive 的 extend。
     */
    override fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend> =
        providers.flatMap { it.getExtendsForBuiltinType(kind) }

    /**
     * 按 provider 顺序返回 callable 所属的第一个 owner extend。
     */
    override fun getContainingExtend(symbol: CfirCallableSymbol<*>): CfirExtend? =
        providers.firstNotNullOfOrNull { it.getContainingExtend(symbol) }

    /**
     * 按 provider 顺序返回 extend 所在包。
     */
    override fun getPackageFqName(extend: CfirExtend): FqName? =
        providers.firstNotNullOfOrNull { it.getPackageFqName(extend) }

    /**
     * 所有子 provider 均认为可访问时，组合结果才可访问。
     */
    override fun isExtendAccessible(extend: CfirExtend): Boolean =
        providers.all { it.isExtendAccessible(extend) }
}
