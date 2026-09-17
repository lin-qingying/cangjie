package org.cangnova.cangjie.cfir.expressions

import java.math.BigInteger
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeVArrayType

/**
 * 仓颉编译期解释器的值，不是构造器调用语法的副本。
 *
 * Kotlin FIR 可以用 literal / annotation call 表示其注解值；仓颉 const 构造器还会执行
 * 字段初始化和赋值，因此必须保留实际类型、字段存储与对象身份。
 * 对应官方 ComputeAnnotations.h 的 AnnoInstanceValue / AnnoInstanceClassInst。
 */
public sealed interface CfirConstantValue {
    public val type: ConeCangJieType

    /** 整数用 BigInteger 保留 UInt64 全域；Rune 保存 Unicode 标量而非 UTF-16 code unit。 */
    public data class Primitive(
        override val type: ConeCangJieType,
        val kind: CfirLiteralKind,
        val value: Any,
    ) : CfirConstantValue {
        init {
            require(when (kind) {
                CfirLiteralKind.INT, CfirLiteralKind.BYTE -> value is BigInteger
                CfirLiteralKind.FLOAT -> value is Double
                CfirLiteralKind.BOOLEAN -> value is Boolean
                CfirLiteralKind.RUNE -> value is Int && value in 0..0x10ffff && value !in 0xd800..0xdfff
                CfirLiteralKind.STRING -> value is String
                CfirLiteralKind.UNIT -> value === Unit
            }) { "Non-canonical constant payload for $kind: ${value::class}" }
        }
    }

    public data class Tuple(
        override val type: ConeCangJieType,
        val elements: List<CfirConstantValue>,
    ) : CfirConstantValue

    /**
     * 内置 Annotation.target 的专用数组参数；普通 Array 表达式不因此成为 const。
     * 官方 AnnotationChecker 为元素分别创建 const 变量，数组只负责保留参数集合。
     */
    public data class ArrayValue(
        override val type: ConeCangJieType,
        val elements: List<CfirConstantValue>,
    ) : CfirConstantValue

    public data class VArray(
        override val type: ConeVArrayType,
        val elements: List<CfirConstantValue>,
    ) : CfirConstantValue {
        init {
            require(type.size == elements.size.toLong()) { "VArray constant size does not match its type" }
        }
    }

    /**
     * fields 只包含执行完成后的存储槽。主构造参数通过 correspondingProperty 显式关联
     * 到真实成员，不能按参数名或调用实参顺序推断字段。
     *
     * 此类故意不使用 data class：两个字段内容相同的 class 对象仍有不同身份。
     * fields 由求值器发布为只读对象图，重复引用共享同一 identity。
     */
    public class ObjectValue(
        override val type: ConeCangJieType,
        val fields: Map<CfirCallableSymbol<*>, CfirConstantValue>,
        val identity: Long,
    ) : CfirConstantValue

    /** 零参数与带 payload 的枚举统一保留真实 constructor symbol，避免丢失枚举身份。 */
    public data class EnumValue(
        override val type: ConeCangJieType,
        val constructor: CfirEnumConstructorSymbol,
        val arguments: List<CfirConstantValue>,
    ) : CfirConstantValue
}
