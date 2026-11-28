package org.cangnova.cangjie.cfir.resolve.dfa

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import org.cangnova.cangjie.cfir.DfaType
import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * DFA 中某一程序点的抽象 flow。
 *
 * flow 记录已知变量、类型陈述、条件蕴含以及别名关系。持久化和可变实现共享该查询接口。
 */
sealed class Flow(protected val previousFlow: PersistentFlow?) {
    internal abstract val approvedTypeStatements: Map<ValueVariable, PersistentTypeStatement>
    internal abstract val implications: Map<ValueVariable, PersistentList<Implication>>
    internal abstract val assignmentIndex: Map<RealVariable, Int>
    internal abstract val directAliasMap: Map<RealVariable, RealVariable>
    /** 实际Class引用的成员地址代表；与类型/值知识的direct alias独立。 */
    internal abstract val receiverAliasMap: Map<RealVariable, RealVariable>
    internal abstract val backwardsAliasMap: Map<RealVariable, PersistentSet<RealVariable>>
    internal abstract val oneWayAliasMap: Map<RealVariable, PersistentSet<RealVariable>>
    internal abstract val realVariables: Map<RealVariable, RealVariable>
    internal abstract val memberVariables: Map<RealVariable, PersistentSet<RealVariable>>
    internal abstract val storageValues: Map<RealVariable, StorageValueState>
    internal abstract val valueBindings: Map<RealVariable, ValueBinding>
    internal abstract val storageChanges: Map<RealVariable, StorageChange>
    internal abstract val expressionValues: Map<CfirExpression, ExpressionValueState>
    internal abstract val tupleValues: Map<ValueVariable, PersistentList<ValueUse>>
    internal abstract val projectedValues: Map<ProjectedValueVariable, ProjectedValueVariable>

    /** 完整访问实际存储，knownVariables 只是查询投影，不能覆盖蕴含、历史、别名与赋值索引。 */
    abstract fun acceptSourceChildren(visitor: DfaSourceVisitor)

    /** 当前 flow 已知的变量集合。 */
    val knownVariables: Set<DataFlowVariable>
        get() = approvedTypeStatements.keys + directAliasMap.keys

    /**
     * 展开真实变量的直接别名。
     */
    fun unwrapVariable(variable: RealVariable): RealVariable = directAliasMap[variable] ?: variable

    /** 值相等不等于存储地址相同；Struct/Enum副本保留各自 receiver 前缀。 */
    fun unwrapReceiverAddress(variable: RealVariable, session: CfirSession): RealVariable {
        val representative = receiverAliasMap[variable] ?: variable
        if (representative == variable) return variable
        val actual = valueBindings[variable]?.value?.originalType ?: return variable
        check(actual.fullyExpandedType(session).toSymbol(session)?.cfir is org.cangnova.cangjie.cfir.declarations.CfirClass)
        check(valueBindings[variable]?.value === valueBindings[representative]?.value)
        return representative
    }

    /**
     * 获取变量当前已知的类型陈述。
     */
    fun getTypeStatement(variable: DataFlowVariable): TypeStatement? =
        knownValue(variable)?.let { approvedTypeStatements[it]?.forQuery(variable) }

    /** Real查询额外smartcast知识，Value查询完整值事实；仅固有且等同声明类型的上界不构成额外精化。 */
    private fun PersistentTypeStatement.forQuery(variable: DataFlowVariable): PersistentTypeStatement? {
        val upper = if (variable is RealVariable && variable.originalType in intrinsicUpperTypes) {
            upperTypes.remove(variable.originalType)
        } else upperTypes
        if (upper.isEmpty() && lowerTypes.isEmpty()) return null
        return copy(variable = variable, upperTypes = upper, intrinsicUpperTypes = intrinsicUpperTypes.retainAll(upper))
    }

    /**
     * 获取以变量为条件的逻辑蕴含集合。
     */
    fun getImplications(variable: DataFlowVariable): Collection<Implication>? =
        knownValue(variable)?.let { implications[it] }

    /** 类型查询读取单向别名的事实；分支合并与写回仍只使用变量自身的陈述。 */
    fun getTypeStatementWithOneWayData(variable: DataFlowVariable): TypeStatement? {
        val value = knownValue(variable) ?: return null
        val upper = persistentHashSetOf<ConeCangJieType>().builder()
        val lower = persistentHashSetOf<DfaType>().builder()
        val intrinsic = persistentHashSetOf<ConeCangJieType>().builder()
        val explicit = mutableSetOf<ConeCangJieType>()
        fun gatherStatement(statement: TypeStatement) {
            upper.addAll(statement.upperTypes)
            lower.addAll(statement.lowerTypes)
            intrinsic.addAll(statement.intrinsicUpperTypes)
            explicit.addAll(statement.upperTypes - statement.intrinsicUpperTypes)
        }
        val owners = storageValues.keys.filter { knownValue(it) == value }
        val visited = mutableSetOf<RealVariable>()
        fun gather(owner: RealVariable) {
            val representative = unwrapVariable(owner)
            if (!visited.add(representative)) return
            knownValue(representative)?.let { approvedTypeStatements[it] }?.let {
                gatherStatement(it)
            }
            oneWayAliasMap[representative].orEmpty().forEach(::gather)
        }
        approvedTypeStatements[value]?.let {
            gatherStatement(it)
        }
        owners.forEach(::gather)
        return if (upper.isEmpty() && lower.isEmpty()) null
        else PersistentTypeStatement(variable, upper.build(), lower.build(), intrinsic.build().removeAll(explicit)).forQuery(variable)
    }

    /** 缺 binding/staging 表示没有值事实，查询返回空；绝不回查旧 storage/synthetic 键。 */
    internal fun knownValue(variable: DataFlowVariable): ValueVariable? = when (variable) {
        is ValueVariable -> variable
        is RealVariable -> storageValues[canonicalStorageVariable(variable)]?.value
        is SyntheticVariable -> expressionValues[variable.fir]?.completion?.value
    }

    /** 事实提交必须已有实际 entry/read/begin；未初始化 local 不可由此建立 binding。 */
    internal fun requireValue(variable: DataFlowVariable): ValueVariable =
        checkNotNull(knownValue(variable)) { "No admitted value for a fact: $variable" }

    /** 只查询真实初始化关系；缺失不会由读取创建。 */
    fun getValueBinding(variable: RealVariable): ValueBinding? = valueBindings[canonicalStorageVariable(variable)]

    fun getBoundValue(variable: RealVariable): ValueVariable =
        checkNotNull(getValueBinding(variable)) { "Storage has no admitted binding" }.value

    /** Value-key 的类型批准结果用此查真实 implicit bindings，不将事实节点伪装成 RealVariable。 */
    fun getBoundImplicitVariables(value: ValueVariable): List<RealVariable> =
        valueBindings.keys.filter { it.isImplicit && valueBindings.getValue(it).value == value }

    /** 缺失或尚未完成是当前产品的明确状态，不回查旧表达式/存储键。 */
    fun getExpressionCompletionOrNull(expression: CfirExpression): ExpressionCompletion? {
        val original = if (expression is org.cangnova.cangjie.cfir.expressions.CfirSmartCastExpression) expression.originalExpression else expression
        val completed = (expressionValues[original] as? ExpressionValueState.Complete)?.completion ?: return null
        return if (expression === original) completed else expression.coneTypeOrNull?.let { completed.copy(type = it) }
    }

    fun getExpressionCompletion(expression: CfirExpression): ExpressionCompletion {
        if (expression is org.cangnova.cangjie.cfir.expressions.CfirSmartCastExpression) {
            val original = getExpressionCompletion(expression.originalExpression)
            return original.copy(type = checkNotNull(expression.coneTypeOrNull))
        }
        val state = checkNotNull(expressionValues[expression]) { "Expression has no event" }
        check(state is ExpressionValueState.Complete) { "Expression event is still staging" }
        return state.completion
    }

    /** 变量规范实例属于当前 flow；未登记原型只用于查询，不写入其它分支。 */
    context(holder: SessionHolder)
    fun getVariable(
        fir: CfirExpression,
        unwrapAlias: (RealVariable) -> RealVariable? = { it },
        unwrapAliasInReceivers: (RealVariable) -> RealVariable? = { unwrapReceiverAddress(it, holder.session) },
        symbolMapper: (CfirBasedSymbol<*>) -> CfirBasedSymbol<*> = { it },
    ): DataFlowVariable? = DataFlowVariable.of(
        fir,
        unwrap = { unwrapAlias(getVariableIfKnown(it) ?: it) },
        unwrapInReceivers = { unwrapAliasInReceivers(getVariableIfKnown(it) ?: it) },
        symbolMapper = symbolMapper,
    )

    /** 只查询此 flow 已登记的真实变量；合成变量仍保持表达式身份。 */
    context(holder: SessionHolder)
    fun getVariableIfKnown(
        fir: CfirExpression,
        unwrapAlias: (RealVariable) -> RealVariable? = { it },
        unwrapAliasInReceivers: (RealVariable) -> RealVariable? = { unwrapReceiverAddress(it, holder.session) },
        symbolMapper: (CfirBasedSymbol<*>) -> CfirBasedSymbol<*> = { it },
    ): DataFlowVariable? = DataFlowVariable.of(
        fir,
        unwrap = { getVariableIfKnown(it)?.let(unwrapAlias) },
        unwrapInReceivers = { getVariableIfKnown(it)?.let(unwrapAliasInReceivers) },
        symbolMapper = symbolMapper,
    )

    fun getVariableIfKnown(variable: RealVariable): RealVariable? = realVariables[canonicalStorageVariable(variable)]

    /** 只规范化存储的receiver，根变量自身仍保留独立的类型/SSA/声明身份。 */
    internal fun canonicalStorageVariable(variable: RealVariable): RealVariable {
        fun receiver(value: RealVariable): RealVariable {
            val normalized = canonicalStorageVariable(value)
            val known = realVariables[normalized] ?: normalized
            return receiverAliasMap[known] ?: known
        }
        val dispatch = variable.dispatchReceiver?.let(::receiver)
        val extension = variable.extensionReceiver?.let(::receiver)
        return if (dispatch === variable.dispatchReceiver && extension === variable.extensionReceiver) variable
        else RealVariable(variable.symbol, variable.isImplicit, dispatch, extension, variable.originalType)
    }

    /**
     * 展开任意数据流变量的别名。
     *
     * 合成变量没有符号身份，不参与真实变量别名映射。
     */
    fun unwrapVariable(variable: DataFlowVariable): DataFlowVariable =
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
    /**
     * 当前 flow 的父快照。
     */
    previousFlow: PersistentFlow?,
    /**
     * 已确认的变量类型陈述。
     */
    internal override val approvedTypeStatements: PersistentMap<ValueVariable, PersistentTypeStatement>,
    /**
     * 条件到效果的蕴含集合。
     */
    internal override val implications: PersistentMap<ValueVariable, PersistentList<Implication>>,
    /**
     * 真实变量最后赋值序号。
     */
    internal override val assignmentIndex: PersistentMap<RealVariable, Int>,
    /**
     * 真实变量到直接别名目标的映射。
     */
    internal override val directAliasMap: PersistentMap<RealVariable, RealVariable>,
    internal override val receiverAliasMap: PersistentMap<RealVariable, RealVariable>,
    /**
     * 别名目标到引用它的变量集合。
     */
    internal override val backwardsAliasMap: PersistentMap<RealVariable, PersistentSet<RealVariable>>,
    internal override val oneWayAliasMap: PersistentMap<RealVariable, PersistentSet<RealVariable>>,
    internal override val realVariables: PersistentMap<RealVariable, RealVariable>,
    internal override val memberVariables: PersistentMap<RealVariable, PersistentSet<RealVariable>>,
    internal override val storageValues: PersistentMap<RealVariable, StorageValueState>,
    internal override val valueBindings: PersistentMap<RealVariable, ValueBinding>,
    internal override val storageChanges: PersistentMap<RealVariable, StorageChange>,
    internal override val expressionValues: PersistentMap<CfirExpression, ExpressionValueState>,
    internal override val tupleValues: PersistentMap<ValueVariable, PersistentList<ValueUse>>,
    internal override val projectedValues: PersistentMap<ProjectedValueVariable, ProjectedValueVariable>,
) : Flow(previousFlow) {
    override fun acceptSourceChildren(visitor: DfaSourceVisitor) {
        previousFlow?.let(visitor::visitFlow)
        acceptFlowSourceChildren(visitor, approvedTypeStatements, implications, assignmentIndex, directAliasMap, backwardsAliasMap,
            oneWayAliasMap, realVariables, memberVariables, storageValues, valueBindings, storageChanges, expressionValues, tupleValues, projectedValues, receiverAliasMap)
    }

    /** 当前 flow 在快照链中的深度。 */
    private val level: Int = previousFlow?.let { it.level + 1 } ?: 0

    val allVariablesForDebug: Set<DataFlowVariable>
        get() = knownVariables + implications.keys + implications.values.flatten().map { it.effect.variable }

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
        receiverAliasMap = receiverAliasMap.builder(),
        backwardsAliasMap = backwardsAliasMap.builder(),
        oneWayAliasMap = oneWayAliasMap.builder(),
        realVariables = realVariables.builder(),
        memberVariables = memberVariables.builder(),
        storageValues = storageValues.builder(),
        valueBindings = valueBindings.builder(),
        storageChanges = storageChanges.builder(),
        expressionValues = expressionValues.builder(),
        tupleValues = tupleValues.builder(),
        projectedValues = projectedValues.builder(),
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
    /**
     * 可变 flow 所基于的父快照。
     */
    previousFlow: PersistentFlow?,
    /**
     * 可写的类型陈述 builder。
     */
    internal override val approvedTypeStatements: PersistentMap.Builder<ValueVariable, PersistentTypeStatement>,
    /**
     * 可写的逻辑蕴含 builder。
     */
    internal override val implications: PersistentMap.Builder<ValueVariable, PersistentList<Implication>>,
    /**
     * 可写的赋值序号 builder。
     */
    internal override val assignmentIndex: PersistentMap.Builder<RealVariable, Int>,
    /**
     * 可写的直接别名 builder。
     */
    internal override val directAliasMap: PersistentMap.Builder<RealVariable, RealVariable>,
    internal override val receiverAliasMap: PersistentMap.Builder<RealVariable, RealVariable>,
    /**
     * 可写的反向别名 builder。
     */
    internal override val backwardsAliasMap: PersistentMap.Builder<RealVariable, PersistentSet<RealVariable>>,
    internal override val oneWayAliasMap: PersistentMap.Builder<RealVariable, PersistentSet<RealVariable>>,
    internal override val realVariables: PersistentMap.Builder<RealVariable, RealVariable>,
    internal override val memberVariables: PersistentMap.Builder<RealVariable, PersistentSet<RealVariable>>,
    internal override val storageValues: PersistentMap.Builder<RealVariable, StorageValueState>,
    internal override val valueBindings: PersistentMap.Builder<RealVariable, ValueBinding>,
    internal override val storageChanges: PersistentMap.Builder<RealVariable, StorageChange>,
    internal override val expressionValues: PersistentMap.Builder<CfirExpression, ExpressionValueState>,
    internal override val tupleValues: PersistentMap.Builder<ValueVariable, PersistentList<ValueUse>>,
    internal override val projectedValues: PersistentMap.Builder<ProjectedValueVariable, ProjectedValueVariable>,
) : Flow(previousFlow) {
    override fun acceptSourceChildren(visitor: DfaSourceVisitor) {
        previousFlow?.let(visitor::visitFlow)
        acceptFlowSourceChildren(visitor, approvedTypeStatements, implications, assignmentIndex, directAliasMap, backwardsAliasMap,
            oneWayAliasMap, realVariables, memberVariables, storageValues, valueBindings, storageChanges, expressionValues, tupleValues, projectedValues, receiverAliasMap)
    }

    constructor() : this(
        previousFlow = null,
        approvedTypeStatements = emptyPersistentHashMapBuilder(),
        implications = emptyPersistentHashMapBuilder(),
        assignmentIndex = emptyPersistentHashMapBuilder(),
        directAliasMap = emptyPersistentHashMapBuilder(),
        receiverAliasMap = emptyPersistentHashMapBuilder(),
        backwardsAliasMap = emptyPersistentHashMapBuilder(),
        oneWayAliasMap = emptyPersistentHashMapBuilder(),
        realVariables = emptyPersistentHashMapBuilder(),
        memberVariables = emptyPersistentHashMapBuilder(),
        storageValues = emptyPersistentHashMapBuilder(),
        valueBindings = emptyPersistentHashMapBuilder(),
        storageChanges = emptyPersistentHashMapBuilder(),
        expressionValues = emptyPersistentHashMapBuilder(),
        tupleValues = emptyPersistentHashMapBuilder(),
        projectedValues = emptyPersistentHashMapBuilder(),
    )

    /**
     * 冻结当前可变 flow，产出可共享的持久化快照。
     */
    fun freeze(): PersistentFlow = PersistentFlow(
        previousFlow = previousFlow,
        approvedTypeStatements = approvedTypeStatements.build(),
        implications = implications.build(),
        assignmentIndex = assignmentIndex.build(),
        directAliasMap = directAliasMap.build(),
        receiverAliasMap = receiverAliasMap.build(),
        backwardsAliasMap = backwardsAliasMap.build(),
        oneWayAliasMap = oneWayAliasMap.build(),
        realVariables = realVariables.build(),
        memberVariables = memberVariables.build(),
        storageValues = storageValues.build(),
        valueBindings = valueBindings.build(),
        storageChanges = storageChanges.build(),
        expressionValues = expressionValues.build(),
        tupleValues = tupleValues.build(),
        projectedValues = projectedValues.build(),
    )

    /** 登记表达式及其真实 receiver；符号投影来自同次拥有图事务。 */
    context(holder: SessionHolder)
    fun remember(
        fir: CfirExpression,
        unwrapAlias: (RealVariable) -> RealVariable? = { it },
        unwrapAliasInReceivers: (RealVariable) -> RealVariable? = { unwrapReceiverAddress(it, holder.session) },
        symbolMapper: (CfirBasedSymbol<*>) -> CfirBasedSymbol<*> = { it },
    ): DataFlowVariable? = DataFlowVariable.of(
        fir,
        unwrap = { unwrapAlias(rememberWithReceivers(it)) },
        unwrapInReceivers = { unwrapAliasInReceivers(rememberWithReceivers(it)) },
        symbolMapper = symbolMapper,
    )

    fun remember(variable: RealVariable): RealVariable = rememberWithReceivers(variable.mapReceivers(::remember))

    private fun rememberWithReceivers(variable: RealVariable): RealVariable {
        return registerVariable(canonicalStorageVariable(variable))
    }

    /** 合流先登记输入库存；不能用共同祖先的旧receiver关系提前改写新输入地址。 */
    internal fun registerVariable(variable: RealVariable): RealVariable {
        variable.dispatchReceiver?.let(::registerVariable)
        variable.extensionReceiver?.let(::registerVariable)
        val known = realVariables.getOrPut(variable) {
            variable.dispatchReceiver?.let { memberVariables.add(it, variable) }
            variable.extensionReceiver?.let { memberVariables.add(it, variable) }
            variable
        }
        storageValues.getOrPut(known) { StorageValueState.Uninitialized(StorageValueVariable(known, assignmentIndex[known], storageChanges[known]?.event)) }
        return known
    }

    private inline fun RealVariable.mapReceivers(block: (RealVariable) -> RealVariable): RealVariable =
        RealVariable(symbol, isImplicit, dispatchReceiver?.let(block), extensionReceiver?.let(block), originalType)

    /** receiver 重新赋值时，同一 flow 的成员事实随代表变量迁移或失效。 */
    fun replaceReceiverReferencesInMembers(from: RealVariable, to: RealVariable?, processMember: (RealVariable, RealVariable?) -> Unit) {
        for (member in memberVariables[from].orEmpty()) {
            val remapped = to?.let { rememberWithReceivers(member.mapReceivers { if (it == from) to else it }) }
            processMember(member, remapped)
        }
    }
}

internal fun <K, V> MutableMap<K, PersistentSet<V>>.add(key: K, value: V) {
    this[key] = (this[key] ?: persistentHashSetOf()).add(value)
}

internal fun <K, V> MutableMap<K, PersistentSet<V>>.addAll(key: K, values: Collection<V>) {
    this[key] = (this[key] ?: persistentHashSetOf()).addAll(values)
}

/**
 * 创建空的持久化 map builder。
 */
private fun <K, V> emptyPersistentHashMapBuilder(): PersistentMap.Builder<K, V> =
    persistentHashMapOf<K, V>().builder()

/** 两种 flow 的完整表结构相同；只遍历其载荷，不执行别名展开或 smart cast 查询。 */
private fun acceptFlowSourceChildren(
    visitor: DfaSourceVisitor,
    typeStatements: Map<ValueVariable, TypeStatement>,
    implications: Map<ValueVariable, Collection<Implication>>,
    assignments: Map<RealVariable, Int>,
    aliases: Map<RealVariable, RealVariable>,
    backwardsAliases: Map<RealVariable, Set<RealVariable>>,
    oneWayAliases: Map<RealVariable, Set<RealVariable>>,
    realVariables: Map<RealVariable, RealVariable>,
    memberVariables: Map<RealVariable, Set<RealVariable>>,
    storageValues: Map<RealVariable, StorageValueState>,
    valueBindings: Map<RealVariable, ValueBinding>,
    storageChanges: Map<RealVariable, StorageChange>,
    expressionValues: Map<CfirExpression, ExpressionValueState>,
    tupleValues: Map<ValueVariable, List<ValueUse>>,
    projectedValues: Map<ProjectedValueVariable, ProjectedValueVariable>,
    receiverAliases: Map<RealVariable, RealVariable>,
) {
    receiverAliases.forEach { (from, to) -> visitor.visitVariable(from); visitor.visitVariable(to) }
    storageValues.forEach { (storage, state) ->
        visitor.visitVariable(storage)
        visitor.visitVariable(state.value)
    }
    fun bindingVersion(version: BindingVersion) {
        when (version) {
            is BindingVersion.Entry -> visitor.visitElement(version.event.origin)
            is BindingVersion.Read -> visitor.visitElement(version.event.origin)
            is BindingVersion.Assignment -> Unit
            is BindingVersion.Merged -> {
                visitor.visitElement(version.event.origin)
                version.versions.forEach(::bindingVersion)
            }
        }
    }
    valueBindings.forEach { (storage, binding) ->
        visitor.visitVariable(storage)
        visitor.visitVariable(binding.variable)
        visitor.visitVariable(binding.value)
        visitor.visitType(binding.type)
        bindingVersion(binding.version)
    }
    storageChanges.forEach { (storage, change) ->
        visitor.visitVariable(storage)
        visitor.visitElement(change.event.origin)
        change.mutation?.let { visitor.visitElement(it.origin) }
    }
    expressionValues.forEach { (expression, state) ->
        visitor.visitElement(expression)
        state.completion.acceptSourceChildren(visitor)
        visitor.visitElement(state.update.origin)
    }
    tupleValues.forEach { (tuple, slots) ->
        visitor.visitVariable(tuple)
        slots.forEach { it.acceptSourceChildren(visitor) }
    }
    projectedValues.forEach { (key, value) ->
        visitor.visitVariable(key)
        visitor.visitVariable(value)
    }
    fun statement(value: Statement) {
        visitor.visitVariable(value.variable)
        if (value is TypeStatement) {
            value.upperTypes.forEach(visitor::visitType)
            value.lowerTypes.forEach { bound ->
                when (bound) {
                    is DfaType.Cone -> visitor.visitType(bound.type)
                    is DfaType.Symbol -> visitor.visitElement(bound.symbol.cfir)
                    is DfaType.BooleanLiteral -> Unit
                }
            }
        }
    }
    typeStatements.forEach { (variable, value) ->
        visitor.visitVariable(variable)
        statement(value)
    }
    implications.forEach { (variable, values) ->
        visitor.visitVariable(variable)
        values.forEach {
            statement(it.condition)
            statement(it.effect)
        }
    }
    assignments.keys.forEach(visitor::visitVariable)
    aliases.forEach { (variable, alias) ->
        visitor.visitVariable(variable)
        visitor.visitVariable(alias)
    }
    backwardsAliases.forEach { (variable, aliases) ->
        visitor.visitVariable(variable)
        aliases.forEach(visitor::visitVariable)
    }
    oneWayAliases.forEach { (variable, aliases) ->
        visitor.visitVariable(variable)
        aliases.forEach(visitor::visitVariable)
    }
    realVariables.forEach { (prototype, variable) ->
        visitor.visitVariable(prototype)
        visitor.visitVariable(variable)
    }
    memberVariables.forEach { (receiver, members) ->
        visitor.visitVariable(receiver)
        members.forEach(visitor::visitVariable)
    }
}
