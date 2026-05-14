package org.cangnova.cangjie.analysis.api.annotations

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallElement
import org.cangnova.cangjie.psi.CjElement

/**
 * Analysis API 中公开的注解语义视图。
 *
 * - 表示一处注解使用(callsite),而非注解类型本身;
 * - 同时承载 ClassId、shortName、PSI、构造参数与构造器符号,
 *   供 IDE/工具层无需碰底层即可定位、渲染、回填注解。
 *
 * 对齐 Kotlin Analysis API 的 `KaAnnotation`。
 */
interface CaAnnotation : CaLifetimeOwner {
    /** 注解类型的 ClassId,无法解析时为 `null`。 */
    val classId: ClassId?

    /** 注解类型的短名,与 [classId] 同步缺失时为 `null`。 */
    val shortName: Name?

    /** 源码中产生该注解的调用 PSI;非源码注解为 `null`。 */
    val psi: CjCallElement?

    /** 注解携带的具名参数列表(按定义顺序)。 */
    val arguments: List<CaNamedAnnotationValue>

    /** 解析得到的注解构造器符号,可能为 `null`(无法解析时)。 */
    val constructorSymbol: CaConstructorSymbol?


}

/**
 * 常量注解值的密封根接口。
 *
 * - 与 [CaAnnotationValue.ConstantValue] 配合,把注解参数中的字面量包装为强类型视图;
 * - 子接口与仓颉基础类型一一对应(Bool / Rune / 整数 / 浮点 / String 等);
 * - 特殊值通过 [ErrorValue] 表达常量求值失败,与正常路径区分。
 */
sealed interface CaConstantValue {
    /**
     * 常量的具体值。
     *
     * 类型与具体子接口对应,例如 [BoolValue.value] 返回 [Boolean]。
     * 对 [ErrorValue] 等特殊值,该字段会承载特定的占位语义。
     */
    val value: Any

    /**
     * 该常量产生自的源码 PSI;非源码常量返回 `null`。
     */
    val sourcePsi: CjElement?

    /**
     * 渲染为可写回源码的常量字面量,
     * 如 `true`、`'A'`、`1i8`、`3.14f32`、`null`、`"text"`。
     */
    fun render(): String


    /** 表示一个仓颉 [Bool] 类型常量值。 */
    interface BoolValue : CaConstantValue {
        override val value: Boolean
    }

    /** 表示一个仓颉 [Rune] 类型常量值，载荷为 Unicode 码点。 */
    interface RuneValue : CaConstantValue {
        override val value: Int  // Unicode code point
    }

    // ── 有符号整数 ────────────────────────────────────────────

    /** 表示一个仓颉 [Int8] 类型常量值。 */
    interface Int8Value : CaConstantValue {
        override val value: Byte
    }

    /** 表示一个仓颉 [Int16] 类型常量值。 */
    interface Int16Value : CaConstantValue {
        override val value: Short
    }

    /** 表示一个仓颉 [Int32] 类型常量值。 */
    interface Int32Value : CaConstantValue {
        override val value: Int
    }

    /** 表示一个仓颉 [Int64] 类型常量值。 */
    interface Int64Value : CaConstantValue {
        override val value: Long
    }

    /** 表示一个仓颉 [IntNative] 类型常量值。 */
    interface IntNativeValue : CaConstantValue {
        override val value: Long  // 平台相关，用 Long 兜底
    }

    // ── 无符号整数 ────────────────────────────────────────────

    /** 表示一个仓颉 [UInt8] 类型常量值。 */
    interface UInt8Value : CaConstantValue {
        override val value: UByte
    }

    /** 表示一个仓颉 [UInt16] 类型常量值。 */
    interface UInt16Value : CaConstantValue {
        override val value: UShort
    }

    /** 表示一个仓颉 [UInt32] 类型常量值。 */
    interface UInt32Value : CaConstantValue {
        override val value: UInt
    }

    /** 表示一个仓颉 [UInt64] 类型常量值。 */
    interface UInt64Value : CaConstantValue {
        override val value: ULong
    }

    /** 表示一个仓颉 [UIntNative] 类型常量值。 */
    interface UIntNativeValue : CaConstantValue {
        override val value: ULong  // 平台相关，用 ULong 兜底
    }

    // ── 浮点数 ────────────────────────────────────────────────

    /** 表示一个仓颉 [Float16] 类型常量值。 */
    interface Float16Value : CaConstantValue {
        override val value: Float  // JVM 无 Float16，用 Float 兜底
    }

    /** 表示一个仓颉 [Float32] 类型常量值。 */
    interface Float32Value : CaConstantValue {
        override val value: Float
    }

    /** 表示一个仓颉 [Float64] 类型常量值。 */
    interface Float64Value : CaConstantValue {
        override val value: Double
    }

    // ── 字符串 ────────────────────────────────────────────────

    /** 表示一个仓颉 [String] 类型常量值。 */
    interface StringValue : CaConstantValue {
        override val value: String
    }

    // ── 特殊值 ────────────────────────────────────────────────

    /**
     * 表示非常量值,或者常量求值过程中的错误(如除零)。
     */
    interface ErrorValue : CaConstantValue {
        /** 求值错误的描述信息。 */
        val errorMessage: String

        override val value: Nothing
    }
}

/**
 * 注解参数值的密封根接口。
 *
 * 表示出现在注解 callsite 中的实参形态:
 * 常量、枚举常量、tuple、class 实例、struct 实例;
 * 与官方仓颉编译器的注解常量求值结果分类对齐。
 */
sealed interface CaAnnotationValue : CaLifetimeOwner {
    /**
     * 该值在源码中对应的 PSI 元素。
     *
     * 仅源码注解定义,库文件中的注解返回 `null`。
     */
    val sourcePsi: CjElement?

    /**
     * 常量类型的注解值,例如 `1` 或 `"foo"`。
     *
     * @see CaConstantValue
     */
    interface ConstantValue : CaAnnotationValue {
        /**
         * 包装为 [CaConstantValue] 的常量值(数字、[Boolean]、[Char]、[String] 等)。
         */
        val value: CaConstantValue
    }

    /**
     * 仓颉官方编译器中的枚举注解值。
     *
     * 与 Kotlin 风格的"仅枚举项"不同，仓颉官方实现允许带参数的枚举常量进入注解值域，
     * 因此这里使用 [EnumValue] 统一表示无参和有参两类枚举值。
     */
    interface EnumValue : CaAnnotationValue {
        /**
         * 枚举构造器或枚举项的符号标识。
         */
        val callableId: CallableId?

        /**
         * 枚举值携带的构造参数。
         *
         * 无参枚举值返回空列表。
         */
        val arguments: List<CaAnnotationValue>
    }

    /**
     * 仓颉官方编译器中的 tuple 注解值。
     *
     * 官方实现会将 tuple 常量表达式直接纳入注解值求值结果，因此分析 API 需要显式暴露该语义分支。
     */
    interface TupleValue : CaAnnotationValue {
        /**
         * tuple 中按位置保存的元素值。
         */
        val values: List<CaAnnotationValue>
    }

    /**
     * 仓颉官方编译器中的类实例注解值。
     *
     * 这里对应官方常量求值结果中的 class 实例，而不是 Kotlin 风格的嵌套注解或类字面量。
     */
    interface ClassInstanceValue : CaAnnotationValue {
        /**
         * 实例所属的类标识。
         */
        val classId: ClassId?

        /**
         * 实例初始化后写入的具名字段值。
         */
        val arguments: List<CaNamedAnnotationValue>
    }

    /**
     * 仓颉官方编译器中的 struct 实例注解值。
     *
     * 官方常量求值会将 struct 常量实例与 class 实例分开识别，因此 API 层也保持同样的语义区分。
     */
    interface StructInstanceValue : CaAnnotationValue {
        /**
         * 实例所属的 struct 标识。
         */
        val classId: ClassId?

        /**
         * 实例初始化后写入的具名字段值。
         */
        val arguments: List<CaNamedAnnotationValue>
    }
}

/**
 * 具名注解参数。
 *
 * 注解参数总以 `name = value` 的形式出现,这里统一封装为(名称, 值)二元组。
 */
interface CaNamedAnnotationValue : CaLifetimeOwner {
    /** 参数名。 */
    val name: Name

    /** 参数值。 */
    val expression: CaAnnotationValue
}
