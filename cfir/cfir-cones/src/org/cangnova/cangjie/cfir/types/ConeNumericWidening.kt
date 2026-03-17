package org.cangnova.cangjie.cfir.types

/**
 * 仓颉数值类型拓宽规则。
 *
 * 拓宽是隐式类型转换，允许较小的数值类型自动转换为较大的类型。仓颉支持三条独立的拓宽链（严格同族）：
 * - 有符号整数：Int8 → Int16 → Int32 → Int64
 * - 无符号整数：UInt8 → UInt16 → UInt32 → UInt64
 * - 浮点：Float16 → Float32 → Float64
 *
 * 不支持跨族拓宽（如 Int32 → UInt32、Int32 → Float64），这些需要显式转换。
 */
object ConeNumericWidening {
    /** 有符号整数拓宽秩：Int8(0) → Int16(1) → Int32(2) → Int64(3) */
    private val SIGNED_INT_RANK: Map<PrimitiveTypeKind, Int> = mapOf(
        PrimitiveTypeKind.INT8 to 0,
        PrimitiveTypeKind.INT16 to 1,
        PrimitiveTypeKind.INT32 to 2,
        PrimitiveTypeKind.INT64 to 3,
    )

    /** 无符号整数拓宽秩：UInt8(0) → UInt16(1) → UInt32(2) → UInt64(3) */
    private val UNSIGNED_INT_RANK: Map<PrimitiveTypeKind, Int> = mapOf(
        PrimitiveTypeKind.UINT8 to 0,
        PrimitiveTypeKind.UINT16 to 1,
        PrimitiveTypeKind.UINT32 to 2,
        PrimitiveTypeKind.UINT64 to 3,
    )

    /** 浮点拓宽秩：Float16(0) → Float32(1) → Float64(2) */
    private val FLOAT_RANK: Map<PrimitiveTypeKind, Int> = mapOf(
        PrimitiveTypeKind.FLOAT16 to 0,
        PrimitiveTypeKind.FLOAT32 to 1,
        PrimitiveTypeKind.FLOAT64 to 2,
    )

    /**
     * 判断 sub 是否可隐式拓宽为 super。
     *
     * 拓宽成立当且仅当：
     * - 两个类型属于同一族（均为有符号整数、均为无符号整数、均为浮点）
     * - 且 rank(sub) <= rank(super)
     *
     * 例如：`Int32 <: Int64` 成立，`Int32 <: UInt64` 不成立。
     */
    fun isWideningAllowed(sub: PrimitiveTypeKind, super_: PrimitiveTypeKind): Boolean {
        if (sub == super_) return true

        // 有符号整数族
        if (sub in SIGNED_INT_RANK && super_ in SIGNED_INT_RANK) {
            return SIGNED_INT_RANK[sub]!! < SIGNED_INT_RANK[super_]!!
        }

        // 无符号整数族
        if (sub in UNSIGNED_INT_RANK && super_ in UNSIGNED_INT_RANK) {
            return UNSIGNED_INT_RANK[sub]!! < UNSIGNED_INT_RANK[super_]!!
        }

        // 浮点族
        if (sub in FLOAT_RANK && super_ in FLOAT_RANK) {
            return FLOAT_RANK[sub]!! < FLOAT_RANK[super_]!!
        }

        return false
    }

    /**
     * 从两个同族数值类型中取较宽的。
     *
     * 用于二元运算（如 `Int32 + Int64`）的结果类型推断。
     *
     * @return 较宽的类型，或 null 若两个类型不同族
     */
    fun widerOf(a: PrimitiveTypeKind, b: PrimitiveTypeKind): PrimitiveTypeKind? {
        if (a == b) return a

        // 有符号整数族
        if (a in SIGNED_INT_RANK && b in SIGNED_INT_RANK) {
            return if (SIGNED_INT_RANK[a]!! > SIGNED_INT_RANK[b]!!) a else b
        }

        // 无符号整数族
        if (a in UNSIGNED_INT_RANK && b in UNSIGNED_INT_RANK) {
            return if (UNSIGNED_INT_RANK[a]!! > UNSIGNED_INT_RANK[b]!!) a else b
        }

        // 浮点族
        if (a in FLOAT_RANK && b in FLOAT_RANK) {
            return if (FLOAT_RANK[a]!! > FLOAT_RANK[b]!!) a else b
        }

        // 不同族 → 无公共拓宽类型
        return null
    }
}
