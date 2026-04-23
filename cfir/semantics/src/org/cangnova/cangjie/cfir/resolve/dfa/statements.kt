package org.cangnova.cangjie.cfir.resolve.dfa

import org.cangnova.cangjie.cfir.DfaType
import org.cangnova.cangjie.cfir.types.ConeCangJieType

sealed class Statement {
    abstract val variable: DataFlowVariable
}

data class OperationStatement(
    override val variable: DataFlowVariable,
    val operation: Operation,
) : Statement() {
    override fun toString(): String = "$variable $operation"
}

sealed class TypeStatement : Statement() {
    abstract override val variable: DataFlowVariable
    abstract val upperTypes: Set<ConeCangJieType>
    abstract val lowerTypes: Set<DfaType>

    val isEmpty: Boolean
        get() = upperTypes.isEmpty() && lowerTypes.isEmpty()

    val isNotEmpty: Boolean
        get() = !isEmpty

    val upperTypesOrNull: Set<ConeCangJieType>?
        get() = upperTypes.takeIf { it.isNotEmpty() }

    val lowerTypesOrNull: Set<DfaType>?
        get() = lowerTypes.takeIf { it.isNotEmpty() }

    override fun toString(): String = "$variable: ${renderType()}"

    fun renderType(): String = listOfNotNull(
        upperTypesOrNull?.joinToString(" & "),
        lowerTypesOrNull?.joinToString(" | ")?.let { "¬($it)" },
    ).joinToString(" & ")
}

class Implication(
    val condition: OperationStatement,
    val effect: Statement,
) {
    override fun toString(): String = "$condition -> $effect"
}

enum class Operation {
    EqTrue,
    EqFalse,
    EqNull,
    NotEqNull,
    ;

    fun valueIfKnown(given: Operation): Boolean? = when (this) {
        EqTrue, EqFalse -> if (given == NotEqNull) null else given == this
        EqNull -> given == EqNull
        NotEqNull -> given != EqNull
    }

    override fun toString(): String = when (this) {
        EqTrue -> "== True"
        EqFalse -> "== False"
        EqNull -> "== Null"
        NotEqNull -> "!= Null"
    }
}
