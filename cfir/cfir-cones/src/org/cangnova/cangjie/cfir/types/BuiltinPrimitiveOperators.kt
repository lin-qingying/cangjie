package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions

data class BuiltinPrimitiveOperatorSignature(
    val name: Name,
    val receiverKind: PrimitiveTypeKind,
    val parameterKinds: List<PrimitiveTypeKind>,
    val returnKind: PrimitiveTypeKind,
)

data class BuiltinPrimitiveOperatorMatch(
    val signature: BuiltinPrimitiveOperatorSignature,
) {
    val returnType: ConePrimitiveType = ConePrimitiveType(signature.returnKind)
}

object BuiltinPrimitiveOperators {
    private val arithmeticNames = listOf(
        OperatorNameConventions.PLUS,
        OperatorNameConventions.MINUS,
        OperatorNameConventions.TIMES,
        OperatorNameConventions.DIV,
    )
    private val bitwiseNames = listOf(
        OperatorNameConventions.AND,
        OperatorNameConventions.OR,
        OperatorNameConventions.XOR,
    )
    private val shiftNames = listOf(
        OperatorNameConventions.LEFT_SHIFT,
        OperatorNameConventions.RIGHT_SHIFT,
    )
    private val equalityNames = listOf(
        OperatorNameConventions.EQUALS,
        OperatorNameConventions.NOT_EQUALS,
    )
    private val orderingNames = listOf(
        OperatorNameConventions.COMPARE_LT,
        OperatorNameConventions.COMPARE_LTEQ,
        OperatorNameConventions.COMPARE_GT,
        OperatorNameConventions.COMPARE_GTEQ,
    )
    private val unaryNumericNames = listOf(
        OperatorNameConventions.UNARY_MINUS,
        OperatorNameConventions.UNARY_PLUS,
    )
    private val unaryIntegerNames = listOf(
        OperatorNameConventions.INC,
        OperatorNameConventions.DEC,
    )

    private val integerKinds = PrimitiveTypeKind.entries.filter { it.isInteger }
    private val numericKinds = PrimitiveTypeKind.entries.filter { it.isNumeric }
    private val comparableKinds = numericKinds + PrimitiveTypeKind.RUNE
    private val equatableKinds = comparableKinds + PrimitiveTypeKind.BOOLEAN + PrimitiveTypeKind.UNIT

    private val signaturesByReceiver: Map<PrimitiveTypeKind, List<BuiltinPrimitiveOperatorSignature>> = buildMap {
        for (kind in PrimitiveTypeKind.entries) {
            val signatures = buildList {
                if (kind in numericKinds) {
                    addSameOperandOperators(kind, arithmeticNames)
                    addSameOperandUnaryOperators(kind, unaryNumericNames)
                }

                if (kind in integerKinds) {
                    addSameOperandOperators(kind, listOf(OperatorNameConventions.REM) + bitwiseNames)
                    addSameOperandUnaryOperators(kind, unaryIntegerNames)
                    for (shiftName in shiftNames) {
                        for (rhsKind in integerKinds) {
                            add(signature(shiftName, kind, rhsKind, kind))
                        }
                    }
                }

                if (kind in equatableKinds) {
                    addSameOperandOperators(kind, equalityNames, returnKind = PrimitiveTypeKind.BOOLEAN)
                }

                if (kind in comparableKinds) {
                    addSameOperandOperators(kind, orderingNames, returnKind = PrimitiveTypeKind.BOOLEAN)
                }

                if (kind == PrimitiveTypeKind.BOOLEAN) {
                    add(signature(OperatorNameConventions.NOT, kind, emptyList(), PrimitiveTypeKind.BOOLEAN))
                }

                when (kind) {
                    PrimitiveTypeKind.INT64 -> {
                        add(
                            signature(
                                OperatorNameConventions.EXPONENTIATION,
                                kind,
                                PrimitiveTypeKind.UINT64,
                                PrimitiveTypeKind.INT64,
                            ),
                        )
                    }

                    PrimitiveTypeKind.FLOAT64 -> {
                        add(
                            signature(
                                OperatorNameConventions.EXPONENTIATION,
                                kind,
                                PrimitiveTypeKind.INT64,
                                PrimitiveTypeKind.FLOAT64,
                            ),
                        )
                        add(
                            signature(
                                OperatorNameConventions.EXPONENTIATION,
                                kind,
                                PrimitiveTypeKind.FLOAT64,
                                PrimitiveTypeKind.FLOAT64,
                            ),
                        )
                    }

                    else -> Unit
                }
            }

            put(kind, signatures)
        }
    }

    fun signaturesFor(receiverKind: PrimitiveTypeKind): List<BuiltinPrimitiveOperatorSignature> =
        signaturesByReceiver[receiverKind].orEmpty()

    fun resolve(
        name: Name,
        receiverType: ConeCangJieType?,
        argumentTypes: List<ConeCangJieType>,
    ): BuiltinPrimitiveOperatorMatch? {
        val receiverKind = (receiverType as? ConePrimitiveType)?.kind ?: return null
        val argumentKinds = argumentTypes.map { (it as? ConePrimitiveType)?.kind ?: return null }
        val signature = signaturesFor(receiverKind).firstOrNull { candidate ->
            candidate.name == name && candidate.parameterKinds == argumentKinds
        } ?: return null
        return BuiltinPrimitiveOperatorMatch(signature)
    }

    private fun MutableList<BuiltinPrimitiveOperatorSignature>.addSameOperandOperators(
        kind: PrimitiveTypeKind,
        names: List<Name>,
        returnKind: PrimitiveTypeKind = kind,
    ) {
        for (name in names) {
            add(signature(name, kind, kind, returnKind))
        }
    }

    private fun MutableList<BuiltinPrimitiveOperatorSignature>.addSameOperandUnaryOperators(
        kind: PrimitiveTypeKind,
        names: List<Name>,
        returnKind: PrimitiveTypeKind = kind,
    ) {
        for (name in names) {
            add(signature(name, kind, emptyList(), returnKind))
        }
    }

    private fun signature(
        name: Name,
        receiverKind: PrimitiveTypeKind,
        parameterKind: PrimitiveTypeKind,
        returnKind: PrimitiveTypeKind,
    ): BuiltinPrimitiveOperatorSignature {
        return BuiltinPrimitiveOperatorSignature(name, receiverKind, listOf(parameterKind), returnKind)
    }

    private fun signature(
        name: Name,
        receiverKind: PrimitiveTypeKind,
        parameterKinds: List<PrimitiveTypeKind>,
        returnKind: PrimitiveTypeKind,
    ): BuiltinPrimitiveOperatorSignature {
        return BuiltinPrimitiveOperatorSignature(name, receiverKind, parameterKinds, returnKind)
    }
}
