package org.cangnova.cangjie.codegen.function

import org.cangnova.cangjie.chir.core.attribute.ChirAttribute
import org.cangnova.cangjie.chir.core.attribute.ChirBooleanAttribute
import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirThrowTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirUnwindTerminator
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirCustomTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirNominalTypeDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirFieldExpression
import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryOperation
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirGenericType
import org.cangnova.cangjie.chir.core.type.ChirRawArrayType
import org.cangnova.cangjie.chir.core.type.ChirRefType
import org.cangnova.cangjie.chir.core.type.ChirTypeDeclarationId
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirType
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.type.ChirThisType
import org.cangnova.cangjie.chir.core.value.ChirBlockGroupValue
import org.cangnova.cangjie.chir.core.value.ChirBlockValue
import org.cangnova.cangjie.chir.core.value.ChirConstantKind
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirGlobalValue
import org.cangnova.cangjie.chir.core.value.ChirImportedFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirImportedVariableValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.chir.core.value.ChirParameterValue
import org.cangnova.cangjie.chir.core.value.ChirValue
import org.cangnova.cangjie.codegen.context.CGContext
import org.cangnova.cangjie.codegen.diagnostics.CodegenLoweringException
import org.cangnova.cangjie.codegen.dispatcher.ExpressionLoweringDispatcher
import org.cangnova.cangjie.codegen.ir.IRBuilder
import org.cangnova.cangjie.codegen.ir.LlvmFunctionArtifact
import org.cangnova.cangjie.codegen.ir.llvmSymbolIdentifier
import org.cangnova.cangjie.codegen.ir.sanitizeIdentifier
import org.cangnova.cangjie.codegen.ir.uniquifyIdentifier
import org.cangnova.cangjie.codegen.types.LLVM_UNIT_TYPE_NAME
import org.cangnova.cangjie.codegen.types.LlvmRawArrayRuntime
import org.cangnova.cangjie.codegen.types.LlvmRuntimeTypeInfo
import org.cangnova.cangjie.codegen.types.LlvmAggregateMember
import org.cangnova.cangjie.codegen.types.resolveLlvmAggregateMember
import org.cangnova.cangjie.codegen.types.LlvmStringLiteralRuntime

/**
 * 单个 CHIR 函数到 LLVM function IR 的 lowering 单元。
 */
class CGFunction(
    /**
     * 待 lowering 的 CHIR 函数声明。
     */
    private val declaration: ChirFunctionDeclaration,
    /**
     * 当前 codegen 共享上下文。
     */
    private val context: CGContext,
    /**
     * 表达式 lowering 分派器。
     */
    private val dispatcher: ExpressionLoweringDispatcher,
    /** 声明所属的类型；成员函数 ABI 在普通参数后携带官方规定的隐藏类型信息。 */
    private val ownerType: ChirCustomTypeDeclaration? = null,
    /** 当前 package 中成员函数的稳定身份集合，用于校验调用点携带 owner TypeInfo。 */
    private val memberFunctionIds: Set<ChirSemanticId> = emptySet(),
    /** 当前 package 中静态成员函数的稳定身份集合，用于构造完整的静态调用 ABI。 */
    private val staticMemberFunctionIds: Set<ChirSemanticId> = emptySet(),
    /** 当前 package 中成员函数到声明 owner 的映射，用于恢复静态调用的 ThisTypeInfo。 */
    private val memberFunctionOwnersById: Map<ChirSemanticId, ChirCustomTypeDeclaration> = emptyMap(),
    /** 稳定名义身份到 CHIR 声明的索引，用于解析官方按成员名编码的字段访问。 */
    private val nominalTypeDeclarationsById: Map<ChirTypeDeclarationId, ChirNominalTypeDeclaration>,
    /** module 级静态 String 字面量按值常量。 */
    private val stringLiteralValues: Map<String, String> = emptyMap(),
) {
    /**
     * CHIR 参数 semanticId 到 LLVM 参数名的映射。
     */
    private val parameterNameById: Map<ChirSemanticId, String> = declaration.parameters.associate { parameter ->
        parameter.semanticId to sanitizeIdentifier(parameter.name, "param")
    }
    /** 函数 ABI 中按声明顺序传入的运行时泛型 TypeInfo 参数。 */
    private val genericTypeInfoParameters: List<Pair<ChirGenericType, String>> = declaration.genericTypeParameters.mapIndexed { index, typeRef ->
        val type = (typeRef as? ChirResolvedTypeRef)?.type as? ChirGenericType
            ?: throw CodegenLoweringException(
                "function ${declaration.name} generic parameter #$index is not a resolved generic type",
                declaration.semanticId,
            )
        type to sanitizeIdentifier("ti.${type.identifier}.$index", "typeinfo")
    }
    private val genericTypeInfoParameterNameByIdentity: Map<String, String> = genericTypeInfoParameters
        .associate { (type, name) -> type.semanticIdentity to name }
    /** owner 类型形参在 OuterTypeInfo.typeArgs 数组中的 ABI 顺序。 */
    private val ownerGenericTypeInfoParameterIndexByIdentity: Map<String, Int> = buildMap {
        ownerType?.typeParameterTypes?.forEachIndexed { index, typeRef ->
            val type = (typeRef as? ChirResolvedTypeRef)?.type as? ChirGenericType
                ?: throw CodegenLoweringException(
                    "owner ${ownerType.name} type parameter #$index is not a resolved generic CHIR type",
                    ownerType.semanticId,
                )
            check(put(type.semanticIdentity, index) == null) {
                "owner ${ownerType.name} declares duplicate generic parameter identity ${type.semanticIdentity}"
            }
        }
    }
    private val memberTypeInfoParameterName: String? = ownerType?.let { owner ->
        sanitizeIdentifier("ti.${owner.name}.outer", "typeinfo")
    }
    private val thisTypeInfoParameterName: String? = ownerType
        ?.takeIf { declaration.isStatic }
        ?.let { owner -> sanitizeIdentifier("ti.${owner.name}.this", "typeinfo") }
    /**
     * CHIR block semanticId 到 LLVM label 的映射。
     */
    private val blockLabelById: Map<ChirSemanticId, String> by lazy(::buildBlockLabels)
    /** Raw LLVM blocks introduced by multi-block expression lowering. */
    private val syntheticLabelsByKey = linkedMapOf<Pair<ChirSemanticId, String>, String>()

    /**
     * 需要实体 SSA 定义的 Unit 结果局部值身份。
     */
    private val unitValueResultIds: Set<ChirSemanticId> = declaration.blocks
        .asSequence()
        .flatMap { it.expressions.asSequence() }
        .filter { expression ->
            (expression.resultType as? ChirResolvedTypeRef)?.type == ChirPrimitiveType.UNIT &&
                (expression is ChirFieldExpression ||
                    expression is ChirMemoryExpression &&
                    ChirMemoryOperation.parse(expression.operation) in setOf(
                        ChirMemoryOperation.LOAD,
                        ChirMemoryOperation.GET_ELEMENT_BY_NAME,
                    ))
        }
        .map(::resultLocalValueId)
        .toSet()

    /**
     * 无需 LLVM 指令即可表示的零大小 Unit 表达式结果。
     */
    private val unitConstantResultIds: Set<ChirSemanticId> = declaration.blocks
        .asSequence()
        .flatMap { it.expressions.asSequence() }
        .filter { expression ->
            (expression.resultType as? ChirResolvedTypeRef)?.type == ChirPrimitiveType.UNIT &&
                resultLocalValueId(expression) !in unitValueResultIds
        }
        .map(::resultLocalValueId)
        .toSet()

    private val expressionResultIds: Set<ChirSemanticId> = declaration.blocks
        .asSequence()
        .flatMap { it.expressions.asSequence() }
        .filter { expression ->
            val resultType = expression.resultType ?: return@filter false
            val primitive = (resultType as? ChirResolvedTypeRef)?.type as? ChirPrimitiveType
            primitive !in setOf(ChirPrimitiveType.VOID, ChirPrimitiveType.NOTHING) &&
                (primitive != ChirPrimitiveType.UNIT || resultLocalValueId(expression) in unitValueResultIds)
        }
        .map(::resultLocalValueId)
        .toSet()

    /**
     * 降低当前函数并返回 LLVM 函数产物。
     */
    fun lower(): LlvmFunctionArtifact {
        verifyControlFlow()
        val builder = IRBuilder()
        builder.emit(buildFunctionHeader())
        lowerableBlocks().forEach { block ->
            builder.label(blockLabel(block.semanticId))
            block.expressions.forEach { expression ->
                dispatcher.lower(this, expression).forEach(builder::emit)
            }
            lowerTerminator(checkNotNull(block.terminator)).forEach(builder::emit)
        }
        builder.emit("}")
        return LlvmFunctionArtifact(declaration.name, builder.build().joinToString(System.lineSeparator()))
    }

    /**
     * 通过上下文类型 lowering 服务降低 CHIR 类型。
     */
    internal fun lowerType(type: ChirTypeRef): String = context.typeLowering.lower(type)

    /**
     * 按 CHIR 声明布局解析名义聚合类型中的具名成员。
     */
    internal fun resolveAggregateMember(
        aggregateType: ChirTypeRef,
        memberName: String,
        semanticId: ChirSemanticId,
    ): LlvmAggregateMember = resolveLlvmAggregateMember(
        aggregateType = aggregateType,
        memberName = memberName,
        declarationsById = nominalTypeDeclarationsById,
        semanticId = semanticId,
    )

    /**
     * 降低当前 CHIR 函数的 ABI 返回类型。
     */
    internal fun lowerFunctionReturnType(type: ChirTypeRef): String = context.typeLowering.lowerFunctionReturn(type)

    /**
     * 生成表达式结果的 LLVM SSA 引用名。
     */
    internal fun resultRef(semanticId: ChirSemanticId): String = "%${sanitizeIdentifier(semanticId.value, "tmp")}"

    /**
     * 生成表达式结果局部值的 LLVM SSA 引用名。
     */
    internal fun resultRef(expression: ChirExpression): String = resultRef(resultLocalValueId(expression))

    private fun resultLocalValueId(expression: ChirExpression): ChirSemanticId =
        expression.resultLocalValue?.semanticId ?: throw CodegenLoweringException(
            "expression ${expression.semanticId.value} has no result local value",
            expression.semanticId,
        )

    /**
     * 为多指令表达式生成稳定且不与表达式结果共用的中间 SSA 名称。
     */
    internal fun intermediateRef(semanticId: ChirSemanticId, ordinal: Int): String {
        require(ordinal >= 0) { "intermediate SSA ordinal must not be negative" }
        return "%${sanitizeIdentifier("${semanticId.value}.intermediate.$ordinal", "tmp")}"
    }

    /**
     * 为一条 CHIR 表达式 lowering 生成与原始 CFG 标签不冲突的内部 LLVM basic block 名称。
     */
    internal fun syntheticBlockLabel(semanticId: ChirSemanticId, role: String): String {
        val key = semanticId to role
        return syntheticLabelsByKey.getOrPut(key) {
            val usedLabels = linkedMapOf<String, Int>()
            (blockLabelById.values + syntheticLabelsByKey.values).forEach { usedLabels[it] = 1 }
            uniquifyIdentifier(sanitizeIdentifier("${semanticId.value}.$role", "bb"), usedLabels)
        }
    }

    /**
     * 将 CHIR value 渲染为 LLVM value 引用或常量。
     */
    internal fun renderValue(value: ChirValue): String {
        return when (value) {
            is ChirConstantValue -> when {
                value.constantKind == ChirConstantKind.NULL -> normalizeConstant("null", lowerType(value.type))
                LlvmStringLiteralRuntime.isStringType(value.type) -> stringLiteralValues[value.literal]
                    ?: throw CodegenLoweringException(
                        "String literal was not included in the module literal plan",
                        value.semanticId,
                    )
                else -> normalizeConstant(value.literal, lowerType(value.type))
            }
            is ChirLocalValue -> {
                when {
                    value.semanticId in unitConstantResultIds &&
                    (value.type as? ChirResolvedTypeRef)?.type == ChirPrimitiveType.UNIT
                    -> "zeroinitializer"
                    value.semanticId !in expressionResultIds -> throw CodegenLoweringException(
                        "local value ${value.semanticId.value} has no defining CHIR expression",
                        value.semanticId,
                    )
                    else -> resultRef(value.semanticId)
                }
            }
            is ChirParameterValue -> {
                val paramName = parameterNameById[value.semanticId] ?: throw CodegenLoweringException(
                    "parameter value ${value.semanticId.value} is not declared in function ${declaration.name}",
                    value.semanticId,
                )
                "%$paramName"
            }
            is ChirGlobalValue -> "@${llvmSymbolIdentifier(value.name, value.semanticId)}"
            is ChirFunctionValue -> "@${llvmSymbolIdentifier(value.name, value.semanticId)}"
            is ChirImportedFunctionValue -> "@${llvmSymbolIdentifier(value.name, value.semanticId)}"
            is ChirImportedVariableValue -> "@${llvmSymbolIdentifier(value.name, value.semanticId)}"
            is ChirBlockValue -> "%${blockLabel(value.semanticId)}"
            is ChirBlockGroupValue -> "%${sanitizeIdentifier(value.name, "block_group")}"
        }
    }

    /**
     * 将 CHIR value 渲染为 `type value` 形式。
     */
    internal fun renderTypedValue(value: ChirValue): String = "${lowerType(value.type)} ${renderValue(value)}"

    /**
     * 为 generic call 与 RawArrayAllocate 生成官方运行时 TypeInfo 值。
     */
    internal fun lowerTypeInfoValue(
        typeRef: ChirTypeRef,
        semanticId: ChirSemanticId,
        firstTemporaryOrdinal: Int = 16,
    ): LoweredTypeInfoValue {
        val instructions = mutableListOf<String>()
        var ordinal = firstTemporaryOrdinal
        fun temporary(): String = intermediateRef(semanticId, ordinal++)

        fun lower(typeReference: ChirTypeRef): String {
            val type = (typeReference as? ChirResolvedTypeRef)?.type ?: throw CodegenLoweringException(
                "cannot lower TypeInfo for unresolved type '${typeReference.renderName}'",
                semanticId,
            )
            if (type is ChirRawArrayType && type.dimensions != 1u) {
                throw CodegenLoweringException(
                    "RawArray TypeInfo lowering does not support dimension count ${type.dimensions}",
                    semanticId,
                )
            }
            return when (type) {
                is ChirGenericType -> {
                    genericTypeInfoParameterNameByIdentity[type.semanticIdentity]?.let { parameterName ->
                        return@lower "%$parameterName"
                    }
                    val ownerParameterIndex = ownerGenericTypeInfoParameterIndexByIdentity[type.semanticIdentity]
                        ?: throw CodegenLoweringException(
                            "generic TypeInfo '${type.identifier}' is not available in function ${declaration.name}",
                            semanticId,
                        )
                    val outerTypeInfoParameter = memberTypeInfoParameterName ?: throw CodegenLoweringException(
                        "owner TypeInfo for generic parameter '${type.identifier}' is not available in function ${declaration.name}",
                        semanticId,
                    )
                    val typeArgumentsAddress = temporary()
                    val typeArguments = temporary()
                    val typeArgumentAddress = temporary()
                    val typeInfo = temporary()
                    instructions += "  $typeArgumentsAddress = getelementptr ${LlvmRawArrayRuntime.TYPE_INFO_TYPE_NAME}, " +
                        "ptr %$outerTypeInfoParameter, i32 0, i32 ${LlvmRawArrayRuntime.TYPE_INFO_TYPE_ARGUMENTS_FIELD_INDEX}"
                    instructions += "  $typeArguments = load ptr, ptr $typeArgumentsAddress"
                    instructions += "  $typeArgumentAddress = getelementptr ptr, ptr $typeArguments, i32 $ownerParameterIndex"
                    instructions += "  $typeInfo = load ptr, ptr $typeArgumentAddress"
                    typeInfo
                }
                is ChirThisType -> {
                    val parameterName = memberTypeInfoParameterName ?: throw CodegenLoweringException(
                        "owner TypeInfo for '${type.ownerTypeName}' is not available in function ${declaration.name}",
                        semanticId,
                    )
                    "%$parameterName"
                }
                is ChirRefType -> lower(type.referencedType)
                else -> {
                    val template = LlvmRuntimeTypeInfo.dynamicTemplateReference(typeReference)
                    if (template != null) {
                        if (template.typeArgumentCount > Int.MAX_VALUE) {
                            throw CodegenLoweringException(
                                "runtime TypeInfo for '${type.renderName}' exceeds the i32 type-argument ABI limit",
                                semanticId,
                            )
                        }
                        val typeInfoArguments = buildList(template.typeArgumentCount.toInt()) {
                            template.forEachTypeArgument { argument -> add(lower(argument)) }
                        }
                        val typeArgumentStorage = temporary()
                        val dynamicTypeInfo = temporary()
                        instructions += "  $typeArgumentStorage = alloca [${typeInfoArguments.size} x ptr]"
                        typeInfoArguments.forEachIndexed { index, argumentTypeInfo ->
                            val typeArgumentAddress = temporary()
                            instructions += "  $typeArgumentAddress = getelementptr inbounds [${typeInfoArguments.size} x ptr], " +
                                "ptr $typeArgumentStorage, i64 0, i64 $index"
                            instructions += "  store ptr $argumentTypeInfo, ptr $typeArgumentAddress"
                        }
                        instructions += "  $dynamicTypeInfo = call ptr @${LlvmRawArrayRuntime.GET_OR_CREATE_TYPE_INFO}(" +
                            "ptr ${LlvmRawArrayRuntime.global(template.symbolName)}, " +
                            "i32 ${typeInfoArguments.size}, ptr $typeArgumentStorage)"
                        dynamicTypeInfo
                    } else {
                        if (LlvmRawArrayRuntime.containsGeneric(typeReference)) {
                            throw CodegenLoweringException(
                                "runtime TypeInfo lowering for generic composite '${type.renderName}' is not implemented",
                                semanticId,
                            )
                        }
                        LlvmRawArrayRuntime.global(LlvmRawArrayRuntime.typeInfoSymbol(typeReference, semanticId))
                    }
                }
            }
        }

        val value = lower(typeRef)
        return LoweredTypeInfoValue(instructions, value, ordinal)
    }

    /**
     * 降低静态成员调用 ABI 中的 ThisTypeInfo。
     *
     * 同一类型的静态成员相互调用时，官方 ABI 复用当前函数收到的 ThisTypeInfo；跨类型或包级调用则
     * 根据调用点的 this 类型构造对应信息。
     */
    internal fun lowerStaticCallThisTypeInfoValue(
        targetFunctionId: ChirSemanticId,
        semanticId: ChirSemanticId,
        firstTemporaryOrdinal: Int,
        alreadyLoweredTypeInfo: LoweredTypeInfoValue,
    ): LoweredTypeInfoValue {
        val targetOwner = memberFunctionOwnersById[targetFunctionId]
        if (declaration.isStatic && ownerType != null && targetOwner?.semanticId == ownerType.semanticId) {
            val parameterName = thisTypeInfoParameterName ?: throw CodegenLoweringException(
                "ThisTypeInfo for static member function ${declaration.name} is not available",
                semanticId,
            )
            return LoweredTypeInfoValue(emptyList(), "%$parameterName", alreadyLoweredTypeInfo.nextTemporaryOrdinal)
        }
        if (!declaration.isStatic && ownerType is ChirClassDeclaration && targetOwner?.semanticId == ownerType.semanticId) {
            val receiver = declaration.parameters.firstOrNull() ?: throw CodegenLoweringException(
                "instance member function ${declaration.name} has no receiver parameter",
                semanticId,
            )
            val receiverName = parameterNameById[receiver.semanticId] ?: throw CodegenLoweringException(
                "receiver parameter for ${declaration.name} is not part of its function signature",
                semanticId,
            )
            val receiverType = lowerType(receiver.type)
            if (receiverType != "ptr") {
                throw CodegenLoweringException(
                    "reference-type receiver for ${declaration.name} must lower to ptr, got $receiverType",
                    semanticId,
                )
            }
            val address = intermediateRef(semanticId, firstTemporaryOrdinal)
            val typeInfo = intermediateRef(semanticId, firstTemporaryOrdinal + 1)
            return LoweredTypeInfoValue(
                instructions = listOf(
                    "  $address = addrspacecast ptr %$receiverName to ptr addrspace(1)",
                    "  $typeInfo = load ptr, ptr addrspace(1) $address",
                ),
                value = typeInfo,
                nextTemporaryOrdinal = firstTemporaryOrdinal + 2,
            )
        }
        return LoweredTypeInfoValue(
            instructions = emptyList(),
            value = alreadyLoweredTypeInfo.value,
            nextTemporaryOrdinal = alreadyLoweredTypeInfo.nextTemporaryOrdinal,
        )
    }

    /**
     * 渲染调用实参，保留允许出现在参数位置的属性 token。
     */
    internal fun renderCallArgument(value: ChirValue): String {
        val tokens = collectAttributeTokens(value.attributes, callArgumentReservedAttributeKeys)
        return if (tokens.isEmpty()) {
            renderTypedValue(value)
        } else {
            "${lowerType(value.type)} ${tokens.joinToString(" ")} ${renderValue(value)}"
        }
    }

    /** 判断目标函数是否属于 CHIR 中嵌套的类型成员声明。 */
    internal fun isMemberFunction(semanticId: ChirSemanticId): Boolean = semanticId in memberFunctionIds

    /** 查询函数是否声明为静态类型成员。 */
    internal fun isStaticMemberFunction(semanticId: ChirSemanticId): Boolean = semanticId in staticMemberFunctionIds

    /**
     * 从调用目标属性中解析 tail/musttail/notail 调用种类。
     */
    internal fun callTailKind(attributes: Set<ChirAttribute>): String? {
        val explicit = attributeValue(attributes, "tail")
        if (!explicit.isNullOrBlank()) return explicit
        val enabledBoolean = attributes.asSequence()
            .filterIsInstance<ChirBooleanAttribute>()
            .firstOrNull { it.enabled && it.key in callTailKinds }
        return enabledBoolean?.key
    }

    /**
     * 解析基本块 id 对应的 LLVM label。
     */
    internal fun blockLabel(blockId: ChirSemanticId): String {
        return blockLabelById[blockId] ?: throw CodegenLoweringException(
            "function ${declaration.name} references missing block ${blockId.value}",
            blockId,
        )
    }

    /**
     * 按 semanticId 字符串或 block 名称解析 LLVM label。
     */
    internal fun resolveBlockLabel(reference: String): String? {
        val byId = blockLabelById[ChirSemanticId(reference)]
        if (byId != null) return byId
        val normalizedRef = sanitizeIdentifier(reference, "bb")
        val byName = declaration.blocks
            .firstOrNull { sanitizeIdentifier(it.name.ifBlank { it.semanticId.value }, "bb") == normalizedRef }
            ?.semanticId
        return byName?.let(blockLabelById::get)
    }

    /**
     * 降低 CHIR terminator 到 LLVM terminator 指令。
     */
    private fun lowerTerminator(terminator: ChirTerminator): List<String> {
        return when (terminator) {
            is ChirReturnTerminator -> lowerReturnTerminator(terminator)
            is ChirBranchTerminator -> listOf("  br label %${blockLabel(terminator.targetBlockId)}")
            is ChirConditionalBranchTerminator -> {
                requireSameType("i1", lowerType(terminator.condition.type), terminator.semanticId, "conditional branch condition")
                listOf(
                    "  br i1 ${renderValue(terminator.condition)}, label %${blockLabel(terminator.trueTargetBlockId)}, label %${blockLabel(terminator.falseTargetBlockId)}",
                )
            }
            is ChirThrowTerminator -> {
                requireSameType("ptr", lowerType(terminator.exceptionValue.type), terminator.semanticId, "throw exception")
                val lines = mutableListOf<String>()
                lines += "  call void @cangjie.throw(ptr ${renderValue(terminator.exceptionValue)})"
                val unwindTarget = terminator.unwindTargetBlockId
                if (unwindTarget != null) {
                    lines += "  br label %${blockLabel(unwindTarget)}"
                } else {
                    lines += "  unreachable"
                }
                lines
            }
            is ChirUnwindTerminator -> listOf("  br label %${blockLabel(terminator.targetBlockId)}")
            else -> throw CodegenLoweringException(
                "unsupported terminator type: ${terminator::class.simpleName}",
                declaration.semanticId,
            )
        }
    }

    /**
     * 降低 return terminator，并校验返回值类型与函数返回类型一致。
     */
    private fun lowerReturnTerminator(terminator: ChirReturnTerminator): List<String> {
        val functionReturnType = lowerFunctionReturnType(declaration.returnType)
        val returnValue = terminator.returnValue
        if (functionReturnType == "void") {
            if (returnValue != null) {
                requireSameType(
                    lowerType(declaration.returnType),
                    lowerType(returnValue.type),
                    terminator.semanticId,
                    "return value",
                )
            }
            return listOf("  ret void")
        }
        if (returnValue == null) {
            throw CodegenLoweringException(
                "function ${declaration.name} returns $functionReturnType but return terminator has no value",
                terminator.semanticId,
            )
        }
        val valueType = lowerType(returnValue.type)
        requireSameType(functionReturnType, valueType, terminator.semanticId, "return value")
        return listOf("  ret $valueType ${renderValue(returnValue)}")
    }

    /**
     * 构建 LLVM function header。
     */
    private fun buildFunctionHeader(): String {
        val linkage = attributeValue(declaration.attributes, "linkage")
        val callingConvention = attributeValue(declaration.attributes, "calling_conv")
            ?: attributeValue(declaration.attributes, "cc")
        val personality = attributeValue(declaration.attributes, "personality")

        val params = (declaration.parameters.map { parameter ->
            val parameterType = lowerType(parameter.type)
            val parameterName = parameterNameById[parameter.semanticId] ?: sanitizeIdentifier(parameter.name, "param")
            val attrs = collectAttributeTokens(parameter.attributes)
            if (attrs.isEmpty()) {
                "$parameterType %$parameterName"
            } else {
                "$parameterType ${attrs.joinToString(" ")} %$parameterName"
            }
        } + genericTypeInfoParameters.map { (_, name) -> "ptr %$name" } +
            listOfNotNull(memberTypeInfoParameterName?.let { name -> "ptr %$name" }) +
            listOfNotNull(thisTypeInfoParameterName?.let { name -> "ptr %$name" })).joinToString(", ")

        return buildString {
            append("define ")
            if (!linkage.isNullOrBlank()) {
                append(linkage)
                append(' ')
            }
            if (!callingConvention.isNullOrBlank()) {
                append(callingConvention)
                append(' ')
            }
            append(lowerFunctionReturnType(declaration.returnType))
            append(" @")
            append(llvmSymbolIdentifier(declaration.name, declaration.semanticId))
            append('(')
            append(params)
            append(')')

            val trailingAttributes = collectAttributeTokens(declaration.attributes)
            if (trailingAttributes.isNotEmpty()) {
                append(' ')
                append(trailingAttributes.joinToString(" "))
            }
            if (!personality.isNullOrBlank()) {
                append(" personality ptr @")
                append(sanitizeIdentifier(personality, "personality"))
            }
            append(" {")
        }
    }

    /**
     * 收集可直接打印到 LLVM IR 的属性 token。
     */
    internal fun collectAttributeTokens(
        attributes: Set<ChirAttribute>,
        reservedKeys: Set<String> = reservedAttributeKeys,
    ): List<String> {
        val tokens = mutableListOf<String>()
        attributes.forEach { attribute ->
            when (attribute) {
                is ChirBooleanAttribute -> {
                    if (attribute.enabled && attribute.key !in reservedKeys) {
                        tokens += attribute.key
                    }
                }
                is ChirStringAttribute -> {
                    if (attribute.key == "align") {
                        tokens += "align ${attribute.value}"
                    }
                }
            }
        }
        return tokens
    }

    /**
     * 从属性集合中读取指定字符串属性值。
     */
    internal fun attributeValue(attributes: Set<ChirAttribute>, key: String): String? {
        return attributes.asSequence()
            .filterIsInstance<ChirStringAttribute>()
            .firstOrNull { it.key == key }
            ?.value
    }

    /**
     * 返回当前函数的基本块 lowering 顺序。
     */
    private fun orderedBlocks() = buildList {
        val entry = declaration.blocks.firstOrNull { it.semanticId == declaration.entryBlockId }
        if (entry != null) {
            add(entry)
        }
        declaration.blocks.forEach { block ->
            if (entry == null || block.semanticId != entry.semanticId) {
                add(block)
            }
        }
    }

    /**
     * 删除没有后继且不可能到达的未终止块；其余未终止块属于损坏 CFG，不能生成后端 IR。
     */
    private fun lowerableBlocks(): List<ChirBlock> {
        val successorIds = declaration.blocks.flatMap { block ->
            when (val terminator = block.terminator) {
                is ChirBranchTerminator -> listOf(terminator.targetBlockId)
                is ChirConditionalBranchTerminator -> listOf(terminator.trueTargetBlockId, terminator.falseTargetBlockId)
                is ChirUnwindTerminator -> listOf(terminator.targetBlockId)
                is ChirThrowTerminator -> listOfNotNull(terminator.unwindTargetBlockId)
                else -> emptyList()
            }
        }.toSet()
        val entryBlockId = declaration.entryBlockId
        declaration.blocks.firstOrNull { block ->
            block.terminator == null && (block.semanticId == entryBlockId || block.semanticId in successorIds)
        }?.let { block ->
            throw CodegenLoweringException("reachable block ${block.semanticId.value} has no terminator", block.semanticId)
        }
        return orderedBlocks().filter { it.terminator != null }
    }

    /**
     * 为函数中所有基本块构建唯一 LLVM label。
     */
    private fun buildBlockLabels(): Map<ChirSemanticId, String> {
        val usedLabels = linkedMapOf<String, Int>()
        val labels = linkedMapOf<ChirSemanticId, String>()
        declaration.blocks.forEach { block ->
            val base = sanitizeIdentifier(block.name.ifBlank { block.semanticId.value }, "bb")
            labels[block.semanticId] = uniquifyIdentifier(base, usedLabels)
        }
        return labels
    }

    /**
     * 在写出函数前校验基本控制流引用。
     */
    private fun verifyControlFlow() {
        if (!context.options.verifyBeforeWrite) return
        if (!declaration.hasBody) {
            throw CodegenLoweringException(
                "function ${declaration.name} is a declaration without a body",
                declaration.semanticId,
            )
        }
        if (declaration.blocks.isEmpty()) {
            throw CodegenLoweringException(
                "function ${declaration.name} must contain at least one block",
                declaration.semanticId,
            )
        }

        val blockIds = declaration.blocks.map { it.semanticId }.toSet()
        val entryBlockId = declaration.entryBlockId
            ?: throw CodegenLoweringException("function ${declaration.name} has no entry block", declaration.semanticId)
        if (entryBlockId !in blockIds) {
            throw CodegenLoweringException(
                "function ${declaration.name} entry block ${entryBlockId.value} is missing",
                entryBlockId,
            )
        }

        declaration.blocks.forEach { block ->
            when (val terminator = block.terminator) {
                null -> Unit
                is ChirBranchTerminator -> {
                    if (terminator.targetBlockId !in blockIds) {
                        throw CodegenLoweringException(
                            "function ${declaration.name} has branch to missing block ${terminator.targetBlockId.value}",
                            terminator.semanticId,
                        )
                    }
                }
                is ChirConditionalBranchTerminator -> {
                    if (terminator.trueTargetBlockId !in blockIds) {
                        throw CodegenLoweringException(
                            "function ${declaration.name} has cond-branch true target ${terminator.trueTargetBlockId.value} missing",
                            terminator.semanticId,
                        )
                    }
                    if (terminator.falseTargetBlockId !in blockIds) {
                        throw CodegenLoweringException(
                            "function ${declaration.name} has cond-branch false target ${terminator.falseTargetBlockId.value} missing",
                            terminator.semanticId,
                        )
                    }
                }
                is ChirUnwindTerminator -> {
                    if (terminator.targetBlockId !in blockIds) {
                        throw CodegenLoweringException(
                            "function ${declaration.name} has unwind target ${terminator.targetBlockId.value} missing",
                            terminator.semanticId,
                        )
                    }
                }
                is ChirThrowTerminator -> {
                    val unwindTarget = terminator.unwindTargetBlockId
                    if (unwindTarget != null && unwindTarget !in blockIds) {
                        throw CodegenLoweringException(
                            "function ${declaration.name} has throw unwind target ${unwindTarget.value} missing",
                            terminator.semanticId,
                        )
                    }
                }
                is ChirReturnTerminator -> Unit
            }
        }
    }

    /**
     * 将 CHIR 常量字面量规范化为 LLVM 常量文本。
     */
    private fun normalizeConstant(literal: String, llvmType: String): String {
        return when {
            literal.equals("null", ignoreCase = true) && llvmType == "ptr" -> "null"
            literal.equals("null", ignoreCase = true) && llvmType.startsWith("ptr addrspace(") -> "null"
            literal.equals("null", ignoreCase = true) && Regex("i\\d+").matches(llvmType) -> "0"
            literal.equals("null", ignoreCase = true) && llvmType in setOf("half", "float", "double") -> "0.0"
            literal.equals("null", ignoreCase = true) -> "zeroinitializer"
            llvmType == "i1" && literal.equals("true", ignoreCase = true) -> "1"
            llvmType == "i1" && literal.equals("false", ignoreCase = true) -> "0"
            llvmType == LLVM_UNIT_TYPE_NAME && literal == "unit" -> "zeroinitializer"
            else -> literal
        }
    }

    /**
     * 要求两个 LLVM textual type 完全一致。
     */
    private fun requireSameType(expected: String, actual: String, sourceId: ChirSemanticId, subject: String) {
        if (expected != actual) {
            throw CodegenLoweringException(
                "$subject type mismatch: expected $expected, got $actual",
                sourceId,
            )
        }
    }

    private companion object {
        val reservedAttributeKeys = setOf("linkage", "personality", "calling_conv", "cc")
        val callArgumentReservedAttributeKeys = reservedAttributeKeys + setOf("tail", "musttail", "notail")
        val callTailKinds = setOf("tail", "musttail", "notail")
    }
}

/** TypeInfo lowering 的运行时指令与结果值。 */
internal data class LoweredTypeInfoValue(
    val instructions: List<String>,
    val value: String,
    val nextTemporaryOrdinal: Int,
)
