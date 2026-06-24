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

    /**
     * `Unit` 内建类型。
     */
    val unitType: ConePrimitiveType = ConePrimitiveType.UNIT

    /**
     * `Bool` 内建类型。
     */
    val boolType: ConePrimitiveType = ConePrimitiveType.BOOLEAN

    /**
     * `Int8` 内建类型。
     */
    val int8Type: ConePrimitiveType = ConePrimitiveType.INT8

    /**
     * `Int16` 内建类型。
     */
    val int16Type: ConePrimitiveType = ConePrimitiveType.INT16

    /**
     * `Int32` 内建类型。
     */
    val int32Type: ConePrimitiveType = ConePrimitiveType.INT32

    /**
     * `Int64` 内建类型。
     */
    val int64Type: ConePrimitiveType = ConePrimitiveType.INT64

    /**
     * 平台相关宽度的 `IntNative` 内建类型。
     */
    val intNativeType: ConePrimitiveType = ConePrimitiveType.INT_NATIVE

    /**
     * `UInt8` 内建类型。
     */
    val uint8Type: ConePrimitiveType = ConePrimitiveType.UINT8

    /**
     * `UInt16` 内建类型。
     */
    val uint16Type: ConePrimitiveType = ConePrimitiveType.UINT16

    /**
     * `UInt32` 内建类型。
     */
    val uint32Type: ConePrimitiveType = ConePrimitiveType.UINT32

    /**
     * `UInt64` 内建类型。
     */
    val uint64Type: ConePrimitiveType = ConePrimitiveType.UINT64

    /**
     * 平台相关宽度的 `UIntNative` 内建类型。
     */
    val uintNativeType: ConePrimitiveType = ConePrimitiveType.UINT_NATIVE

    /**
     * `Float16` 内建类型。
     */
    val float16Type: ConePrimitiveType = ConePrimitiveType.FLOAT16

    /**
     * `Float32` 内建类型。
     */
    val float32Type: ConePrimitiveType = ConePrimitiveType.FLOAT32

    /**
     * `Float64` 内建类型。
     */
    val float64Type: ConePrimitiveType = ConePrimitiveType.FLOAT64

    /**
     * `Rune` 内建类型。
     */
    val runeType: ConePrimitiveType = ConePrimitiveType.RUNE

    /**
     * `Nothing` 内建类型。
     */
    val nothingType: ConePrimitiveType = ConePrimitiveType.NOTHING

    /**
     * 通过 [PrimitiveTypeKind] 获取内建类型。
     *
     * 该入口对齐官方编译器 `TypeManager::GetPrimitiveTy`。
     */
    fun getPrimitiveType(kind: PrimitiveTypeKind): ConePrimitiveType {
        return PRIMITIVE_TYPE_MAP[kind] ?: error("Unknown primitive type kind: $kind")
    }

    /**
     * 通过语言层类型名获取内建类型。
     */
    fun getPrimitiveTypeByName(name: String): ConePrimitiveType? {
        return PRIMITIVE_NAME_MAP[name]
    }

    /**
     * 内建类型查找表。
     */
    private companion object {
        /**
         * [PrimitiveTypeKind] 到 [ConePrimitiveType] 的映射。
         */
        val PRIMITIVE_TYPE_MAP: Map<PrimitiveTypeKind, ConePrimitiveType> = buildMap {
            for (kind in PrimitiveTypeKind.entries) {
                put(kind, ConePrimitiveType(kind))
            }
        }

        /**
         * 语言层类型名到 [ConePrimitiveType] 的映射。
         */
        val PRIMITIVE_NAME_MAP: Map<String, ConePrimitiveType> = buildMap {
            for (kind in PrimitiveTypeKind.entries) {
                put(kind.typeName, ConePrimitiveType(kind))
            }
        }
    }
}

/**
 * 当前 session 中注册的内建类型组件。
 */
val CfirSession.builtinTypes: CfirBuiltinTypes by CfirSession.sessionComponentAccessor()
