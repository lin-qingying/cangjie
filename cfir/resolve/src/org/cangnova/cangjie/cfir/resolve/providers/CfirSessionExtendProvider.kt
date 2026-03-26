package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendIndexStore
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendSemanticModel
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * Session-scoped extend provider backed by [CfirExtendIndexStore].
 *
 * It only reads EXTENSIONS-phase prebuilt indexes and does not rescan files
 * during BODY_RESOLVE, keeping phase boundaries explicit.
 */
class CfirSessionExtendProvider(
    private val indexStore: CfirExtendIndexStore,
) : CfirExtendProvider {

    override fun getExtendsForClass(classId: ClassId): List<CfirExtend> {
        return indexStore.modelsForClass(classId).map(CfirExtendSemanticModel::declaration)
    }

    override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> {
        return indexStore.modelsInPackage(packageFqName).map(CfirExtendSemanticModel::declaration)
    }

    override fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend> {
        return indexStore.modelsForClass(kind.classId).map(CfirExtendSemanticModel::declaration)
    }
}

