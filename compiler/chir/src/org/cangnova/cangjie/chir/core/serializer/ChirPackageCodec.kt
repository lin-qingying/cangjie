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

        val exprMeta = ctx.exprs.joinToString(";") { "${it.id}=${it.semanticId}" }
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

        val (packageSemanticId, exprSemantics) = parsePathMetadata(root.path)
        val typeById = decodeTypes(root)
        val valueState = decodeValues(root, typeById)
        val exprById = decodeExpressions(root, typeById, valueState, exprSemantics)

        if (valueState.functions.isEmpty()) {
            throw ChirSerializationException("damaged payload: no functions")
        }

        val modules = linkedMapOf<Pair<String, String>, MutableList<DefaultChirFunctionDeclaration>>()
        for (fn in valueState.functions) {
            val params = fn.paramValueIds.mapNotNull { valueState.parameters[it] }.map {
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
                    val expressions = block.exprIds.mapNotNull { exprById[it] as? ChirExpression }
                    val terminator = block.exprIds.lastOrNull()?.let { exprById[it] as? ChirTerminator }
                        ?: ChirReturnTerminator(ChirSemanticId("term:fallback"))
                    ChirBlock(block.semanticId, block.name, expressions, terminator)
                }

            val declaration = DefaultChirFunctionDeclaration(
                semanticId = fn.semanticId,
                name = fn.name,
                returnType = fn.returnType,
                parameters = params,
                blocks = blocks,
                entryBlockId = valueState.blockSemanticByValueId[fn.entryBlockValueId] ?: ChirSemanticId("block:entry"),
            )

            val key = fn.moduleSemanticId.value to fn.moduleName
            modules.getOrPut(key) { mutableListOf() }.add(declaration)
        }

        return ChirPackage(
            semanticId = packageSemanticId,
            name = root.name ?: "unknown",
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
        2 -> ChirPackageAccessLevel.PUBLIC
        else -> ChirPackageAccessLevel.INTERNAL
    }

    private fun encodeType(builder: FlatBufferBuilder, id: UInt, ref: ChirTypeRef): Pair<UByte, Int> {
        val kind = when ((ref as? ChirResolvedTypeRef)?.type) {
            ChirPrimitiveType.BOOL -> CHIRTypeKind.BOOLEAN
            ChirPrimitiveType.UNIT -> CHIRTypeKind.UNIT
            ChirPrimitiveType.NOTHING -> CHIRTypeKind.NOTHING
            ChirPrimitiveType.FLOAT16 -> CHIRTypeKind.FLOAT16
            ChirPrimitiveType.FLOAT32 -> CHIRTypeKind.FLOAT32
            ChirPrimitiveType.FLOAT64 -> CHIRTypeKind.FLOAT64
            ChirPrimitiveType.INT8 -> CHIRTypeKind.INT8
            ChirPrimitiveType.INT16 -> CHIRTypeKind.INT16
            ChirPrimitiveType.INT32 -> CHIRTypeKind.INT32
            ChirPrimitiveType.INT64 -> CHIRTypeKind.INT64
            ChirPrimitiveType.UINT8 -> CHIRTypeKind.UINT8
            ChirPrimitiveType.UINT16 -> CHIRTypeKind.UINT16
            ChirPrimitiveType.UINT32 -> CHIRTypeKind.UINT32
            ChirPrimitiveType.UINT64 -> CHIRTypeKind.UINT64
            else -> CHIRTypeKind.GENERIC
        }
        val base = Type.createType(builder, kind, id, 0, 0u)
        return when (kind) {
            CHIRTypeKind.BOOLEAN -> TypeElem.BooleanType to BooleanType.createBooleanType(builder, base, 0)
            CHIRTypeKind.UNIT -> TypeElem.UnitType to UnitType.createUnitType(builder, base, 0)
            CHIRTypeKind.NOTHING -> TypeElem.NothingType to NothingType.createNothingType(builder, base, 0)
            CHIRTypeKind.FLOAT16, CHIRTypeKind.FLOAT32, CHIRTypeKind.FLOAT64 -> {
                val num = NumericType.createNumericType(builder, base, 0)
                TypeElem.FloatType to FloatType.createFloatType(builder, num)
            }
            CHIRTypeKind.INT8, CHIRTypeKind.INT16, CHIRTypeKind.INT32, CHIRTypeKind.INT64,
            CHIRTypeKind.UINT8, CHIRTypeKind.UINT16, CHIRTypeKind.UINT32, CHIRTypeKind.UINT64 -> {
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
                val base = Value.createValue(builder, 0, ctx.typeIdOf(cv.type), builder.createString(cv.semanticId.value), ValueKind.LITERAL, v.id, 0u, 0)
                val lit = LiteralValue.createLiteralValue(builder, base, LitConstKind.Integer)
                ValueElem.IntLiteral to IntLiteral.createIntLiteral(builder, lit, cv.literal.toULongOrNull() ?: 0u)
            }
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun encodeExpr(builder: FlatBufferBuilder, e: EncExpr, ctx: EncodeContext): Pair<UByte, Int> {
        val operation = e.node
        return when (operation) {
            is ChirBinaryExpression -> {
                val ops = uintArrayOf(ctx.valueIdOf(operation.left.semanticId.value), ctx.valueIdOf(operation.right.semanticId.value))
                val base = Expression.createExpression(builder, 0, CHIRExprKind.ADD, e.id, Expression.createOperandsVector(builder, ops), 0, ctx.valueIdOf(e.parentBlockSemanticId), 0u, ctx.typeIdOf(operation.resultType))
                ExpressionElem.BinaryExpression to BinaryExpression.createBinaryExpression(builder, base, OverflowStrategy.NA)
            }
            is ChirBranchTerminator -> {
                val baseExpr = Expression.createExpression(builder, 0, CHIRExprKind.GOTO, e.id, 0, 0, ctx.valueIdOf(e.parentBlockSemanticId), 0u, 0u)
                val term = Terminator.createTerminator(builder, baseExpr, Terminator.createSuccessorsVector(builder, uintArrayOf(ctx.valueIdOf(operation.targetBlockId.value))))
                ExpressionElem.GoTo to GoTo.createGoTo(builder, term)
            }
            is ChirReturnTerminator -> {
                val ops = operation.returnValue?.let { uintArrayOf(ctx.valueIdOf(it.semanticId.value)) }
                val baseExpr = Expression.createExpression(builder, 0, CHIRExprKind.EXIT, e.id, ops?.let { Expression.createOperandsVector(builder, it) } ?: 0, 0, ctx.valueIdOf(e.parentBlockSemanticId), 0u, 0u)
                val term = Terminator.createTerminator(builder, baseExpr, 0)
                ExpressionElem.Exit to Exit.createExit(builder, term)
            }
            else -> {
                val baseExpr = Expression.createExpression(builder, 0, CHIRExprKind.DEBUGEXPR, e.id, 0, 0, ctx.valueIdOf(e.parentBlockSemanticId), 0u, 0u)
                ExpressionElem.Debug to Debug.createDebug(builder, baseExpr, builder.createString(operation::class.simpleName ?: "expr"))
            }
        }
    }

    private fun decodeTypes(root: CHIRPackage): Map<UInt, ChirTypeRef> {
        val result = linkedMapOf<UInt, ChirTypeRef>()
        for (i in 0 until root.typesLength) {
            when (root.typesType(i)) {
                TypeElem.IntType -> root.types(IntType(), i)?.let { t ->
                    val base = (t as IntType).base?.base ?: return@let
                    result[base.typeId] = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
                }
                TypeElem.FloatType -> root.types(FloatType(), i)?.let { t ->
                    val base = (t as FloatType).base?.base ?: return@let
                    result[base.typeId] = ChirResolvedTypeRef(ChirPrimitiveType.FLOAT32)
                }
                TypeElem.BooleanType -> root.types(BooleanType(), i)?.let { t -> result[(t as BooleanType).base?.typeId ?: 0u] = ChirResolvedTypeRef(ChirPrimitiveType.BOOL) }
                TypeElem.UnitType -> root.types(UnitType(), i)?.let { t -> result[(t as UnitType).base?.typeId ?: 0u] = ChirResolvedTypeRef(ChirPrimitiveType.UNIT) }
                TypeElem.NothingType -> root.types(NothingType(), i)?.let { t -> result[(t as NothingType).base?.typeId ?: 0u] = ChirResolvedTypeRef(ChirPrimitiveType.NOTHING) }
                TypeElem.GenericType -> root.types(GenericType(), i)?.let { t ->
                    val g = t as GenericType
                    result[g.base?.typeId ?: 0u] = ChirUnresolvedTypeRef(g.identifier ?: "type")
                }
            }
        }
        return result
    }

    private fun decodeValues(root: CHIRPackage, typeById: Map<UInt, ChirTypeRef>): DecodedValues {
        val out = DecodedValues()
        fun typeOf(id: UInt) = typeById[id] ?: ChirUnresolvedTypeRef("type:$id")
        for (i in 0 until root.valuesLength) {
            when (root.valuesType(i)) {
                ValueElem.IntLiteral -> root.values(IntLiteral(), i)?.let { t ->
                    val b = (t as IntLiteral).base?.base ?: return@let
                    out.values[b.valueId] = ChirConstantValue(ChirSemanticId(b.identifier ?: "const:${b.valueId}"), typeOf(b.type), t.val_.toString())
                }
                ValueElem.Parameter -> root.values(Parameter(), i)?.let { t ->
                    val b = (t as Parameter).base ?: return@let
                    val parts = (b.identifier ?: "param:${b.valueId}").split("|", limit = 2)
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
                    val b = f.base ?: return@let
                    out.functionSemanticByValueId[b.valueId] = ChirSemanticId(f.srcCodeIdentifier ?: "fn:${b.valueId}")
                    out.functions += DecodedFunc(b.valueId, ChirSemanticId(f.srcCodeIdentifier ?: "fn:${b.valueId}"), b.identifier ?: "fn", typeOf(b.type), ChirSemanticId(f.parentName ?: "mod:default"), f.packageName ?: "default", List(f.paramsLength) { f.params(it) }, f.body)
                }
                ValueElem.Block -> root.values(Block(), i)?.let { t ->
                    val b = t as Block
                    val base = b.base ?: return@let
                    val parts = (base.identifier ?: "block:${base.valueId}").split("|", limit = 2)
                    val sid = ChirSemanticId(parts[0])
                    out.blocks[base.valueId] = DecodedBlock(sid, parts.getOrElse(1) { "block" }, b.parentGroup, List(b.exprsLength) { b.exprs(it) })
                    out.blockSemanticByValueId[base.valueId] = sid
                }
                ValueElem.LocalVar -> root.values(LocalVar(), i)?.let { t ->
                    val b = (t as LocalVar).base ?: return@let
                    val parts = (b.identifier ?: "local:${b.valueId}").split("|", limit = 2)
                    out.values[b.valueId] = ChirLocalValue(ChirSemanticId(parts[0]), typeOf(b.type), parts.getOrElse(1) { parts[0] })
                }
            }
        }
        return out
    }

    private fun decodeExpressions(root: CHIRPackage, typeById: Map<UInt, ChirTypeRef>, values: DecodedValues, exprSemantics: Map<UInt, String>): Map<UInt, Any> {
        val out = linkedMapOf<UInt, Any>()
        fun typeOf(id: UInt) = typeById[id] ?: ChirUnresolvedTypeRef("type:$id")
        fun operands(base: Expression) = List(base.operandsLength) { values.values[base.operands(it)] ?: ChirLocalValue(ChirSemanticId("missing:${base.operands(it)}"), unitType, "missing") }
        fun exprSid(id: UInt) = ChirSemanticId(exprSemantics[id] ?: "expr:$id")
        fun termSid(id: UInt) = ChirSemanticId(exprSemantics[id] ?: "term:$id")
        for (i in 0 until root.exprsLength) {
            when (root.exprsType(i)) {
                ExpressionElem.BinaryExpression -> root.exprs(BinaryExpression(), i)?.let { t ->
                    val base = (t as BinaryExpression).base ?: return@let
                    val ops = operands(base)
                    if (ops.size >= 2) out[base.expressionId] = ChirBinaryExpression(exprSid(base.expressionId), "add", ops[0], ops[1], typeOf(base.resultTy))
                }
                ExpressionElem.GoTo -> root.exprs(GoTo(), i)?.let { t ->
                    val term = (t as GoTo).base ?: return@let
                    val target = values.blockSemanticByValueId[term.successors(0)] ?: ChirSemanticId("block:unknown")
                    out[term.base?.expressionId ?: 0u] = ChirBranchTerminator(termSid(term.base?.expressionId ?: 0u), target)
                }
                ExpressionElem.Exit -> root.exprs(Exit(), i)?.let { t ->
                    val term = (t as Exit).base ?: return@let
                    val base = term.base ?: return@let
                    val ret = if (base.operandsLength > 0) values.values[base.operands(0)] else null
                    out[base.expressionId] = ChirReturnTerminator(termSid(base.expressionId), ret)
                }
                ExpressionElem.Debug -> root.exprs(Debug(), i)?.let { t ->
                    val base = (t as Debug).base ?: return@let
                    out[base.expressionId] = ChirOtherExpression(exprSid(base.expressionId), t.srcCodeIdentifier ?: "debug", emptyList(), null)
                }
            }
        }
        return out
    }

    private data class EncType(val id: UInt, val ref: ChirTypeRef)
    private enum class EncValueKind { Function, Parameter, Block, Local, Constant }
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

    private fun parsePathMetadata(path: String?): Pair<ChirSemanticId, Map<UInt, String>> {
        if (path.isNullOrBlank()) return ChirSemanticId("pkg:unknown") to emptyMap()
        val split = path.split("|", limit = 2)
        val pkg = ChirSemanticId(split[0])
        if (split.size == 1 || split[1].isBlank()) return pkg to emptyMap()
        val map = linkedMapOf<UInt, String>()
        split[1].split(";").forEach {
            val parts = it.split("=", limit = 2)
            val id = parts.getOrNull(0)?.toUIntOrNull() ?: return@forEach
            val sid = parts.getOrNull(1) ?: return@forEach
            map[id] = sid
        }
        return pkg to map
    }
}


