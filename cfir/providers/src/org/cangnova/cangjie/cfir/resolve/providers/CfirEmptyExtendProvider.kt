package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * 最小可用的 extend provider。
 */
class CfirEmptyExtendProvider : CfirExtendProvider {
    /**
     * 空 provider 不包含任何目标 key 匹配的 extend。
     */
    override fun getExtendsForTarget(targetKey: CfirExtendTargetKey): List<CfirExtend> = emptyList()

    /**
     * 空 provider 不包含任何 class extend。
     */
    override fun getExtendsForClass(classId: ClassId): List<CfirExtend> = emptyList()

    /**
     * 空 provider 不包含任何包内 extend。
     */
    override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> = emptyList()

    /**
     * 空 provider 不包含任何 builtin primitive extend。
     */
    override fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend> = emptyList()

    /** 空 provider 不拥有任何 extend 的声明文件。 */
    override fun getContainingFile(extend: CfirExtend): CfirFile? = null
}
