package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.ClassId

/**
 * [ConeTypeContext] 鐨勫疄鐜帮紝杩炴帴瀛愮被鍨嬫鏌ョ畻娉曚笌绗﹀彿绯荤粺銆? *
 * 閫氳繃 [CfirSession] 鐨?symbolProvider 鏌ヨ绫荤殑瓒呯被鍨嬩俊鎭紝
 * 灏?CFIR 澹版槑涓殑 superTypeRefs 杞崲涓哄瓙绫诲瀷妫€鏌ュ櫒鍙敤鐨?[ConeCangjieType] 鍒楄〃銆? *
 * 鍙傝€?K2 ConeTypeCheckerContext銆? */
class CfirTypeCheckerContext(
    private val session: CfirSession,
) : ConeTypeContext {

    override fun supertypes(type: ConeCangjieType): Collection<ConeCangjieType> {
        return when (type) {
            is ConeClassLikeType -> classLikeSupertypes(type.classId)
            is ConeStructType -> classLikeSupertypes(type.classId)
            is ConeEnumType -> classLikeSupertypes(type.classId)
            is ConeTypeParameterType -> type.upperBounds
            is ConeIntersectionType -> {
                // 浜ゅ弶绫诲瀷鐨勮秴绫诲瀷鏄悇鎴愬憳瓒呯被鍨嬬殑鍚堝苟
                type.intersectedTypes.flatMap { supertypes(it) }.distinct()
            }
            else -> emptyList()
        }
    }

    override fun isSameTypeConstructor(a: ConeCangjieType, b: ConeCangjieType): Boolean {
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

    /** 閫氳繃 symbolProvider 鏌ユ壘绫?缁撴瀯浣?鏋氫妇鐨勭洿鎺ヨ秴绫诲瀷 */
    private fun classLikeSupertypes(classId: ClassId): List<ConeCangjieType> {
        val classSymbol = session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return emptyList()
        if (!classSymbol.isBound) return emptyList()
        val classDecl = classSymbol.cfir

        return classDecl.superTypeRefs.mapNotNull { typeRef ->
            (typeRef as? CfirResolvedTypeRef)?.coneType
        }
    }
}

