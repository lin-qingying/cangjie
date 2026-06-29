package org.cangnova.cangjie.jvm.runtime

/**
 * JVM 后端的无符号数值转换运行时。
 *
 * JVM 没有无符号整型 carrier，CHIR 的 UInt/ULong 仍以 byte/short/int/long 保存 bit pattern。
 * 浮点到无符号整数的转换需要在写回 carrier 前按无符号范围截断。
 */
object CjJvmUnsignedRuntime {
    /**
     * 2^63 的 double 表示，用于拆分 ULong 的高位区间。
     */
    private const val TWO_TO_63: Double = 9.223372036854776E18

    /**
     * 将 double 转换为 UInt8 carrier。
     */
    @JvmStatic
    fun doubleToUInt8(value: Double): Byte = doubleToUInt64(value).toByte()

    /**
     * 将 double 转换为 UInt16 carrier。
     */
    @JvmStatic
    fun doubleToUInt16(value: Double): Short = doubleToUInt64(value).toShort()

    /**
     * 将 double 转换为 UInt32 carrier。
     */
    @JvmStatic
    fun doubleToUInt32(value: Double): Int = doubleToUInt64(value).toInt()

    /**
     * 将 double 转换为 UInt64 carrier，按 JVM long 保存无符号 bit pattern。
     */
    @JvmStatic
    fun doubleToUInt64(value: Double): Long {
        if (value.isNaN() || value <= 0.0) return 0L
        if (value < TWO_TO_63) return value.toLong()
        val lowerHalf = (value - TWO_TO_63).toLong()
        return lowerHalf xor Long.MIN_VALUE
    }
}
