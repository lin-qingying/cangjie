package org.cangnova.cangjie.cfir.resolve.dfa

import kotlinx.collections.immutable.PersistentList
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

import kotlinx.collections.immutable.toPersistentList
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.declarations.exportFacts

/** 固定 K 的 LogicSystem owner；N 完成值扩展不创建第二套存储或 consumer 实现。 */
abstract class LogicSystem(private val context: ConeInferenceContext) {
    val session: CfirSession get() = context.session
    private val nullableNothingType = session.builtinTypes.nothingType
    private val anyType = session.typeContext.anyType() as ConeCangJieType
    protected open fun ConeCangJieType.isAcceptableForSmartcast(): Boolean = this != nullableNothingType



    /** 初始化参数/receiver 的实际 entry，不制造 assignmentIndex。调用前必须是新入口。 */
    fun initializeEntry(flow: MutableFlow, variable: RealVariable, owner: org.cangnova.cangjie.cfir.CfirElement, type: ConeCangJieType): ValueUse {
        val storage = flow.remember(variable)
        check(storage !in flow.assignmentIndex && storage !in flow.valueBindings)
        flow.replaceVariable(storage, null)
        val value = CompletedValueVariable(owner, type)
        val event = ValueEvent(owner)
        flow.storageValues[storage] = StorageValueState.Initialized(value)
        flow.valueBindings[storage] = ValueBinding(storage, value, type, BindingVersion.Entry(event))
        flow.storageChanges[storage] = StorageChange(event, StorageChangeKind.ENTRY, write = true, establishesValue = true)
        addTypeStatement(flow, value intrinsicTypeEq type)
        establishReceiverAddress(flow, storage)
        flow.normalizeReceiverInventory(flow.freeze())
        return ValueUse(value, type)
    }

    /** 原声明 initializer 已经完成；保存其值而不把初始化事件伪装成一次后续赋值。 */
    fun initializeCompletedValue(flow: MutableFlow, variable: RealVariable, use: ValueUse, owner: org.cangnova.cangjie.cfir.CfirElement) {
        val storage = flow.remember(variable)
        flow.replaceVariable(storage, null)
        val event = ValueEvent(owner)
        flow.storageValues[storage] = StorageValueState.Initialized(use.value)
        flow.valueBindings[storage] = ValueBinding(storage, use.value, use.type, BindingVersion.Entry(event))
        flow.storageChanges[storage] = StorageChange(event, StorageChangeKind.ENTRY, write = true, establishesValue = true)
        establishReceiverAddress(flow, storage)
        flow.normalizeReceiverInventory(flow.freeze())
    }

    /** 仅实际成功的外部稳定读取可首次承认 SSA 值；local 声明必须先有 entry/store。 */
    fun observeExternalStableValue(flow: MutableFlow, variable: RealVariable,
        owner: org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression, type: ConeCangJieType,
        initialization: ExternalReadInitialization): ValueUse {
        val storage = flow.remember(variable)
        val declaration = checkNotNull(storage.symbol.cfir as? org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration)
        require(!declaration.isLocal) { "A local read cannot initialize storage" }
        require(storage.getStability(flow, session) == org.cangnova.cangjie.cfir.expressions.CfirSmartcastStability.STABLE_VALUE)
        require(owner.runtimeReceiversForDfa().all { flow.getExpressionCompletionOrNull(it) != null }) {
            "External target initialization does not replace this read's completed runtime receiver"
        }
        when (initialization) {
            is ExternalReadInitialization.Declaration -> {
                require(initialization.declaration === declaration)
                require(declaration.exportFacts.isInitialized || declaration.exportFacts.isImported || declaration.status.isForeign) {
                    "An external declaration read requires published initialization or platform evidence"
                }
            }
            is ExternalReadInitialization.Receiver -> {
                val receiver = checkNotNull(owner.dispatchReceiver)
                require(flow.getExpressionCompletionOrNull(receiver)?.event === initialization.completion.event) {
                    "Member initialization must use this occurrence's completed receiver"
                }
                require(storage.dispatchReceiver != null)
            }
        }
        val addresses = equivalentMemberAddresses(flow, storage) + storage
        val observed = addresses.mapNotNull { flow.valueBindings[it]?.value }.distinct()
        check(observed.size <= 1) { "Equivalent stable addresses have inconsistent values" }
        val value = if (observed.isEmpty()) flow.storageValues.getValue(storage).value else observed.single()
        val event = ValueEvent(owner)
        for (address in addresses) {
            val old = flow.storageValues.getValue(address).value
            if (old != value) rehomeUninitializedFacts(flow, old, value)
            flow.storageValues[address] = StorageValueState.Initialized(value)
            if (address !in flow.valueBindings) flow.valueBindings[address] = ValueBinding(address, value, type, BindingVersion.Read(event))
            flow.storageChanges[address] = StorageChange(event, StorageChangeKind.OBSERVATION, write = false, establishesValue = true,
                mutation = flow.storageChanges[address]?.mutation)
        }
        addTypeStatement(flow, value intrinsicTypeEq type)
        addresses.forEach { establishReceiverAddress(flow, it) }
        flow.normalizeReceiverInventory(flow.freeze())
        return ValueUse(value, type)
    }

    /** 新计算值的实际出口先 begin，再添加 IS/Bool 等事实，最后 complete。 */
    fun retireExpression(flow: MutableFlow, expression: org.cangnova.cangjie.cfir.expressions.CfirExpression) {
        flow.expressionValues.remove(expression)
    }

    fun beginExpression(flow: MutableFlow, expression: org.cangnova.cangjie.cfir.expressions.CfirExpression, type: ConeCangJieType): ExpressionEvent =
        beginExpressionUse(flow, expression, ValueUse(CompletedValueVariable(expression, type), type), expression)

    /** 调用方已按 K effective stability 证明本次读取稳定；不在此替代 capture 分析。 */
    fun beginStableRead(flow: MutableFlow, expression: org.cangnova.cangjie.cfir.expressions.CfirExpression, variable: RealVariable, type: ConeCangJieType): ExpressionEvent =
        beginExpressionUse(flow, expression, ValueUse(flow.getBoundValue(variable), type), expression)

    /** wrapper/分支结果复用已有捕获值，但本次 use type 和事件仍独立。 */
    fun beginCapturedExpression(flow: MutableFlow, expression: org.cangnova.cangjie.cfir.expressions.CfirExpression, use: ValueUse): ExpressionEvent =
        beginExpressionUse(flow, expression, use, expression)

    private fun beginExpressionUse(flow: MutableFlow, expression: org.cangnova.cangjie.cfir.expressions.CfirExpression, use: ValueUse, origin: org.cangnova.cangjie.cfir.CfirElement): ExpressionEvent {
        check(flow.expressionValues[expression] !is ExpressionValueState.Staging) { "The previous event is not complete" }
        flow.expressionValues.remove(expression)
        val event = ExpressionEvent(expression, origin)
        // 类型随这次 use 保存。批准陈述由检查或赋值传递发布。
        flow.expressionValues[expression] = ExpressionValueState.Staging(ExpressionCompletion(use.value, use.type, event))
        return event
    }

    /** finish 只消费同一个 staging event；禁止跨事件、重复 finish 或把未完成结果当完成值。 */
    fun completeExpression(flow: MutableFlow, event: ExpressionEvent): ExpressionCompletion {
        val state = checkNotNull(flow.expressionValues[event.expression])
        check(state is ExpressionValueState.Staging && state.completion.event === event)
        return state.completion.also {
            addTypeStatement(flow, it.value intrinsicTypeEq it.type)
            flow.expressionValues[event.expression] = ExpressionValueState.Complete(it)
        }
    }

    /** 无正常结果只结束本次 staging；不删除其他 binding/capture 持有的旧值事实。 */
    fun abandonExpression(flow: MutableFlow, event: ExpressionEvent) {
        val state = checkNotNull(flow.expressionValues[event.expression])
        check(state is ExpressionValueState.Staging && state.completion.event === event)
        flow.expressionValues.remove(event.expression)
    }

    /** 元素来源是各真实 element-exit 的被冻结完成记录；没有 pendingTuples 侧表。 */
    fun recordTuple(flow: MutableFlow, tuple: org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral, captured: List<ExpressionCompletion>): ExpressionCompletion {
        val type = checkNotNull(tuple.coneTypeOrNull).fullyExpandedType(session) as ConeTupleType
        require(type.elementTypes.size == captured.size)
        val slots = captured.map { it.asUse() }
        val value = CompletedValueVariable(tuple, type, slots.map { it.value })
        flow.tupleValues[value] = slots.toPersistentList()
        slots.forEach { addTypeStatement(flow, it.value intrinsicTypeEq it.type) }
        return completeExpression(flow, beginExpressionUse(flow, tuple, ValueUse(value, type), tuple))
    }

    /** 路径类型由这次 use 决定，value.originalType 不替代 SmartCast 的 slot type。 */
    fun projectValue(flow: MutableFlow, root: ValueUse, path: List<Int>): ValueUse {
        var current = root
        for (index in path) {
            val type = current.type.fullyExpandedType(session) as ConeTupleType
            require(index in type.elementTypes.indices)
            val componentType = type.elementTypes[index]
            val components = flow.tupleValues[current.value]
            val value = if (components != null) {
                check(components.size == type.elementTypes.size)
                components[index].value
            } else {
                val prototype = ProjectedValueVariable(current.value, index, componentType)
                flow.projectedValues.getOrPut(prototype) { prototype }
            }
            current = ValueUse(value, componentType)
            addTypeStatement(flow, value intrinsicTypeEq componentType)
        }
        return current
    }

    /** 真正 store 接收已经捕获的值/type；只在这里提交实际赋值与 binding。 */
    fun bindCompletedValue(flow: MutableFlow, variable: RealVariable, use: ValueUse, index: Int, owner: org.cangnova.cangjie.cfir.CfirElement) {
        val storage = flow.remember(variable)
        val addresses = equivalentMemberAddresses(flow, storage) + storage
        recordNewAssignment(flow, storage, index)
        val event = ValueEvent(owner)
        for (address in addresses) {
            flow.storageValues[address] = StorageValueState.Initialized(use.value)
            flow.valueBindings[address] = ValueBinding(address, use.value, use.type, BindingVersion.Assignment(index))
            flow.storageChanges[address] = StorageChange(event, StorageChangeKind.STORE, write = true, establishesValue = true)
        }
        addTypeStatement(flow, use.value intrinsicTypeEq use.type)
        addresses.forEach { establishReceiverAddress(flow, it) }
        flow.normalizeReceiverInventory(flow.freeze())
    }

    /** Class复制只建立成员地址关系；显式类型的值代表不能因此被direct alias折叠。 */
    private fun establishReceiverAddress(flow: MutableFlow, variable: RealVariable) {
        if (flow.canonicalStorageVariable(variable) != variable) return
        val value = flow.valueBindings[variable]?.value ?: return
        if (value.originalType.fullyExpandedType(session).toSymbol(session)?.cfir !is org.cangnova.cangjie.cfir.declarations.CfirClass) return
        val peer = flow.valueBindings.entries.firstOrNull {
            it.key != variable && it.value.value === value && flow.canonicalStorageVariable(it.key) == it.key
        }?.key ?: return
        val target = selectReceiverRepresentative(flow, value, flow.valueBindings.entries.filter { it.value.value === value }.map { it.key }, flow.receiverAliasMap[peer] ?: peer)
        if (variable == target) return
        flow.receiverAliasMap[variable] = target
        flow.replaceReceiverReferencesInMembers(variable, target) { old, new -> flow.replaceVariable(old, new) }
    }

    /** 真实重赋值解绑引用地址；依赖该存储的成员不能冒充仍存活的独立引用。 */
    private fun MutableFlow.detachReceiverAddress(variable: RealVariable, replaceMember: (RealVariable, RealVariable?) -> Unit): Boolean {
        val value = valueBindings[variable]?.value ?: return false
        if (value.originalType.fullyExpandedType(session).toSymbol(session)?.cfir !is org.cangnova.cangjie.cfir.declarations.CfirClass) return false
        if (receiverAliasMap.remove(variable) != null) return true
        val aliases = receiverAliasMap.entries.filter { it.value == variable }.map { it.key }
        fun dependsOn(candidate: RealVariable): Boolean {
            if (candidate == variable || canonicalStorageVariable(candidate) == variable) return true
            return candidate.dispatchReceiver?.let(::dependsOn) == true || candidate.extensionReceiver?.let(::dependsOn) == true
        }
        fun depth(candidate: RealVariable): Int = 1 + max(candidate.dispatchReceiver?.let(::depth) ?: 0, candidate.extensionReceiver?.let(::depth) ?: 0)
        val replacement = aliases.filter { !dependsOn(it) }.minByOrNull(::depth)
        aliases.forEach { receiverAliasMap.remove(it) }
        if (replacement != null) aliases.filter { it != replacement && !dependsOn(it) }.forEach { receiverAliasMap[it] = replacement }
        replaceReceiverReferencesInMembers(variable, replacement, replaceMember)
        return true
    }

    /** 地址重命名传递同一引用的代表，不执行新的写入或改变SSA版本。 */
    private fun MutableFlow.renameReceiverAddress(from: RealVariable, to: RealVariable) {
        val oldTarget = receiverAliasMap.remove(from)
        val target = oldTarget ?: receiverAliasMap[to]?.takeUnless { it == from } ?: to
        val aliases = receiverAliasMap.entries.filter { it.value == from }.map { it.key }
        aliases.forEach { alias ->
            if (alias == target) receiverAliasMap.remove(alias) else receiverAliasMap[alias] = target
        }
        if (to != target) receiverAliasMap[to] = target
    }

    /** 合流只依据完成binding的实际Class值重建地址组，不从类型相同或名称推断别名。 */
    private fun MutableFlow.rebuildReceiverAddresses(common: PersistentFlow) {
        receiverAliasMap.clear()
        val groups = valueBindings.entries.filter {
            it.value.value.originalType.fullyExpandedType(session).toSymbol(session)?.cfir is org.cangnova.cangjie.cfir.declarations.CfirClass
        }.groupBy { it.value.value }
        fun depth(variable: RealVariable): Int = 1 + max(variable.dispatchReceiver?.let(::depth) ?: 0, variable.extensionReceiver?.let(::depth) ?: 0)
        groups.entries.sortedBy { (_, entries) -> entries.minOf { depth(it.key) } }.forEach { (value, entries) ->
            // 原始Binding贡献及待登记的规范端点都属于该引用组；不能在迁移前丢掉嵌套Class成员。
            val storages = entries.flatMap { listOf(it.key, canonicalStorageVariable(it.key)) }.distinct()
            if (storages.isEmpty()) return@forEach
            val preferred = storages.firstOrNull { common.valueBindings[it]?.value === value && it !in common.receiverAliasMap }
            val target = selectReceiverRepresentative(this, value, storages, preferred)
            storages.filter { it != target }.forEach { receiverAliasMap[it] = target }
        }
        // 全部最浅代表固定后，按receiver依赖从浅到深封闭raw/canonical两种端点。
        // alias目标不比其来源更深，因此深端点只依赖已经投影的更浅地址，计划有限且不重跑求值。
        val originalAliases = receiverAliasMap.build()
        receiverAliasMap.clear()
        originalAliases.entries.sortedBy { depth(it.key) }.forEach { (from, target) ->
            val canonicalTarget = canonicalStorageVariable(target)
            val canonicalFrom = canonicalStorageVariable(from)
            if (from != canonicalTarget) receiverAliasMap[from] = canonicalTarget
            if (canonicalFrom != canonicalTarget) receiverAliasMap[canonicalFrom] = canonicalTarget
        }
    }

    /** 代表不能经任何receiver依赖同一引用组；合法对象环不等于合法地址代表环。 */
    private fun selectReceiverRepresentative(flow: Flow, value: ValueVariable, candidates: List<RealVariable>, preferred: RealVariable?): RealVariable {
        fun dependsOnValue(variable: RealVariable): Boolean =
            flow.getValueBinding(variable)?.value === value ||
                variable.dispatchReceiver?.let(::dependsOnValue) == true || variable.extensionReceiver?.let(::dependsOnValue) == true
        fun createsAddressCycle(target: RealVariable): Boolean {
            val proposed = flow.receiverAliasMap.toMutableMap()
            candidates.forEach { proposed.remove(it) }
            candidates.filter { it != target }.forEach { proposed[it] = target }
            val visited = mutableSetOf<RealVariable>()
            val active = mutableSetOf<RealVariable>()
            fun visit(variable: RealVariable): Boolean {
                if (variable in active) return true
                if (!visited.add(variable)) return false
                active.add(variable)
                val cyclic = proposed[variable]?.let(::visit) == true ||
                    variable.dispatchReceiver?.let(::visit) == true || variable.extensionReceiver?.let(::visit) == true
                active.remove(variable)
                return cyclic
            }
            return proposed.keys.any(::visit)
        }
        val legal = candidates.filter { candidate ->
            flow.canonicalStorageVariable(candidate) == candidate &&
                candidate.dispatchReceiver?.let(::dependsOnValue) != true && candidate.extensionReceiver?.let(::dependsOnValue) != true &&
                !createsAddressCycle(candidate)
        }
        check(legal.isNotEmpty()) { "A reference address group has no independent storage representative" }
        fun depth(variable: RealVariable): Int = 1 + max(variable.dispatchReceiver?.let(::depth) ?: 0, variable.extensionReceiver?.let(::depth) ?: 0)
        val minimumDepth = legal.minOf(::depth)
        val independent = legal.filter { depth(it) == minimumDepth }
        if (preferred in independent) return checkNotNull(preferred)
        return independent.first()
    }

    /** receiver 的现有 real alias 证明同一成员地址；仅同值的局部副本不在这个集合。 */
    private fun equivalentMemberAddresses(flow: Flow, variable: RealVariable): List<RealVariable> {
        if (variable.dispatchReceiver == null && variable.extensionReceiver == null) return emptyList()
        fun same(a: RealVariable?, b: RealVariable?): Boolean {
            if (a == b) return true
            if (a == null || b == null) return false
            val original = flow.getValueBinding(a)?.value?.originalType ?: return false
            if (original.fullyExpandedType(session).toSymbol(session)?.cfir !is org.cangnova.cangjie.cfir.declarations.CfirClass) return false
            return flow.unwrapReceiverAddress(a, session) == flow.unwrapReceiverAddress(b, session)
        }
        return flow.realVariables.values.distinct().filter {
            it != variable && flow.canonicalStorageVariable(it) == it &&
                it.symbol == variable.symbol && it.isImplicit == variable.isImplicit &&
                same(it.dispatchReceiver, variable.dispatchReceiver) && same(it.extensionReceiver, variable.extensionReceiver)
        }
    }

    /** K alias primitive 可合并未初始化知识，但不把初始化位传给另一个 storage。 */
    fun addLocalVariableAlias(flow: MutableFlow, alias: RealVariable, underlyingVariable: RealVariable) {
        val from = flow.remember(alias)
        val target = flow.unwrapVariable(flow.remember(underlyingVariable))
        if (from == target) return
        val oldAlias = flow.directAliasMap[from]
        check(oldAlias == null || oldAlias == target)
        if (oldAlias == target) return
        val left = flow.storageValues.getValue(from).value
        val right = flow.storageValues.getValue(target).value
        val leftCaptured = flow.valueBindings.values.any { it.value == left }
        val rightCaptured = flow.valueBindings.values.any { it.value == right }
        check(!leftCaptured || !rightCaptured || left == right) { "Distinct actual values cannot be aliased without a store" }
        val chosen = if (leftCaptured) left else right
        if (left != chosen) rehomeUninitializedFacts(flow, left, chosen)
        if (right != chosen) rehomeUninitializedFacts(flow, right, chosen)
        val members = (flow.backwardsAliasMap.remove(from) ?: persistentSetOf()).add(from).remove(target)
        for (member in members) {
            flow.directAliasMap[member] = target
            val state = flow.storageValues.getValue(member)
            flow.storageValues[member] = when (state) {
                is StorageValueState.Uninitialized -> StorageValueState.Uninitialized(chosen)
                is StorageValueState.Initialized -> {
                    check(state.value == chosen)
                    state
                }
            }
        }
        flow.backwardsAliasMap.addAll(target, members)
    }

    /** 被合并的未初始化知识从未成为实际 completion；实际捕获值永不改名或删除。 */
    private fun rehomeUninitializedFacts(flow: MutableFlow, from: ValueVariable, to: ValueVariable) {
        check(flow.valueBindings.values.none { it.value == from })
        check(flow.expressionValues.values.none { it.completion.value == from })
        val old = flow.approvedTypeStatements.remove(from)
        old?.let { addTypeStatement(flow, it.copy(variable = to)) }
        fun move(statement: Statement): Statement = if (statement.variable != from) statement else when (statement) {
            is OperationStatement -> statement.copy(variable = to)
            is TypeStatement -> statement.toPersistent().copy(variable = to)
        }
        val implications = flow.implications.build()
        flow.implications.clear()
        for ((key, list) in implications) {
            val newKey = if (key == from) to else key
            val existing = flow.implications[newKey].orEmpty()
            val moved = list.map { Implication(move(it.condition) as OperationStatement, move(it.effect)) }
            flow.implications[newKey] = (existing + moved).toPersistentList()
        }
        for ((storage, state) in flow.storageValues.build()) {
            if (state.value == from) {
                check(state is StorageValueState.Uninitialized)
                flow.storageValues[storage] = StorageValueState.Uninitialized(to)
            }
        }
    }

    fun addOneWayAlias(flow: MutableFlow, alias: RealVariable, underlyingVariable: RealVariable) {
        val from = flow.unwrapVariable(alias)
        val to = flow.unwrapVariable(underlyingVariable)
        if (from == to) return
        flow.oneWayAliasMap.add(to, from)
    }

    fun copyImplicationsForOneWayAlias(flow: MutableFlow, alias: RealVariable, underlyingVariable: RealVariable) {
        val from = flow.requireValue(underlyingVariable)
        val to = flow.requireValue(alias)
        if (from == to) return
        for (implication in flow.implications[from].orEmpty()) {
            addImplication(flow, Implication(OperationStatement(to, implication.condition.operation), implication.effect))
        }
    }

    fun addTypeStatement(flow: MutableFlow, statement: TypeStatement): TypeStatement? {
        if (statement.isEmpty) return null
        val value = flow.requireValue(statement.variable)
        val old = flow.approvedTypeStatements[value]
        val upper = old?.upperTypes?.addAll(statement.upperTypes) ?: statement.upperTypes.toPersistentSet()
        val lower = old?.lowerTypes?.addAll(statement.lowerTypes) ?: statement.lowerTypes.toPersistentSet()
        val explicit = (old?.upperTypes.orEmpty() - old?.intrinsicUpperTypes.orEmpty()) +
            (statement.upperTypes - statement.intrinsicUpperTypes)
        val intrinsic = ((old?.intrinsicUpperTypes.orEmpty() + statement.intrinsicUpperTypes) - explicit).toPersistentSet()
        if (upper === old?.upperTypes && lower === old?.lowerTypes && intrinsic == old?.intrinsicUpperTypes) return null
        return PersistentTypeStatement(value, upper, lower, intrinsic).also { flow.approvedTypeStatements[value] = it }
    }

    fun addTypeStatements(flow: MutableFlow, statements: TypeStatements): List<TypeStatement> =
        statements.values.mapNotNull { addTypeStatement(flow, it) }

    private fun normalizeStatement(flow: Flow, statement: Statement): Statement {
        val value = flow.requireValue(statement.variable)
        return when (statement) {
            is OperationStatement -> statement.copy(variable = value)
            is TypeStatement -> statement.toPersistent().copy(variable = value)
        }
    }

    fun addImplication(flow: MutableFlow, implication: Implication) {
        val condition = normalizeStatement(flow, implication.condition) as OperationStatement
        val effect = normalizeStatement(flow, implication.effect)
        if (effect == condition || effect is TypeStatement && (effect.isEmpty || flow.containsAlready(effect))) return
        val value = condition.variable as ValueVariable
        flow.implications[value] = flow.implications[value]?.add(Implication(condition, effect))
            ?: persistentListOf(Implication(condition, effect))
    }

    private fun MutableFlow.containsAlready(effect: TypeStatement): Boolean {
        val approved = approvedTypeStatements[effect.variable as ValueVariable] ?: return false
        return approved.upperTypes.containsAll(effect.upperTypes) && approved.lowerTypes.containsAll(effect.lowerTypes) &&
            (effect.upperTypes - effect.intrinsicUpperTypes).none { it in approved.intrinsicUpperTypes }
    }

    /** 来源值的事实可被多个 capture 使用，迁移复制 condition，不摧毁原值 implication。 */
    fun translateVariableFromConditionInStatements(flow: MutableFlow, originalVariable: DataFlowVariable, newVariable: DataFlowVariable, transform: (Implication) -> Implication? = { it }) {
        val from = flow.requireValue(originalVariable)
        val to = flow.requireValue(newVariable)
        if (from == to) return
        for (implication in flow.implications[from].orEmpty()) {
            transform(OperationStatement(to, implication.condition.operation) implies implication.effect)?.let {
                addImplication(flow, it)
            }
        }
    }

    fun approveOperationStatement(flow: PersistentFlow, statement: OperationStatement): TypeStatements =
        approveOperationStatement(flow.implications.toMutableMap(), normalizeStatement(flow, statement) as OperationStatement, false)

    fun approveOperationStatement(flow: MutableFlow, statement: OperationStatement, removeApprovedOrImpossible: Boolean): TypeStatements =
        approveOperationStatement(flow.implications, normalizeStatement(flow, statement) as OperationStatement, removeApprovedOrImpossible)

    /** 完整 K 赋值失效；可读局部/实际目标保留未知 SSA 值，成员依赖由递归事务忘记。 */
    fun recordNewAssignment(flow: MutableFlow, variable: RealVariable, index: Int) {
        val storage = flow.remember(variable)
        val addresses = equivalentMemberAddresses(flow, storage) + storage
        val initialized = addresses.associateWith { flow.valueBindings[it] != null }
        val event = ValueEvent(storage.symbol.cfir)
        val value = StorageValueVariable(storage, index, event)
        for (address in addresses) {
            flow.replaceVariable(address, null)
            flow.assignmentIndex[address] = index
            flow.storageValues[address] = if (initialized.getValue(address)) StorageValueState.Initialized(value)
                else StorageValueState.Uninitialized(value)
            flow.storageChanges[address] = StorageChange(event, StorageChangeKind.INVALIDATION, write = true, establishesValue = false)
            if (initialized.getValue(address)) flow.valueBindings[address] = ValueBinding(address, value, address.originalType, BindingVersion.Assignment(index))
        }
        flow.normalizeReceiverInventory(flow.freeze())
    }

    /** 原样保持固定 K 的 SSA assignment identity，初始 null==null 仍为 true。 */
    fun isSameValueIn(left: Flow, right: Flow, variable: RealVariable): Boolean =
        left.assignmentIndex[left.canonicalStorageVariable(variable)] == right.assignmentIndex[right.canonicalStorageVariable(variable)]

    /** 实际成员重命名只迁移已有版本；新 receiver 属性从成功读取重新承认，不制造 index。 */
    private fun MutableFlow.replaceVariable(variable: RealVariable, replacement: RealVariable?) {
        val common = freeze()
        replaceVariableContents(variable, replacement, common)
        normalizeReceiverInventory(common)
    }

    /** 一次迁移中递归处理所有成员；组关系与现行库存只在整个迁移完成后共同封口。 */
    private fun MutableFlow.replaceVariableContents(variable: RealVariable, replacement: RealVariable?, common: PersistentFlow) {
        val addressDetached = replacement == null && detachReceiverAddress(variable) { old, new -> replaceVariableContents(old, new, common) }
        val state = storageValues.remove(variable)
        val binding = valueBindings.remove(variable)
        val oldChange = storageChanges.remove(variable)
        val oldIndex = assignmentIndex[variable]
        val original = directAliasMap.remove(variable)
        val aliases = if (original == null) backwardsAliasMap.remove(variable) else null
        val representative = original ?: replacement ?: aliases?.first()

        fun transfer(to: RealVariable) {
            if (state == null) return
            val existingState = storageValues[to]
            val existing = valueBindings[to]
            val explicitMemberRename = replacement != null && to == replacement
            if (!explicitMemberRename) {
                // 换 real 代表不证明该 sibling 已初始化，也不覆盖 sibling 自己的赋值版本。
                check(existingState != null && existingState.value == state.value)
                return
            }
            if (existing != null) {
                if (state is StorageValueState.Uninitialized && state.value != existing.value) {
                    rehomeUninitializedFacts(this, state.value, existing.value)
                } else {
                    check(existing.value == state.value) { "Member remapping conflicts with an existing value" }
                }
            } else {
                check(assignmentIndex[to] == null || assignmentIndex[to] == oldIndex)
                if (oldIndex == null) assignmentIndex.remove(to) else assignmentIndex[to] = oldIndex
                if (existingState != null && existingState.value != state.value) {
                    check(existingState is StorageValueState.Uninitialized)
                    rehomeUninitializedFacts(this, existingState.value, state.value)
                }
                storageValues[to] = when (state) {
                    is StorageValueState.Uninitialized -> StorageValueState.Uninitialized(state.value)
                    is StorageValueState.Initialized -> StorageValueState.Initialized(state.value)
                }
                if (binding != null) valueBindings[to] = binding.copy(variable = to)
                if (oldChange != null) storageChanges[to] = oldChange
            }
        }
        if (representative != null) transfer(representative)
        if (replacement != null) renameReceiverAddress(variable, replacement)
        val addressRepresentative = replacement ?: representative?.takeIf {
            state is StorageValueState.Initialized && state.value.originalType.fullyExpandedType(session).toSymbol(session)?.cfir is org.cangnova.cangjie.cfir.declarations.CfirClass
        }
        if (!addressDetached) replaceReceiverReferencesInMembers(variable, addressRepresentative) { old, new -> replaceVariableContents(old, new, common) }

        if (original != null) {
            val siblings = backwardsAliasMap.getValue(original).remove(variable)
            if (siblings.isEmpty()) backwardsAliasMap.remove(original) else backwardsAliasMap[original] = siblings
            if (replacement != null && replacement != original) {
                transfer(replacement)
                addLocalVariableAlias(this, replacement, original)
            }
        } else if (aliases != null && representative != null) {
            directAliasMap.remove(representative)
            val rest = aliases.remove(representative)
            if (rest.isNotEmpty()) {
                rest.forEach { directAliasMap[it] = representative }
                backwardsAliasMap.addAll(representative, rest)
            }
        }
        val ownOneWay = oneWayAliasMap.remove(variable)
        if (representative != null && ownOneWay != null) {
            oneWayAliasMap.addAll(representative, ownOneWay.remove(representative))
        }
        for ((target, sources) in oneWayAliasMap.build()) {
            if (variable !in sources) continue
            val changed = sources.remove(variable).let { if (representative != null && target != representative) it.add(representative) else it }
            if (changed.isEmpty()) oneWayAliasMap.remove(target) else oneWayAliasMap[target] = changed
        }
        if (replacement != null) {
            assignmentIndex.remove(variable)
            // 旧 prefix 将来可能表示新 receiver 的属性，禁止重新使用初始知识节点。
            storageChanges[variable] = StorageChange(ValueEvent(variable.symbol.cfir), StorageChangeKind.MEMBER_FORGET, true, false)
        }
        if (replacement == null) {
            val member = variable.dispatchReceiver != null || variable.extensionReceiver != null
            storageChanges[variable] = StorageChange(ValueEvent(variable.symbol.cfir),
                if (member) StorageChangeKind.MEMBER_FORGET else StorageChangeKind.INVALIDATION, true, false)
        }
        // 已登记旧 prefix 仍可保存类型知识，但新的知识节点不证明初始化。
        if (variable !in storageValues) {
            val event = storageChanges.getValue(variable).event
            storageValues[variable] = StorageValueState.Uninitialized(StorageValueVariable(variable, assignmentIndex[variable], event))
        }
        // 未初始化存储的知识节点没有完成值。失去最后一个地址后，当前流不再保留它的陈述及蕴含。
        if (state is StorageValueState.Uninitialized && representative == null &&
            storageValues.values.none { it.value == state.value }
        ) {
            approvedTypeStatements.remove(state.value)
            implications.remove(state.value)
            for ((condition, effects) in implications.build()) {
                val live = effects.filterNot { it.effect.variable == state.value }
                if (live.size == effects.size) continue
                if (live.isEmpty()) implications.remove(condition)
                else implications[condition] = live.toPersistentList()
            }
        }
    }

    /** 清理非现行地址的Initialized记录；退休prefix只留知识，不可被下一次rename重新激活。 */
    private fun MutableFlow.normalizeReceiverInventory(common: PersistentFlow) {
        rebuildReceiverAddresses(common)
        val inventory = realVariables.keys.toList()
        val remaps = inventory.mapNotNull { variable ->
            val canonical = canonicalStorageVariable(variable)
            check(canonicalStorageVariable(canonical) == canonical) { "Receiver address quotient is not idempotent" }
            if (canonical != variable && (variable in valueBindings || storageChanges[variable]?.kind != StorageChangeKind.MEMBER_FORGET)) {
                variable to registerVariable(canonical)
            } else null
        }
        remaps.forEach { (from, to) -> replaceVariableContents(from, to, common) }
        // 保留本次安全quotient，rename已传递所有端点；不能另选代表让已登记的嵌套目标失效。
        valueBindings.keys.forEach { variable ->
            check(canonicalStorageVariable(variable) == variable) { "A retired receiver prefix still owns a current Binding" }
            check(getVariableIfKnown(variable) === valueBindings.getValue(variable).variable) { "A current Binding is missing its canonical Real inventory" }
        }
    }

    private fun MutableFlow.mergeVariables(flows: Collection<PersistentFlow>) {
        for (flow in flows) {
            realVariables += flow.realVariables
            for ((receiver, members) in flow.memberVariables) memberVariables.addAll(receiver, members)
            for ((tuple, slots) in flow.tupleValues) {
                val existing = tupleValues[tuple]
                check(existing == null || existing == slots)
                tupleValues[tuple] = slots
            }
            projectedValues += flow.projectedValues
        }
    }

    private fun MutableFlow.mergeAssignments(flows: Collection<PersistentFlow>) {
        val changed = mutableMapOf<RealVariable, Int>()
        for (flow in flows) {
            for ((variable, index) in flow.assignmentIndex) {
                if (assignmentIndex[variable] != index) changed[variable] = max(index, changed[variable] ?: 0)
            }
        }
        for ((variable, index) in changed) recordNewAssignment(this, variable, index)
    }

    private fun MutableFlow.copyCommonAliases(flows: Collection<PersistentFlow>) {
        for ((from, to) in flows.first().directAliasMap) {
            if (flows.all { it.unwrapVariable(from) == to } && knownValue(from) != null && knownValue(from) == knownValue(to))
                addLocalVariableAlias(this, from, to)
        }
        for ((to, sources) in flows.first().oneWayAliasMap) {
            for (from in sources) {
                if (flows.all { from in it.oneWayAliasMap[to].orEmpty() } && knownValue(from) != null && knownValue(to) != null)
                    addOneWayAlias(this, from, to)
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
            if (to != null && knownValue(from) != null && knownValue(from) == knownValue(to)) addLocalVariableAlias(this, from, to)
        }
    }

    private fun MutableFlow.copyImplications(flows: Collection<PersistentFlow>) {
        if (flows.size == 1) implications += flows.single().implications
        // 固定 K 对多 statementFlow 不计算新 implication 交集；共同祖先事实继续保留。
    }

    /** 固定输入域和一次局部 memo：binding、expression 与递归 component 共用它。 */
    fun joinFlow(flows: Collection<PersistentFlow>, statementFlows: Collection<PersistentFlow>, union: Boolean, valueOwner: org.cangnova.cangjie.cfir.CfirElement,
        expressionResult: ExpressionJoinInput? = null): MutableFlow {
        require(statementFlows.all { statement -> flows.any { it === statement } })
        expressionResult?.let { require(it.values.size == flows.size) }
        when (flows.size) {
            0 -> return MutableFlow()
            1 -> return flows.single().fork().also { result ->
                expressionResult?.let { result.joinExpressionResult(flows.toList(), statementFlows, union, valueOwner, it, mutableMapOf()) }
            }
        }
        val ordered = flows.toList()
        val common = ordered.reduce { a, b -> a.lowestCommonAncestor(b) ?: error("No common ancestor") }
        val result = common.fork()
        result.mergeVariables(ordered)
        result.mergeAssignments(ordered)
        val memo = mutableMapOf<ValueJoinKey, ValueVariable>()
        result.joinValueState(ordered, statementFlows, common, union, valueOwner, memo)
        result.normalizeReceiverInventory(common)
        expressionResult?.let { result.joinExpressionResult(ordered, statementFlows, union, valueOwner, it, memo) }
        if (union) result.copyNonConflictingAliases(ordered, common) else result.copyCommonAliases(ordered)
        result.copyImplications(statementFlows)
        return result
    }

    private fun MutableFlow.joinExpressionResult(flows: List<PersistentFlow>, statements: Collection<PersistentFlow>, union: Boolean,
        origin: org.cangnova.cangjie.cfir.CfirElement, result: ExpressionJoinInput, memo: MutableMap<ValueJoinKey, ValueVariable>) {
        expressionValues.remove(result.expression)
        if (result.required.indices.any { result.required[it] && result.values[it] == null }) return
        val inputs = result.values.mapIndexed { index, value ->
            value?.let { JoinedValueInput(flows[index], it.asUse(), it.event) }
        }
        if (inputs.all { it == null }) return
        val use = joinCompletedValues(inputs, statements, union, origin, memo)
        val event = ExpressionEvent(result.expression, origin, result.values.filterNotNull().map { it.event })
        addTypeStatement(this, use.value intrinsicTypeEq use.type)
        expressionValues[result.expression] = ExpressionValueState.Complete(ExpressionCompletion(use.value, use.type, event))
    }

    private fun MutableFlow.joinValueState(flows: List<PersistentFlow>, statementFlows: Collection<PersistentFlow>, common: PersistentFlow, union: Boolean, origin: org.cangnova.cangjie.cfir.CfirElement, memo: MutableMap<ValueJoinKey, ValueVariable>) {
        val storages = flows.flatMap { it.storageValues.keys + it.storageChanges.keys }.mapTo(mutableSetOf()) { registerVariable(it) }
        val statesByStorage = mutableMapOf<RealVariable, StorageValueState>()
        val bindings = mutableMapOf<RealVariable, ValueBinding>()
        val changes = mutableMapOf<RealVariable, StorageChange>()
        for (storage in storages) {
            val commonStorage = common.canonicalStorageVariable(storage)
            val inputStorages = flows.map { it.canonicalStorageVariable(storage) }
            val commonChange = common.storageChanges[commonStorage]
            val newChanges = flows.mapIndexed { index, flow -> flow.storageChanges[inputStorages[index]] }
            val writers = newChanges.map { it?.mutation != null && it.mutation !== commonChange?.mutation }
            val anyWriter = writers.any { it }
            val inputs = flows.mapIndexed { index, flow ->
                val selected = !union || !anyWriter || writers[index]
                if (!selected) null else {
                    val inputStorage = inputStorages[index]
                    val value = flow.storageValues[inputStorage]?.value
                        ?: StorageValueVariable(storage, flow.assignmentIndex[inputStorage], ValueEvent(origin))
                    val group = if (union && !anyWriter) common else flow.assignmentIndex[inputStorage]?.let(BindingVersion::Assignment)
                        ?: newChanges[index]?.event ?: flow
                    val binding = flow.valueBindings[inputStorage]
                    JoinedValueInput(flow, ValueUse(value, binding?.type ?: storage.originalType), group, intrinsic = binding != null)
                }
            }
            // 实际输入地址决定成员SSA；旧raw prefix的版本不能进入现行Binding。
            val inputIndices = flows.mapIndexedNotNull { index, flow -> flow.assignmentIndex[inputStorages[index]] }
            val joinedIndex = inputIndices.maxOrNull()
            if (joinedIndex == null) assignmentIndex.remove(storage) else assignmentIndex[storage] = joinedIndex
            val member = storage.dispatchReceiver != null || storage.extensionReceiver != null
            val allInitialized = flows.indices.all { inputStorages[it] in flows[it].valueBindings }
            val initialized = if (!union) allInitialized
            else (common.valueBindings[commonStorage] != null || newChanges.any { it?.establishesValue == true }) &&
                (!member || allInitialized)
            val use = joinCompletedValues(inputs, statementFlows, union, origin, memo)
            statesByStorage[storage] = if (initialized) StorageValueState.Initialized(use.value)
                else StorageValueState.Uninitialized(use.value)
            if (initialized) {
                val versions = flows.mapIndexedNotNull { index, flow -> flow.valueBindings[inputStorages[index]]?.version }
                check(versions.isNotEmpty())
                val index = assignmentIndex[storage]
                val version = if (index != null) BindingVersion.Assignment(index)
                else if (versions.distinct().size == 1) versions.first()
                else BindingVersion.Merged(ValueEvent(origin), versions)
                bindings[storage] = ValueBinding(storage, use.value, use.type, version)
            }
            val same = newChanges.first()
            changes[storage] = if (same != null && newChanges.all { it?.event === same.event }) same
            else {
                val event = ValueEvent(origin)
                StorageChange(event, StorageChangeKind.JOIN, writers.any { it }, initialized,
                    mutation = if (writers.any { it }) event else commonChange?.mutation)
            }
        }
        storageValues.clear()
        storageValues.putAll(statesByStorage)
        valueBindings.clear()
        valueBindings.putAll(bindings)
        storageChanges.clear()
        storageChanges.putAll(changes)

        val expressions = flows.flatMapTo(mutableSetOf()) { it.expressionValues.keys }
        val states = mutableMapOf<org.cangnova.cangjie.cfir.expressions.CfirExpression, ExpressionValueState>()
        for (expression in expressions) {
            val prior = common.expressionValues[expression]
            val all = flows.map { it.expressionValues[expression] }
            val writers = all.map { it != null && it.update !== prior?.update }
            if (!union && all.any { it == null }) continue
            val active = all.mapIndexed { index, state -> if (!union || writers.none { it } || writers[index]) state else null }
            val present = active.filterNotNull()
            if (present.isEmpty()) continue
            val staging = present.first() is ExpressionValueState.Staging
            check(present.all { (it is ExpressionValueState.Staging) == staging }) { "Cannot join unfinished and completed events" }
            if (staging) check(present.all { it.completion.event === present.first().completion.event })
            val inputs = active.mapIndexed { index, state ->
                state?.let { JoinedValueInput(flows[index], it.completion.asUse(), it.update, intrinsic = !staging) }
            }
            val use = joinCompletedValues(inputs, statementFlows, union, origin, memo)
            val events = present.map { it.completion.event }.distinct()
            val event = if (events.size == 1) events.single() else ExpressionEvent(expression, origin, events)
            val completion = ExpressionCompletion(use.value, use.type, event)
            val updates = present.map { it.update }.distinct()
            val update = if (updates.size == 1) updates.single() else ValueEvent(origin)
            if (!staging) addTypeStatement(this, use.value intrinsicTypeEq use.type)
            states[expression] = if (staging) ExpressionValueState.Staging(completion, update) else ExpressionValueState.Complete(completion, update)
        }
        expressionValues.clear()
        expressionValues.putAll(states)
    }

    private fun MutableFlow.joinCompletedValues(inputs: List<JoinedValueInput?>, statementFlows: Collection<PersistentFlow>, union: Boolean, origin: org.cangnova.cangjie.cfir.CfirElement, memo: MutableMap<ValueJoinKey, ValueVariable>): ValueUse {
        val present = inputs.filterNotNull()
        require(present.isNotEmpty())
        val identityType = checkNotNull(context.commonSuperTypeOrNull(present.map { it.use.type }))
        val proofInputs = present.filter { input -> statementFlows.any { it === input.flow } }
        val type = identityType
        val key = ValueJoinKey(inputs.map { it?.use?.value }.toPersistentList())
        val candidates = present.map { it.use.value }.distinct()
        val value = memo.getOrPut(key) { if (candidates.size == 1) candidates.single() else CompletedValueVariable(origin, identityType, candidates) }
        fun facts(input: JoinedValueInput): TypeStatement {
            val intrinsic = if (input.intrinsic) mutableSetOf(input.use.type) else mutableSetOf()
            val statement = MutableTypeStatement(value, intrinsic.toMutableSet(), intrinsicUpperTypes = intrinsic)
            input.flow.getTypeStatement(input.use.value)?.let {
                statement += it
            }
            return statement
        }
        val statements = if (!union) proofInputs.map(::facts)
        else proofInputs.groupBy { it.group }.values.map { group -> checkNotNull(and(group.map(::facts))) }
        or(statements)?.let { addTypeStatement(this, it) }
        if (proofInputs.isNotEmpty() && present.all { it.intrinsic }) addTypeStatement(this, value intrinsicTypeEq type)

        val tupleType = type.fullyExpandedType(session) as? ConeTupleType
        val sourceShapes = present.map { it.use.type.fullyExpandedType(session) as? ConeTupleType }
        if (tupleType != null && candidates.size > 1 && present.all { it.intrinsic } &&
            sourceShapes.all { it != null && it.elementTypes.size == tupleType.elementTypes.size }) {
            val slots = tupleType.elementTypes.indices.map { index ->
                val childInputs = inputs.map { input ->
                    input?.let {
                        val sourceType = it.use.type.fullyExpandedType(session) as ConeTupleType
                        require(sourceType.elementTypes.size == tupleType.elementTypes.size)
                        val parts = it.flow.tupleValues[it.use.value]
                        val child = if (parts != null) parts[index].value else {
                            val prototype = ProjectedValueVariable(it.use.value, index, sourceType.elementTypes[index])
                            projectedValues.getOrPut(prototype) { prototype }
                        }
                        JoinedValueInput(it.flow, ValueUse(child, sourceType.elementTypes[index]), it.group)
                    }
                }
                joinCompletedValues(childInputs, statementFlows, union, origin, memo)
            }
            val existing = tupleValues[value]
            check(existing == null || existing.map { it.value } == slots.map { it.value })
            tupleValues[value] = slots.toPersistentList()
        }
        return ValueUse(value, type)
    }

    private fun approveOperationStatement(
        logicStatements: Map<ValueVariable, PersistentList<Implication>>,
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
            val variable = next.variable as ValueVariable
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
        val explicit = (upperTypes - intrinsicUpperTypes) + (other.upperTypes - other.intrinsicUpperTypes)
        intrinsicUpperTypes.addAll(other.intrinsicUpperTypes)
        intrinsicUpperTypes.removeAll(explicit)
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
        // OR只能从每条必需路径的独立批准证明取得显式来源；固有类型不能升格为无条件批准。
        val explicitlyProved = newUpperTypes.filterTo(mutableSetOf()) { upper ->
            statements.all { statement ->
                val explicit = statement.upperTypes - statement.intrinsicUpperTypes
                explicit.isNotEmpty() && AbstractTypeChecker.isSubtypeOf(
                    context, ConeTypeIntersector.intersectTypes(context, explicit.toList()), upper,
                )
            }
        }
        val explicitStatements = statements.map { statement ->
            PersistentTypeStatement(variable, (statement.upperTypes - statement.intrinsicUpperTypes).toPersistentSet(), persistentSetOf())
        }
        val explicitUpper = if (explicitStatements.any { it.upperTypes.isEmpty() }) null
            else explicitStatements.getUnifiedUpperType()
        val independentExplicit = when {
            explicitUpper == null || explicitUpper.isOptionalAny() -> persistentSetOf()
            explicitUpper.isAcceptableForSmartcast() -> persistentSetOf(explicitUpper)
            explicitUpper.canBeNullInDfa() -> persistentSetOf()
            else -> persistentSetOf(context.anyType() as ConeCangJieType)
        }
        // 来源可观察性独立于类型集合的蕴含裁剪：更强的固有D不能删掉每路都证明的显式B。
        val allUpper = newUpperTypes.addAll(independentExplicit)
        val intrinsic = newUpperTypes.removeAll(explicitlyProved + independentExplicit)
        return if (allUpper.isNotEmpty() || newLowerTypes.isNotEmpty()) {
            PersistentTypeStatement(variable, allUpper, newLowerTypes.toPersistentSet(), intrinsic)
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
