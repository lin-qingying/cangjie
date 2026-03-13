package org.cangjie.cfir.resolve.providers

import org.cangjie.cfir.declarations.CfirExtend
import org.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * Composite extend provider.
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
}
