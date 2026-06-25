package org.cangnova.cangjie.cfir.resolve.dfa

import org.cangnova.cangjie.cfir.DfaType
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * DFA 逻辑系统中的陈述基类。
 */
sealed class Statement {
    /** 该陈述约束的数据流变量。 */
    abstract val variable: DataFlowVariable
}

/**
 * 布尔/空值操作陈述。
 *
 * @property variable 被判断的数据流变量。
 * @property operation 对变量应用的操作。
 */
data class OperationStatement(
    /**
     * 被判断的数据流变量。
     */
    override val variable: DataFlowVariable,
    /**
     * 对变量应用的布尔或空值操作。
     */
    val operation: Operation,
) : Statement() {
    /** 陈述的调试文本。 */
    override fun toString(): String = "$variable $operation"
}

/**
 * 类型陈述。
 *
 * 上界表示变量被 smart cast 到的类型集合；下界表示变量被排除的类型、常量或符号集合。
 */
sealed class TypeStatement : Statement() {
    /** 该类型陈述约束的数据流变量。 */
    abstract override val variable: DataFlowVariable

    /** 变量已知满足的上界类型集合。 */
    abstract val upperTypes: Set<ConeCangJieType>

    /** 变量已知不等于或不属于的下界集合。 */
    abstract val lowerTypes: Set<DfaType>

    /** 该陈述是否没有任何上下界信息。 */
    val isEmpty: Boolean
        get() = upperTypes.isEmpty() && lowerTypes.isEmpty()

    /** 该陈述是否包含至少一个上下界信息。 */
    val isNotEmpty: Boolean
        get() = !isEmpty

    /** 非空上界集合；为空集合时返回 `null`。 */
    val upperTypesOrNull: Set<ConeCangJieType>?
        get() = upperTypes.takeIf { it.isNotEmpty() }

    /** 非空下界集合；为空集合时返回 `null`。 */
    val lowerTypesOrNull: Set<DfaType>?
        get() = lowerTypes.takeIf { it.isNotEmpty() }

    /** 陈述的调试文本。 */
    override fun toString(): String = "$variable: ${renderType()}"

    /**
     * 渲染类型上下界组合。
     */
    fun renderType(): String = listOfNotNull(
        upperTypesOrNull?.joinToString(" & "),
        lowerTypesOrNull?.joinToString(" | ")?.let { "¬($it)" },
    ).joinToString(" & ")
}

/**
 * 条件成立时推出的 DFA 效果。
 *
 * @property condition 条件陈述。
 * @property effect 条件成立后可加入 flow 的效果陈述。
 */
class Implication(
    /**
     * 条件陈述。
     */
    val condition: OperationStatement,
    /**
     * 条件成立后可加入 flow 的效果陈述。
     */
    val effect: Statement,
) {
    /** 蕴含关系的调试文本。 */
    override fun toString(): String = "$condition -> $effect"
}

/**
 * DFA 支持的布尔/空值操作。
 */
enum class Operation {
    /** 变量等于 `true`。 */
    EqTrue,

    /** 变量等于 `false`。 */
    EqFalse,

    /** 变量等于 `null`。 */
    EqNull,

    /** 变量不等于 `null`。 */
    NotEqNull,
    ;

    /**
     * 在已知 [given] 成立时判断当前操作是否可确定。
     */
    fun valueIfKnown(given: Operation): Boolean? = when (this) {
        EqTrue, EqFalse -> if (given == NotEqNull) null else given == this
        EqNull -> given == EqNull
        NotEqNull -> given != EqNull
    }

    /** 操作的调试文本。 */
    override fun toString(): String = when (this) {
        EqTrue -> "== True"
        EqFalse -> "== False"
        EqNull -> "== Null"
        NotEqNull -> "!= Null"
    }
}
