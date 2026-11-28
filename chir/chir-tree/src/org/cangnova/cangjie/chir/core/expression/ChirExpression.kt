package org.cangnova.cangjie.chir.core.expression

import org.cangnova.cangjie.chir.core.attribute.ChirAttribute
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirNode
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.chir.core.value.ChirValue

/**
 * CHIR 表达式所属的语义域。
 */
enum class ChirExpressionDomain {
    UNARY,
    BINARY,
    MEMORY,
    CALL,
    TUPLE,
    FIELD,
    CONSTANT,
    OTHERS,
}

/**
 * CHIR 表达式节点公共接口。
 */
interface ChirExpression : ChirNode {
    /**
     * 表达式所属语义域。
     */
    val domain: ChirExpressionDomain

    /**
     * 表达式结果类型；无值表达式或未知类型时为 `null`。
     */
    val resultType: ChirTypeRef?

    /**
     * 表达式定义的局部值身份。它与表达式表中的 [semanticId] 属于不同 ID 域，官方 CHIR 不要求两者相同。
     */
    val resultLocalValue: ChirLocalValue?
}

private fun defaultResultLocalValue(semanticId: ChirSemanticId, resultType: ChirTypeRef?): ChirLocalValue? =
    resultType?.let {
        ChirLocalValue(
            semanticId = semanticId,
            type = it,
            name = "",
            associatedExpressionId = semanticId,
        )
    }

/**
 * 一元表达式。
 */
data class ChirUnaryExpression(
    /**
     * 表达式语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 一元操作符名称。
     */
    val operator: String,

    /**
     * 一元操作数。
     */
    val operand: ChirValue,

    /**
     * 一元表达式结果类型。
     */
    override val resultType: ChirTypeRef,

    /**
     * 表达式定义的局部值；仓内构造器沿用表达式身份，官方导入可提供独立值身份。
     */
    override val resultLocalValue: ChirLocalValue? = defaultResultLocalValue(semanticId, resultType),

    /** 仓颉整数/浮点一元运算的溢出语义。 */
    val overflowStrategy: ChirOverflowStrategy = ChirOverflowStrategy.NOT_APPLICABLE,
) : ChirExpression {
    /**
     * 一元表达式的固定语义域。
     */
    override val domain: ChirExpressionDomain = ChirExpressionDomain.UNARY
}

/**
 * 二元表达式。
 */
data class ChirBinaryExpression(
    /**
     * 表达式语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 二元操作符名称。
     */
    val operator: String,

    /**
     * 左操作数。
     */
    val left: ChirValue,

    /**
     * 右操作数。
     */
    val right: ChirValue,

    /**
     * 二元表达式结果类型。
     */
    override val resultType: ChirTypeRef,

    /**
     * 表达式定义的局部值；官方导入可提供独立于表达式身份的 VALUE 身份。
     */
    override val resultLocalValue: ChirLocalValue? = defaultResultLocalValue(semanticId, resultType),

    /** 仓颉整数/浮点二元运算的溢出语义。 */
    val overflowStrategy: ChirOverflowStrategy = ChirOverflowStrategy.NOT_APPLICABLE,
) : ChirExpression {
    /**
     * 二元表达式的固定语义域。
     */
    override val domain: ChirExpressionDomain = ChirExpressionDomain.BINARY
}

/**
 * 内存操作表达式。
 */
data class ChirMemoryExpression(
    /**
     * 表达式语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 内存操作名称。
     */
    val operation: String,

    /**
     * 内存地址值。
     */
    val address: ChirValue,

    /**
     * 写入或交换等操作使用的值；纯读取操作可为空。
     */
    val value: ChirValue? = null,

    /**
     * 内存表达式结果类型。
     */
    override val resultType: ChirTypeRef? = null,

    /**
     * 按成员名访问时从地址根开始的字段路径。仅 BY_NAME 操作允许携带该路径。
     */
    val memberNames: List<String> = emptyList(),

    /**
     * 结果局部值身份；无结果类型的副作用操作不定义局部值。
     */
    override val resultLocalValue: ChirLocalValue? = defaultResultLocalValue(semanticId, resultType),
) : ChirExpression {
    /**
     * 内存表达式的固定语义域。
     */
    override val domain: ChirExpressionDomain = ChirExpressionDomain.MEMORY
}

/**
 * 调用表达式。
 */
data class ChirCallExpression(
    /**
     * 表达式语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 被调用目标值。
     */
    val callee: ChirValue,

    /**
     * 调用实参值列表。
     */
    val arguments: List<ChirValue>,

    /**
     * 调用表达式结果类型。
     */
    override val resultType: ChirTypeRef,

    /**
     * 调用是否使用父类分派语义。
     */
    val isSuperCall: Boolean = false,

    /**
     * 泛型调用点实际化的类型实参，顺序与被调用声明的类型参数一致。
     */
    val instantiatedTypeArguments: List<ChirTypeRef> = emptyList(),

    /**
     * 官方 CHIR 调用点记录的 this 类型；它是类型事实，不额外构造接收者值。
     */
    val thisType: ChirTypeRef? = null,

    /**
     * 动态分派调用携带的官方虚方法签名信息；静态 Apply 调用不设置此字段。
     */
    val virtualMethodContext: ChirVirtualMethodContext? = null,

    /** 动态分派调用的接收者，与可调用目标分开保存。 */
    val dispatchReceiver: ChirValue? = null,

    /**
     * 表达式定义的局部值；官方导入可与表达式 identity 分离。
     */
    override val resultLocalValue: ChirLocalValue? = defaultResultLocalValue(semanticId, resultType),
) : ChirExpression {
    /**
     * 调用表达式的固定语义域。
     */
    override val domain: ChirExpressionDomain = ChirExpressionDomain.CALL
}

/**
 * 官方虚调用保存的源方法身份和声明期泛型签名。
 */
data class ChirVirtualMethodContext(
    /** 虚方法的仓颉源码标识符。 */
    val sourceCodeIdentifier: String,

    /** 未按调用点类型实参实例化的原始函数签名。 */
    val originalFunctionType: ChirFunctionType,

    /** 虚方法声明的泛型参数，顺序与原始签名保持一致。 */
    val genericTypeParameters: List<ChirTypeRef>,
)

/**
 * 有序的 CHIR 聚合构造表达式。
 *
 * 仓颉 CHIR 的 Tuple 节点根据结果类型构造元组、结构体或枚举值；
 * 操作数顺序与结果布局一一对应，具体合法形状由结果类型决定。
 */
data class ChirTupleExpression(
    /**
     * 表达式语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 按布局顺序排列的聚合元素。
     */
    val elements: List<ChirValue>,

    /**
     * 聚合结果类型；决定元素的数量、顺序和语义形状。
     */
    override val resultType: ChirTypeRef,

    /**
     * 表达式定义的局部值；官方导入可提供不同于表达式 identity 的值 identity。
     */
    override val resultLocalValue: ChirLocalValue? = defaultResultLocalValue(semanticId, resultType),
) : ChirExpression {
    /**
     * 聚合表达式的固定语义域。
     */
    override val domain: ChirExpressionDomain = ChirExpressionDomain.TUPLE
}

/**
 * 从值聚合中按官方 CHIR 布局路径读取字段值。
 *
 * 该节点表示值投影，与返回字段地址的内存引用操作具有不同语义。
 */
data class ChirFieldExpression(
    /** 表达式语义标识。 */
    override val semanticId: ChirSemanticId,

    /** 被投影的聚合值。 */
    val base: ChirValue,

    /** 从外层到内层的字段布局索引。 */
    val path: List<ULong>,

    /** 字段投影结果类型。 */
    override val resultType: ChirTypeRef,

    /**
     * 表达式定义的局部值；官方导入可提供不同于表达式 identity 的值 identity。
     */
    override val resultLocalValue: ChirLocalValue? = defaultResultLocalValue(semanticId, resultType),
) : ChirExpression {
    /** 字段投影表达式的固定语义域。 */
    override val domain: ChirExpressionDomain = ChirExpressionDomain.FIELD
}

/**
 * 将官方 CHIR 字面量值定义为局部结果的常量表达式。
 */
data class ChirConstantExpression(
    /** 表达式语义标识。 */
    override val semanticId: ChirSemanticId,

    /** 被物化为局部结果的字面量值。 */
    val value: ChirConstantValue,

    /** 常量表达式结果类型。 */
    override val resultType: ChirTypeRef,

    /** 与官方 VALUE 身份对应的表达式结果局部值。 */
    override val resultLocalValue: ChirLocalValue,
) : ChirExpression {
    /** 常量表达式的固定语义域。 */
    override val domain: ChirExpressionDomain = ChirExpressionDomain.CONSTANT
}

/**
 * 其他后端或平台专用表达式。
 */
data class ChirOtherExpression(
    /**
     * 表达式语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 平台专用操作名称。
     */
    val operation: String,

    /**
     * 操作数列表。
     */
    val operands: List<ChirValue>,

    /**
     * 表达式结果类型。
     */
    override val resultType: ChirTypeRef? = null,
    /**
     * 后端专用表达式契约属性。
     *
     * CHIR 仍然是 JVM 后端的唯一输入；字段、构造器等 JVM ABI 信息必须通过结构化属性传入，
     * 后端禁止从展示名称或 operation 文本中猜测 owner/name/descriptor。
     */
    val attributes: Set<ChirAttribute> = emptySet(),

    /**
     * 表达式定义的局部值；无结果类型的副作用操作不定义局部值。
     */
    override val resultLocalValue: ChirLocalValue? = defaultResultLocalValue(semanticId, resultType),

    /** 官方 CHIR intrinsic union 携带的类型化操作种类。 */
    val intrinsicKind: ChirIntrinsicKind? = null,

    /** 官方 CHIR intrinsic 调用点实例化的类型实参。 */
    val intrinsicTypeArguments: List<ChirTypeRef> = emptyList(),

    /** 保留官方 Debug union 的源变量和准确环境对象类型。 */
    val debugMetadata: ChirDebugMetadata? = null,
) : ChirExpression {
    /**
     * 其他表达式的固定语义域。
     */
    override val domain: ChirExpressionDomain = ChirExpressionDomain.OTHERS
}

/** 官方 CHIR Debug 表携带的调试语义信息。 */
data class ChirDebugMetadata(
    /** Debug 表中的仓颉源标识符。 */
    val sourceCodeIdentifier: String,

    /** Debug 表声明的准确环境对象类型。 */
    val accurateEnvironmentObjectType: ChirTypeRef?,
)
