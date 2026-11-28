package org.cangnova.cangjie.cfir.resolve.dfa

import kotlinx.collections.immutable.toPersistentList

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

    /** 仅由完成值固有类型证明的上界；显式批准同一上界时从此集合移除，事实本身保留。 */
    abstract val intrinsicUpperTypes: Set<ConeCangJieType>

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

/** 本次使用的类型可比 value.originalType 窄，例如 Any 存储上的 Int smart cast。 */
data class ValueUse(val value: ValueVariable, val type: ConeCangJieType) {
    fun acceptSourceChildren(visitor: DfaSourceVisitor) {
        visitor.visitVariable(value)
        visitor.visitType(type)
    }
}

/** 事件身份独立于值身份；同一稳定值的两次读取仍是不同事件。 */
class ValueEvent(val origin: org.cangnova.cangjie.cfir.CfirElement)

/** 实际表达式完成或显式合流的事件；inputs 记录合流来源，不制造临时 AST。 */
class ExpressionEvent(
    val expression: org.cangnova.cangjie.cfir.expressions.CfirExpression,
    val origin: org.cangnova.cangjie.cfir.CfirElement,
    inputs: List<ExpressionEvent> = emptyList(),
) {
    val inputs = inputs.toPersistentList()
    fun acceptSourceChildren(visitor: DfaSourceVisitor) {
        visitor.visitElement(expression)
        visitor.visitElement(origin)
        inputs.forEach { it.acceptSourceChildren(visitor) }
    }
}

data class ExpressionCompletion(val value: ValueVariable, val type: ConeCangJieType, val event: ExpressionEvent) {
    fun asUse(): ValueUse = ValueUse(value, type)
    fun acceptSourceChildren(visitor: DfaSourceVisitor) {
        visitor.visitVariable(value)
        visitor.visitType(type)
        event.acceptSourceChildren(visitor)
    }
}

/**
 * 外部读取的初始化证明，与 smartcast 稳定性独立。
 * 声明证明消费正式初始化/导入/FFI事实；成员证明消费本次已完成 receiver。
 * 构造中的 own receiver 由 analyzer 的真实容器上下文排除，不以类型知识代替初始化。
 */
sealed class ExternalReadInitialization {
    class Declaration(val declaration: org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration) : ExternalReadInitialization()
    class Receiver(val completion: ExpressionCompletion) : ExternalReadInitialization()
}

/** 单一表达式状态机：begin 替换旧索引，只有 finish 才可查询完成结果。 */
sealed class ExpressionValueState {
    abstract val completion: ExpressionCompletion
    abstract val update: ValueEvent
    class Staging(
        override val completion: ExpressionCompletion,
        override val update: ValueEvent = ValueEvent(completion.event.origin),
    ) : ExpressionValueState()
    class Complete(
        override val completion: ExpressionCompletion,
        override val update: ValueEvent = ValueEvent(completion.event.origin),
    ) : ExpressionValueState()
}

/** 入口、成功观察与实际赋值的版本明确区分；初始化不分配赋值编号。 */
sealed class BindingVersion {
    class Entry(val event: ValueEvent) : BindingVersion()
    class Read(val event: ValueEvent) : BindingVersion()
    data class Assignment(val index: Int) : BindingVersion()
    class Merged(val event: ValueEvent, versions: List<BindingVersion>) : BindingVersion() {
        val versions = versions.toPersistentList()
    }
}

/** 只能由实际入口、成功读取或 store/合流提交；不能作为 Implication effect。 */
data class ValueBinding(val variable: RealVariable, val value: ValueVariable, val type: ConeCangJieType, val version: BindingVersion)

enum class StorageChangeKind { ENTRY, OBSERVATION, STORE, INVALIDATION, MEMBER_FORGET, JOIN }

/** write 标识可能改变当前值的事件；观察不是 writer，事件编号也不替代 assignmentIndex。 */
data class StorageChange(
    val event: ValueEvent,
    val kind: StorageChangeKind,
    val write: Boolean,
    val establishesValue: Boolean,
    /** 写来源独立于后续观察；读取不能掩盖真正 writer。 */
    val mutation: ValueEvent? = if (write) event else null,
)

/** 对齐一条实际输入；null 槽是显式缺失，不允许滤掉后改变分支位置。 */
internal data class JoinedValueInput(val flow: PersistentFlow, val use: ValueUse, val group: Any, val intrinsic: Boolean = true)

/** memo 只属于一次 join 的固定输入顺序；null 保留非候选边的坐标。 */
internal data class ValueJoinKey(val values: kotlinx.collections.immutable.PersistentList<ValueVariable?>)

/** 同一CFG合流的结果输入与存储输入共用memo，required不包括postponed data-flow-only边。 */
class ExpressionJoinInput(
    val expression: org.cangnova.cangjie.cfir.expressions.CfirExpression,
    values: List<ExpressionCompletion?>,
    required: List<Boolean>,
) {
    val values = values.toPersistentList()
    val required = required.toPersistentList()
    init { require(values.size == required.size) }
}

/** 已登记存储始终有事实节点；初始化位只能由真实 entry/read/store 提交。 */
sealed class StorageValueState {
    abstract val value: ValueVariable
    class Uninitialized(override val value: ValueVariable) : StorageValueState()
    class Initialized(override val value: ValueVariable) : StorageValueState()
}
