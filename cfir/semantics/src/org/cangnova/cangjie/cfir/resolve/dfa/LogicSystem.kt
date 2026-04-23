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
    val session: CfirSession get() = context.session
    private val nullableNothingType = session.builtinTypes.nothingType
    private val anyType = session.typeContext.anyType() as ConeCangJieType

    abstract val variableStorage: VariableStorage

    protected open fun ConeCangJieType.isAcceptableForSmartcast(): Boolean {
        return this != nullableNothingType
    }

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

    fun addLocalVariableAlias(flow: MutableFlow, alias: RealVariable, underlyingVariable: RealVariable) {
        if (underlyingVariable == alias) return
        flow.directAliasMap[alias] = underlyingVariable
        flow.backwardsAliasMap[underlyingVariable] =
            flow.backwardsAliasMap[underlyingVariable]?.add(alias) ?: persistentSetOf(alias)
    }

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

    fun addTypeStatements(flow: MutableFlow, statements: TypeStatements): List<TypeStatement> =
        statements.values.mapNotNull { addTypeStatement(flow, it) }

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

    private fun MutableFlow.containsAlready(effect: TypeStatement): Boolean {
        val approved = approvedTypeStatements[effect.variable] ?: return false
        return approved.upperTypes.containsAll(effect.upperTypes) && approved.lowerTypes.containsAll(effect.lowerTypes)
    }

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

    fun approveOperationStatement(flow: PersistentFlow, statement: OperationStatement): TypeStatements {
        return approveOperationStatement(flow.implications.toMutableMap(), statement, removeApprovedOrImpossible = false)
    }

    fun approveOperationStatement(
        flow: MutableFlow,
        statement: OperationStatement,
        removeApprovedOrImpossible: Boolean,
    ): TypeStatements {
        return approveOperationStatement(flow.implications, statement, removeApprovedOrImpossible)
    }

    fun recordNewAssignment(flow: MutableFlow, variable: RealVariable, index: Int) {
        flow.replaceVariable(variable, null)
        flow.assignmentIndex[variable] = index
    }

    fun isSameValueIn(left: PersistentFlow, right: PersistentFlow, variable: RealVariable): Boolean {
        return left.assignmentIndex[variable] == right.assignmentIndex[variable]
    }

    fun isSameValueIn(left: PersistentFlow, right: MutableFlow, variable: RealVariable): Boolean {
        return left.assignmentIndex[variable] == right.assignmentIndex[variable]
    }

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

    private fun MutableFlow.copyCommonAliases(flows: Collection<PersistentFlow>) {
        for ((from, to) in flows.first().directAliasMap) {
            if (directAliasMap[from] != to && flows.all { it.unwrapVariable(from) == to }) {
                addLocalVariableAlias(this, from, to)
            }
        }
    }

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

    private fun MutableFlow.copyImplications(flows: Collection<PersistentFlow>) {
        when (flows.size) {
            0 -> Unit
            1 -> implications += flows.first().implications
            else -> Unit
        }
    }

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

    fun orForTypeStatements(left: TypeStatements, right: TypeStatements): TypeStatements = when {
        left.isEmpty() -> left
        right.isEmpty() -> right
        else -> buildMap {
            for ((variable, leftStatement) in left) {
                put(variable, or(listOf(leftStatement, right[variable] ?: continue)) ?: continue)
            }
        }
    }

    fun andForTypeStatements(left: TypeStatements, right: TypeStatements): TypeStatements = when {
        left.isEmpty() -> right
        right.isEmpty() -> left
        else -> left.toMutableMap().apply {
            for ((variable, rightStatement) in right) {
                this[variable] = and(this[variable], rightStatement)
            }
        }
    }

    private operator fun MutableTypeStatement.plusAssign(other: TypeStatement) {
        upperTypes += other.upperTypes
        lowerTypes += other.lowerTypes
    }

    fun and(left: TypeStatement?, right: TypeStatement): TypeStatement {
        return left?.toMutable()?.apply { this += right } ?: right
    }

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

    private fun Collection<TypeStatement>.getUnifiedUpperType(): ConeCangJieType? {
        val intersectedUpperTypes = map { statement ->
            statement.upperTypesOrNull?.toList()?.let { ConeTypeIntersector.intersectTypes(context, it) }
                ?: ConeAnyType
        }
        return context.commonSuperTypeOrNull(intersectedUpperTypes)
    }

    private fun Collection<TypeStatement>.getIntersectedLowerType(): ConeCangJieType? =
        flatMap { statement ->
            statement.lowerTypes.mapNotNull { (it as? DfaType.Cone)?.type }.takeIf { it.isNotEmpty() }
                ?: listOf(context.nothingType() as ConeCangJieType)
        }.let {
            ConeTypeIntersector.intersectTypes(context, it)
        }.takeUnless { it == session.builtinTypes.nothingType }

    private fun Collection<TypeStatement>.getCommonExcludedValues(): Set<DfaType.Symbol> =
        flatMap { it.lowerTypes.filterIsInstance<DfaType.Symbol>() }
            .groupingBy { it }
            .eachCount()
            .filterValues { it == size }
            .keys
}

@JvmName("replaceVariableInStatements")
private fun MutableMap<DataFlowVariable, PersistentTypeStatement>.replaceVariable(from: DataFlowVariable, to: DataFlowVariable?) {
    val existing = remove(from) ?: return
    if (to != null) {
        put(to, existing.copy(variable = to))
    }
}

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

private inline fun <T> PersistentList<T>.replaceAll(block: (T) -> T): PersistentList<T> {
    return mutate { result ->
        val iterator = result.listIterator()
        while (iterator.hasNext()) {
            iterator.set(block(iterator.next()))
        }
    }
}

private fun Implication.replaceVariable(from: RealVariable, to: RealVariable): Implication = when {
    condition.variable == from -> Implication(condition.copy(variable = to), effect.replaceVariable(from, to))
    effect.variable == from -> Implication(condition, effect.replaceVariable(from, to))
    else -> this
}

private fun Statement.replaceVariable(from: RealVariable, to: RealVariable): Statement {
    if (variable != from) return this
    return when (this) {
        is OperationStatement -> copy(variable = to)
        is PersistentTypeStatement -> copy(variable = to)
        is MutableTypeStatement -> MutableTypeStatement(to, upperTypes, lowerTypes)
    }
}

private fun ConeCangJieType.canBeNullInDfa(): Boolean {
    return isOption
}

private fun ConeCangJieType.isOptionalAny(): Boolean {
    return isOption && typeArguments.singleOrNull()?.type == ConeAnyType
}
