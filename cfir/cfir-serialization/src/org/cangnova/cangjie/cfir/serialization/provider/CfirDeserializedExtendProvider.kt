package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

class CfirDeserializedExtendProvider(
    private val providers: List<AbstractCfirDeserializedSymbolProvider>,
) : CfirExtendProvider {
    private val index: ExtendIndex by lazy(LazyThreadSafetyMode.PUBLICATION) { buildIndex() }

    override fun getExtendsForClass(classId: ClassId): List<CfirExtend> =
        index.byTargetClassId[classId].orEmpty()

    override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> =
        index.byPackage[packageFqName].orEmpty()

    override fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend> =
        getExtendsForClass(kind.classId)

    private fun buildIndex(): ExtendIndex {
        val byTargetClassId = linkedMapOf<ClassId, MutableList<CfirExtend>>()
        val byPackage = linkedMapOf<FqName, MutableList<CfirExtend>>()

        for (provider in providers) {
            val packageNames = provider.symbolNamesProvider.getPackageNames().orEmpty()
            for (packageFqName in packageNames) {
                val extends = provider.getTopLevelExtendDeclarations(packageFqName)
                if (extends.isEmpty()) continue

                byPackage.getOrPut(packageFqName) { mutableListOf() }.addAll(extends)
                for (extend in extends) {
                    val targetClassId =
                        extend.extendedTypeRef.coneTypeOrNull?.classIdOrPrimitiveClassId ?: continue
                    byTargetClassId.getOrPut(targetClassId) { mutableListOf() }.add(extend)
                }
            }
        }

        return ExtendIndex(
            byTargetClassId = byTargetClassId.mapValues { (_, extends) -> extends.distinct() },
            byPackage = byPackage.mapValues { (_, extends) -> extends.distinct() },
        )
    }

    private data class ExtendIndex(
        val byTargetClassId: Map<ClassId, List<CfirExtend>>,
        val byPackage: Map<FqName, List<CfirExtend>>,
    )
}
