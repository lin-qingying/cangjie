package org.cangnova.cangjie.chir.core.serializer

import PackageFormat.*
import com.google.flatbuffers.FlatBufferBuilder
import org.cangnova.cangjie.chir.core.controlflow.*
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration
import org.cangnova.cangjie.chir.core.expression.*
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.model.ChirPackageAccessLevel
import org.cangnova.cangjie.chir.core.model.ChirPackageMembers
import org.cangnova.cangjie.chir.core.type.*
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirGlobalValue
import org.cangnova.cangjie.chir.core.value.ChirImportedFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirImportedVariableValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.chir.core.value.ChirParameterValue
import org.cangnova.cangjie.chir.core.value.ChirValue
import java.nio.ByteBuffer

object ChirPackageCodec {
    private const val MODULE_META_PREFIX = "mod::"
    private const val BLOCK_NAME_PREFIX = "block_name::"
    private val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)

    @OptIn(ExperimentalUnsignedTypes::class)
    fun serialize(chirPackage: ChirPackage): ByteArray {
        val builder = FlatBufferBuilder(4096)
        val ctx = EncodeContext(chirPackage)

        val typeTags = UByteArray(ctx.types.size)
        val typeOffsets = IntArray(ctx.types.size)
        ctx.types.forEachIndexed { index, t ->
            val built = encodeType(builder, t.id, t.ref)
            typeTags[index] = built.first
            typeOffsets[index] = built.second
        }

        val valueTags = UByteArray(ctx.values.size)
        val valueOffsets = IntArray(ctx.values.size)
        ctx.values.forEachIndexed { index, v ->
            val built = encodeValue(builder, v, ctx)
            valueTags[index] = built.first
            valueOffsets[index] = built.second
        }

        val exprTags = UByteArray(ctx.exprs.size)
        val exprOffsets = IntArray(ctx.exprs.size)
        ctx.exprs.forEachIndexed { index, e ->
            val built = encodeExpr(builder, e, ctx)
            exprTags[index] = built.first
            exprOffsets[index] = built.second
        }

        val exprMeta = ctx.exprs.joinToString(";") { expr ->
            buildString {
                append(expr.id)
                append('=')
                append(expr.semanticId)
                expressionOperationName(expr.node)?.let { operation ->
                    append(",op:")
                    append(operation)
                }
            }
        }
        val rootOffset = CHIRPackage.createCHIRPackage(
            builder = builder,
            nameOffset = builder.createString(chirPackage.name),
            pathOffset = builder.createString("${chirPackage.semanticId.value}|$exprMeta"),
            pkgAccessLevel = encodeAccessLevel(chirPackage.accessLevel),
            typesTypeOffset = CHIRPackage.createTypesTypeVector(builder, typeTags),
            typesOffset = CHIRPackage.createTypesVector(builder, typeOffsets),
            valuesTypeOffset = CHIRPackage.createValuesTypeVector(builder, valueTags),
            valuesOffset = CHIRPackage.createValuesVector(builder, valueOffsets),
            exprsTypeOffset = CHIRPackage.createExprsTypeVector(builder, exprTags),
            exprsOffset = CHIRPackage.createExprsVector(builder, exprOffsets),
            defsTypeOffset = 0,
            defsOffset = 0,
            packageInitFunc = chirPackage.packageInitFunctionId?.let { ctx.valueIdOf(it.value) } ?: 0u,
            phase = ChirSerializationSchema.CURRENT_VERSION.toUByte(),
            packageLiteralInitFunc = chirPackage.packageLiteralInitFunctionId?.let { ctx.valueIdOf(it.value) } ?: 0u,
            maxImportedValueId = 0u,
            maxImportedStructId = 0u,
            maxImportedClassId = 0u,
            maxImportedEnumId = 0u,
            maxImportedExtendId = 0u,
        )
        CHIRPackage.finishCHIRPackageBuffer(builder, rootOffset)
        return builder.sizedByteArray()
    }

    fun deserialize(bytes: ByteArray): ChirPackage {
        val root = CHIRPackage.getRootAsCHIRPackage(ByteBuffer.wrap(bytes))
        if (root.phase.toInt() != ChirSerializationSchema.CURRENT_VERSION) {
            throw ChirSerializationException("unsupported schema version: ${root.phase}")
        }

        val (packageSemanticId, exprMetadata) = parsePathMetadata(root.path)
        val typeById = decodeTypes(root)
        val valueState = decodeValues(root, typeById)
        val exprById = decodeExpressions(root, typeById, valueState, exprMetadata)

        if (valueState.functions.isEmpty()) {
            throw ChirSerializationException("damaged payload: no functions")
        }

        val modules = linkedMapOf<Pair<String, String>, MutableList<DefaultChirFunctionDeclaration>>()
        for (fn in valueState.functions) {
            val params = fn.paramValueIds.map {
                valueState.parameters[it]
                    ?: throw ChirSerializationException("damaged payload: function ${fn.semanticId.value} references missing parameter value $it")
            }.map {
                DefaultChirVariableDeclaration(
                    semanticId = it.semanticId,
                    name = it.name,
                    type = it.type,
                    mutable = it.mutable,
                )
            }

            val blocks = valueState.blocks.values
                .filter { it.parentFunctionValueId == fn.valueId }
                .map { block ->
                    val terminatorId = block.exprIds.lastOrNull()
                        ?: throw ChirSerializationException("damaged payload: block ${block.semanticId.value} has no terminator")
                    val expressions = block.exprIds.dropLast(1).map { exprId ->
                        exprById[exprId] as? ChirExpression
                            ?: throw ChirSerializationException("damaged payload: expression $exprId is missing or is not a CHIR expression in block ${block.semanticId.value}")
                    }
                    val terminator = exprById[terminatorId] as? ChirTerminator
                        ?: throw ChirSerializationException("damaged payload: expression $terminatorId is not a terminator for block ${block.semanticId.value}")
                    ChirBlock(block.semanticId, block.name, expressions, terminator)
                }

            val declaration = DefaultChirFunctionDeclaration(
                semanticId = fn.semanticId,
                name = fn.name,
                returnType = fn.returnType,
                parameters = params,
                blocks = blocks,
                entryBlockId = valueState.blockSemanticByValueId[fn.entryBlockValueId]
                    ?: throw ChirSerializationException("damaged payload: function ${fn.semanticId.value} references missing entry block value ${fn.entryBlockValueId}"),
            )

            val key = fn.moduleSemanticId.value to fn.moduleName
            modules.getOrPut(key) { mutableListOf() }.add(declaration)
        }

        return ChirPackage(
            semanticId = packageSemanticId,
            name = root.name ?: throw ChirSerializationException("damaged payload: package name is missing"),
            modules = modules.map { (k, v) -> ChirModule(ChirSemanticId(k.first), k.second, v) },
            members = ChirPackageMembers(),
            typeDefinitions = emptyList(),
            importedTypeDefinitions = emptyList(),
            packageInitFunctionId = valueState.functionSemanticByValueId[root.packageInitFunc],
            packageLiteralInitFunctionId = valueState.functionSemanticByValueId[root.packageLiteralInitFunc],
            accessLevel = decodeAccessLevel(root.pkgAccessLevel),
        )
    }

    private fun encodeAccessLevel(accessLevel: ChirPackageAccessLevel): UByte = when (accessLevel) {
        ChirPackageAccessLevel.PRIVATE -> 0u
        ChirPackageAccessLevel.INTERNAL -> 1u
        ChirPackageAccessLevel.PUBLIC -> 2u
    }

    private fun decodeAccessLevel(encoded: UByte): ChirPackageAccessLevel = when (encoded.toInt()) {
        0 -> ChirPackageAccessLevel.PRIVATE
        1 -> ChirPackageAccessLevel.INTERNAL
        2 -> ChirPackageAccessLevel.PUBLIC
        else -> throw ChirSerializationException("unsupported package access level: $encoded")
    }

    private fun encodeType(builder: FlatBufferBuilder, id: UInt, ref: ChirTypeRef): Pair<UByte, Int> {
        val kind = when ((ref as? ChirResolvedTypeRef)?.type) {
            ChirPrimitiveType.BOOL -> CHIRTypeKind.BOOLEAN
            ChirPrimitiveType.UNIT -> CHIRTypeKind.UNIT
            ChirPrimitiveType.NOTHING -> CHIRTypeKind.NOTHING
            ChirPrimitiveType.VOID -> CHIRTypeKind.VOID
            ChirPrimitiveType.RUNE -> CHIRTypeKind.RUNE
            ChirPrimitiveType.FLOAT16 -> CHIRTypeKind.FLOAT16
            ChirPrimitiveType.FLOAT32 -> CHIRTypeKind.FLOAT32
            ChirPrimitiveType.FLOAT64 -> CHIRTypeKind.FLOAT64
            ChirPrimitiveType.INT8 -> CHIRTypeKind.INT8
            ChirPrimitiveType.INT16 -> CHIRTypeKind.INT16
            ChirPrimitiveType.INT32 -> CHIRTypeKind.INT32
            ChirPrimitiveType.INT64 -> CHIRTypeKind.INT64
            ChirPrimitiveType.INT_NATIVE -> CHIRTypeKind.INT_NATIVE
            ChirPrimitiveType.UINT8 -> CHIRTypeKind.UINT8
            ChirPrimitiveType.UINT16 -> CHIRTypeKind.UINT16
            ChirPrimitiveType.UINT32 -> CHIRTypeKind.UINT32
            ChirPrimitiveType.UINT64 -> CHIRTypeKind.UINT64
            ChirPrimitiveType.UINT_NATIVE -> CHIRTypeKind.UINT_NATIVE
            else -> CHIRTypeKind.GENERIC
        }
        val base = Type.createType(builder, kind, id, 0, 0u)
        return when (kind) {
            CHIRTypeKind.BOOLEAN -> TypeElem.BooleanType to BooleanType.createBooleanType(builder, base, 0)
            CHIRTypeKind.UNIT -> TypeElem.UnitType to UnitType.createUnitType(builder, base, 0)
            CHIRTypeKind.NOTHING -> TypeElem.NothingType to NothingType.createNothingType(builder, base, 0)
            CHIRTypeKind.VOID -> TypeElem.VoidType to VoidType.createVoidType(builder, base)
            CHIRTypeKind.RUNE -> TypeElem.RuneType to RuneType.createRuneType(builder, base, 0)
            CHIRTypeKind.FLOAT16, CHIRTypeKind.FLOAT32, CHIRTypeKind.FLOAT64 -> {
                val num = NumericType.createNumericType(builder, base, 0)
                TypeElem.FloatType to FloatType.createFloatType(builder, num)
            }
            CHIRTypeKind.INT8, CHIRTypeKind.INT16, CHIRTypeKind.INT32, CHIRTypeKind.INT64,
            CHIRTypeKind.INT_NATIVE,
            CHIRTypeKind.UINT8, CHIRTypeKind.UINT16, CHIRTypeKind.UINT32, CHIRTypeKind.UINT64,
            CHIRTypeKind.UINT_NATIVE,
            -> {
                val num = NumericType.createNumericType(builder, base, 0)
                TypeElem.IntType to IntType.createIntType(builder, num)
            }
            else -> {
                TypeElem.GenericType to GenericType.createGenericType(
                    builder,
                    base,
                    false,
                    false,
                    builder.createString(ref.renderName),
                    builder.createString(ref.renderName),
                    0,
                )
            }
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun encodeValue(builder: FlatBufferBuilder, v: EncValue, ctx: EncodeContext): Pair<UByte, Int> {
        return when (v.kind) {
            EncValueKind.Function -> {
                val fn = v.function!!
                val base = Value.createValue(builder, 0, ctx.typeIdOf(fn.returnType), builder.createString(fn.name), ValueKind.FUNC, v.id, 0u, 0)
                val params = Func.createParamsVector(builder, fn.parameters.map { ctx.valueIdOf(it.semanticId.value) }.toUIntArray())
                ValueElem.Func to Func.createFunc(
                    builder,
                    base,
                    builder.createString(fn.semanticId.value),
                    builder.createString(fn.name),
                    builder.createString(v.moduleName!!),
                    0u,
                    0u,
                    FuncKind.DEFAULT,
                    false,
                    false,
                    0u,
                    0,
                    0,
                    0u,
                    ctx.valueIdOf(fn.entryBlockId.value),
                    params,
                    0u,
                    builder.createString(v.moduleSemanticId!!),
                    0,
                    0u,
                    0u,
                    0u,
                )
            }
            EncValueKind.Parameter -> {
                val p = v.parameter!!
                val idAndName = "${p.semanticId.value}|${p.name}"
                val attrs = if (p.mutable) 1uL else 0uL
                val base = Value.createValue(builder, 0, ctx.typeIdOf(p.type), builder.createString(idAndName), ValueKind.PARAMETER, v.id, attrs, 0)
                ValueElem.Parameter to Parameter.createParameter(builder, base, ctx.valueIdOf(v.ownerFunctionSemanticId!!), 0u)
            }
            EncValueKind.Block -> {
                val b = v.block!!
                val idAndName = "${b.semanticId.value}|${b.name}"
                val exprIds = b.expressions.map { ctx.exprIdOf(it.semanticId.value) } + ctx.exprIdOf(b.terminator.semanticId.value)
                val base = Value.createValue(builder, 0, ctx.typeIdOf(unitType), builder.createString(idAndName), ValueKind.BLOCK, v.id, 0u, 0)
                ValueElem.Block to Block.createBlock(builder, base, ctx.valueIdOf(v.ownerFunctionSemanticId!!), Block.createExprsVector(builder, exprIds.toUIntArray()), 0, false, 0)
            }
            EncValueKind.Local -> {
                val lv = v.value as ChirLocalValue
                val idAndName = "${lv.semanticId.value}|${lv.name}"
                val base = Value.createValue(builder, 0, ctx.typeIdOf(lv.type), builder.createString(idAndName), ValueKind.LOCALVAR, v.id, 0u, 0)
                ValueElem.LocalVar to LocalVar.createLocalVar(builder, base, 0u, false)
            }
            EncValueKind.Constant -> {
                val cv = v.value as ChirConstantValue
                val literal = cv.literal.toULongOrNull()
                    ?: throw ChirSerializationException("unsupported integer literal for codec: '${cv.literal}'")
                val base = Value.createValue(builder, 0, ctx.typeIdOf(cv.type), builder.createString(cv.semanticId.value), ValueKind.LITERAL, v.id, 0u, 0)
                val lit = LiteralValue.createLiteralValue(builder, base, LitConstKind.Integer)
                ValueElem.IntLiteral to IntLiteral.createIntLiteral(builder, lit, literal)
            }
            EncValueKind.Global -> {
                val gv = v.value as ChirGlobalValue
                val idAndName = "${gv.semanticId.value}|${gv.name}"
                val base = Value.createValue(builder, 0, ctx.typeIdOf(gv.type), builder.createString(idAndName), ValueKind.GLOBALVAR, v.id, 0u, 0)
                ValueElem.GlobalVar to GlobalVar.createGlobalVar(
                    builder,
                    base,
                    builder.createString(gv.name),
                    builder.createString(gv.semanticId.value),
                    builder.createString(""),
                    0u,
                    0u,
                    0u,
                )
            }
            EncValueKind.ImportedFunction -> {
                val fv = v.value as ChirImportedFunctionValue
                val idAndName = "${fv.semanticId.value}|${fv.name}"
                val base = Value.createValue(builder, 0, ctx.typeIdOf(fv.type), builder.createString(idAndName), ValueKind.IMPORTED_FUNC, v.id, 0u, 0)
                val imported = ImportedValue.createImportedValue(builder, base)
                ValueElem.ImportedFunc to ImportedFunc.createImportedFunc(
                    builder,
                    imported,
                    builder.createString(fv.semanticId.value),
                    builder.createString(fv.name),
                    builder.createString(""),
                    0u,
                    0u,
                    FuncKind.DEFAULT,
                    false,
                    false,
                    0u,
                    0,
                    0,
                    0u,
                    0,
                )
            }
            EncValueKind.ImportedVariable -> {
                val iv = v.value as ChirImportedVariableValue
                val idAndName = "${iv.semanticId.value}|${iv.name}"
                val base = Value.createValue(builder, 0, ctx.typeIdOf(iv.type), builder.createString(idAndName), ValueKind.IMPORTED_VAR, v.id, 0u, 0)
                val imported = ImportedValue.createImportedValue(builder, base)
                ValueElem.ImportedVar to ImportedVar.createImportedVar(
                    builder,
                    imported,
                    builder.createString(""),
                    builder.createString(iv.semanticId.value),
                    builder.createString(iv.name),
                )
            }
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun encodeExpr(builder: FlatBufferBuilder, e: EncExpr, ctx: EncodeContext): Pair<UByte, Int> {
        val operation = e.node
        return when (operation) {
            is ChirUnaryExpression -> {
                val operator = ChirUnaryOperator.parse(operation.operator)
                    ?: throw ChirSerializationException("unsupported unary operator: ${operation.operator}")
                val ops = uintArrayOf(ctx.valueIdOf(operation.operand.semanticId.value))
                val base = encodeBaseExpression(builder, e, ctx, encodeUnaryExprKind(operator), ops, ctx.typeIdOf(operation.resultType))
                ExpressionElem.UnaryExpression to UnaryExpression.createUnaryExpression(builder, base, OverflowStrategy.NA)
            }
            is ChirBinaryExpression -> {
                val operator = ChirBinaryOperator.parse(operation.operator)
                    ?: throw ChirSerializationException("unsupported binary operator: ${operation.operator}")
                val ops = uintArrayOf(ctx.valueIdOf(operation.left.semanticId.value), ctx.valueIdOf(operation.right.semanticId.value))
                val base = encodeBaseExpression(builder, e, ctx, encodeBinaryExprKind(operator), ops, ctx.typeIdOf(operation.resultType))
                ExpressionElem.BinaryExpression to BinaryExpression.createBinaryExpression(builder, base, OverflowStrategy.NA)
            }
            is ChirMemoryExpression -> {
                val memoryOperation = ChirMemoryOperation.parse(operation.operation)
                    ?: throw ChirSerializationException("unsupported memory operation: ${operation.operation}")
                val ops = listOfNotNull(operation.address, operation.value)
                    .map { ctx.valueIdOf(it.semanticId.value) }
                    .toUIntArray()
                val resultType = operation.resultType?.let(ctx::typeIdOf) ?: 0u
                val base = encodeBaseExpression(builder, e, ctx, encodeMemoryExprKind(memoryOperation), ops, resultType)
                when (memoryOperation) {
                    ChirMemoryOperation.LOAD -> ExpressionElem.Load to Load.createLoad(builder, base)
                    ChirMemoryOperation.STORE -> ExpressionElem.Store to Store.createStore(builder, base)
                    ChirMemoryOperation.ALLOCA -> ExpressionElem.Allocate to Allocate.createAllocate(builder, base, resultType)
                    ChirMemoryOperation.GET_ELEMENT_PTR,
                    ChirMemoryOperation.GET_ELEMENT_PTR_INBOUNDS,
                    -> {
                        val path = GetElementRef.createPathVector(builder, ulongArrayOf())
                        ExpressionElem.GetElementRef to GetElementRef.createGetElementRef(builder, base, path)
                    }
                }
            }
            is ChirCallExpression -> {
                val ops = (listOf(operation.callee) + operation.arguments)
                    .map { ctx.valueIdOf(it.semanticId.value) }
                    .toUIntArray()
                val base = encodeBaseExpression(builder, e, ctx, CHIRExprKind.APPLY, ops, ctx.typeIdOf(operation.resultType))
                ExpressionElem.Apply to Apply.createApply(builder, base, false, 0, 0u)
            }
            is ChirOtherExpression -> {
                val otherOperation = ChirOtherOperation.parse(operation.operation)
                    ?: throw ChirSerializationException("unsupported other operation: ${operation.operation}")
                val ops = operation.operands.map { ctx.valueIdOf(it.semanticId.value) }.toUIntArray()
                val resultType = operation.resultType?.let(ctx::typeIdOf) ?: 0u
                val base = encodeBaseExpression(builder, e, ctx, encodeOtherExprKind(otherOperation), ops, resultType)
                when (otherOperation) {
                    ChirOtherOperation.BITCAST,
                    ChirOtherOperation.PTRTOINT,
                    ChirOtherOperation.INTTOPTR,
                    ChirOtherOperation.TRUNC,
                    ChirOtherOperation.ZEXT,
                    ChirOtherOperation.SEXT,
                    ChirOtherOperation.FPTRUNC,
                    ChirOtherOperation.FPEXT,
                    ChirOtherOperation.SITOFP,
                    ChirOtherOperation.UITOFP,
                    ChirOtherOperation.FPTOSI,
                    ChirOtherOperation.FPTOUI,
                    -> ExpressionElem.TypeCast to TypeCast.createTypeCast(builder, base, OverflowStrategy.NA)

                    else -> ExpressionElem.Debug to Debug.createDebug(builder, base, builder.createString(otherOperation.canonicalName))
                }
            }
            is ChirBranchTerminator -> {
                val baseExpr = encodeBaseExpression(builder, e, ctx, CHIRExprKind.GOTO, uintArrayOf(), 0u)
                val term = Terminator.createTerminator(builder, baseExpr, Terminator.createSuccessorsVector(builder, uintArrayOf(ctx.valueIdOf(operation.targetBlockId.value))))
                ExpressionElem.GoTo to GoTo.createGoTo(builder, term)
            }
            is ChirConditionalBranchTerminator -> {
                val baseExpr = encodeBaseExpression(
                    builder = builder,
                    e = e,
                    ctx = ctx,
                    kind = CHIRExprKind.BRANCH,
                    operands = uintArrayOf(ctx.valueIdOf(operation.condition.semanticId.value)),
                    resultType = 0u,
                )
                val successors = Terminator.createSuccessorsVector(
                    builder,
                    uintArrayOf(
                        ctx.valueIdOf(operation.trueTargetBlockId.value),
                        ctx.valueIdOf(operation.falseTargetBlockId.value),
                    ),
                )
                val term = Terminator.createTerminator(builder, baseExpr, successors)
                ExpressionElem.Branch to Branch.createBranch(builder, term, 0u)
            }
            is ChirReturnTerminator -> {
                val ops = operation.returnValue?.let { uintArrayOf(ctx.valueIdOf(it.semanticId.value)) }
                val baseExpr = encodeBaseExpression(builder, e, ctx, CHIRExprKind.EXIT, ops ?: uintArrayOf(), 0u)
                val term = Terminator.createTerminator(builder, baseExpr, 0)
                ExpressionElem.Exit to Exit.createExit(builder, term)
            }
            is ChirThrowTerminator -> {
                val baseExpr = encodeBaseExpression(
                    builder = builder,
                    e = e,
                    ctx = ctx,
                    kind = CHIRExprKind.RAISE_EXCEPTION,
                    operands = uintArrayOf(ctx.valueIdOf(operation.exceptionValue.semanticId.value)),
                    resultType = 0u,
                )
                val successors = operation.unwindTargetBlockId?.let {
                    Terminator.createSuccessorsVector(builder, uintArrayOf(ctx.valueIdOf(it.value)))
                } ?: 0
                val term = Terminator.createTerminator(builder, baseExpr, successors)
                ExpressionElem.RaiseException to RaiseException.createRaiseException(builder, term)
            }
            is ChirUnwindTerminator -> {
                val baseExpr = encodeBaseExpression(builder, e, ctx, CHIRExprKind.GOTO, uintArrayOf(), 0u)
                val term = Terminator.createTerminator(builder, baseExpr, Terminator.createSuccessorsVector(builder, uintArrayOf(ctx.valueIdOf(operation.targetBlockId.value))))
                ExpressionElem.GoTo to GoTo.createGoTo(builder, term)
            }
            else -> throw ChirSerializationException("unsupported CHIR node in serializer: ${operation::class.qualifiedName}")
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun encodeBaseExpression(
        builder: FlatBufferBuilder,
        e: EncExpr,
        ctx: EncodeContext,
        kind: UByte,
        operands: UIntArray,
        resultType: UInt,
    ): Int {
        val operandsOffset = if (operands.isEmpty()) 0 else Expression.createOperandsVector(builder, operands)
        return Expression.createExpression(
            builder,
            0,
            kind,
            e.id,
            operandsOffset,
            0,
            ctx.valueIdOf(e.parentBlockSemanticId),
            0u,
            resultType,
        )
    }

    private fun encodeUnaryExprKind(operator: ChirUnaryOperator): UByte = when (operator) {
        ChirUnaryOperator.INT_NEG,
        ChirUnaryOperator.FLOAT_NEG,
        -> CHIRExprKind.NEG
        ChirUnaryOperator.BIT_NOT -> CHIRExprKind.BITNOT
        ChirUnaryOperator.LOGICAL_NOT -> CHIRExprKind.NOT
        ChirUnaryOperator.IDENTITY -> CHIRExprKind.DEBUGEXPR
    }

    private fun encodeBinaryExprKind(operator: ChirBinaryOperator): UByte = when (operator) {
        ChirBinaryOperator.ADD -> CHIRExprKind.ADD
        ChirBinaryOperator.SUB -> CHIRExprKind.SUB
        ChirBinaryOperator.MUL -> CHIRExprKind.MUL
        ChirBinaryOperator.SIGNED_DIV,
        ChirBinaryOperator.UNSIGNED_DIV,
        -> CHIRExprKind.DIV
        ChirBinaryOperator.SIGNED_REM,
        ChirBinaryOperator.UNSIGNED_REM,
        -> CHIRExprKind.MOD
        ChirBinaryOperator.BIT_AND -> CHIRExprKind.BITAND
        ChirBinaryOperator.BIT_OR -> CHIRExprKind.BITOR
        ChirBinaryOperator.BIT_XOR -> CHIRExprKind.BITXOR
        ChirBinaryOperator.SHIFT_LEFT -> CHIRExprKind.LSHIFT
        ChirBinaryOperator.SIGNED_SHIFT_RIGHT,
        ChirBinaryOperator.UNSIGNED_SHIFT_RIGHT,
        -> CHIRExprKind.RSHIFT
        ChirBinaryOperator.EQUAL,
        ChirBinaryOperator.FLOAT_EQUAL,
        -> CHIRExprKind.EQUAL
        ChirBinaryOperator.NOT_EQUAL,
        ChirBinaryOperator.FLOAT_NOT_EQUAL,
        -> CHIRExprKind.NOTEQUAL
        ChirBinaryOperator.SIGNED_LESS,
        ChirBinaryOperator.UNSIGNED_LESS,
        ChirBinaryOperator.FLOAT_LESS,
        -> CHIRExprKind.LT
        ChirBinaryOperator.SIGNED_LESS_OR_EQUAL,
        ChirBinaryOperator.UNSIGNED_LESS_OR_EQUAL,
        ChirBinaryOperator.FLOAT_LESS_OR_EQUAL,
        -> CHIRExprKind.LE
        ChirBinaryOperator.SIGNED_GREATER,
        ChirBinaryOperator.UNSIGNED_GREATER,
        ChirBinaryOperator.FLOAT_GREATER,
        -> CHIRExprKind.GT
        ChirBinaryOperator.SIGNED_GREATER_OR_EQUAL,
        ChirBinaryOperator.UNSIGNED_GREATER_OR_EQUAL,
        ChirBinaryOperator.FLOAT_GREATER_OR_EQUAL,
        -> CHIRExprKind.GE
    }

    private fun encodeMemoryExprKind(operation: ChirMemoryOperation): UByte = when (operation) {
        ChirMemoryOperation.LOAD -> CHIRExprKind.LOAD
        ChirMemoryOperation.STORE -> CHIRExprKind.STORE
        ChirMemoryOperation.ALLOCA -> CHIRExprKind.ALLOCATE
        ChirMemoryOperation.GET_ELEMENT_PTR,
        ChirMemoryOperation.GET_ELEMENT_PTR_INBOUNDS,
        -> CHIRExprKind.GET_ELEMENT_REF
    }

    private fun encodeOtherExprKind(operation: ChirOtherOperation): UByte = when (operation) {
        ChirOtherOperation.SELECT -> CHIRExprKind.IF
        ChirOtherOperation.PHI -> CHIRExprKind.DEBUGEXPR
        ChirOtherOperation.BITCAST,
        ChirOtherOperation.PTRTOINT,
        ChirOtherOperation.INTTOPTR,
        ChirOtherOperation.TRUNC,
        ChirOtherOperation.ZEXT,
        ChirOtherOperation.SEXT,
        ChirOtherOperation.FPTRUNC,
        ChirOtherOperation.FPEXT,
        ChirOtherOperation.SITOFP,
        ChirOtherOperation.UITOFP,
        ChirOtherOperation.FPTOSI,
        ChirOtherOperation.FPTOUI,
        -> CHIRExprKind.TYPECAST
        else -> CHIRExprKind.DEBUGEXPR
    }

    private fun decodeTypes(root: CHIRPackage): Map<UInt, ChirTypeRef> {
        val result = linkedMapOf<UInt, ChirTypeRef>()
        for (i in 0 until root.typesLength) {
            when (root.typesType(i)) {
                TypeElem.IntType -> root.types(IntType(), i)?.let { t ->
                    val base = (t as IntType).base?.base
                        ?: throw ChirSerializationException("damaged payload: int type #$i has no base")
                    result[base.typeId] = ChirResolvedTypeRef(decodePrimitiveType(base.kind))
                }
                TypeElem.FloatType -> root.types(FloatType(), i)?.let { t ->
                    val base = (t as FloatType).base?.base
                        ?: throw ChirSerializationException("damaged payload: float type #$i has no base")
                    result[base.typeId] = ChirResolvedTypeRef(decodePrimitiveType(base.kind))
                }
                TypeElem.BooleanType -> root.types(BooleanType(), i)?.let { t ->
                    val base = (t as BooleanType).base
                        ?: throw ChirSerializationException("damaged payload: boolean type #$i has no base")
                    result[base.typeId] = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
                }
                TypeElem.UnitType -> root.types(UnitType(), i)?.let { t ->
                    val base = (t as UnitType).base
                        ?: throw ChirSerializationException("damaged payload: unit type #$i has no base")
                    result[base.typeId] = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
                }
                TypeElem.NothingType -> root.types(NothingType(), i)?.let { t ->
                    val base = (t as NothingType).base
                        ?: throw ChirSerializationException("damaged payload: nothing type #$i has no base")
                    result[base.typeId] = ChirResolvedTypeRef(ChirPrimitiveType.NOTHING)
                }
                TypeElem.VoidType -> root.types(VoidType(), i)?.let { t ->
                    val base = (t as VoidType).base
                        ?: throw ChirSerializationException("damaged payload: void type #$i has no base")
                    result[base.typeId] = ChirResolvedTypeRef(ChirPrimitiveType.VOID)
                }
                TypeElem.RuneType -> root.types(RuneType(), i)?.let { t ->
                    val base = (t as RuneType).base
                        ?: throw ChirSerializationException("damaged payload: rune type #$i has no base")
                    result[base.typeId] = ChirResolvedTypeRef(ChirPrimitiveType.RUNE)
                }
                TypeElem.GenericType -> root.types(GenericType(), i)?.let { t ->
                    val g = t as GenericType
                    val base = g.base
                        ?: throw ChirSerializationException("damaged payload: generic type #$i has no base")
                    val identifier = g.identifier
                        ?: throw ChirSerializationException("damaged payload: generic type #$i has no identifier")
                    result[base.typeId] = ChirResolvedTypeRef(ChirGenericType(identifier))
                }
            }
        }
        return result
    }

    private fun decodePrimitiveType(kind: UByte): ChirPrimitiveType = when (kind) {
        CHIRTypeKind.BOOLEAN -> ChirPrimitiveType.BOOL
        CHIRTypeKind.UNIT -> ChirPrimitiveType.UNIT
        CHIRTypeKind.NOTHING -> ChirPrimitiveType.NOTHING
        CHIRTypeKind.VOID -> ChirPrimitiveType.VOID
        CHIRTypeKind.FLOAT16 -> ChirPrimitiveType.FLOAT16
        CHIRTypeKind.FLOAT32 -> ChirPrimitiveType.FLOAT32
        CHIRTypeKind.FLOAT64 -> ChirPrimitiveType.FLOAT64
        CHIRTypeKind.INT8 -> ChirPrimitiveType.INT8
        CHIRTypeKind.INT16 -> ChirPrimitiveType.INT16
        CHIRTypeKind.INT32 -> ChirPrimitiveType.INT32
        CHIRTypeKind.INT64 -> ChirPrimitiveType.INT64
        CHIRTypeKind.INT_NATIVE -> ChirPrimitiveType.INT_NATIVE
        CHIRTypeKind.UINT8 -> ChirPrimitiveType.UINT8
        CHIRTypeKind.UINT16 -> ChirPrimitiveType.UINT16
        CHIRTypeKind.UINT32 -> ChirPrimitiveType.UINT32
        CHIRTypeKind.UINT64 -> ChirPrimitiveType.UINT64
        CHIRTypeKind.UINT_NATIVE -> ChirPrimitiveType.UINT_NATIVE
        else -> throw ChirSerializationException("cannot decode primitive type kind $kind")
    }

    private fun decodeValues(root: CHIRPackage, typeById: Map<UInt, ChirTypeRef>): DecodedValues {
        val out = DecodedValues()
        fun typeOf(id: UInt) = typeById[id]
            ?: throw ChirSerializationException("damaged payload: value references unknown type $id")
        for (i in 0 until root.valuesLength) {
            when (root.valuesType(i)) {
                ValueElem.IntLiteral -> root.values(IntLiteral(), i)?.let { t ->
                    val b = (t as IntLiteral).base?.base
                        ?: throw ChirSerializationException("damaged payload: int literal value #$i has no base")
                    val identifier = b.identifier
                        ?: throw ChirSerializationException("damaged payload: int literal value ${b.valueId} has no identifier")
                    out.values[b.valueId] = ChirConstantValue(ChirSemanticId(identifier), typeOf(b.type), t.val_.toString())
                }
                ValueElem.Parameter -> root.values(Parameter(), i)?.let { t ->
                    val b = (t as Parameter).base
                        ?: throw ChirSerializationException("damaged payload: parameter value #$i has no base")
                    val parts = valueIdentifierParts(
                        b.identifier ?: throw ChirSerializationException("damaged payload: parameter value ${b.valueId} has no identifier"),
                    )
                    out.parameters[b.valueId] = DecodedParam(ChirSemanticId(parts[0]), parts.getOrElse(1) { parts[0] }, typeOf(b.type), (b.attributes and 1uL) == 1uL)
                    out.values[b.valueId] = ChirParameterValue(
                        semanticId = ChirSemanticId(parts[0]),
                        type = typeOf(b.type),
                        name = parts.getOrElse(1) { parts[0] },
                        ownerFunctionId = ChirSemanticId("fn-owner:${t.ownedFunc}"),
                    )
                }
                ValueElem.Func -> root.values(Func(), i)?.let { t ->
                    val f = t as Func
                    val b = f.base
                        ?: throw ChirSerializationException("damaged payload: function value #$i has no base")
                    val semanticId = f.srcCodeIdentifier
                        ?: throw ChirSerializationException("damaged payload: function value ${b.valueId} has no semantic id")
                    val name = b.identifier
                        ?: throw ChirSerializationException("damaged payload: function value ${b.valueId} has no name")
                    val moduleSemanticId = f.parentName
                        ?: throw ChirSerializationException("damaged payload: function value ${b.valueId} has no module id")
                    val moduleName = f.packageName
                        ?: throw ChirSerializationException("damaged payload: function value ${b.valueId} has no module name")
                    out.functionSemanticByValueId[b.valueId] = ChirSemanticId(semanticId)
                    out.functions += DecodedFunc(b.valueId, ChirSemanticId(semanticId), name, typeOf(b.type), ChirSemanticId(moduleSemanticId), moduleName, List(f.paramsLength) { f.params(it) }, f.body)
                }
                ValueElem.Block -> root.values(Block(), i)?.let { t ->
                    val b = t as Block
                    val base = b.base
                        ?: throw ChirSerializationException("damaged payload: block value #$i has no base")
                    val parts = valueIdentifierParts(
                        base.identifier ?: throw ChirSerializationException("damaged payload: block value ${base.valueId} has no identifier"),
                    )
                    val sid = ChirSemanticId(parts[0])
                    out.blocks[base.valueId] = DecodedBlock(sid, parts.getOrElse(1) { "block" }, b.parentGroup, List(b.exprsLength) { b.exprs(it) })
                    out.blockSemanticByValueId[base.valueId] = sid
                }
                ValueElem.LocalVar -> root.values(LocalVar(), i)?.let { t ->
                    val b = (t as LocalVar).base
                        ?: throw ChirSerializationException("damaged payload: local value #$i has no base")
                    val parts = valueIdentifierParts(
                        b.identifier ?: throw ChirSerializationException("damaged payload: local value ${b.valueId} has no identifier"),
                    )
                    out.values[b.valueId] = ChirLocalValue(ChirSemanticId(parts[0]), typeOf(b.type), parts.getOrElse(1) { parts[0] })
                }
                ValueElem.GlobalVar -> root.values(GlobalVar(), i)?.let { t ->
                    val gv = t as GlobalVar
                    val b = gv.base
                        ?: throw ChirSerializationException("damaged payload: global value #$i has no base")
                    val parts = valueIdentifierParts(
                        b.identifier ?: throw ChirSerializationException("damaged payload: global value ${b.valueId} has no identifier"),
                    )
                    out.values[b.valueId] = ChirGlobalValue(
                        semanticId = ChirSemanticId(parts[0]),
                        type = typeOf(b.type),
                        name = parts.getOrElse(1) { parts[0] },
                    )
                }
                ValueElem.ImportedFunc -> root.values(ImportedFunc(), i)?.let { t ->
                    val importedFunction = t as ImportedFunc
                    val b = importedFunction.base?.base
                        ?: throw ChirSerializationException("damaged payload: imported function value #$i has no base")
                    val parts = valueIdentifierParts(
                        b.identifier ?: throw ChirSerializationException("damaged payload: imported function value ${b.valueId} has no identifier"),
                    )
                    out.values[b.valueId] = ChirImportedFunctionValue(
                        semanticId = ChirSemanticId(parts[0]),
                        type = typeOf(b.type),
                        name = parts.getOrElse(1) { parts[0] },
                    )
                }
                ValueElem.ImportedVar -> root.values(ImportedVar(), i)?.let { t ->
                    val importedVariable = t as ImportedVar
                    val b = importedVariable.base?.base
                        ?: throw ChirSerializationException("damaged payload: imported variable value #$i has no base")
                    val parts = valueIdentifierParts(
                        b.identifier ?: throw ChirSerializationException("damaged payload: imported variable value ${b.valueId} has no identifier"),
                    )
                    out.values[b.valueId] = ChirImportedVariableValue(
                        semanticId = ChirSemanticId(parts[0]),
                        type = typeOf(b.type),
                        name = parts.getOrElse(1) { parts[0] },
                    )
                }
            }
        }
        return out
    }

    private fun decodeExpressions(
        root: CHIRPackage,
        typeById: Map<UInt, ChirTypeRef>,
        values: DecodedValues,
        exprMetadata: Map<UInt, DecodedExprMetadata>,
    ): Map<UInt, Any> {
        val out = linkedMapOf<UInt, Any>()
        fun typeOf(id: UInt) = typeById[id]
            ?: throw ChirSerializationException("damaged payload: expression references unknown type $id")
        fun operands(base: Expression) = List(base.operandsLength) { index ->
            val valueId = base.operands(index)
            values.values[valueId]
                ?: throw ChirSerializationException("damaged payload: expression ${base.expressionId} references unknown value $valueId")
        }
        fun exprSid(id: UInt) = ChirSemanticId(exprMetadata[id]?.semanticId ?: "expr:$id")
        fun termSid(id: UInt) = ChirSemanticId(exprMetadata[id]?.semanticId ?: "term:$id")
        fun operationOf(id: UInt) = exprMetadata[id]?.operation
        for (i in 0 until root.exprsLength) {
            when (root.exprsType(i)) {
                ExpressionElem.UnaryExpression -> root.exprs(UnaryExpression(), i)?.let { t ->
                    val base = (t as UnaryExpression).base ?: return@let
                    val ops = operands(base)
                    if (ops.isNotEmpty()) {
                        val operation = operationOf(base.expressionId) ?: decodeUnaryExprKind(base.kind)
                        out[base.expressionId] = ChirUnaryExpression(
                            exprSid(base.expressionId),
                            operation,
                            ops[0],
                            typeOf(base.resultTy),
                        )
                    }
                }
                ExpressionElem.BinaryExpression -> root.exprs(BinaryExpression(), i)?.let { t ->
                    val base = (t as BinaryExpression).base ?: return@let
                    val ops = operands(base)
                    if (ops.size >= 2) {
                        val operation = operationOf(base.expressionId) ?: decodeBinaryExprKind(base.kind)
                        out[base.expressionId] = ChirBinaryExpression(exprSid(base.expressionId), operation, ops[0], ops[1], typeOf(base.resultTy))
                    }
                }
                ExpressionElem.Load -> root.exprs(Load(), i)?.let { t ->
                    val base = (t as Load).base ?: return@let
                    val ops = operands(base)
                    if (ops.isNotEmpty()) {
                        out[base.expressionId] = ChirMemoryExpression(
                            exprSid(base.expressionId),
                            operationOf(base.expressionId) ?: ChirMemoryOperation.LOAD.canonicalName,
                            ops[0],
                            null,
                            typeOf(base.resultTy),
                        )
                    }
                }
                ExpressionElem.Store -> root.exprs(Store(), i)?.let { t ->
                    val base = (t as Store).base ?: return@let
                    val ops = operands(base)
                    if (ops.size >= 2) {
                        out[base.expressionId] = ChirMemoryExpression(
                            exprSid(base.expressionId),
                            operationOf(base.expressionId) ?: ChirMemoryOperation.STORE.canonicalName,
                            ops[0],
                            ops[1],
                            null,
                        )
                    }
                }
                ExpressionElem.Allocate -> root.exprs(Allocate(), i)?.let { t ->
                    val base = (t as Allocate).base ?: return@let
                    val ops = operands(base)
                    if (ops.isNotEmpty()) {
                        out[base.expressionId] = ChirMemoryExpression(
                            exprSid(base.expressionId),
                            operationOf(base.expressionId) ?: ChirMemoryOperation.ALLOCA.canonicalName,
                            ops[0],
                            null,
                            typeOf(base.resultTy),
                        )
                    }
                }
                ExpressionElem.GetElementRef -> root.exprs(GetElementRef(), i)?.let { t ->
                    val base = (t as GetElementRef).base ?: return@let
                    val ops = operands(base)
                    if (ops.isNotEmpty()) {
                        out[base.expressionId] = ChirMemoryExpression(
                            exprSid(base.expressionId),
                            operationOf(base.expressionId) ?: ChirMemoryOperation.GET_ELEMENT_PTR.canonicalName,
                            ops[0],
                            ops.getOrNull(1),
                            typeOf(base.resultTy),
                        )
                    }
                }
                ExpressionElem.Apply -> root.exprs(Apply(), i)?.let { t ->
                    val base = (t as Apply).base ?: return@let
                    val ops = operands(base)
                    if (ops.isNotEmpty()) {
                        out[base.expressionId] = ChirCallExpression(
                            exprSid(base.expressionId),
                            ops[0],
                            ops.drop(1),
                            typeOf(base.resultTy),
                        )
                    }
                }
                ExpressionElem.TypeCast -> root.exprs(TypeCast(), i)?.let { t ->
                    val base = (t as TypeCast).base ?: return@let
                    val ops = operands(base)
                    val operation = operationOf(base.expressionId)
                        ?: throw ChirSerializationException("typecast expression ${base.expressionId} is missing operation metadata")
                    out[base.expressionId] = ChirOtherExpression(exprSid(base.expressionId), operation, ops, typeOf(base.resultTy))
                }
                ExpressionElem.GoTo -> root.exprs(GoTo(), i)?.let { t ->
                    val term = (t as GoTo).base ?: return@let
                    if (term.successorsLength != 1) {
                        throw ChirSerializationException("goto terminator ${term.base?.expressionId} must have exactly one successor")
                    }
                    val target = values.blockSemanticByValueId[term.successors(0)]
                        ?: throw ChirSerializationException("goto terminator references unknown block value ${term.successors(0)}")
                    out[term.base?.expressionId ?: 0u] = ChirBranchTerminator(termSid(term.base?.expressionId ?: 0u), target)
                }
                ExpressionElem.Branch -> root.exprs(Branch(), i)?.let { t ->
                    val term = (t as Branch).base ?: return@let
                    val base = term.base ?: return@let
                    val ops = operands(base)
                    if (term.successorsLength != 2 || ops.isEmpty()) {
                        throw ChirSerializationException("branch terminator ${base.expressionId} must have one condition and two successors")
                    }
                    val trueTarget = values.blockSemanticByValueId[term.successors(0)]
                        ?: throw ChirSerializationException("branch true target references unknown block value ${term.successors(0)}")
                    val falseTarget = values.blockSemanticByValueId[term.successors(1)]
                        ?: throw ChirSerializationException("branch false target references unknown block value ${term.successors(1)}")
                    out[base.expressionId] = ChirConditionalBranchTerminator(termSid(base.expressionId), ops[0], trueTarget, falseTarget)
                }
                ExpressionElem.Exit -> root.exprs(Exit(), i)?.let { t ->
                    val term = (t as Exit).base ?: return@let
                    val base = term.base ?: return@let
                    val ret = if (base.operandsLength > 0) {
                        values.values[base.operands(0)]
                            ?: throw ChirSerializationException("damaged payload: return terminator ${base.expressionId} references unknown value ${base.operands(0)}")
                    } else {
                        null
                    }
                    out[base.expressionId] = ChirReturnTerminator(termSid(base.expressionId), ret)
                }
                ExpressionElem.RaiseException -> root.exprs(RaiseException(), i)?.let { t ->
                    val term = (t as RaiseException).base ?: return@let
                    val base = term.base ?: return@let
                    val ops = operands(base)
                    val exceptionValue = ops.firstOrNull()
                        ?: throw ChirSerializationException("raise terminator ${base.expressionId} is missing exception operand")
                    val unwindTarget = if (term.successorsLength > 0) {
                        values.blockSemanticByValueId[term.successors(0)]
                            ?: throw ChirSerializationException("raise terminator references unknown unwind block value ${term.successors(0)}")
                    } else {
                        null
                    }
                    out[base.expressionId] = ChirThrowTerminator(termSid(base.expressionId), exceptionValue, unwindTarget)
                }
                ExpressionElem.Debug -> root.exprs(Debug(), i)?.let { t ->
                    val base = (t as Debug).base ?: return@let
                    val ops = operands(base)
                    val operation = operationOf(base.expressionId) ?: t.srcCodeIdentifier
                        ?: throw ChirSerializationException("debug expression ${base.expressionId} is missing operation metadata")
                    val resultType = if (base.resultTy == 0u) null else typeOf(base.resultTy)
                    out[base.expressionId] = ChirOtherExpression(exprSid(base.expressionId), operation, ops, resultType)
                }
            }
        }
        return out
    }

    private fun decodeUnaryExprKind(kind: UByte): String = when (kind) {
        CHIRExprKind.NEG -> ChirUnaryOperator.INT_NEG.canonicalName
        CHIRExprKind.BITNOT -> ChirUnaryOperator.BIT_NOT.canonicalName
        CHIRExprKind.NOT -> ChirUnaryOperator.LOGICAL_NOT.canonicalName
        else -> throw ChirSerializationException("cannot decode unary expression kind $kind without operation metadata")
    }

    private fun decodeBinaryExprKind(kind: UByte): String = when (kind) {
        CHIRExprKind.ADD -> ChirBinaryOperator.ADD.canonicalName
        CHIRExprKind.SUB -> ChirBinaryOperator.SUB.canonicalName
        CHIRExprKind.MUL -> ChirBinaryOperator.MUL.canonicalName
        CHIRExprKind.DIV -> ChirBinaryOperator.SIGNED_DIV.canonicalName
        CHIRExprKind.MOD -> ChirBinaryOperator.SIGNED_REM.canonicalName
        CHIRExprKind.BITAND -> ChirBinaryOperator.BIT_AND.canonicalName
        CHIRExprKind.BITOR -> ChirBinaryOperator.BIT_OR.canonicalName
        CHIRExprKind.BITXOR -> ChirBinaryOperator.BIT_XOR.canonicalName
        CHIRExprKind.LSHIFT -> ChirBinaryOperator.SHIFT_LEFT.canonicalName
        CHIRExprKind.RSHIFT -> ChirBinaryOperator.SIGNED_SHIFT_RIGHT.canonicalName
        CHIRExprKind.EQUAL -> ChirBinaryOperator.EQUAL.canonicalName
        CHIRExprKind.NOTEQUAL -> ChirBinaryOperator.NOT_EQUAL.canonicalName
        CHIRExprKind.LT -> ChirBinaryOperator.SIGNED_LESS.canonicalName
        CHIRExprKind.LE -> ChirBinaryOperator.SIGNED_LESS_OR_EQUAL.canonicalName
        CHIRExprKind.GT -> ChirBinaryOperator.SIGNED_GREATER.canonicalName
        CHIRExprKind.GE -> ChirBinaryOperator.SIGNED_GREATER_OR_EQUAL.canonicalName
        else -> throw ChirSerializationException("cannot decode binary expression kind $kind without operation metadata")
    }

    private data class EncType(val id: UInt, val ref: ChirTypeRef)
    private enum class EncValueKind {
        Function,
        Parameter,
        Block,
        Local,
        Constant,
        Global,
        ImportedFunction,
        ImportedVariable,
    }
    private data class EncValue(
        val id: UInt,
        val kind: EncValueKind,
        val moduleSemanticId: String? = null,
        val moduleName: String? = null,
        val ownerFunctionSemanticId: String? = null,
        val function: ChirFunctionDeclaration? = null,
        val parameter: org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration? = null,
        val block: ChirBlock? = null,
        val value: ChirValue? = null,
    )
    private data class EncExpr(val id: UInt, val parentBlockSemanticId: String, val semanticId: String, val node: Any)

    private class EncodeContext(pkg: ChirPackage) {
        val types = mutableListOf<EncType>()
        val values = mutableListOf<EncValue>()
        val exprs = mutableListOf<EncExpr>()
        private val typeByName = linkedMapOf<String, UInt>()
        private val valueBySemantic = linkedMapOf<String, UInt>()
        private val exprBySemantic = linkedMapOf<String, UInt>()
        init {
            pkg.modules.forEach { m ->
                m.declarations.filterIsInstance<ChirFunctionDeclaration>().forEach { fn ->
                    registerValue(fn.semanticId.value, EncValueKind.Function, m.semanticId.value, m.name, fn.semanticId.value, fn, null, null, null)
                    typeIdOf(fn.returnType)
                    fn.parameters.forEach { p -> registerValue(p.semanticId.value, EncValueKind.Parameter, m.semanticId.value, m.name, fn.semanticId.value, fn, p, null, null); typeIdOf(p.type) }
                    fn.blocks.forEach { b ->
                        registerValue(b.semanticId.value, EncValueKind.Block, m.semanticId.value, m.name, fn.semanticId.value, fn, null, b, null)
                        b.expressions.forEach { x -> registerExpr(x.semanticId.value, b.semanticId.value, x); collectValues(x) }
                        registerExpr(b.terminator.semanticId.value, b.semanticId.value, b.terminator); collectValues(b.terminator)
                    }
                }
            }
        }
        fun typeIdOf(type: ChirTypeRef): UInt = typeByName.getOrPut(type.renderName) { val id = (typeByName.size + 1).toUInt(); types += EncType(id, type); id }
        fun valueIdOf(semantic: String): UInt = valueBySemantic[semantic] ?: throw ChirSerializationException("missing value id: $semantic")
        fun exprIdOf(semantic: String): UInt = exprBySemantic[semantic] ?: throw ChirSerializationException("missing expr id: $semantic")
        private fun registerValue(semantic: String, kind: EncValueKind, moduleSid: String?, moduleName: String?, ownerFn: String?, fn: ChirFunctionDeclaration?, p: org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration?, b: ChirBlock?, value: ChirValue?) {
            if (valueBySemantic.containsKey(semantic)) return
            val id = (valueBySemantic.size + 1).toUInt()
            valueBySemantic[semantic] = id
            values += EncValue(id, kind, moduleSid, moduleName, ownerFn, fn, p, b, value)
        }
        private fun registerExpr(semantic: String, parentBlockSemanticId: String, node: Any) {
            if (exprBySemantic.containsKey(semantic)) return
            val id = (exprBySemantic.size + 1).toUInt()
            exprBySemantic[semantic] = id
            exprs += EncExpr(id, parentBlockSemanticId, semantic, node)
        }
        private fun collectValues(node: Any) {
            val valuesToAdd = when (node) {
                is ChirUnaryExpression -> listOf(node.operand)
                is ChirBinaryExpression -> listOf(node.left, node.right)
                is ChirMemoryExpression -> listOfNotNull(node.address, node.value)
                is ChirCallExpression -> listOf(node.callee) + node.arguments
                is ChirOtherExpression -> node.operands
                is ChirConditionalBranchTerminator -> listOf(node.condition)
                is ChirReturnTerminator -> listOfNotNull(node.returnValue)
                is ChirThrowTerminator -> listOf(node.exceptionValue)
                else -> emptyList()
            }
            valuesToAdd.forEach {
                when (it) {
                    is ChirLocalValue -> registerValue(it.semanticId.value, EncValueKind.Local, null, null, null, null, null, null, it)
                    is ChirConstantValue -> registerValue(it.semanticId.value, EncValueKind.Constant, null, null, null, null, null, null, it)
                    is ChirGlobalValue -> registerValue(it.semanticId.value, EncValueKind.Global, null, null, null, null, null, null, it)
                    is ChirImportedFunctionValue -> registerValue(it.semanticId.value, EncValueKind.ImportedFunction, null, null, null, null, null, null, it)
                    is ChirImportedVariableValue -> registerValue(it.semanticId.value, EncValueKind.ImportedVariable, null, null, null, null, null, null, it)
                    else -> Unit
                }
                typeIdOf(it.type)
            }
            if (node is ChirExpression) node.resultType?.let(::typeIdOf)
        }
    }

    private class DecodedValues {
        val values = linkedMapOf<UInt, ChirValue>()
        val functions = mutableListOf<DecodedFunc>()
        val parameters = linkedMapOf<UInt, DecodedParam>()
        val blocks = linkedMapOf<UInt, DecodedBlock>()
        val blockSemanticByValueId = linkedMapOf<UInt, ChirSemanticId>()
        val functionSemanticByValueId = linkedMapOf<UInt, ChirSemanticId>()
    }
    private data class DecodedFunc(val valueId: UInt, val semanticId: ChirSemanticId, val name: String, val returnType: ChirTypeRef, val moduleSemanticId: ChirSemanticId, val moduleName: String, val paramValueIds: List<UInt>, val entryBlockValueId: UInt)
    private data class DecodedParam(val semanticId: ChirSemanticId, val name: String, val type: ChirTypeRef, val mutable: Boolean)
    private data class DecodedBlock(val semanticId: ChirSemanticId, val name: String, val parentFunctionValueId: UInt, val exprIds: List<UInt>)
    private data class DecodedExprMetadata(val semanticId: String, val operation: String?)

    private fun valueIdentifierParts(identifier: String): List<String> {
        val parts = identifier.split("|", limit = 2)
        if (parts[0].isBlank()) {
            throw ChirSerializationException("damaged payload: value identifier has blank semantic id")
        }
        return parts
    }

    private fun parsePathMetadata(path: String?): Pair<ChirSemanticId, Map<UInt, DecodedExprMetadata>> {
        if (path.isNullOrBlank()) {
            throw ChirSerializationException("damaged payload: package path metadata is missing")
        }
        val split = path.split("|", limit = 2)
        if (split[0].isBlank()) {
            throw ChirSerializationException("damaged payload: package semantic id is blank")
        }
        val pkg = ChirSemanticId(split[0])
        if (split.size == 1 || split[1].isBlank()) return pkg to emptyMap()
        val map = linkedMapOf<UInt, DecodedExprMetadata>()
        split[1].split(";").forEach {
            val parts = it.split("=", limit = 2)
            val id = parts.getOrNull(0)?.toUIntOrNull()
                ?: throw ChirSerializationException("damaged payload: expression metadata id is invalid in '$it'")
            val payload = parts.getOrNull(1)
                ?: throw ChirSerializationException("damaged payload: expression metadata payload is missing for $id")
            val metadataParts = payload.split(",op:", limit = 2)
            val sid = metadataParts[0]
            if (sid.isBlank()) {
                throw ChirSerializationException("damaged payload: expression metadata semantic id is blank for $id")
            }
            val operation = metadataParts.getOrNull(1)
            map[id] = DecodedExprMetadata(sid, operation)
        }
        return pkg to map
    }

    private fun expressionOperationName(node: Any): String? = when (node) {
        is ChirUnaryExpression -> node.operator
        is ChirBinaryExpression -> node.operator
        is ChirMemoryExpression -> node.operation
        is ChirOtherExpression -> node.operation
        else -> null
    }
}


