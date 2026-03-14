package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * 基于 session 文件的 extend 声明提供器。
 *
 * 扫描 session 中所有 CfirFile 的 CfirExtend 声明，
 * 解析 extend 的 extendedTypeRef → 获得目标 ClassId，
 * 按 ClassId 建立索引。
 *
 * 使用惰性初始化：首次查询时构建索引。
 */
class CfirSessionExtendProvider(
    /** 提供待扫描文件的函数（惰性获取，避免循环依赖） */
    private val filesProvider: () -> List<CfirFile>,
) : CfirExtendProvider {

    /** 按目标 ClassId 索引的 extend 声明 */
    private val extendsByClassId: Map<ClassId, List<CfirExtend>> by lazy {
        buildExtendIndex()
    }

    /** 按包名索引的 extend 声明 */
    private val extendsByPackage: Map<FqName, List<CfirExtend>> by lazy {
        buildPackageIndex()
    }

    override fun getExtendsForClass(classId: ClassId): List<CfirExtend> {
        return extendsByClassId[classId] ?: emptyList()
    }

    override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> {
        return extendsByPackage[packageFqName] ?: emptyList()
    }

    override fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend> {
        // 内建原始类型的 extend：通过类型名称匹配
        val builtinName = kind.name // 如 "Int64", "Float64" 等
        return extendsByClassId.entries
            .filter { it.key.shortClassName.asString() == builtinName }
            .flatMap { it.value }
    }

    /**
     * 构建按 ClassId 索引的 extend 声明映射。
     *
     * 遍历所有文件中的 extend 声明，解析 extendedTypeRef 获取目标 ClassId。
     */
    private fun buildExtendIndex(): Map<ClassId, List<CfirExtend>> {
        val index = mutableMapOf<ClassId, MutableList<CfirExtend>>()

        for (file in filesProvider()) {
            for (decl in file.declarations) {
                if (decl !is CfirExtend) continue

                val targetClassId = resolveExtendTargetClassId(decl) ?: continue
                index.getOrPut(targetClassId) { mutableListOf() }.add(decl)
            }
        }

        return index
    }

    /** 构建按包名索引 */
    private fun buildPackageIndex(): Map<FqName, List<CfirExtend>> {
        val index = mutableMapOf<FqName, MutableList<CfirExtend>>()

        for (file in filesProvider()) {
            val packageFqName = file.packageDirective.packageFqName
            for (decl in file.declarations) {
                if (decl !is CfirExtend) continue
                index.getOrPut(packageFqName) { mutableListOf() }.add(decl)
            }
        }

        return index
    }

    /**
     * 从 extend 声明中解析目标类型的 ClassId。
     */
    private fun resolveExtendTargetClassId(extend: CfirExtend): ClassId? {
        val typeRef = extend.extendedTypeRef
        val coneType = (typeRef as? CfirResolvedTypeRef)?.coneType ?: return null

        return when (coneType) {
            is ConeClassLikeType -> coneType.classId
            is ConeStructType -> coneType.classId
            is ConeEnumType -> coneType.classId
            else -> null
        }
    }
}
