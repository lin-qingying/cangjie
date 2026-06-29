package org.cangnova.cangjie.analysis.api.impl.base.annotations

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationValue
import org.cangnova.cangjie.analysis.api.annotations.CaConstantValue
import org.cangnova.cangjie.analysis.api.annotations.CaNamedAnnotationValue
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjElement

/**
 * 注解值对象的基础实现层。
 *
 * 这里严格对齐 Kotlin `analysis-api-impl-base` 的实现分层：
 * - 具体实现类只实现 `CaAnnotationValue.*` / `CaConstantValue.*` 子接口
 * - 不直接实现 sealed 根接口
 * - 常量值只保存类型化后的真实值，由对象自身负责规范渲染
 */
public object CaBaseAnnotationValues {
    /**
     * 创建包装常量值的注解值对象。
     */
    public fun constant(
        value: CaConstantValue,
        sourcePsi: CjElement?,
        token: CaLifetimeToken,
    ): CaAnnotationValue.ConstantValue = ConstantValueImpl(value, sourcePsi, token)

    /**
     * 创建 enum 注解值对象。
     */
    public fun enumValue(
        callableId: CallableId?,
        arguments: List<CaAnnotationValue>,
        sourcePsi: CjElement?,
        token: CaLifetimeToken,
    ): CaAnnotationValue.EnumValue = EnumValueImpl(callableId, arguments, sourcePsi, token)

    /**
     * 创建 tuple 注解值对象。
     */
    public fun tupleValue(
        values: List<CaAnnotationValue>,
        sourcePsi: CjElement?,
        token: CaLifetimeToken,
    ): CaAnnotationValue.TupleValue = TupleValueImpl(values, sourcePsi, token)

    /**
     * 创建 class instance 注解值对象。
     */
    public fun classInstanceValue(
        classId: ClassId?,
        arguments: List<CaNamedAnnotationValue>,
        sourcePsi: CjElement?,
        token: CaLifetimeToken,
    ): CaAnnotationValue.ClassInstanceValue = ClassInstanceValueImpl(classId, arguments, sourcePsi, token)

    /**
     * 创建 struct instance 注解值对象。
     */
    public fun structInstanceValue(
        classId: ClassId?,
        arguments: List<CaNamedAnnotationValue>,
        sourcePsi: CjElement?,
        token: CaLifetimeToken,
    ): CaAnnotationValue.StructInstanceValue = StructInstanceValueImpl(classId, arguments, sourcePsi, token)

    /**
     * 创建 Bool 常量注解值。
     */
    public fun boolValue(value: Boolean, sourcePsi: CjElement?): CaConstantValue.BoolValue =
        BoolValueImpl(value, sourcePsi)

    /**
     * 创建 Rune 常量注解值。
     */
    public fun runeValue(value: Int, sourcePsi: CjElement?): CaConstantValue.RuneValue =
        RuneValueImpl(value, sourcePsi)

    /**
     * 创建 Int8 常量注解值。
     */
    public fun int8Value(value: Byte, sourcePsi: CjElement?): CaConstantValue.Int8Value =
        Int8ValueImpl(value, sourcePsi)

    /**
     * 创建 Int16 常量注解值。
     */
    public fun int16Value(value: Short, sourcePsi: CjElement?): CaConstantValue.Int16Value =
        Int16ValueImpl(value, sourcePsi)

    /**
     * 创建 Int32 常量注解值。
     */
    public fun int32Value(value: Int, sourcePsi: CjElement?): CaConstantValue.Int32Value =
        Int32ValueImpl(value, sourcePsi)

    /**
     * 创建 Int64 常量注解值。
     */
    public fun int64Value(value: Long, sourcePsi: CjElement?): CaConstantValue.Int64Value =
        Int64ValueImpl(value, sourcePsi)

    /**
     * 创建 IntNative 常量注解值。
     */
    public fun intNativeValue(value: Long, sourcePsi: CjElement?): CaConstantValue.IntNativeValue =
        IntNativeValueImpl(value, sourcePsi)

    /**
     * 创建 UInt8 常量注解值。
     */
    public fun uint8Value(value: UByte, sourcePsi: CjElement?): CaConstantValue.UInt8Value =
        UInt8ValueImpl(value, sourcePsi)

    /**
     * 创建 UInt16 常量注解值。
     */
    public fun uint16Value(value: UShort, sourcePsi: CjElement?): CaConstantValue.UInt16Value =
        UInt16ValueImpl(value, sourcePsi)

    /**
     * 创建 UInt32 常量注解值。
     */
    public fun uint32Value(value: UInt, sourcePsi: CjElement?): CaConstantValue.UInt32Value =
        UInt32ValueImpl(value, sourcePsi)

    /**
     * 创建 UInt64 常量注解值。
     */
    public fun uint64Value(value: ULong, sourcePsi: CjElement?): CaConstantValue.UInt64Value =
        UInt64ValueImpl(value, sourcePsi)

    /**
     * 创建 UIntNative 常量注解值。
     */
    public fun uintNativeValue(value: ULong, sourcePsi: CjElement?): CaConstantValue.UIntNativeValue =
        UIntNativeValueImpl(value, sourcePsi)

    /**
     * 创建 Float16 常量注解值。
     */
    public fun float16Value(value: Float, sourcePsi: CjElement?): CaConstantValue.Float16Value =
        Float16ValueImpl(value, sourcePsi)

    /**
     * 创建 Float32 常量注解值。
     */
    public fun float32Value(value: Float, sourcePsi: CjElement?): CaConstantValue.Float32Value =
        Float32ValueImpl(value, sourcePsi)

    /**
     * 创建 Float64 常量注解值。
     */
    public fun float64Value(value: Double, sourcePsi: CjElement?): CaConstantValue.Float64Value =
        Float64ValueImpl(value, sourcePsi)

    /**
     * 创建 String 常量注解值。
     */
    public fun stringValue(value: String, sourcePsi: CjElement?): CaConstantValue.StringValue =
        StringValueImpl(value, sourcePsi)

    /**
     * 创建错误常量注解值。
     */
    public fun errorValue(errorMessage: String, sourcePsi: CjElement?): CaConstantValue.ErrorValue =
        ErrorValueImpl(errorMessage, sourcePsi)
}

/**
 * 包装 [CaConstantValue] 的注解值实现。
 */
private class ConstantValueImpl(
    /**
     * 被包装的类型化常量值。
     */
    private val backingValue: CaConstantValue,
    /**
     * 产生该注解值的源码 PSI。
     */
    private val backingSourcePsi: CjElement?,
    /**
     * 该注解值绑定的 lifetime token。
     */
    override val token: CaLifetimeToken,
) : CaAnnotationValue.ConstantValue {
    /**
     * 返回被包装的常量值。
     */
    override val value: CaConstantValue
        get() = withValidityAssertion { backingValue }

    /**
     * 返回该注解值的来源 PSI。
     */
    override val sourcePsi: CjElement?
        get() = withValidityAssertion { backingSourcePsi }
}

/**
 * enum 注解值实现。
 */
private class EnumValueImpl(
    /**
     * enum callable 的稳定标识。
     */
    private val backingCallableId: CallableId?,
    /**
     * enum 调用携带的注解实参。
     */
    private val backingArguments: List<CaAnnotationValue>,
    /**
     * 产生该注解值的源码 PSI。
     */
    private val backingSourcePsi: CjElement?,
    /**
     * 该注解值绑定的 lifetime token。
     */
    override val token: CaLifetimeToken,
) : CaAnnotationValue.EnumValue {
    /**
     * 返回 enum callable 标识。
     */
    override val callableId: CallableId?
        get() = withValidityAssertion { backingCallableId }

    /**
     * 返回 enum 注解实参列表。
     */
    override val arguments: List<CaAnnotationValue>
        get() = withValidityAssertion { backingArguments }

    /**
     * 返回该注解值的来源 PSI。
     */
    override val sourcePsi: CjElement?
        get() = withValidityAssertion { backingSourcePsi }
}

/**
 * tuple 注解值实现。
 */
private class TupleValueImpl(
    /**
     * tuple 内部的注解值列表。
     */
    private val backingValues: List<CaAnnotationValue>,
    /**
     * 产生该注解值的源码 PSI。
     */
    private val backingSourcePsi: CjElement?,
    /**
     * 该注解值绑定的 lifetime token。
     */
    override val token: CaLifetimeToken,
) : CaAnnotationValue.TupleValue {
    /**
     * 返回 tuple 元素值列表。
     */
    override val values: List<CaAnnotationValue>
        get() = withValidityAssertion { backingValues }

    /**
     * 返回该注解值的来源 PSI。
     */
    override val sourcePsi: CjElement?
        get() = withValidityAssertion { backingSourcePsi }
}

/**
 * class instance 注解值实现。
 */
private class ClassInstanceValueImpl(
    /**
     * class instance 对应类型的 classId。
     */
    private val backingClassId: ClassId?,
    /**
     * class instance 的命名注解实参。
     */
    private val backingArguments: List<CaNamedAnnotationValue>,
    /**
     * 产生该注解值的源码 PSI。
     */
    private val backingSourcePsi: CjElement?,
    /**
     * 该注解值绑定的 lifetime token。
     */
    override val token: CaLifetimeToken,
) : CaAnnotationValue.ClassInstanceValue {
    /**
     * 返回 class instance 的 classId。
     */
    override val classId: ClassId?
        get() = withValidityAssertion { backingClassId }

    /**
     * 返回 class instance 的命名实参。
     */
    override val arguments: List<CaNamedAnnotationValue>
        get() = withValidityAssertion { backingArguments }

    /**
     * 返回该注解值的来源 PSI。
     */
    override val sourcePsi: CjElement?
        get() = withValidityAssertion { backingSourcePsi }
}

/**
 * struct instance 注解值实现。
 */
private class StructInstanceValueImpl(
    /**
     * struct instance 对应类型的 classId。
     */
    private val backingClassId: ClassId?,
    /**
     * struct instance 的命名注解实参。
     */
    private val backingArguments: List<CaNamedAnnotationValue>,
    /**
     * 产生该注解值的源码 PSI。
     */
    private val backingSourcePsi: CjElement?,
    /**
     * 该注解值绑定的 lifetime token。
     */
    override val token: CaLifetimeToken,
) : CaAnnotationValue.StructInstanceValue {
    /**
     * 返回 struct instance 的 classId。
     */
    override val classId: ClassId?
        get() = withValidityAssertion { backingClassId }

    /**
     * 返回 struct instance 的命名实参。
     */
    override val arguments: List<CaNamedAnnotationValue>
        get() = withValidityAssertion { backingArguments }

    /**
     * 返回该注解值的来源 PSI。
     */
    override val sourcePsi: CjElement?
        get() = withValidityAssertion { backingSourcePsi }
}

/**
 * 常量值渲染辅助基类。
 *
 * 该基类只承载共享的 `sourcePsi` / `toString()` 行为，
 * 不直接参与 sealed 根接口继承。
 */
private abstract class AbstractConstantValue(
    /**
     * 产生该常量值的源码 PSI。
     */
    protected val backingSourcePsi: CjElement?,
) {
    /**
     * 使用常量值的稳定渲染文本作为字符串表示。
     */
    final override fun toString(): String = renderString()

    /**
     * 渲染该常量值的仓颉字面量文本。
     */
    protected abstract fun renderString(): String
}

/**
 * Bool 常量值实现。
 */
private class BoolValueImpl(
    /**
     * Bool 常量的真实值。
     */
    override val value: Boolean,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.BoolValue {
    /**
     * 返回该常量值的来源 PSI。
     */
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    /**
     * 渲染 Bool 常量。
     */
    override fun render(): String = renderString()

    /**
     * 返回 Bool 字面量文本。
     */
    override fun renderString(): String = value.toString()
}

/**
 * Rune 常量值实现。
 */
private class RuneValueImpl(
    /**
     * Rune 常量的 Unicode code point。
     */
    override val value: Int,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.RuneValue {
    /**
     * 返回该常量值的来源 PSI。
     */
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    /**
     * 渲染 Rune 常量。
     */
    override fun render(): String = renderString()

    /**
     * 返回 Rune 字面量文本。
     */
    override fun renderString(): String = renderRuneLiteral(value)
}

/**
 * Int8 常量值实现。
 */
private class Int8ValueImpl(
    /**
     * Int8 常量的真实值。
     */
    override val value: Byte,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.Int8Value {
    /**
     * 返回该常量值的来源 PSI。
     */
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    /**
     * 渲染 Int8 常量。
     */
    override fun render(): String = renderString()

    /**
     * 返回带 `i8` 后缀的字面量文本。
     */
    override fun renderString(): String = "${value}i8"
}

/**
 * Int16 常量值实现。
 */
private class Int16ValueImpl(
    /**
     * Int16 常量的真实值。
     */
    override val value: Short,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.Int16Value {
    /**
     * 返回该常量值的来源 PSI。
     */
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    /**
     * 渲染 Int16 常量。
     */
    override fun render(): String = renderString()

    /**
     * 返回带 `i16` 后缀的字面量文本。
     */
    override fun renderString(): String = "${value}i16"
}

/**
 * Int32 常量值实现。
 */
private class Int32ValueImpl(
    /**
     * Int32 常量的真实值。
     */
    override val value: Int,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.Int32Value {
    /**
     * 返回该常量值的来源 PSI。
     */
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    /**
     * 渲染 Int32 常量。
     */
    override fun render(): String = renderString()

    /**
     * 返回带 `i32` 后缀的字面量文本。
     */
    override fun renderString(): String = "${value}i32"
}

/**
 * Int64 常量值实现。
 */
private class Int64ValueImpl(
    /**
     * Int64 常量的真实值。
     */
    override val value: Long,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.Int64Value {
    /**
     * 返回该常量值的来源 PSI。
     */
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    /**
     * 渲染 Int64 常量。
     */
    override fun render(): String = renderString()

    /**
     * 返回带 `i64` 后缀的字面量文本。
     */
    override fun renderString(): String = "${value}i64"
}

/**
 * IntNative 常量值实现。
 */
private class IntNativeValueImpl(
    /**
     * IntNative 常量的真实值。
     */
    override val value: Long,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.IntNativeValue {
    /**
     * 返回该常量值的来源 PSI。
     */
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    /**
     * 渲染 IntNative 常量。
     */
    override fun render(): String = renderString()

    /**
     * 返回带 `inative` 后缀的字面量文本。
     */
    override fun renderString(): String = "${value}inative"
}

/**
 * UInt8 常量值实现。
 */
private class UInt8ValueImpl(
    /**
     * UInt8 常量的真实值。
     */
    override val value: UByte,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.UInt8Value {
    /**
     * 返回该常量值的来源 PSI。
     */
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    /**
     * 渲染 UInt8 常量。
     */
    override fun render(): String = renderString()

    /**
     * 返回带 `u8` 后缀的字面量文本。
     */
    override fun renderString(): String = "${value}u8"
}

/**
 * UInt16 常量值实现。
 */
private class UInt16ValueImpl(
    /**
     * UInt16 常量的真实值。
     */
    override val value: UShort,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.UInt16Value {
    /**
     * 返回该常量值的来源 PSI。
     */
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    /**
     * 渲染 UInt16 常量。
     */
    override fun render(): String = renderString()

    /**
     * 返回带 `u16` 后缀的字面量文本。
     */
    override fun renderString(): String = "${value}u16"
}

/**
 * UInt32 常量值实现。
 */
private class UInt32ValueImpl(
    /**
     * UInt32 常量的真实值。
     */
    override val value: UInt,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.UInt32Value {
    /**
     * 返回该常量值的来源 PSI。
     */
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    /**
     * 渲染 UInt32 常量。
     */
    override fun render(): String = renderString()

    /**
     * 返回带 `u32` 后缀的字面量文本。
     */
    override fun renderString(): String = "${value}u32"
}

/**
 * UInt64 常量值实现。
 */
private class UInt64ValueImpl(
    /**
     * UInt64 常量的真实值。
     */
    override val value: ULong,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.UInt64Value {
    /**
     * 返回该常量值的来源 PSI。
     */
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    /**
     * 渲染 UInt64 常量。
     */
    override fun render(): String = renderString()

    /**
     * 返回带 `u64` 后缀的字面量文本。
     */
    override fun renderString(): String = "${value}u64"
}

/**
 * UIntNative 常量值实现。
 */
private class UIntNativeValueImpl(
    /**
     * UIntNative 常量的真实值。
     */
    override val value: ULong,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.UIntNativeValue {
    /**
     * 返回该常量值的来源 PSI。
     */
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    /**
     * 渲染 UIntNative 常量。
     */
    override fun render(): String = renderString()

    /**
     * 返回带 `unative` 后缀的字面量文本。
     */
    override fun renderString(): String = "${value}unative"
}

/**
 * Float16 常量值实现。
 */
private class Float16ValueImpl(
    /**
     * Float16 常量的真实值。
     */
    override val value: Float,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.Float16Value {
    /**
     * 返回该常量值的来源 PSI。
     */
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    /**
     * 渲染 Float16 常量。
     */
    override fun render(): String = renderString()

    /**
     * 返回带 `f16` 后缀的字面量文本。
     */
    override fun renderString(): String = "${renderFloatingPoint(value.toDouble())}f16"
}

/**
 * Float32 常量值实现。
 */
private class Float32ValueImpl(
    /**
     * Float32 常量的真实值。
     */
    override val value: Float,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.Float32Value {
    /**
     * 返回该常量值的来源 PSI。
     */
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    /**
     * 渲染 Float32 常量。
     */
    override fun render(): String = renderString()

    /**
     * 返回带 `f32` 后缀的字面量文本。
     */
    override fun renderString(): String = "${renderFloatingPoint(value.toDouble())}f32"
}

/**
 * Float64 常量值实现。
 */
private class Float64ValueImpl(
    /**
     * Float64 常量的真实值。
     */
    override val value: Double,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.Float64Value {
    /**
     * 返回该常量值的来源 PSI。
     */
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    /**
     * 渲染 Float64 常量。
     */
    override fun render(): String = renderString()

    /**
     * 返回带 `f64` 后缀的字面量文本。
     */
    override fun renderString(): String = "${renderFloatingPoint(value)}f64"
}

/**
 * String 常量值实现。
 */
private class StringValueImpl(
    /**
     * String 常量的真实值。
     */
    override val value: String,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.StringValue {
    /**
     * 返回该常量值的来源 PSI。
     */
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    /**
     * 渲染 String 常量。
     */
    override fun render(): String = renderString()

    /**
     * 返回带转义的字符串字面量文本。
     */
    override fun renderString(): String = buildString {
        append('"')
        appendEscapedString(value)
        append('"')
    }
}

/**
 * 错误常量值实现。
 */
private class ErrorValueImpl(
    /**
     * 错误常量携带的诊断文本。
     */
    override val errorMessage: String,
    /**
     * 产生该错误常量的源码 PSI。
     */
    override val sourcePsi: CjElement?,
) : CaConstantValue.ErrorValue {
    /**
     * 错误常量没有可读取的正常值。
     */
    override val value: Nothing
        get() = error("Cannot get value for CaConstantValue.ErrorValue: $errorMessage")

    /**
     * 渲染错误常量。
     */
    override fun render(): String = "error(\"${escapeString(errorMessage)}\")"

    /**
     * 使用错误常量渲染文本作为字符串表示。
     */
    override fun toString(): String = render()
}

/**
 * 规范化浮点字面量文本。
 *
 * 这里以语义值为中心生成稳定文本，不回放外部传入的源码片段。
 */
private fun renderFloatingPoint(value: Double): String {
    return when {
        value.isNaN() -> "NaN"
        value == Double.POSITIVE_INFINITY -> "Infinity"
        value == Double.NEGATIVE_INFINITY -> "-Infinity"
        else -> value.toString()
    }
}

/**
 * 将 Rune 渲染为稳定、可再表示的仓颉字面量。
 */
private fun renderRuneLiteral(codePoint: Int): String {
    val escapedContent = when (codePoint) {
        '\\'.code -> "\\\\"
        '\''.code -> "\\'"
        '\b'.code -> "\\b"
        '\t'.code -> "\\t"
        '\n'.code -> "\\n"
        '\r'.code -> "\\r"
        else -> {
            val printable = Character.isBmpCodePoint(codePoint) &&
                Character.isValidCodePoint(codePoint) &&
                !Character.isISOControl(codePoint)
            if (printable) {
                String(Character.toChars(codePoint))
            } else {
                renderUnicodeEscape(codePoint)
            }
        }
    }
    return "'$escapedContent'"
}

/**
 * 将 code point 渲染为 `\\uXXXX` 或 `\\UXXXXXXXX` 转义。
 */
private fun renderUnicodeEscape(codePoint: Int): String {
    return if (codePoint <= 0xFFFF) {
        "\\u${codePoint.toString(16).uppercase().padStart(4, '0')}"
    } else {
        "\\U${codePoint.toString(16).uppercase().padStart(8, '0')}"
    }
}

/**
 * 对字符串内容进行仓颉字面量转义。
 */
private fun escapeString(value: String): String = buildString {
    appendEscapedString(value)
}

/**
 * 向当前 [StringBuilder] 追加已转义的字符串内容。
 */
private fun StringBuilder.appendEscapedString(value: String) {
    value.codePoints().forEach { codePoint ->
        when (codePoint) {
            '\\'.code -> append("\\\\")
            '"'.code -> append("\\\"")
            '\b'.code -> append("\\b")
            '\t'.code -> append("\\t")
            '\n'.code -> append("\\n")
            '\r'.code -> append("\\r")
            else -> {
                if (Character.isISOControl(codePoint)) {
                    append(renderUnicodeEscape(codePoint))
                } else {
                    append(String(Character.toChars(codePoint)))
                }
            }
        }
    }
}
