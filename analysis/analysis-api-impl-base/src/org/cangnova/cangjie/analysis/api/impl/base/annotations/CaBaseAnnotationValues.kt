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
    public fun constant(
        value: CaConstantValue,
        sourcePsi: CjElement?,
        token: CaLifetimeToken,
    ): CaAnnotationValue.ConstantValue = ConstantValueImpl(value, sourcePsi, token)

    public fun enumValue(
        callableId: CallableId?,
        arguments: List<CaAnnotationValue>,
        sourcePsi: CjElement?,
        token: CaLifetimeToken,
    ): CaAnnotationValue.EnumValue = EnumValueImpl(callableId, arguments, sourcePsi, token)

    public fun tupleValue(
        values: List<CaAnnotationValue>,
        sourcePsi: CjElement?,
        token: CaLifetimeToken,
    ): CaAnnotationValue.TupleValue = TupleValueImpl(values, sourcePsi, token)

    public fun classInstanceValue(
        classId: ClassId?,
        arguments: List<CaNamedAnnotationValue>,
        sourcePsi: CjElement?,
        token: CaLifetimeToken,
    ): CaAnnotationValue.ClassInstanceValue = ClassInstanceValueImpl(classId, arguments, sourcePsi, token)

    public fun structInstanceValue(
        classId: ClassId?,
        arguments: List<CaNamedAnnotationValue>,
        sourcePsi: CjElement?,
        token: CaLifetimeToken,
    ): CaAnnotationValue.StructInstanceValue = StructInstanceValueImpl(classId, arguments, sourcePsi, token)

    public fun boolValue(value: Boolean, sourcePsi: CjElement?): CaConstantValue.BoolValue =
        BoolValueImpl(value, sourcePsi)

    public fun runeValue(value: Int, sourcePsi: CjElement?): CaConstantValue.RuneValue =
        RuneValueImpl(value, sourcePsi)

    public fun int8Value(value: Byte, sourcePsi: CjElement?): CaConstantValue.Int8Value =
        Int8ValueImpl(value, sourcePsi)

    public fun int16Value(value: Short, sourcePsi: CjElement?): CaConstantValue.Int16Value =
        Int16ValueImpl(value, sourcePsi)

    public fun int32Value(value: Int, sourcePsi: CjElement?): CaConstantValue.Int32Value =
        Int32ValueImpl(value, sourcePsi)

    public fun int64Value(value: Long, sourcePsi: CjElement?): CaConstantValue.Int64Value =
        Int64ValueImpl(value, sourcePsi)

    public fun intNativeValue(value: Long, sourcePsi: CjElement?): CaConstantValue.IntNativeValue =
        IntNativeValueImpl(value, sourcePsi)

    public fun uint8Value(value: UByte, sourcePsi: CjElement?): CaConstantValue.UInt8Value =
        UInt8ValueImpl(value, sourcePsi)

    public fun uint16Value(value: UShort, sourcePsi: CjElement?): CaConstantValue.UInt16Value =
        UInt16ValueImpl(value, sourcePsi)

    public fun uint32Value(value: UInt, sourcePsi: CjElement?): CaConstantValue.UInt32Value =
        UInt32ValueImpl(value, sourcePsi)

    public fun uint64Value(value: ULong, sourcePsi: CjElement?): CaConstantValue.UInt64Value =
        UInt64ValueImpl(value, sourcePsi)

    public fun uintNativeValue(value: ULong, sourcePsi: CjElement?): CaConstantValue.UIntNativeValue =
        UIntNativeValueImpl(value, sourcePsi)

    public fun float16Value(value: Float, sourcePsi: CjElement?): CaConstantValue.Float16Value =
        Float16ValueImpl(value, sourcePsi)

    public fun float32Value(value: Float, sourcePsi: CjElement?): CaConstantValue.Float32Value =
        Float32ValueImpl(value, sourcePsi)

    public fun float64Value(value: Double, sourcePsi: CjElement?): CaConstantValue.Float64Value =
        Float64ValueImpl(value, sourcePsi)

    public fun stringValue(value: String, sourcePsi: CjElement?): CaConstantValue.StringValue =
        StringValueImpl(value, sourcePsi)

    public fun errorValue(errorMessage: String, sourcePsi: CjElement?): CaConstantValue.ErrorValue =
        ErrorValueImpl(errorMessage, sourcePsi)
}

private class ConstantValueImpl(
    private val backingValue: CaConstantValue,
    private val backingSourcePsi: CjElement?,
    override val token: CaLifetimeToken,
) : CaAnnotationValue.ConstantValue {
    override val value: CaConstantValue
        get() = withValidityAssertion { backingValue }

    override val sourcePsi: CjElement?
        get() = withValidityAssertion { backingSourcePsi }
}

private class EnumValueImpl(
    private val backingCallableId: CallableId?,
    private val backingArguments: List<CaAnnotationValue>,
    private val backingSourcePsi: CjElement?,
    override val token: CaLifetimeToken,
) : CaAnnotationValue.EnumValue {
    override val callableId: CallableId?
        get() = withValidityAssertion { backingCallableId }

    override val arguments: List<CaAnnotationValue>
        get() = withValidityAssertion { backingArguments }

    override val sourcePsi: CjElement?
        get() = withValidityAssertion { backingSourcePsi }
}

private class TupleValueImpl(
    private val backingValues: List<CaAnnotationValue>,
    private val backingSourcePsi: CjElement?,
    override val token: CaLifetimeToken,
) : CaAnnotationValue.TupleValue {
    override val values: List<CaAnnotationValue>
        get() = withValidityAssertion { backingValues }

    override val sourcePsi: CjElement?
        get() = withValidityAssertion { backingSourcePsi }
}

private class ClassInstanceValueImpl(
    private val backingClassId: ClassId?,
    private val backingArguments: List<CaNamedAnnotationValue>,
    private val backingSourcePsi: CjElement?,
    override val token: CaLifetimeToken,
) : CaAnnotationValue.ClassInstanceValue {
    override val classId: ClassId?
        get() = withValidityAssertion { backingClassId }

    override val arguments: List<CaNamedAnnotationValue>
        get() = withValidityAssertion { backingArguments }

    override val sourcePsi: CjElement?
        get() = withValidityAssertion { backingSourcePsi }
}

private class StructInstanceValueImpl(
    private val backingClassId: ClassId?,
    private val backingArguments: List<CaNamedAnnotationValue>,
    private val backingSourcePsi: CjElement?,
    override val token: CaLifetimeToken,
) : CaAnnotationValue.StructInstanceValue {
    override val classId: ClassId?
        get() = withValidityAssertion { backingClassId }

    override val arguments: List<CaNamedAnnotationValue>
        get() = withValidityAssertion { backingArguments }

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
    protected val backingSourcePsi: CjElement?,
) {
    final override fun toString(): String = renderString()

    protected abstract fun renderString(): String
}

private class BoolValueImpl(
    override val value: Boolean,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.BoolValue {
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    override fun render(): String = renderString()

    override fun renderString(): String = value.toString()
}

private class RuneValueImpl(
    override val value: Int,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.RuneValue {
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    override fun render(): String = renderString()

    override fun renderString(): String = renderRuneLiteral(value)
}

private class Int8ValueImpl(
    override val value: Byte,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.Int8Value {
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    override fun render(): String = renderString()

    override fun renderString(): String = "${value}i8"
}

private class Int16ValueImpl(
    override val value: Short,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.Int16Value {
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    override fun render(): String = renderString()

    override fun renderString(): String = "${value}i16"
}

private class Int32ValueImpl(
    override val value: Int,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.Int32Value {
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    override fun render(): String = renderString()

    override fun renderString(): String = "${value}i32"
}

private class Int64ValueImpl(
    override val value: Long,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.Int64Value {
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    override fun render(): String = renderString()

    override fun renderString(): String = "${value}i64"
}

private class IntNativeValueImpl(
    override val value: Long,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.IntNativeValue {
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    override fun render(): String = renderString()

    override fun renderString(): String = "${value}inative"
}

private class UInt8ValueImpl(
    override val value: UByte,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.UInt8Value {
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    override fun render(): String = renderString()

    override fun renderString(): String = "${value}u8"
}

private class UInt16ValueImpl(
    override val value: UShort,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.UInt16Value {
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    override fun render(): String = renderString()

    override fun renderString(): String = "${value}u16"
}

private class UInt32ValueImpl(
    override val value: UInt,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.UInt32Value {
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    override fun render(): String = renderString()

    override fun renderString(): String = "${value}u32"
}

private class UInt64ValueImpl(
    override val value: ULong,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.UInt64Value {
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    override fun render(): String = renderString()

    override fun renderString(): String = "${value}u64"
}

private class UIntNativeValueImpl(
    override val value: ULong,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.UIntNativeValue {
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    override fun render(): String = renderString()

    override fun renderString(): String = "${value}unative"
}

private class Float16ValueImpl(
    override val value: Float,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.Float16Value {
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    override fun render(): String = renderString()

    override fun renderString(): String = "${renderFloatingPoint(value.toDouble())}f16"
}

private class Float32ValueImpl(
    override val value: Float,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.Float32Value {
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    override fun render(): String = renderString()

    override fun renderString(): String = "${renderFloatingPoint(value.toDouble())}f32"
}

private class Float64ValueImpl(
    override val value: Double,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.Float64Value {
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    override fun render(): String = renderString()

    override fun renderString(): String = "${renderFloatingPoint(value)}f64"
}

private class StringValueImpl(
    override val value: String,
    sourcePsi: CjElement?,
) : AbstractConstantValue(sourcePsi), CaConstantValue.StringValue {
    override val sourcePsi: CjElement?
        get() = backingSourcePsi

    override fun render(): String = renderString()

    override fun renderString(): String = buildString {
        append('"')
        appendEscapedString(value)
        append('"')
    }
}

private class ErrorValueImpl(
    override val errorMessage: String,
    override val sourcePsi: CjElement?,
) : CaConstantValue.ErrorValue {
    override val value: Nothing
        get() = error("Cannot get value for CaConstantValue.ErrorValue: $errorMessage")

    override fun render(): String = "error(\"${escapeString(errorMessage)}\")"

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

private fun renderUnicodeEscape(codePoint: Int): String {
    return if (codePoint <= 0xFFFF) {
        "\\u${codePoint.toString(16).uppercase().padStart(4, '0')}"
    } else {
        "\\U${codePoint.toString(16).uppercase().padStart(8, '0')}"
    }
}

private fun escapeString(value: String): String = buildString {
    appendEscapedString(value)
}

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
