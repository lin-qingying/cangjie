package org.cangjie.cfir.resolve.providers

import org.cangjie.cfir.declarations.CfirExtend
import org.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * 最小可用的 extend provider。
 */
class CfirEmptyExtendProvider : CfirExtendProvider {
    override fun getExtendsForClass(classId: ClassId): List<CfirExtend> = emptyList()

    override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> = emptyList()

    override fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend> = emptyList()
}

