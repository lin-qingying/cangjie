package org.cangnova.cangjie.cfir.resolve.dfa

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashMapOf

abstract class Flow {
    abstract val knownVariables: Set<DataFlowVariable>
    abstract fun unwrapVariable(variable: RealVariable): RealVariable
    abstract fun getTypeStatement(variable: DataFlowVariable): TypeStatement?
    abstract fun getImplications(variable: DataFlowVariable): Collection<Implication>?

    open fun unwrapVariable(variable: DataFlowVariable): DataFlowVariable =
        if (variable is RealVariable) unwrapVariable(variable) else variable
}

class PersistentFlow internal constructor(
    private val previousFlow: PersistentFlow?,
    private val approvedTypeStatements: PersistentMap<DataFlowVariable, PersistentTypeStatement>,
    internal val implications: PersistentMap<DataFlowVariable, PersistentList<Implication>>,
    internal val assignmentIndex: PersistentMap<RealVariable, Int>,
    internal val directAliasMap: PersistentMap<RealVariable, RealVariable>,
    private val backwardsAliasMap: PersistentMap<RealVariable, PersistentSet<RealVariable>>,
) : Flow() {
    private val level: Int = previousFlow?.let { it.level + 1 } ?: 0

    override val knownVariables: Set<DataFlowVariable>
        get() = approvedTypeStatements.keys + directAliasMap.keys

    override fun unwrapVariable(variable: RealVariable): RealVariable =
        directAliasMap[variable] ?: variable

    override fun getTypeStatement(variable: DataFlowVariable): TypeStatement? =
        approvedTypeStatements[unwrapVariable(variable)]?.copy(variable = variable)

    override fun getImplications(variable: DataFlowVariable): Collection<Implication>? =
        implications[variable]

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

    fun fork(): MutableFlow = MutableFlow(
        previousFlow = this,
        approvedTypeStatements = approvedTypeStatements.builder(),
        implications = implications.builder(),
        assignmentIndex = assignmentIndex.builder(),
        directAliasMap = directAliasMap.builder(),
        backwardsAliasMap = backwardsAliasMap.builder(),
    )
}

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

    override val knownVariables: Set<DataFlowVariable>
        get() = approvedTypeStatements.keys + directAliasMap.keys

    override fun unwrapVariable(variable: RealVariable): RealVariable =
        directAliasMap[variable] ?: variable

    override fun getTypeStatement(variable: DataFlowVariable): TypeStatement? =
        approvedTypeStatements[unwrapVariable(variable)]?.copy(variable = variable)

    override fun getImplications(variable: DataFlowVariable): Collection<Implication>? =
        implications[variable]

    fun freeze(): PersistentFlow = PersistentFlow(
        previousFlow = previousFlow,
        approvedTypeStatements = approvedTypeStatements.build(),
        implications = implications.build(),
        assignmentIndex = assignmentIndex.build(),
        directAliasMap = directAliasMap.build(),
        backwardsAliasMap = backwardsAliasMap.build(),
    )
}

private fun <K, V> emptyPersistentHashMapBuilder(): PersistentMap.Builder<K, V> =
    persistentHashMapOf<K, V>().builder()
