package org.cangnova.cangjie.cfir.resolve.dfa

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toPersistentSet
import org.cangnova.cangjie.cfir.DfaType
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

data class PersistentTypeStatement(
    override val variable: DataFlowVariable,
    override val upperTypes: PersistentSet<ConeCangJieType>,
    override val lowerTypes: PersistentSet<DfaType>,
) : TypeStatement()

class MutableTypeStatement(
    override val variable: DataFlowVariable,
    override val upperTypes: MutableSet<ConeCangJieType> = linkedSetOf(),
    override val lowerTypes: MutableSet<DfaType> = linkedSetOf(),
) : TypeStatement()

typealias TypeStatements = Map<DataFlowVariable, TypeStatement>

infix fun DataFlowVariable.eq(constant: Boolean): OperationStatement =
    OperationStatement(this, if (constant) Operation.EqTrue else Operation.EqFalse)

@Suppress("UNUSED_PARAMETER")
infix fun DataFlowVariable.eq(constant: Nothing?): OperationStatement =
    OperationStatement(this, Operation.EqNull)

@Suppress("UNUSED_PARAMETER")
infix fun DataFlowVariable.notEq(constant: Nothing?): OperationStatement =
    OperationStatement(this, Operation.NotEqNull)

infix fun OperationStatement.implies(effect: Statement): Implication = Implication(this, effect)

infix fun RealVariable.valueNotEq(symbol: CfirBasedSymbol<*>): MutableTypeStatement =
    MutableTypeStatement(this, lowerTypes = linkedSetOf(DfaType.Symbol(symbol)))

infix fun RealVariable.valueNotEq(symbols: List<CfirBasedSymbol<*>>): MutableTypeStatement =
    MutableTypeStatement(this, lowerTypes = symbols.mapTo(linkedSetOf(), DfaType::Symbol))

infix fun RealVariable.valueNotEq(boolean: Boolean): MutableTypeStatement =
    MutableTypeStatement(this, lowerTypes = linkedSetOf(DfaType.BooleanLiteral(boolean)))

infix fun DataFlowVariable.typeEq(type: ConeCangJieType): MutableTypeStatement =
    MutableTypeStatement(this, upperTypes = linkedSetOf(type))

infix fun DataFlowVariable.typeNotEq(type: ConeCangJieType): MutableTypeStatement =
    MutableTypeStatement(this, lowerTypes = linkedSetOf(DfaType.Cone(type)))

fun TypeStatement.toPersistent(): PersistentTypeStatement = when (this) {
    is PersistentTypeStatement -> this
    else -> PersistentTypeStatement(variable, upperTypes.toPersistentSet(), lowerTypes.toPersistentSet())
}

fun TypeStatement.toMutable(): MutableTypeStatement = when (this) {
    is PersistentTypeStatement -> MutableTypeStatement(variable, upperTypes.builder(), lowerTypes.builder())
    else -> MutableTypeStatement(variable, LinkedHashSet(upperTypes), LinkedHashSet(lowerTypes))
}

@OptIn(ExperimentalContracts::class)
fun DataFlowVariable.isSynthetic(): Boolean {
    contract {
        returns(true) implies (this@isSynthetic is SyntheticVariable)
    }
    return this is SyntheticVariable
}

@OptIn(ExperimentalContracts::class)
fun DataFlowVariable.isReal(): Boolean {
    contract {
        returns(true) implies (this@isReal is RealVariable)
    }
    return this is RealVariable
}
