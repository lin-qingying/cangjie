package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.ClassId

/**
 * [org.cangnova.cangjie.cfir.types.ConeTypeContext] 的实现，负责连接子类型检查算法与符号系统。
 * 它通过 [CfirSession] 的 `symbolProvider` 查询类的直接超类型信息，
 * 并将 CFIR 声明中的 `superTypeRefs` 转换为子类型检查器可用的 [ConeCangJieType] 列表。
 * 参考 K2 `ConeTypeCheckerContext`。
 */
class CfirTypeCheckerContext(
    private val session: CfirSession,
) : ConeTypeContext {

    override fun supertypes(type: ConeCangJieType): Collection<ConeCangJieType> {
        return when (type) {
            is ConeClassLikeType -> classLikeSupertypes(type.classId)
            is ConeStructType -> classLikeSupertypes(type.classId)
            is ConeEnumType -> classLikeSupertypes(type.classId)
            is ConeTypeParameterType -> type.upperBounds
            is ConeIntersectionType -> {
                // 交叉类型的超类型是各成员超类型的合并
                type.intersectedTypes.flatMap { supertypes(it) }.distinct()
            }
            else -> emptyList()
        }
    }

    override fun isSameTypeConstructor(a: ConeCangJieType, b: ConeCangJieType): Boolean {
        return when {
            a is ConeClassLikeType && b is ConeClassLikeType -> a.classId == b.classId
            a is ConeStructType && b is ConeStructType -> a.classId == b.classId
            a is ConeEnumType && b is ConeEnumType -> a.classId == b.classId
            a is ConePrimitiveType && b is ConePrimitiveType -> a.kind == b.kind
            a is ConeFuncType && b is ConeFuncType ->
                a.parameterTypes.size == b.parameterTypes.size && a.isCFunc == b.isCFunc
            a is ConeTupleType && b is ConeTupleType ->
                a.elementTypes.size == b.elementTypes.size
            a is ConeArrayType && b is ConeArrayType -> a.dims == b.dims
            a is ConeVArrayType && b is ConeVArrayType -> a.size == b.size
            else -> a == b
        }
    }

    /** 通过 symbolProvider 查找类、结构体或枚举的直接超类型。 */
    private fun classLikeSupertypes(classId: ClassId): List<ConeCangJieType> {
        val classSymbol = session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return emptyList()
        if (!classSymbol.isBound) return emptyList()
        val classDecl = classSymbol.cfir

        return classDecl.superTypeRefs.mapNotNull { typeRef ->
            (typeRef as? CfirResolvedTypeRef)?.coneType
        }
    }
}

