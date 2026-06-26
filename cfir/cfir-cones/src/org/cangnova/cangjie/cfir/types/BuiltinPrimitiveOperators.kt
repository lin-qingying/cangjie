package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions

/**
 * 内建 primitive 运算符签名。
 *
 * @property name 运算符函数名。
 * @property receiverKind 接收者 primitive 类型。
 * @property parameterKinds 参数 primitive 类型列表；一元运算为空列表。
 * @property returnKind 运算结果 primitive 类型。
 */
data class BuiltinPrimitiveOperatorSignature(
    /** 运算符函数名。 */
    val name: Name,
    /** 接收者 primitive 类型。 */
    val receiverKind: PrimitiveTypeKind,
    /** 参数 primitive 类型列表；一元运算为空列表。 */
    val parameterKinds: List<PrimitiveTypeKind>,
    /** 运算结果 primitive 类型。 */
    val returnKind: PrimitiveTypeKind,
)

/**
 * primitive 内建运算匹配结果。
 *
 * @property signature 命中的内建运算签名。
 */
data class BuiltinPrimitiveOperatorMatch(
    /** 命中的内建运算签名。 */
    val signature: BuiltinPrimitiveOperatorSignature,
) {
    /**
     * 命中签名对应的返回类型。
     */
    val returnType: ConePrimitiveType = ConePrimitiveType(signature.returnKind)
}

/**
 * primitive 类型内建运算符解析表。
 *
 * 该对象集中维护数值、位运算、逻辑、比较、位移和幂运算的 primitive 签名，
 * 供调用解析在未找到显式成员时按语言内建规则匹配。
 */
object BuiltinPrimitiveOperators {
    /**
     * 二元算术运算符名称集合。
     */
    private val arithmeticNames = listOf(
        OperatorNameConventions.PLUS,
        OperatorNameConventions.MINUS,
        OperatorNameConventions.TIMES,
        OperatorNameConventions.DIV,
    )

    /**
     * 整数位运算符名称集合。
     */
    private val bitwiseNames = listOf(
        OperatorNameConventions.AND,
        OperatorNameConventions.OR,
        OperatorNameConventions.XOR,
    )

    /**
     * 布尔逻辑二元运算符名称集合。
     */
    private val logicalNames = listOf(
        OperatorNameConventions.ANDAND,
        OperatorNameConventions.OROR,
    )

    /**
     * 位移运算符名称集合。
     */
    private val shiftNames = listOf(
        OperatorNameConventions.LEFT_SHIFT,
        OperatorNameConventions.RIGHT_SHIFT,
    )

    /**
     * 相等性比较运算符名称集合。
     */
    private val equalityNames = listOf(
        OperatorNameConventions.EQUALS,
        OperatorNameConventions.NOT_EQUALS,
    )

    /**
     * 大小比较运算符名称集合。
     */
    private val orderingNames = listOf(
        OperatorNameConventions.COMPARE_LT,
        OperatorNameConventions.COMPARE_LTEQ,
        OperatorNameConventions.COMPARE_GT,
        OperatorNameConventions.COMPARE_GTEQ,
    )

    /**
     * 数值一元运算符名称集合。
     */
    private val unaryNumericNames = listOf(
        OperatorNameConventions.UNARY_MINUS,
        OperatorNameConventions.UNARY_PLUS,
    )

    /**
     * 整数一元运算符名称集合。
     */
    private val unaryIntegerNames = listOf(
        OperatorNameConventions.INC,
        OperatorNameConventions.DEC,
    )

    /**
     * 可参与整数内建运算的 primitive 种类。
     */
    private val integerKinds = PrimitiveTypeKind.entries.filter { it.isInteger }

    /**
     * 可参与数值内建运算的 primitive 种类。
     */
    private val numericKinds = PrimitiveTypeKind.entries.filter { it.isNumeric }

    /**
     * 支持大小比较的 primitive 种类。
     */
    private val comparableKinds = numericKinds + PrimitiveTypeKind.RUNE

    /**
     * 支持相等性比较的 primitive 种类。
     */
    private val equatableKinds = comparableKinds + PrimitiveTypeKind.BOOLEAN + PrimitiveTypeKind.UNIT

    /**
     * 按接收者 primitive 种类预构建的内建运算签名表。
     */
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

    /**
     * 返回 [receiverKind] 可暴露的 primitive 内建运算签名。
     */
    fun signaturesFor(receiverKind: PrimitiveTypeKind): List<BuiltinPrimitiveOperatorSignature> =
        signaturesByReceiver[receiverKind].orEmpty()

    /**
     * 按调用名称、接收者类型和参数类型解析 primitive 内建运算。
     */
    fun resolve(
        name: Name,
        receiverType: ConeCangJieType?,
        argumentTypes: List<ConeCangJieType>,
    ): BuiltinPrimitiveOperatorMatch? {
        val receiverKind = primitiveOperandKind(receiverType) ?: return null
        val argumentKinds = argumentTypes.map { it.toBuiltinOperatorKind() ?: return null }
        val signature = resolveSignature(name, receiverKind, argumentKinds) ?: return null
        return BuiltinPrimitiveOperatorMatch(signature)
    }

    /**
     * 将源码、stub 或 primitive classifier 视图统一规约为内建 operator 操作数类型。
     */
    fun normalizePrimitiveOperand(type: ConeCangJieType?): ConePrimitiveType? {
        val kind = primitiveOperandKind(type) ?: return null
        return ConePrimitiveType(kind)
    }

    /**
     * 提取类型在内建 operator 规则中的 primitive 种类。
     */
    fun primitiveOperandKind(type: ConeCangJieType?): PrimitiveTypeKind? =
        type?.toBuiltinOperatorKind()

    /**
     * 判断 [type] 是否可作为 primitive 内建运算操作数。
     */
    fun isBuiltinPrimitiveOperand(type: ConeCangJieType?): Boolean =
        primitiveOperandKind(type) != null

    /**
     * 根据幂运算接收者类型返回允许的参数 primitive 种类。
     */
    fun exponentiationArgumentExpectedKinds(receiverType: ConeCangJieType?): List<PrimitiveTypeKind> {
        val receiverKind = primitiveOperandKind(receiverType)?.defaultedIdealKind() ?: return emptyList()
        return when (receiverKind) {
            PrimitiveTypeKind.INT64 -> listOf(PrimitiveTypeKind.UINT64)
            PrimitiveTypeKind.FLOAT64 -> listOf(PrimitiveTypeKind.INT64, PrimitiveTypeKind.FLOAT64)
            else -> emptyList()
        }
    }

    /**
     * 解析 primitive 内建运算签名。
     */
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

    /**
     * 解析左右操作数必须落在同一 primitive 种类的运算。
     */
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

    /**
     * 解析整数位移运算签名。
     */
    private fun resolveShiftSignature(
        name: Name,
        receiverKind: PrimitiveTypeKind,
        argumentKind: PrimitiveTypeKind,
    ): BuiltinPrimitiveOperatorSignature? {
        if (!receiverKind.isBuiltinIntegerOperand() || !argumentKind.isBuiltinIntegerOperand()) return null
        // 官方 BINARY_EXPR_TYPE_MAP 对 IdealInt shift 的返回仍是 IdealInt；
        // 具体目标类型应交给外层 expected-type 近似阶段决定。
        return signature(name, receiverKind, argumentKind, receiverKind)
    }

    /**
     * 解析幂运算签名。
     */
    private fun resolveExponentiationSignature(
        name: Name,
        receiverKind: PrimitiveTypeKind,
        argumentKind: PrimitiveTypeKind,
    ): BuiltinPrimitiveOperatorSignature? {
        val defaultedReceiverKind = receiverKind.defaultedIdealKind()
        val isValid = when (defaultedReceiverKind) {
            PrimitiveTypeKind.INT64 -> argumentKind == PrimitiveTypeKind.UINT64

            PrimitiveTypeKind.FLOAT64 -> argumentKind == PrimitiveTypeKind.INT64 ||
                    argumentKind == PrimitiveTypeKind.FLOAT64

            else -> false
        }
        if (!isValid) return null
        return signature(name, receiverKind, argumentKind, defaultedReceiverKind)
    }

    /**
     * 计算两个内建操作数在指定允许集合中的共同 primitive 结果种类。
     */
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

    /**
     * 判断 primitive 种类是否可作为整数内建运算操作数。
     */
    private fun PrimitiveTypeKind.isBuiltinIntegerOperand(): Boolean =
        this == PrimitiveTypeKind.NOTHING || isInteger

    /**
     * 将 ideal primitive 种类默认落地到具体 primitive。
     */
    private fun PrimitiveTypeKind.defaultedIdealKind(): PrimitiveTypeKind = when (this) {
        PrimitiveTypeKind.IDEAL_INT -> PrimitiveTypeKind.INT64
        PrimitiveTypeKind.IDEAL_FLOAT -> PrimitiveTypeKind.FLOAT64
        else -> this
    }

    /**
     * 将 Cone 类型转换为 primitive 内建运算使用的类型种类。
     */
    private fun ConeCangJieType.toBuiltinOperatorKind(): PrimitiveTypeKind? = when (this) {
        is ConePrimitiveType -> kind
        is ConeIdealIntLiteralType -> PrimitiveTypeKind.IDEAL_INT
        is ConeIdealFloatLiteralType -> PrimitiveTypeKind.IDEAL_FLOAT
        is ConeClassLikeType -> classId.toPrimitiveTypeKindOrNull()
        is ConeStructType -> classId.toPrimitiveTypeKindOrNull()
        is ConeEnumType -> classId.toPrimitiveTypeKindOrNull()
        is ConeTypeAliasType -> expandedClassIdOrPrimitiveClassId?.toPrimitiveTypeKindOrNull()
        else -> classIdOrPrimitiveClassId?.toPrimitiveTypeKindOrNull()
    }

    /**
     * 为同类型二元运算批量添加签名。
     */
    private fun MutableList<BuiltinPrimitiveOperatorSignature>.addSameOperandOperators(
        kind: PrimitiveTypeKind,
        names: List<Name>,
        returnKind: PrimitiveTypeKind = kind,
    ) {
        for (name in names) {
            add(signature(name, kind, kind, returnKind))
        }
    }

    /**
     * 为同类型一元运算批量添加签名。
     */
    private fun MutableList<BuiltinPrimitiveOperatorSignature>.addSameOperandUnaryOperators(
        kind: PrimitiveTypeKind,
        names: List<Name>,
        returnKind: PrimitiveTypeKind = kind,
    ) {
        for (name in names) {
            add(signature(name, kind, emptyList(), returnKind))
        }
    }

    /**
     * 构造单参数 primitive 内建运算签名。
     */
    private fun signature(
        name: Name,
        receiverKind: PrimitiveTypeKind,
        parameterKind: PrimitiveTypeKind,
        returnKind: PrimitiveTypeKind,
    ): BuiltinPrimitiveOperatorSignature {
        return BuiltinPrimitiveOperatorSignature(name, receiverKind, listOf(parameterKind), returnKind)
    }

    /**
     * 构造任意参数列表的 primitive 内建运算签名。
     */
    private fun signature(
        name: Name,
        receiverKind: PrimitiveTypeKind,
        parameterKinds: List<PrimitiveTypeKind>,
        returnKind: PrimitiveTypeKind,
    ): BuiltinPrimitiveOperatorSignature {
        return BuiltinPrimitiveOperatorSignature(name, receiverKind, parameterKinds, returnKind)
    }
}
