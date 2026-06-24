package org.cangnova.cangjie.cfir.resolve.dfa

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashMapOf

/**
 * DFA 中某一程序点的抽象 flow。
 *
 * flow 记录已知变量、类型陈述、条件蕴含以及别名关系。持久化和可变实现共享该查询接口。
 */
abstract class Flow {
    /** 当前 flow 已知的变量集合。 */
    abstract val knownVariables: Set<DataFlowVariable>

    /**
     * 展开真实变量的直接别名。
     */
    abstract fun unwrapVariable(variable: RealVariable): RealVariable

    /**
     * 获取变量当前已知的类型陈述。
     */
    abstract fun getTypeStatement(variable: DataFlowVariable): TypeStatement?

    /**
     * 获取以变量为条件的逻辑蕴含集合。
     */
    abstract fun getImplications(variable: DataFlowVariable): Collection<Implication>?

    /**
     * 展开任意数据流变量的别名。
     *
     * 合成变量没有符号身份，不参与真实变量别名映射。
     */
    open fun unwrapVariable(variable: DataFlowVariable): DataFlowVariable =
        if (variable is RealVariable) unwrapVariable(variable) else variable
}

/**
 * 已冻结的持久化 flow。
 *
 * @property previousFlow 当前 flow 的父快照。
 * @property approvedTypeStatements 已确认的变量类型陈述。
 * @property implications 条件到效果的蕴含集合。
 * @property assignmentIndex 真实变量最后赋值序号。
 * @property directAliasMap 真实变量到直接别名目标的映射。
 * @property backwardsAliasMap 别名目标到引用它的变量集合。
 */
class PersistentFlow internal constructor(
    private val previousFlow: PersistentFlow?,
    private val approvedTypeStatements: PersistentMap<DataFlowVariable, PersistentTypeStatement>,
    internal val implications: PersistentMap<DataFlowVariable, PersistentList<Implication>>,
    internal val assignmentIndex: PersistentMap<RealVariable, Int>,
    internal val directAliasMap: PersistentMap<RealVariable, RealVariable>,
    private val backwardsAliasMap: PersistentMap<RealVariable, PersistentSet<RealVariable>>,
) : Flow() {
    /** 当前 flow 在快照链中的深度。 */
    private val level: Int = previousFlow?.let { it.level + 1 } ?: 0

    /** 当前 flow 已知的变量集合。 */
    override val knownVariables: Set<DataFlowVariable>
        get() = approvedTypeStatements.keys + directAliasMap.keys

    /** 展开真实变量的直接别名。 */
    override fun unwrapVariable(variable: RealVariable): RealVariable =
        directAliasMap[variable] ?: variable

    /** 获取变量当前已知的类型陈述。 */
    override fun getTypeStatement(variable: DataFlowVariable): TypeStatement? =
        approvedTypeStatements[unwrapVariable(variable)]?.copy(variable = variable)

    /** 获取以变量为条件的逻辑蕴含集合。 */
    override fun getImplications(variable: DataFlowVariable): Collection<Implication>? =
        implications[variable]

    /**
     * 计算两个持久化 flow 的最近公共祖先。
     *
     * 分支 join 时通过该祖先判断两个 flow 的共同历史，避免重复合并同一批已知信息。
     */
    fun lowestCommonAncestor(other: PersistentFlow): PersistentFlow? {
        var left = this
        var right = other
        while (left.level > right.level) {
            left = left.previousFlow ?: return null
        }
        while (right.level > left.level) {
            right = right.previousFlow ?: return null
        }
        while (left != right) {
            left = left.previousFlow ?: return null
            right = right.previousFlow ?: return null
        }
        return left
    }

    /**
     * 从当前持久化 flow 创建可变分支。
     */
    fun fork(): MutableFlow = MutableFlow(
        previousFlow = this,
        approvedTypeStatements = approvedTypeStatements.builder(),
        implications = implications.builder(),
        assignmentIndex = assignmentIndex.builder(),
        directAliasMap = directAliasMap.builder(),
        backwardsAliasMap = backwardsAliasMap.builder(),
    )
}

/**
 * 正在构建或修改中的 DFA flow。
 *
 * @property previousFlow 可变 flow 所基于的父快照。
 * @property approvedTypeStatements 可写的类型陈述 builder。
 * @property implications 可写的逻辑蕴含 builder。
 * @property assignmentIndex 可写的赋值序号 builder。
 * @property directAliasMap 可写的直接别名 builder。
 * @property backwardsAliasMap 可写的反向别名 builder。
 */
class MutableFlow internal constructor(
    private val previousFlow: PersistentFlow?,
    internal val approvedTypeStatements: PersistentMap.Builder<DataFlowVariable, PersistentTypeStatement>,
    internal val implications: PersistentMap.Builder<DataFlowVariable, PersistentList<Implication>>,
    internal val assignmentIndex: PersistentMap.Builder<RealVariable, Int>,
    internal val directAliasMap: PersistentMap.Builder<RealVariable, RealVariable>,
    internal val backwardsAliasMap: PersistentMap.Builder<RealVariable, PersistentSet<RealVariable>>,
) : Flow() {
    constructor() : this(
        previousFlow = null,
        approvedTypeStatements = emptyPersistentHashMapBuilder(),
        implications = emptyPersistentHashMapBuilder(),
        assignmentIndex = emptyPersistentHashMapBuilder(),
        directAliasMap = emptyPersistentHashMapBuilder(),
        backwardsAliasMap = emptyPersistentHashMapBuilder(),
    )

    /** 当前 flow 已知的变量集合。 */
    override val knownVariables: Set<DataFlowVariable>
        get() = approvedTypeStatements.keys + directAliasMap.keys

    /** 展开真实变量的直接别名。 */
    override fun unwrapVariable(variable: RealVariable): RealVariable =
        directAliasMap[variable] ?: variable

    /** 获取变量当前已知的类型陈述。 */
    override fun getTypeStatement(variable: DataFlowVariable): TypeStatement? =
        approvedTypeStatements[unwrapVariable(variable)]?.copy(variable = variable)

    /** 获取以变量为条件的逻辑蕴含集合。 */
    override fun getImplications(variable: DataFlowVariable): Collection<Implication>? =
        implications[variable]

    /**
     * 冻结当前可变 flow，产出可共享的持久化快照。
     */
    fun freeze(): PersistentFlow = PersistentFlow(
        previousFlow = previousFlow,
        approvedTypeStatements = approvedTypeStatements.build(),
        implications = implications.build(),
        assignmentIndex = assignmentIndex.build(),
        directAliasMap = directAliasMap.build(),
        backwardsAliasMap = backwardsAliasMap.build(),
    )
}

/**
 * 创建空的持久化 map builder。
 */
private fun <K, V> emptyPersistentHashMapBuilder(): PersistentMap.Builder<K, V> =
    persistentHashMapOf<K, V>().builder()
