package org.cangnova.cangjie.cfir.resolve.dfa

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toPersistentSet
import org.cangnova.cangjie.cfir.DfaType
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * 已冻结的类型陈述。
 *
 * @property variable 被约束的数据流变量。
 * @property upperTypes 变量已知满足的上界类型集合。
 * @property lowerTypes 变量已知排除的下界集合。
 */
data class PersistentTypeStatement(
    override val variable: DataFlowVariable,
    override val upperTypes: PersistentSet<ConeCangJieType>,
    override val lowerTypes: PersistentSet<DfaType>,
) : TypeStatement()

/**
 * 可变类型陈述。
 *
 * @property variable 被约束的数据流变量。
 * @property upperTypes 可写的上界类型集合。
 * @property lowerTypes 可写的下界集合。
 */
class MutableTypeStatement(
    override val variable: DataFlowVariable,
    override val upperTypes: MutableSet<ConeCangJieType> = linkedSetOf(),
    override val lowerTypes: MutableSet<DfaType> = linkedSetOf(),
) : TypeStatement()

/**
 * 按数据流变量索引的类型陈述集合。
 */
typealias TypeStatements = Map<DataFlowVariable, TypeStatement>

/**
 * 构造“变量等于布尔常量”的操作陈述。
 */
infix fun DataFlowVariable.eq(constant: Boolean): OperationStatement =
    OperationStatement(this, if (constant) Operation.EqTrue else Operation.EqFalse)

/**
 * 构造“变量等于 null”的操作陈述。
 */
@Suppress("UNUSED_PARAMETER")
infix fun DataFlowVariable.eq(constant: Nothing?): OperationStatement =
    OperationStatement(this, Operation.EqNull)

/**
 * 构造“变量不等于 null”的操作陈述。
 */
@Suppress("UNUSED_PARAMETER")
infix fun DataFlowVariable.notEq(constant: Nothing?): OperationStatement =
    OperationStatement(this, Operation.NotEqNull)

/**
 * 构造条件蕴含。
 */
infix fun OperationStatement.implies(effect: Statement): Implication = Implication(this, effect)

/**
 * 构造“真实变量值不等于指定符号”的类型陈述。
 */
infix fun RealVariable.valueNotEq(symbol: CfirBasedSymbol<*>): MutableTypeStatement =
    MutableTypeStatement(this, lowerTypes = linkedSetOf(DfaType.Symbol(symbol)))

/**
 * 构造“真实变量值不等于任一指定符号”的类型陈述。
 */
infix fun RealVariable.valueNotEq(symbols: List<CfirBasedSymbol<*>>): MutableTypeStatement =
    MutableTypeStatement(this, lowerTypes = symbols.mapTo(linkedSetOf(), DfaType::Symbol))

/**
 * 构造“真实变量值不等于布尔常量”的类型陈述。
 */
infix fun RealVariable.valueNotEq(boolean: Boolean): MutableTypeStatement =
    MutableTypeStatement(this, lowerTypes = linkedSetOf(DfaType.BooleanLiteral(boolean)))

/**
 * 构造“变量类型等于指定类型”的类型陈述。
 */
infix fun DataFlowVariable.typeEq(type: ConeCangJieType): MutableTypeStatement =
    MutableTypeStatement(this, upperTypes = linkedSetOf(type))

/**
 * 构造“变量类型不等于指定类型”的类型陈述。
 */
infix fun DataFlowVariable.typeNotEq(type: ConeCangJieType): MutableTypeStatement =
    MutableTypeStatement(this, lowerTypes = linkedSetOf(DfaType.Cone(type)))

/**
 * 将类型陈述转换为持久化形态。
 */
fun TypeStatement.toPersistent(): PersistentTypeStatement = when (this) {
    is PersistentTypeStatement -> this
    else -> PersistentTypeStatement(variable, upperTypes.toPersistentSet(), lowerTypes.toPersistentSet())
}

/**
 * 将类型陈述转换为可变形态。
 */
fun TypeStatement.toMutable(): MutableTypeStatement = when (this) {
    is PersistentTypeStatement -> MutableTypeStatement(variable, upperTypes.builder(), lowerTypes.builder())
    else -> MutableTypeStatement(variable, LinkedHashSet(upperTypes), LinkedHashSet(lowerTypes))
}

/**
 * 判断数据流变量是否为合成变量。
 */
@OptIn(ExperimentalContracts::class)
fun DataFlowVariable.isSynthetic(): Boolean {
    contract {
        returns(true) implies (this@isSynthetic is SyntheticVariable)
    }
    return this is SyntheticVariable
}

/**
 * 判断数据流变量是否为真实符号变量。
 */
@OptIn(ExperimentalContracts::class)
fun DataFlowVariable.isReal(): Boolean {
    contract {
        returns(true) implies (this@isReal is RealVariable)
    }
    return this is RealVariable
}
