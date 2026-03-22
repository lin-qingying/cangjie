package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeQuestType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType
import org.cangnova.cangjie.name.ClassId

/**
 * 类型检查工具类。
 * 提供子类型判断和类型渲染能力，供各类 checker 复用。
 */
object CfirTypeCheckUtils {

    enum class RelationResult {
        IDENTICAL,
        SUBTYPE,
        COMPATIBLE_WITH_CONVERSION,
        COMPATIBLE_WITH_QUEST_FALLBACK,
        CONSTRAINABLE_BY_VARIABLES,
        INCOMPATIBLE,
    }

    /** 判断 [subType] 是否为 [superType] 的子类型。 */
    fun isSubtypeOf(subType: ConeCangJieType, superType: ConeCangJieType): Boolean =
        relation(subType, superType) != RelationResult.INCOMPATIBLE

    /**
     * 判断 [subType] 是否为 [superType] 的子类型（session 感知版本）。
     * 通过 symbolProvider 遍历继承链，支持 Dog <: Animal 等场景。
     */
    fun isSubtypeOf(subType: ConeCangJieType, superType: ConeCangJieType, session: CfirSession): Boolean {
        // 先用快速路径判断
        val quickResult = relation(subType, superType)
        if (quickResult != RelationResult.INCOMPATIBLE) return true

        // 对类类型进行继承链遍历
        val subClassId = classIdOf(subType) ?: return false
        val superClassId = classIdOf(superType) ?: return false
        return isSubclassBfs(subClassId, superClassId, session)
    }

    /** 从类类型中提取 ClassId */
    private fun classIdOf(type: ConeCangJieType): ClassId? = when (type) {
        is ConeClassLikeType -> type.classId
        is ConeStructType -> type.classId
        is ConeEnumType -> type.classId
        else -> null
    }

    /** BFS 遍历继承链，判断 subClassId 是否为 superClassId 的子类 */
    private fun isSubclassBfs(subClassId: ClassId, superClassId: ClassId, session: CfirSession): Boolean {
        if (subClassId == superClassId) return true
        val visited = mutableSetOf(subClassId)
        val queue = ArrayDeque<ClassId>()
        queue.add(subClassId)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val symbol = session.symbolProvider.getClassLikeSymbolByClassId(current) ?: continue
            if (!symbol.isBound) continue
            for (superTypeRef in symbol.cfir.superTypeRefs) {
                val superType = (superTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
                val parentClassId = classIdOf(superType) ?: continue
                if (parentClassId == superClassId) return true
                if (visited.add(parentClassId)) {
                    queue.add(parentClassId)
                }
            }
        }
        return false
    }

    fun relation(subType: ConeCangJieType, superType: ConeCangJieType): RelationResult {
        if (subType == superType) return RelationResult.IDENTICAL
        if (subType is ConeQuestType || superType is ConeQuestType) return RelationResult.COMPATIBLE_WITH_QUEST_FALLBACK
        if (subType is ConeTypeParameterType && subType.isPlaceholder) return RelationResult.CONSTRAINABLE_BY_VARIABLES
        if (superType is ConeTypeParameterType && superType.isPlaceholder) return RelationResult.CONSTRAINABLE_BY_VARIABLES
        if (isIdealNumericConversion(subType, superType)) return RelationResult.COMPATIBLE_WITH_CONVERSION

        return when {
            subType is ConePrimitiveType && superType is ConePrimitiveType && subType.kind == superType.kind -> RelationResult.IDENTICAL
            subType is ConeClassLikeType && superType is ConeClassLikeType && subType.classId == superType.classId -> RelationResult.SUBTYPE
            subType is ConeStructType && superType is ConeStructType && subType.classId == superType.classId -> RelationResult.SUBTYPE
            subType is ConeEnumType && superType is ConeEnumType && subType.classId == superType.classId -> RelationResult.SUBTYPE
            subType is ConeFuncType && superType is ConeFuncType && subType.parameterTypes.size == superType.parameterTypes.size -> RelationResult.SUBTYPE
            subType is ConeTupleType && superType is ConeTupleType && subType.elementTypes.size == superType.elementTypes.size -> RelationResult.SUBTYPE
            else -> RelationResult.INCOMPATIBLE
        }
    }

    /** 将类型渲染为可读字符串。 */
    fun renderType(type: ConeCangJieType): String = type.toString()

    private fun isIdealNumericConversion(subType: ConeCangJieType, superType: ConeCangJieType): Boolean {
        if (subType !is ConePrimitiveType || superType !is ConePrimitiveType) return false

        return when (subType.kind) {
            org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.IDEAL_INT -> superType.kind in setOf(
                org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.INT8,
                org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.INT16,
                org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.INT32,
                org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.INT64,
                org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.UINT8,
                org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.UINT16,
                org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.UINT32,
                org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.UINT64,
            )

            org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.IDEAL_FLOAT -> superType.kind in setOf(
                org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.FLOAT16,
                org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.FLOAT32,
                org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.FLOAT64,
            )

            else -> false
        }
    }
}
