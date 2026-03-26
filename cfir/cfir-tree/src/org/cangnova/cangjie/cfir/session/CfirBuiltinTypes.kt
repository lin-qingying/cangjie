package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind

/**
 * 内建类型 session 组件（对齐 C++ 编译器的 TypeManager）。
 *
 * 内建类型是编译器硬编码的全局类型，不属于任何包（GetDeclPtrOfTy() 返回 nullptr）。
 * 与标准库类型（std.core 中的 String, Array, Any 等）不同，内建类型无需导入即可使用。
 *
 * 内建类型通过 PrimitiveTypeKind 枚举定义，对应 C++ 中 TYPE_UNIT ~ TYPE_BOOLEAN 范围。
 */
class CfirBuiltinTypes : CfirSessionComponent {

    val unitType: ConePrimitiveType = ConePrimitiveType.UNIT
    val boolType: ConePrimitiveType = ConePrimitiveType.BOOLEAN
    val int8Type: ConePrimitiveType = ConePrimitiveType.INT8
    val int16Type: ConePrimitiveType = ConePrimitiveType.INT16
    val int32Type: ConePrimitiveType = ConePrimitiveType.INT32
    val int64Type: ConePrimitiveType = ConePrimitiveType.INT64
    val intNativeType: ConePrimitiveType = ConePrimitiveType.INT_NATIVE
    val uint8Type: ConePrimitiveType = ConePrimitiveType.UINT8
    val uint16Type: ConePrimitiveType = ConePrimitiveType.UINT16
    val uint32Type: ConePrimitiveType = ConePrimitiveType.UINT32
    val uint64Type: ConePrimitiveType = ConePrimitiveType.UINT64
    val uintNativeType: ConePrimitiveType = ConePrimitiveType.UINT_NATIVE
    val float16Type: ConePrimitiveType = ConePrimitiveType.FLOAT16
    val float32Type: ConePrimitiveType = ConePrimitiveType.FLOAT32
    val float64Type: ConePrimitiveType = ConePrimitiveType.FLOAT64
    val runeType: ConePrimitiveType = ConePrimitiveType.RUNE
    val nothingType: ConePrimitiveType = ConePrimitiveType.NOTHING

    /** 通过 TypeKind 获取内建类型（对齐 TypeManager::GetPrimitiveTy） */
    fun getPrimitiveType(kind: PrimitiveTypeKind): ConePrimitiveType {
        return PRIMITIVE_TYPE_MAP[kind] ?: error("Unknown primitive type kind: $kind")
    }

    /** 通过类型名获取内建类型 */
    fun getPrimitiveTypeByName(name: String): ConePrimitiveType? {
        return PRIMITIVE_NAME_MAP[name]
    }

    private companion object {
        val PRIMITIVE_TYPE_MAP: Map<PrimitiveTypeKind, ConePrimitiveType> = buildMap {
            for (kind in PrimitiveTypeKind.entries) {
                put(kind, ConePrimitiveType(kind))
            }
        }

        val PRIMITIVE_NAME_MAP: Map<String, ConePrimitiveType> = buildMap {
            for (kind in PrimitiveTypeKind.entries) {
                put(kind.typeName, ConePrimitiveType(kind))
            }
        }
    }
}

val CfirSession.builtinTypes: CfirBuiltinTypes by CfirSession.sessionComponentAccessor()
