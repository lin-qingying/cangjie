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
    private val logicalNames = listOf(
        OperatorNameConventions.ANDAND,
        OperatorNameConventions.OROR,
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
                    addSameOperandUnaryOperators(kind, listOf(OperatorNameConventions.NOT) + unaryIntegerNames)
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
                    addSameOperandOperators(kind, logicalNames, returnKind = PrimitiveTypeKind.BOOLEAN)
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
        val receiverKind = receiverType?.toBuiltinOperatorKind() ?: return null
        val argumentKinds = argumentTypes.map { it.toBuiltinOperatorKind() ?: return null }
        val signature = resolveSignature(name, receiverKind, argumentKinds) ?: return null
        return BuiltinPrimitiveOperatorMatch(signature)
    }

    /**
     * 判断一次 operator 调用是否已经进入内建 primitive 运算语义域。
     *
     * 该判断只描述“应由内建运算规则处理”的形态，不代表匹配成功；
     * 匹配失败时由 body resolve 保留 operator token、左右操作数类型，
     * 交给诊断映射阶段归类为官方的 invalid binary/unary operator。
     */
    fun canDiagnoseInvalidPrimitiveOperator(
        name: Name,
        receiverType: ConeCangJieType?,
        argumentTypes: List<ConeCangJieType>,
    ): Boolean {
        receiverType?.toBuiltinOperatorKind() ?: return false
        if (argumentTypes.any { it.toBuiltinOperatorKind() == null }) return false
        return signaturesByReceiver.values
            .asSequence()
            .flatten()
            .any { signature ->
                signature.name == name && signature.parameterKinds.size == argumentTypes.size
            }
    }

    private fun resolveSignature(
        name: Name,
        receiverKind: PrimitiveTypeKind,
        argumentKinds: List<PrimitiveTypeKind>,
    ): BuiltinPrimitiveOperatorSignature? {
        if (argumentKinds.isEmpty()) {
            return signaturesFor(receiverKind).firstOrNull { candidate ->
                candidate.name == name && candidate.parameterKinds.isEmpty()
            }
        }

        val argumentKind = argumentKinds.singleOrNull() ?: return null

        // 对齐官方 BuiltInOperatorUtil：理想字面量参与内建运算匹配，
        // 但不会作为对外暴露的 primitive 成员签名。
        return when (name) {
            in arithmeticNames -> resolveSameOperandSignature(name, receiverKind, argumentKind, numericKinds)
            OperatorNameConventions.REM -> resolveSameOperandSignature(name, receiverKind, argumentKind, integerKinds)
            in bitwiseNames -> resolveSameOperandSignature(name, receiverKind, argumentKind, integerKinds)
            in logicalNames -> resolveSameOperandSignature(
                name,
                receiverKind,
                argumentKind,
                listOf(PrimitiveTypeKind.BOOLEAN),
                PrimitiveTypeKind.BOOLEAN,
            )
            in shiftNames -> resolveShiftSignature(name, receiverKind, argumentKind)
            in equalityNames -> resolveSameOperandSignature(
                name,
                receiverKind,
                argumentKind,
                equatableKinds,
                PrimitiveTypeKind.BOOLEAN,
            )

            in orderingNames -> resolveSameOperandSignature(
                name,
                receiverKind,
                argumentKind,
                comparableKinds,
                PrimitiveTypeKind.BOOLEAN,
            )

            OperatorNameConventions.EXPONENTIATION -> resolveExponentiationSignature(name, receiverKind, argumentKind)
            else -> signaturesFor(receiverKind).firstOrNull { candidate ->
                candidate.name == name && candidate.parameterKinds == argumentKinds
            }
        }
    }

    private fun resolveSameOperandSignature(
        name: Name,
        receiverKind: PrimitiveTypeKind,
        argumentKind: PrimitiveTypeKind,
        allowedKinds: Collection<PrimitiveTypeKind>,
        returnKind: PrimitiveTypeKind? = null,
    ): BuiltinPrimitiveOperatorSignature? {
        val operandKind = commonBuiltinOperandKind(receiverKind, argumentKind, allowedKinds) ?: return null
        return signature(name, receiverKind, argumentKind, returnKind ?: operandKind)
    }

    private fun resolveShiftSignature(
        name: Name,
        receiverKind: PrimitiveTypeKind,
        argumentKind: PrimitiveTypeKind,
    ): BuiltinPrimitiveOperatorSignature? {
        if (!receiverKind.isBuiltinIntegerOperand() || !argumentKind.isBuiltinIntegerOperand()) return null
        return signature(name, receiverKind, argumentKind, receiverKind.defaultedIdealKind())
    }

    private fun resolveExponentiationSignature(
        name: Name,
        receiverKind: PrimitiveTypeKind,
        argumentKind: PrimitiveTypeKind,
    ): BuiltinPrimitiveOperatorSignature? {
        val defaultedReceiverKind = receiverKind.defaultedIdealKind()
        val isValid = when (defaultedReceiverKind) {
            PrimitiveTypeKind.INT64 -> argumentKind == PrimitiveTypeKind.UINT64 ||
                    argumentKind == PrimitiveTypeKind.IDEAL_INT

            PrimitiveTypeKind.FLOAT64 -> argumentKind == PrimitiveTypeKind.INT64 ||
                    argumentKind == PrimitiveTypeKind.FLOAT64 ||
                    argumentKind == PrimitiveTypeKind.IDEAL_INT ||
                    argumentKind == PrimitiveTypeKind.IDEAL_FLOAT

            else -> false
        }
        if (!isValid) return null
        return signature(name, receiverKind, argumentKind, defaultedReceiverKind)
    }

    private fun commonBuiltinOperandKind(
        left: PrimitiveTypeKind,
        right: PrimitiveTypeKind,
        allowedKinds: Collection<PrimitiveTypeKind>,
    ): PrimitiveTypeKind? {
        if (left == PrimitiveTypeKind.NOTHING) return right.takeIf { it in allowedKinds }?.defaultedIdealKind()
        if (right == PrimitiveTypeKind.NOTHING) return left.takeIf { it in allowedKinds }?.defaultedIdealKind()
        if (left !in allowedKinds || right !in allowedKinds) return null
        if (left == right) return left
        if (left == PrimitiveTypeKind.IDEAL_INT && right.isInteger && !right.isIdeal) return right
        if (right == PrimitiveTypeKind.IDEAL_INT && left.isInteger && !left.isIdeal) return left
        if (left == PrimitiveTypeKind.IDEAL_FLOAT && right.isFloat && !right.isIdeal) return right
        if (right == PrimitiveTypeKind.IDEAL_FLOAT && left.isFloat && !left.isIdeal) return left
        return null
    }

    private fun PrimitiveTypeKind.isBuiltinIntegerOperand(): Boolean =
        this == PrimitiveTypeKind.NOTHING || isInteger

    private fun PrimitiveTypeKind.defaultedIdealKind(): PrimitiveTypeKind = when (this) {
        PrimitiveTypeKind.IDEAL_INT -> PrimitiveTypeKind.INT64
        PrimitiveTypeKind.IDEAL_FLOAT -> PrimitiveTypeKind.FLOAT64
        else -> this
    }

    private fun ConeCangJieType.toBuiltinOperatorKind(): PrimitiveTypeKind? = when (this) {
        is ConePrimitiveType -> kind
        is ConeIdealIntLiteralType -> PrimitiveTypeKind.IDEAL_INT
        is ConeIdealFloatLiteralType -> PrimitiveTypeKind.IDEAL_FLOAT
        else -> null
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
