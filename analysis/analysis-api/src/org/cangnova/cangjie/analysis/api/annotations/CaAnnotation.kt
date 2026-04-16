package org.cangnova.cangjie.analysis.api.annotations

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallElement
import org.cangnova.cangjie.psi.CjElement

/**
 * Analysis API 中公开的注解语义视图。
 */
interface CaAnnotation : CaLifetimeOwner {
    val classId: ClassId?

    val shortName: Name?
    public val psi: CjCallElement?
    public val arguments: List<CaNamedAnnotationValue>
    public val constructorSymbol: CaConstructorSymbol?


}

public sealed interface CaConstantValue {
    /**
     * The value of the constant. The type of [value] matches its represented class,
     * e.g. [BooleanValue.value] will return a [Boolean].
     *
     * For [NullValue] and [ErrorValue], [value] contains a special value.
     */
    public val value: Any

    /**
     * A source element from which the value was created.
     * The PSI might be `null` for constants from non-source files.
     */
    public val sourcePsi: CjElement?

    /**
     * Renders the value as a representable constant value [String],
     * such as `true`, `'A'`, `1i8`, `3.14f32`, `null`, `"text"`.
     */
    public fun render(): String


    /** Represents a [Bool](仓颉 Bool) value. */
    public interface BoolValue : CaConstantValue {
        override val value: Boolean
    }

    /** Represents a [Rune](仓颉 Rune) value, i.e. a Unicode code point. */
    public interface RuneValue : CaConstantValue {
        override val value: Int  // Unicode code point
    }

    // ── 有符号整数 ────────────────────────────────────────────

    /** Represents an [Int8](仓颉 Int8) value. */
    public interface Int8Value : CaConstantValue {
        override val value: Byte
    }

    /** Represents an [Int16](仓颉 Int16) value. */
    public interface Int16Value : CaConstantValue {
        override val value: Short
    }

    /** Represents an [Int32](仓颉 Int32) value. */
    public interface Int32Value : CaConstantValue {
        override val value: Int
    }

    /** Represents an [Int64](仓颉 Int64) value. */
    public interface Int64Value : CaConstantValue {
        override val value: Long
    }

    /** Represents an [IntNative](仓颉 IntNative) value. */
    public interface IntNativeValue : CaConstantValue {
        override val value: Long  // 平台相关，用 Long 兜底
    }

    // ── 无符号整数 ────────────────────────────────────────────

    /** Represents a [UInt8](仓颉 UInt8) value. */
    public interface UInt8Value : CaConstantValue {
        override val value: UByte
    }

    /** Represents a [UInt16](仓颉 UInt16) value. */
    public interface UInt16Value : CaConstantValue {
        override val value: UShort
    }

    /** Represents a [UInt32](仓颉 UInt32) value. */
    public interface UInt32Value : CaConstantValue {
        override val value: UInt
    }

    /** Represents a [UInt64](仓颉 UInt64) value. */
    public interface UInt64Value : CaConstantValue {
        override val value: ULong
    }

    /** Represents a [UIntNative](仓颉 UIntNative) value. */
    public interface UIntNativeValue : CaConstantValue {
        override val value: ULong  // 平台相关，用 ULong 兜底
    }

    // ── 浮点数 ────────────────────────────────────────────────

    /** Represents a [Float16](仓颉 Float16) value. */
    public interface Float16Value : CaConstantValue {
        override val value: Float  // JVM 无 Float16，用 Float 兜底
    }

    /** Represents a [Float32](仓颉 Float32) value. */
    public interface Float32Value : CaConstantValue {
        override val value: Float
    }

    /** Represents a [Float64](仓颉 Float64) value. */
    public interface Float64Value : CaConstantValue {
        override val value: Double
    }

    // ── 字符串 ────────────────────────────────────────────────

    /** Represents a [String](仓颉 String) value. */
    public interface StringValue : CaConstantValue {
        override val value: String
    }

    // ── 特殊值 ────────────────────────────────────────────────

    /**
     * Represents either a non-constant value, or a constant evaluation error
     * (such as a division by zero).
     */
    public interface ErrorValue : CaConstantValue {
        public val errorMessage: String
        override val value: Nothing
    }
}

public sealed interface CaAnnotationValue : CaLifetimeOwner {
    /**
     * The [CjElement] underlying the annotation value. This is only defined for annotations in source files. For libraries, it always
     * returns `null`.
     */
    public val sourcePsi: CjElement?

    /**
     * A constant annotation value, such as `1` or `"foo"`.
     *
     * @see CaConstantValue
     */
    public interface ConstantValue : CaAnnotationValue {
        /**
         * A constant value (a number, [Boolean], [Char], or [String]) wrapped into the [CaConstantValue] abstraction.
         */
        public val value: CaConstantValue
    }

    /**
     * 仓颉官方编译器中的枚举注解值。
     *
     * 与 Kotlin 风格的“仅枚举项”不同，仓颉官方实现允许带参数的枚举常量进入注解值域，
     * 因此这里使用 [EnumValue] 统一表示无参和有参两类枚举值。
     */
    public interface EnumValue : CaAnnotationValue {
        /**
         * 枚举构造器或枚举项的符号标识。
         */
        public val callableId: CallableId?

        /**
         * 枚举值携带的构造参数。
         *
         * 无参枚举值返回空列表。
         */
        public val arguments: List<CaAnnotationValue>
    }

    /**
     * 仓颉官方编译器中的 tuple 注解值。
     *
     * 官方实现会将 tuple 常量表达式直接纳入注解值求值结果，因此分析 API 需要显式暴露该语义分支。
     */
    public interface TupleValue : CaAnnotationValue {
        /**
         * tuple 中按位置保存的元素值。
         */
        public val values: List<CaAnnotationValue>
    }

    /**
     * 仓颉官方编译器中的类实例注解值。
     *
     * 这里对应官方常量求值结果中的 class 实例，而不是 Kotlin 风格的嵌套注解或类字面量。
     */
    public interface ClassInstanceValue : CaAnnotationValue {
        /**
         * 实例所属的类标识。
         */
        public val classId: ClassId?

        /**
         * 实例初始化后写入的具名字段值。
         */
        public val arguments: List<CaNamedAnnotationValue>
    }

    /**
     * 仓颉官方编译器中的 struct 实例注解值。
     *
     * 官方常量求值会将 struct 常量实例与 class 实例分开识别，因此 API 层也保持同样的语义区分。
     */
    public interface StructInstanceValue : CaAnnotationValue {
        /**
         * 实例所属的 struct 标识。
         */
        public val classId: ClassId?

        /**
         * 实例初始化后写入的具名字段值。
         */
        public val arguments: List<CaNamedAnnotationValue>
    }
}

public interface CaNamedAnnotationValue : CaLifetimeOwner {
    /**
     * The name of the annotation argument.
     */
    public val name: Name

    /**
     * The value of the annotation argument.
     */
    public val expression: CaAnnotationValue
}
