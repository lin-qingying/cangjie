package org.cangnova.cangjie.cfir.resolve.dfa

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import org.cangnova.cangjie.cfir.DfaType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.types.ConeAnyType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeInferenceContext
import org.cangnova.cangjie.cfir.types.ConeTypeIntersector
import org.cangnova.cangjie.cfir.types.commonSuperTypeOrNull
import org.cangnova.cangjie.cfir.types.isOption
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.AbstractTypeChecker
import java.util.LinkedList
import kotlin.math.max

/**
 * 对位 Kotlin FIR `LogicSystem`。
 *
 * 保留原始 DFA 主干：流合并、别名传播、赋值失效、implication 批准与
 * TypeStatement 的 and/or 组合，只把类型系统 API 适配到仓颉。
 */
abstract class LogicSystem(private val context: ConeInferenceContext) {
    /** 当前逻辑系统绑定的 CFIR session。 */
    val session: CfirSession get() = context.session

    /** DFA 中 `null` 判断对应的 Nothing 类型。 */
    private val nullableNothingType = session.builtinTypes.nothingType

    /** DFA 中未知但可存在的顶层 Any 类型。 */
    private val anyType = session.typeContext.anyType() as ConeCangJieType

    /** 当前逻辑系统使用的数据流变量存储。 */
    abstract val variableStorage: VariableStorage

    /**
     * 判断类型是否适合作为 smart cast 结果。
     */
    protected open fun ConeCangJieType.isAcceptableForSmartcast(): Boolean {
        return this != nullableNothingType
    }

    /**
     * 合并多个持久化 flow。
     *
     * @param flows 待合并的控制流分支。
     * @param statementFlows 用于合并类型陈述与蕴含的 flow 集合。
     * @param union 是否按 union 语义合并分支；否则按 common 语义合并。
     */
    fun joinFlow(flows: Collection<PersistentFlow>, statementFlows: Collection<PersistentFlow>, union: Boolean): MutableFlow {
        when (flows.size) {
            0 -> return MutableFlow()
            1 -> return flows.first().fork()
        }

        val commonFlow = flows.reduce { left, right ->
            left.lowestCommonAncestor(right) ?: error("No common ancestor in $left, $right")
        }
        val result = commonFlow.fork()
        result.mergeAssignments(flows)
        if (union) {
            result.copyNonConflictingAliases(flows, commonFlow)
        } else {
            result.copyCommonAliases(flows)
        }
        result.copyStatements(statementFlows, commonFlow, union)
        result.copyImplications(statementFlows)
        return result
    }

    /**
     * 为局部变量登记别名。
     *
     * @param flow 要写入的可变 flow。
     * @param alias 别名变量。
     * @param underlyingVariable 别名指向的底层变量。
     */
    fun addLocalVariableAlias(flow: MutableFlow, alias: RealVariable, underlyingVariable: RealVariable) {
        if (underlyingVariable == alias) return
        flow.directAliasMap[alias] = underlyingVariable
        flow.backwardsAliasMap[underlyingVariable] =
            flow.backwardsAliasMap[underlyingVariable]?.add(alias) ?: persistentSetOf(alias)
    }

    /**
     * 将类型陈述加入 flow。
     *
     * 已存在陈述时会合并上下界；如果没有产生新信息则返回 `null`。
     */
    fun addTypeStatement(flow: MutableFlow, statement: TypeStatement): TypeStatement? {
        if (statement.isEmpty) return null
        val variable = statement.variable
        val oldStatement = flow.approvedTypeStatements[variable]
        val oldUpperTypes = oldStatement?.upperTypes
        val oldLowerTypes = oldStatement?.lowerTypes
        val newUpperTypes = oldUpperTypes?.addAll(statement.upperTypes) ?: statement.upperTypes.toPersistentSet()
        val newLowerTypes = oldLowerTypes?.addAll(statement.lowerTypes) ?: statement.lowerTypes.toPersistentSet()
        if (newUpperTypes === oldUpperTypes && newLowerTypes === oldLowerTypes) return null
        return PersistentTypeStatement(variable, newUpperTypes, newLowerTypes)
            .also { flow.approvedTypeStatements[variable] = it }
    }

    /**
     * 批量加入类型陈述。
     *
     * @return 实际写入并产生新信息的陈述列表。
     */
    fun addTypeStatements(flow: MutableFlow, statements: TypeStatements): List<TypeStatement> =
        statements.values.mapNotNull { addTypeStatement(flow, it) }

    /**
     * 将条件蕴含加入 flow。
     *
     * 空效果、重复效果或对无法追踪合成变量的冗余效果会被跳过。
     */
    fun addImplication(flow: MutableFlow, implication: Implication) {
        val effect = implication.effect
        val redundant = effect == implication.condition || when (effect) {
            is TypeStatement -> effect.isEmpty || flow.containsAlready(effect)
            else -> effect.variable is SyntheticVariable && effect.variable !in flow.implications
        }
        if (redundant) return
        val variable = implication.condition.variable
        flow.implications[variable] = flow.implications[variable]?.add(implication) ?: persistentListOf(implication)
    }

    /**
     * 判断指定类型陈述是否已经被当前 flow 完全包含。
     */
    private fun MutableFlow.containsAlready(effect: TypeStatement): Boolean {
        val approved = approvedTypeStatements[effect.variable] ?: return false
        return approved.upperTypes.containsAll(effect.upperTypes) && approved.lowerTypes.containsAll(effect.lowerTypes)
    }

    /**
     * 将 implication 条件中的变量从旧变量迁移到新变量。
     *
     * 常用于 smart cast、别名替换或合成变量落到真实变量后，把已记录的条件继续挂到可追踪变量上。
     */
    fun translateVariableFromConditionInStatements(
        flow: MutableFlow,
        originalVariable: DataFlowVariable,
        newVariable: DataFlowVariable,
        transform: (Implication) -> Implication? = { it },
    ) {
        val statements = if (originalVariable.isSynthetic()) {
            flow.implications.remove(originalVariable)
        } else {
            flow.implications[originalVariable]
        }
        if (statements.isNullOrEmpty()) return
        val existing = flow.implications[newVariable] ?: persistentListOf()
        flow.implications[newVariable] = statements.mapNotNullTo(existing.builder()) {
            transform(OperationStatement(newVariable, it.condition.operation) implies it.effect)
        }.build()
    }

    /**
     * 在持久化 flow 上批准一个操作陈述，并计算其推出的类型陈述。
     */
    fun approveOperationStatement(flow: PersistentFlow, statement: OperationStatement): TypeStatements {
        return approveOperationStatement(flow.implications.toMutableMap(), statement, removeApprovedOrImpossible = false)
    }

    /**
     * 在可变 flow 上批准一个操作陈述，并计算其推出的类型陈述。
     *
     * @param removeApprovedOrImpossible 是否从 implication 表中移除已确定或不可能成立的蕴含。
     */
    fun approveOperationStatement(
        flow: MutableFlow,
        statement: OperationStatement,
        removeApprovedOrImpossible: Boolean,
    ): TypeStatements {
        return approveOperationStatement(flow.implications, statement, removeApprovedOrImpossible)
    }

    /**
     * 记录真实变量的新赋值。
     *
     * 赋值会使变量旧别名和依赖该变量的陈述失效。
     */
    fun recordNewAssignment(flow: MutableFlow, variable: RealVariable, index: Int) {
        flow.replaceVariable(variable, null)
        flow.assignmentIndex[variable] = index
    }

    /**
     * 判断变量在两个持久化 flow 中是否仍是同一次赋值。
     */
    fun isSameValueIn(left: PersistentFlow, right: PersistentFlow, variable: RealVariable): Boolean {
        return left.assignmentIndex[variable] == right.assignmentIndex[variable]
    }

    /**
     * 判断变量在持久化 flow 与可变 flow 中是否仍是同一次赋值。
     */
    fun isSameValueIn(left: PersistentFlow, right: MutableFlow, variable: RealVariable): Boolean {
        return left.assignmentIndex[variable] == right.assignmentIndex[variable]
    }

    /**
     * 合并各分支中的赋值序号，并让发生重赋值的变量失效。
     */
    private fun MutableFlow.mergeAssignments(flows: Collection<PersistentFlow>) {
        val reassignedVariables = mutableMapOf<RealVariable, Int>()
        for (flow in flows) {
            for ((variable, index) in flow.assignmentIndex) {
                if (assignmentIndex[variable] != index) {
                    reassignedVariables[variable] = max(index, reassignedVariables[variable] ?: 0)
                }
            }
        }
        for ((variable, index) in reassignedVariables) {
            recordNewAssignment(this, variable, index)
        }
    }

    /**
     * 复制所有分支共同拥有的别名。
     */
    private fun MutableFlow.copyCommonAliases(flows: Collection<PersistentFlow>) {
        for ((from, to) in flows.first().directAliasMap) {
            if (directAliasMap[from] != to && flows.all { it.unwrapVariable(from) == to }) {
                addLocalVariableAlias(this, from, to)
            }
        }
    }

    /**
     * 在 union 合并中复制没有冲突的别名。
     */
    private fun MutableFlow.copyNonConflictingAliases(flows: Collection<PersistentFlow>, commonFlow: PersistentFlow) {
        val candidates = mutableMapOf<RealVariable, RealVariable?>()
        for (flow in flows) {
            for ((from, to) in flow.directAliasMap) {
                candidates[from] = when {
                    commonFlow.assignmentIndex[from] == flow.assignmentIndex[from] -> continue
                    from in candidates && candidates[from] != to -> null
                    else -> to
                }
            }
        }
        for ((from, to) in candidates) {
            addLocalVariableAlias(this, from, to ?: continue)
        }
    }

    /**
     * 从分支 flow 中复制并合并类型陈述。
     */
    private fun MutableFlow.copyStatements(flows: Collection<PersistentFlow>, commonFlow: PersistentFlow, union: Boolean) {
        flows.flatMapTo(mutableSetOf()) { it.knownVariables }.forEach computeStatement@{ variable ->
            val statement = if (variable in directAliasMap) {
                return@computeStatement
            } else if (!union) {
                or(flows.mapTo(mutableSetOf()) { it.getTypeStatement(variable) ?: return@computeStatement })
            } else if (assignmentIndex[variable] == commonFlow.assignmentIndex[variable]) {
                and(flows.mapNotNullTo(mutableSetOf()) { it.getTypeStatement(variable) })
            } else {
                val byAssignment =
                    flows.groupByTo(mutableMapOf(), { it.assignmentIndex[variable] ?: -1 }, { it.getTypeStatement(variable) })
                byAssignment.remove(commonFlow.assignmentIndex[variable] ?: -1)
                or(byAssignment.values.mapTo(mutableSetOf()) { and(it.filterNotNull()) ?: return@computeStatement })
            }
            if (statement?.isNotEmpty == true) {
                approvedTypeStatements[variable] = statement.toPersistent()
            }
        }
    }

    /**
     * 复制分支 flow 中仍然可用的 implication。
     */
    private fun MutableFlow.copyImplications(flows: Collection<PersistentFlow>) {
        when (flows.size) {
            0 -> Unit
            1 -> implications += flows.first().implications
            else -> Unit
        }
    }

    /**
     * 替换或移除 flow 中变量的所有引用。
     *
     * 该方法同时更新别名表、成员变量 receiver 引用、implication 和已批准的类型陈述。
     */
    private fun MutableFlow.replaceVariable(variable: RealVariable, replacement: RealVariable?) {
        val original = directAliasMap.remove(variable)
        if (original != null) {
            if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
                assert(variable !in backwardsAliasMap)
                assert(variable !in implications)
                assert(variable !in approvedTypeStatements)
            }
            val siblings = backwardsAliasMap.getValue(original)
            if (siblings.size > 1) {
                backwardsAliasMap[original] = siblings.remove(variable)
            } else {
                backwardsAliasMap.remove(original)
            }
            if (replacement != null) {
                addLocalVariableAlias(this, replacement, original)
            }
        } else {
            val aliases = backwardsAliasMap.remove(variable)
            val replacementOrNext = replacement ?: aliases?.first()
            variableStorage.replaceReceiverReferencesInMembers(variable, replacementOrNext) { old, new ->
                replaceVariable(old, new)
            }
            implications.replaceVariable(variable, replacementOrNext)
            approvedTypeStatements.replaceVariable(variable, replacementOrNext)
            if (aliases != null && replacementOrNext != null) {
                directAliasMap -= replacementOrNext
                val withoutSelf = aliases - replacementOrNext
                if (withoutSelf.isNotEmpty()) {
                    withoutSelf.associateWithTo(directAliasMap) { replacementOrNext }
                    backwardsAliasMap[replacementOrNext] =
                        backwardsAliasMap[replacementOrNext]?.addAll(withoutSelf) ?: withoutSelf.toPersistentSet()
                }
            }
        }
    }

    /**
     * 执行操作陈述批准的共享实现。
     *
     * 该过程会沿 implication 图传播条件，生成对应的类型陈述，并可按需清理已经确定或不可能成立的
     * implication。
     */
    private fun approveOperationStatement(
        logicStatements: Map<DataFlowVariable, PersistentList<Implication>>,
        approvedStatement: OperationStatement,
        removeApprovedOrImpossible: Boolean,
    ): TypeStatements {
        val result = mutableMapOf<DataFlowVariable, MutableTypeStatement>()
        val queue = LinkedList<OperationStatement>().apply { add(approvedStatement) }
        val approved = mutableSetOf<OperationStatement>()
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            if (!removeApprovedOrImpossible && !approved.add(next)) continue

            val operation = next.operation
            val variable = next.variable
            val impliedType = if (operation == Operation.EqNull) nullableNothingType else anyType
            val resultStatement = result.getOrPut(variable) { MutableTypeStatement(variable) }
            resultStatement.upperTypes.add(impliedType)
            when (operation) {
                Operation.EqTrue -> resultStatement.lowerTypes.add(DfaType.BooleanLiteral(false))
                Operation.EqFalse -> resultStatement.lowerTypes.add(DfaType.BooleanLiteral(true))
                else -> Unit
            }

            val statements = logicStatements[variable] ?: continue
            val stillUnknown = statements.removeAll {
                val knownValue = it.condition.operation.valueIfKnown(operation)
                if (knownValue == true) {
                    when (val effect = it.effect) {
                        is OperationStatement -> queue += effect
                        is TypeStatement -> result.getOrPut(effect.variable) { MutableTypeStatement(effect.variable) } += effect
                    }
                }
                removeApprovedOrImpossible && knownValue != null
            }
            if (stillUnknown != statements && logicStatements is MutableMap) {
                if (stillUnknown.isEmpty()) {
                    logicStatements.remove(variable)
                } else {
                    logicStatements[variable] = stillUnknown
                }
            }
        }
        return result
    }

    /**
     * 判断当前 flow 是否已经批准指定类型陈述。
     */
    fun approveTypeStatement(flow: Flow, statement: TypeStatement): Boolean {
        val variable = statement.variable
        val known = flow.getTypeStatement(variable)
        val approvedStatements = when {
            known != null -> mapOf(variable to known.toMutable().also { it.upperTypes += variable.originalType })
            else -> mapOf(variable to MutableTypeStatement(variable, mutableSetOf(variable.originalType)))
        }

        val approvedUpper = approvedStatements.values.getUnifiedUpperType()
        val approvedLower = approvedStatements.values.getIntersectedLowerType()
        val statementUpper = listOf(statement).getUnifiedUpperType()
        val statementLower = listOf(statement).getIntersectedLowerType()

        return (approvedUpper == null || statementUpper == null || AbstractTypeChecker.isSubtypeOf(context, approvedUpper, statementUpper))
            && (approvedLower == null || statementUpper == null || !AbstractTypeChecker.isSubtypeOf(context, statementUpper, approvedLower))
            && (statementLower == null || approvedUpper == null || !AbstractTypeChecker.isSubtypeOf(context, approvedUpper, statementLower))
    }

    /**
     * 对两组类型陈述执行 or 合并。
     */
    fun orForTypeStatements(left: TypeStatements, right: TypeStatements): TypeStatements = when {
        left.isEmpty() -> left
        right.isEmpty() -> right
        else -> buildMap {
            for ((variable, leftStatement) in left) {
                put(variable, or(listOf(leftStatement, right[variable] ?: continue)) ?: continue)
            }
        }
    }

    /**
     * 对两组类型陈述执行 and 合并。
     */
    fun andForTypeStatements(left: TypeStatements, right: TypeStatements): TypeStatements = when {
        left.isEmpty() -> right
        right.isEmpty() -> left
        else -> left.toMutableMap().apply {
            for ((variable, rightStatement) in right) {
                this[variable] = and(this[variable], rightStatement)
            }
        }
    }

    /**
     * 将另一个类型陈述并入当前可变陈述。
     */
    private operator fun MutableTypeStatement.plusAssign(other: TypeStatement) {
        upperTypes += other.upperTypes
        lowerTypes += other.lowerTypes
    }

    /**
     * 对两个类型陈述执行 and 合并。
     */
    fun and(left: TypeStatement?, right: TypeStatement): TypeStatement {
        return left?.toMutable()?.apply { this += right } ?: right
    }

    /**
     * 对一组类型陈述执行 and 合并。
     */
    fun and(statements: Collection<TypeStatement>): TypeStatement? {
        when (statements.size) {
            0 -> return null
            1 -> return statements.first()
        }
        val iterator = statements.iterator()
        val result = iterator.next().toMutable()
        while (iterator.hasNext()) {
            result += iterator.next()
        }
        return result
    }

    /**
     * 对一组类型陈述执行 or 合并。
     */
    fun or(statements: Collection<TypeStatement>): TypeStatement? {
        when (statements.size) {
            0 -> return null
            1 -> return statements.first()
        }
        val variable = statements.first().variable
        assert(statements.all { it.variable == variable }) { "Folding statements for different variables" }
        if (statements.any { it.isEmpty }) return null
        val unifiedUpperType = statements.getUnifiedUpperType()
        val newUpperTypes = when {
            unifiedUpperType == null -> persistentSetOf()
            unifiedUpperType.isOptionalAny() -> persistentSetOf()
            unifiedUpperType.isAcceptableForSmartcast() -> persistentSetOf(unifiedUpperType)
            unifiedUpperType.canBeNullInDfa() -> persistentSetOf()
            else -> persistentSetOf(context.anyType() as ConeCangJieType)
        }
        val newLowerTypes = setOfNotNull(statements.getIntersectedLowerType()?.let(DfaType::Cone)) + statements.getCommonExcludedValues()
        return if (newUpperTypes.isNotEmpty() || newLowerTypes.isNotEmpty()) {
            PersistentTypeStatement(variable, newUpperTypes, newLowerTypes.toPersistentSet())
        } else {
            null
        }
    }

    /**
     * 计算类型陈述集合的统一上界。
     */
    private fun Collection<TypeStatement>.getUnifiedUpperType(): ConeCangJieType? {
        val intersectedUpperTypes = map { statement ->
            statement.upperTypesOrNull?.toList()?.let { ConeTypeIntersector.intersectTypes(context, it) }
                ?: ConeAnyType
        }
        return context.commonSuperTypeOrNull(intersectedUpperTypes)
    }

    /**
     * 计算类型陈述集合下界中的相交类型。
     */
    private fun Collection<TypeStatement>.getIntersectedLowerType(): ConeCangJieType? =
        flatMap { statement ->
            statement.lowerTypes.mapNotNull { (it as? DfaType.Cone)?.type }.takeIf { it.isNotEmpty() }
                ?: listOf(context.nothingType() as ConeCangJieType)
        }.let {
            ConeTypeIntersector.intersectTypes(context, it)
        }.takeUnless { it == session.builtinTypes.nothingType }

    /**
     * 找出所有分支共同排除的符号值。
     */
    private fun Collection<TypeStatement>.getCommonExcludedValues(): Set<DfaType.Symbol> =
        flatMap { it.lowerTypes.filterIsInstance<DfaType.Symbol>() }
            .groupingBy { it }
            .eachCount()
            .filterValues { it == size }
            .keys
}

/**
 * 在已批准类型陈述表中替换变量。
 */
@JvmName("replaceVariableInStatements")
private fun MutableMap<DataFlowVariable, PersistentTypeStatement>.replaceVariable(from: DataFlowVariable, to: DataFlowVariable?) {
    val existing = remove(from) ?: return
    if (to != null) {
        put(to, existing.copy(variable = to))
    }
}

/**
 * 在 implication 表中替换变量。
 */
@JvmName("replaceVariableInImplications")
private fun MutableMap<DataFlowVariable, PersistentList<Implication>>.replaceVariable(from: RealVariable, to: RealVariable?) {
    val existing = remove(from)
    val toReplace = entries.mapNotNull { (variable, implications) ->
        val newImplications = if (to != null) {
            implications.replaceAll { it.replaceVariable(from, to) }
        } else {
            implications.removeAll { it.effect.variable == from }
        }
        if (newImplications != implications) variable to newImplications else null
    }
    for ((variable, implications) in toReplace) {
        if (implications.isEmpty()) {
            remove(variable)
        } else {
            put(variable, implications)
        }
    }
    if (existing != null && to != null) {
        put(to, existing.replaceAll { it.replaceVariable(from, to) })
    }
}

/**
 * 对持久化列表中的每个元素执行替换。
 */
private inline fun <T> PersistentList<T>.replaceAll(block: (T) -> T): PersistentList<T> {
    return mutate { result ->
        val iterator = result.listIterator()
        while (iterator.hasNext()) {
            iterator.set(block(iterator.next()))
        }
    }
}

/**
 * 替换 implication 中出现的真实变量。
 */
private fun Implication.replaceVariable(from: RealVariable, to: RealVariable): Implication = when {
    condition.variable == from -> Implication(condition.copy(variable = to), effect.replaceVariable(from, to))
    effect.variable == from -> Implication(condition, effect.replaceVariable(from, to))
    else -> this
}

/**
 * 替换 statement 中出现的真实变量。
 */
private fun Statement.replaceVariable(from: RealVariable, to: RealVariable): Statement {
    if (variable != from) return this
    return when (this) {
        is OperationStatement -> copy(variable = to)
        is PersistentTypeStatement -> copy(variable = to)
        is MutableTypeStatement -> MutableTypeStatement(to, upperTypes, lowerTypes)
    }
}

/**
 * 判断类型在 DFA 语义下是否可能为空。
 */
private fun ConeCangJieType.canBeNullInDfa(): Boolean {
    return isOption
}

/**
 * 判断类型是否为 `Option<Any>`。
 */
private fun ConeCangJieType.isOptionalAny(): Boolean {
    return isOption && typeArguments.singleOrNull()?.type == ConeAnyType
}
