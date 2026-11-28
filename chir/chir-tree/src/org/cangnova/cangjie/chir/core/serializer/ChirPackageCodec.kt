package org.cangnova.cangjie.chir.core.serializer

import PackageFormat.*
import com.google.flatbuffers.FlatBufferBuilder
import org.cangnova.cangjie.chir.core.controlflow.*
import org.cangnova.cangjie.chir.core.declaration.ChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration
import org.cangnova.cangjie.chir.core.expression.*
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.model.ChirPackageAccessLevel
import org.cangnova.cangjie.chir.core.model.ChirPackageMembers
import org.cangnova.cangjie.chir.core.model.allDeclarations
import org.cangnova.cangjie.chir.core.type.*
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirConstantKind
import org.cangnova.cangjie.chir.core.value.ChirGlobalValue
import org.cangnova.cangjie.chir.core.value.ChirImportedFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirImportedVariableValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.chir.core.value.ChirParameterValue
import org.cangnova.cangjie.chir.core.value.ChirValue
import java.nio.ByteBuffer

/**
 * CHIR 包 FlatBuffers 编解码器。
 *
 * 该对象负责在项目内部 CHIR 模型和 `PackageFormat` 生成结构之间转换，并保留必要的
 * 语义标识与操作名元数据以支持往返校验。
 */
object ChirPackageCodec {
    private const val DEBUG_METADATA_OPERATION = "internal.official.debug-expression"

    /**
     * 模块元数据在附加字符串中的前缀。
     */
    private const val MODULE_META_PREFIX = "mod::"

    /**
     * 基本块名称元数据在附加字符串中的前缀。
     */
    private const val BLOCK_NAME_PREFIX = "block_name::"

    /**
     * FlatBuffers 中表示无值结果的 Unit 类型引用。
     */
    private val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)

    /**
     * FlatBuffers 中布尔字面量必须保留布尔类型，而不是将其降格为整数。
     */
    private val booleanType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)

    /** 浮点值共享官方 FloatLiteral 编码，同时保留声明精度类型。 */
    private val floatingLiteralTypes = setOf(
        ChirPrimitiveType.FLOAT16,
        ChirPrimitiveType.FLOAT32,
        ChirPrimitiveType.FLOAT64,
    )

    /** 官方 rune 字面量以 uint32 码点编码。 */
    private val runeType = ChirResolvedTypeRef(ChirPrimitiveType.RUNE)

    /**
     * 将 [chirPackage] 序列化为当前 CHIR FlatBuffers schema 的二进制 payload。
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    fun serialize(chirPackage: ChirPackage): ByteArray {
        val builder = FlatBufferBuilder(4096)
        val ctx = EncodeContext(chirPackage)

        val typeTags = UByteArray(ctx.types.size)
        val typeOffsets = IntArray(ctx.types.size)
        ctx.types.forEachIndexed { index, t ->
            val built = encodeType(builder, t.id, t.ref, ctx)
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

        val definitions = encodeDefinitions(builder, ctx)
        val definitionTags = UByteArray(definitions.size) { definitions[it].first }
        val definitionOffsets = IntArray(definitions.size) { definitions[it].second }

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
            defsTypeOffset = if (definitionTags.isEmpty()) 0 else CHIRPackage.createDefsTypeVector(builder, definitionTags),
            defsOffset = if (definitionOffsets.isEmpty()) 0 else CHIRPackage.createDefsVector(builder, definitionOffsets),
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

    /**
     * 从当前 CHIR FlatBuffers schema 的二进制 payload 反序列化 CHIR 包。
     */
    fun deserialize(bytes: ByteArray): ChirPackage {
        val root = CHIRPackage.getRootAsCHIRPackage(ByteBuffer.wrap(bytes))
        if (root.phase.toInt() != ChirSerializationSchema.CURRENT_VERSION) {
            throw ChirSerializationException("unsupported schema version: ${root.phase}")
        }

        val (packageSemanticId, exprMetadata) = parsePathMetadata(root.path)
        val typeById = decodeTypes(root)
        val valueState = decodeValues(root, typeById, exprMetadata)
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
                    val lastExpressionId = block.exprIds.lastOrNull()
                    val terminatorId = lastExpressionId?.takeIf { exprById[it] is ChirTerminator }
                    val expressionIds = if (terminatorId == null) block.exprIds else block.exprIds.dropLast(1)
                    val expressions = expressionIds.map { exprId ->
                        exprById[exprId] as? ChirExpression
                            ?: throw ChirSerializationException("damaged payload: expression $exprId is missing or is not a CHIR expression in block ${block.semanticId.value}")
                    }
                    val terminator = terminatorId?.let { exprById[it] as ChirTerminator }
                    ChirBlock(block.semanticId, block.name, expressions, terminator)
                }

            val declaration = DefaultChirFunctionDeclaration(
                semanticId = fn.semanticId,
                name = fn.name,
                returnType = fn.returnType,
                parameters = params,
                blocks = blocks,
                entryBlockId = if (fn.entryBlockValueId == 0u) {
                    null
                } else {
                    valueState.blockSemanticByValueId[fn.entryBlockValueId]
                        ?: throw ChirSerializationException("damaged payload: function ${fn.semanticId.value} references missing entry block value ${fn.entryBlockValueId}")
                },
                hasBody = fn.entryBlockValueId != 0u,
                genericTypeParameters = fn.genericTypeParameters,
                isStatic = fn.isStatic,
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

    /**
     * 将包访问级别编码为 FlatBuffers 中的无符号字节值。
     */
    private fun encodeAccessLevel(accessLevel: ChirPackageAccessLevel): UByte = when (accessLevel) {
        ChirPackageAccessLevel.PRIVATE -> 0u
        ChirPackageAccessLevel.INTERNAL -> 1u
        ChirPackageAccessLevel.PUBLIC -> 2u
    }

    /**
     * 将 FlatBuffers 中的访问级别字节解码为 CHIR 包访问级别。
     */
    private fun decodeAccessLevel(encoded: UByte): ChirPackageAccessLevel = when (encoded.toInt()) {
        0 -> ChirPackageAccessLevel.PRIVATE
        1 -> ChirPackageAccessLevel.INTERNAL
        2 -> ChirPackageAccessLevel.PUBLIC
        else -> throw ChirSerializationException("unsupported package access level: $encoded")
    }

    /**
     * 编码单个类型引用并返回 union tag 与 table offset。
     */
    private fun encodeType(
        builder: FlatBufferBuilder,
        id: UInt,
        ref: ChirTypeRef,
        ctx: EncodeContext,
    ): Pair<UByte, Int> {
        val type = (ref as? ChirResolvedTypeRef)?.type
        ctx.enumConstructorOwnerTypeId(id)?.let { ownerTypeId ->
            val functionType = type as? ChirFunctionType
                ?: throw ChirSerializationException("enum constructor type #$id is not a function type")
            val base = encodeTypeBaseWithIds(
                builder,
                CHIRTypeKind.FUNC,
                id,
                functionType.parameterTypes.map(ctx::typeIdOf) + ownerTypeId,
            )
            val receiverType = functionType.receiverType?.let(ctx::typeIdOf) ?: 0u
            return TypeElem.FuncType to FuncType.createFuncType(
                builder,
                base,
                functionType.isCFunctionType,
                functionType.hasVarArg,
                receiverType,
                functionType.throws,
            )
        }
        val primitiveKind = when (type) {
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
            else -> null
        }
        if (primitiveKind != null) {
            val base = encodeTypeBase(builder, primitiveKind, id, emptyList(), ctx)
            return when (primitiveKind) {
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
            else -> error("unreachable CHIR primitive type kind: $primitiveKind")
            }
        }

        return when (type) {
            is ChirPrimitiveType -> throw ChirSerializationException(
                "unsupported primitive type kind: ${type.kind}",
            )

            is ChirTupleType -> {
                val base = encodeTypeBase(builder, CHIRTypeKind.TUPLE, id, type.elementTypes, ctx)
                TypeElem.TupleType to TupleType.createTupleType(builder, base)
            }

            is ChirRawArrayType -> {
                val base = encodeTypeBase(builder, CHIRTypeKind.RAWARRAY, id, listOf(type.elementType), ctx)
                TypeElem.RawArrayType to RawArrayType.createRawArrayType(builder, base, type.dimensions)
            }

            is ChirVArrayType -> {
                val base = encodeTypeBase(builder, CHIRTypeKind.VARRAY, id, listOf(type.elementType), ctx)
                TypeElem.VArrayType to VArrayType.createVArrayType(builder, base, type.size)
            }

            is ChirFunctionType -> {
                val base = encodeTypeBase(builder, CHIRTypeKind.FUNC, id, type.parameterTypes + type.returnType, ctx)
                val receiverType = type.receiverType?.let(ctx::typeIdOf) ?: 0u
                TypeElem.FuncType to FuncType.createFuncType(
                    builder,
                    base,
                    type.isCFunctionType,
                    type.hasVarArg,
                    receiverType,
                    type.throws,
                )
            }

            is ChirCStringType -> {
                val base = encodeTypeBase(builder, CHIRTypeKind.C_STRING, id, emptyList(), ctx)
                TypeElem.CStringType to CStringType.createCStringType(builder, base, 0)
            }

            is ChirCPointerType -> {
                val base = encodeTypeBase(builder, CHIRTypeKind.C_POINTER, id, listOf(type.pointeeType), ctx)
                TypeElem.CPointerType to CPointerType.createCPointerType(builder, base, 0)
            }

            is ChirGenericType -> encodeGenericType(builder, id, ref, type.semanticIdentity, type.identifier, type.upperBounds, ctx)
            is ChirRefType -> {
                val base = encodeTypeBase(builder, CHIRTypeKind.REFTYPE, id, listOf(type.referencedType), ctx)
                TypeElem.RefType to RefType.createRefType(builder, base, type.mutable)
            }

            is ChirBoxType -> {
                val base = encodeTypeBase(builder, CHIRTypeKind.BOXTYPE, id, listOf(type.boxedType), ctx)
                TypeElem.BoxType to BoxType.createBoxType(builder, base)
            }

            is ChirThisType -> {
                val base = encodeTypeBase(builder, CHIRTypeKind.THIS, id, emptyList(), ctx)
                TypeElem.ThisType to ThisType.createThisType(builder, base, builder.createString(type.ownerTypeName))
            }

            is ChirStructType -> {
                val base = encodeTypeBase(builder, CHIRTypeKind.STRUCT, id, type.typeArguments, ctx)
                val custom = encodeCustomType(
                    builder = builder,
                    baseOffset = base,
                    definitionId = ctx.nominalDefinitionId(id),
                    instantiatedMemberTypes = type.fieldTypes,
                    implementedInterfaceTypes = emptyList(),
                    ctx = ctx,
                )
                TypeElem.StructType to StructType.createStructType(builder, custom)
            }

            is ChirClassType -> {
                val base = encodeTypeBase(builder, CHIRTypeKind.CLASS, id, type.typeArguments, ctx)
                val superclass = type.superTypes.firstOrNull()?.let(ctx::typeIdOf) ?: 0u
                val custom = encodeCustomType(
                    builder = builder,
                    baseOffset = base,
                    definitionId = ctx.nominalDefinitionId(id),
                    instantiatedMemberTypes = type.fieldTypes,
                    implementedInterfaceTypes = type.superTypes.drop(1),
                    ctx = ctx,
                )
                val encoded = ClassType.createClassType(builder, custom, superclass, superclass != 0u)
                TypeElem.ClassType to encoded
            }

            is ChirEnumType -> {
                val base = encodeTypeBase(builder, CHIRTypeKind.ENUM, id, type.typeArguments, ctx)
                val custom = encodeCustomType(
                    builder = builder,
                    baseOffset = base,
                    definitionId = ctx.nominalDefinitionId(id),
                    instantiatedMemberTypes = emptyList(),
                    implementedInterfaceTypes = emptyList(),
                    ctx = ctx,
                )
                TypeElem.EnumType to EnumType.createEnumType(builder, custom, false)
            }

            is ChirNamedType -> throw ChirSerializationException(
                "named type '${type.name}' has no official CHIR nominal kind or layout and cannot be serialized losslessly",
            )

            null -> encodeGenericType(builder, id, ref, ref.renderName, ref.renderName, emptyList(), ctx)
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun encodeTypeBase(
        builder: FlatBufferBuilder,
        kind: UByte,
        id: UInt,
        arguments: List<ChirTypeRef>,
        ctx: EncodeContext,
    ): Int = encodeTypeBaseWithIds(builder, kind, id, arguments.map(ctx::typeIdOf))

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun encodeTypeBaseWithIds(
        builder: FlatBufferBuilder,
        kind: UByte,
        id: UInt,
        argumentIds: List<UInt>,
    ): Int {
        val encodedArgumentIds = argumentIds.toUIntArray()
        val argumentOffset = if (encodedArgumentIds.isEmpty()) 0 else Type.createArgTysVector(builder, encodedArgumentIds)
        return Type.createType(builder, kind, id, argumentOffset, 0u)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun encodeCustomType(
        builder: FlatBufferBuilder,
        baseOffset: Int,
        definitionId: UInt,
        instantiatedMemberTypes: List<ChirTypeRef>,
        implementedInterfaceTypes: List<ChirTypeRef>,
        ctx: EncodeContext,
    ): Int {
        val memberTypeIds = instantiatedMemberTypes.map(ctx::typeIdOf).toUIntArray()
        val implementedInterfaceTypeIds = implementedInterfaceTypes.map(ctx::typeIdOf).toUIntArray()
        val memberTypesOffset = if (memberTypeIds.isEmpty()) {
            0
        } else {
            CustomType.createInstantiatedMemberTysVector(builder, memberTypeIds)
        }
        val interfaceTypesOffset = if (implementedInterfaceTypeIds.isEmpty()) {
            0
        } else {
            CustomType.createImplementedInterfaceTysVector(builder, implementedInterfaceTypeIds)
        }
        return CustomType.createCustomType(
            builder,
            baseOffset,
            interfaceTypesOffset,
            memberTypesOffset,
            definitionId,
            hasSetSuperInterface = implementedInterfaceTypeIds.isNotEmpty(),
            hasSetInstMemberTy = memberTypeIds.isNotEmpty(),
        )
    }

    /**
     * 为每个具名聚合类型写入 CHIR 定义表；类型表通过同一编号关联回这些定义。
     */
    private fun encodeDefinitions(builder: FlatBufferBuilder, ctx: EncodeContext): List<Pair<UByte, Int>> =
        ctx.nominalTypes.map { nominal ->
            val customType = nominal.type
            val declarationId = when (customType) {
                is ChirStructType -> customType.declarationId
                is ChirClassType -> customType.declarationId
                is ChirEnumType -> customType.declarationId
                else -> throw ChirSerializationException("unsupported nominal type ${customType::class.simpleName}")
            }
            val name = declarationId.qualifiedName
            val baseOffset = Base.createBase(builder, 0, 0)
            val customDefinition = CustomTypeDef.createCustomTypeDef(
                builder = builder,
                baseOffset = baseOffset,
                kind = when (customType) {
                    is ChirStructType -> CustomDefKind.STRUCT
                    is ChirClassType -> CustomDefKind.CLASS
                    is ChirEnumType -> CustomDefKind.ENUM
                    else -> throw ChirSerializationException("unsupported nominal type ${customType::class.simpleName}")
                },
                customTypeDefId = nominal.definitionId,
                srcCodeIdentifierOffset = builder.createString(declarationId.declarationName),
                identifierOffset = builder.createString(name),
                packageNameOffset = builder.createString(declarationId.packageName),
                type = nominal.typeId,
                genericDecl = 0u,
                methodsOffset = 0,
                implementedInterfacesOffset = 0,
                instanceMemberVarsOffset = 0,
                staticMemberVarsOffset = 0,
                attributes = 0uL,
                annoInfoOffset = 0,
                vtableOffset = 0,
                extendsOffset = 0,
                varInitializationFunc = 0u,
            )
            when (customType) {
                is ChirStructType -> CustomTypeDefElem.StructDef to
                    StructDef.createStructDef(builder, customDefinition, false)
                is ChirClassType -> CustomTypeDefElem.ClassDef to ClassDef.createClassDef(
                    builder,
                    customDefinition,
                    ctx.classDefKind(customType.name),
                    false,
                    customType.superTypes.firstOrNull()?.let(ctx::typeIdOf) ?: 0u,
                    0,
                    0,
                )
                is ChirEnumType -> {
                    val constructors = customType.cases.mapIndexed { caseIndex, enumCase ->
                        EnumCtorInfo.createEnumCtorInfo(
                            builder,
                            builder.createString(enumCase.name),
                            builder.createString(enumCase.name),
                            ctx.enumConstructorTypeId(nominal.definitionId, caseIndex),
                        )
                    }
                    val constructorsOffset = if (constructors.isEmpty()) {
                        0
                    } else {
                        EnumDef.createCtorsVector(builder, constructors.toIntArray())
                    }
                    CustomTypeDefElem.EnumDef to EnumDef.createEnumDef(
                        builder,
                        customDefinition,
                        constructorsOffset,
                        customType.nonExhaustive,
                    )
                }
                else -> throw ChirSerializationException("unsupported nominal type ${customType::class.simpleName}")
            }
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun encodeGenericType(
        builder: FlatBufferBuilder,
        id: UInt,
        ref: ChirTypeRef,
        identifier: String,
        sourceIdentifier: String,
        upperBounds: List<ChirTypeRef>,
        ctx: EncodeContext,
    ): Pair<UByte, Int> {
        val base = encodeTypeBase(builder, CHIRTypeKind.GENERIC, id, emptyList(), ctx)
        val upperBoundIds = upperBounds.map(ctx::typeIdOf).toUIntArray()
        val upperBoundsOffset = if (upperBoundIds.isEmpty()) 0 else GenericType.createUpperBoundsVector(builder, upperBoundIds)
        return TypeElem.GenericType to GenericType.createGenericType(
                    builder,
                    base,
                    false,
                    false,
                    builder.createString(identifier.ifBlank { ref.renderName }),
                    builder.createString(sourceIdentifier.ifBlank { ref.renderName }),
                    upperBoundsOffset,
                )
    }

    /**
     * 编码单个值节点并返回 union tag 与 table offset。
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    private fun encodeValue(builder: FlatBufferBuilder, v: EncValue, ctx: EncodeContext): Pair<UByte, Int> {
        return when (v.kind) {
            EncValueKind.Function -> {
                val fn = v.function!!
                val attributes = if (fn.isStatic) OfficialChirV113Attribute.STATIC.mask else 0uL
                val base = Value.createValue(
                    builder,
                    0,
                    ctx.typeIdOf(fn.returnType),
                    builder.createString(fn.name),
                    ValueKind.FUNC,
                    v.id,
                    attributes,
                    0,
                )
                val params = Func.createParamsVector(builder, fn.parameters.map { ctx.valueIdOf(it.semanticId.value) }.toUIntArray())
                val genericTypeParameterIds = fn.genericTypeParameters.map(ctx::typeIdOf).toUIntArray()
                val genericTypeParameters = if (genericTypeParameterIds.isEmpty()) {
                    0
                } else {
                    Func.createGenericTypeParamsVector(builder, genericTypeParameterIds)
                }
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
                    genericTypeParameters,
                    0u,
                    if (fn.hasBody) {
                        ctx.valueIdOf(requireNotNull(fn.entryBlockId) { "bodyful function ${fn.semanticId} has no entry block" }.value)
                    } else {
                        0u
                    },
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
                val exprIds = b.expressions.map { ctx.exprIdOf(it.semanticId.value) } +
                    listOfNotNull(b.terminator?.let { ctx.exprIdOf(it.semanticId.value) })
                val base = Value.createValue(builder, 0, ctx.typeIdOf(unitType), builder.createString(idAndName), ValueKind.BLOCK, v.id, 0u, 0)
                ValueElem.Block to Block.createBlock(builder, base, ctx.valueIdOf(v.ownerFunctionSemanticId!!), Block.createExprsVector(builder, exprIds.toUIntArray()), 0, false, 0)
            }
            EncValueKind.Local -> {
                val lv = v.value as ChirLocalValue
                val idAndName = "${lv.semanticId.value}|${lv.name}"
                val base = Value.createValue(builder, 0, ctx.typeIdOf(lv.type), builder.createString(idAndName), ValueKind.LOCALVAR, v.id, 0u, 0)
                val associatedExpression = lv.associatedExpressionId?.let { ctx.exprIdOf(it.value) } ?: 0u
                ValueElem.LocalVar to LocalVar.createLocalVar(builder, base, associatedExpression, lv.isReturnValue)
            }
            EncValueKind.Constant -> {
                val cv = v.value as ChirConstantValue
                val base = Value.createValue(builder, 0, ctx.typeIdOf(cv.type), builder.createString(cv.semanticId.value), ValueKind.LITERAL, v.id, 0u, 0)
                if (cv.constantKind == ChirConstantKind.NULL) {
                    val literal = LiteralValue.createLiteralValue(builder, base, ConstantValueKind.NULL)
                    return ValueElem.NullLiteral to NullLiteral.createNullLiteral(builder, literal)
                }
                if (cv.type == unitType && cv.literal == "unit") {
                    val literal = LiteralValue.createLiteralValue(builder, base, ConstantValueKind.UNIT)
                    ValueElem.UnitLiteral to UnitLiteral.createUnitLiteral(builder, literal)
                } else {
                    if (cv.type == unitType) {
                        throw ChirSerializationException("unsupported Unit literal for codec: '${cv.literal}'")
                    }
                    if (cv.type == booleanType) {
                        val value = when (cv.literal) {
                            "1", "true" -> true
                            "0", "false" -> false
                            else -> null
                        }
                            ?: throw ChirSerializationException("unsupported Bool literal for codec: '${cv.literal}'")
                        val literal = LiteralValue.createLiteralValue(builder, base, ConstantValueKind.BOOL)
                        return ValueElem.BoolLiteral to BoolLiteral.createBoolLiteral(builder, literal, value)
                    }
                    if ((cv.type as? ChirResolvedTypeRef)?.type == ChirCStringType) {
                        val literal = LiteralValue.createLiteralValue(builder, base, ConstantValueKind.STRING)
                        return ValueElem.StringLiteral to StringLiteral.createStringLiteral(
                            builder,
                            literal,
                            builder.createString(cv.literal),
                        )
                    }
                    val primitiveType = (cv.type as? ChirResolvedTypeRef)?.type as? ChirPrimitiveType
                    if (primitiveType in floatingLiteralTypes) {
                        val value = cv.literal.toDoubleOrNull()
                            ?: throw ChirSerializationException("unsupported floating-point literal for codec: '${cv.literal}'")
                        val literal = LiteralValue.createLiteralValue(builder, base, ConstantValueKind.FLOAT)
                        return ValueElem.FloatLiteral to FloatLiteral.createFloatLiteral(builder, literal, value)
                    }
                    if (primitiveType == ChirPrimitiveType.RUNE) {
                        val value = cv.literal.toUIntOrNull()
                            ?: throw ChirSerializationException("unsupported rune literal for codec: '${cv.literal}'")
                        val literal = LiteralValue.createLiteralValue(builder, base, ConstantValueKind.RUNE)
                        return ValueElem.RuneLiteral to RuneLiteral.createRuneLiteral(builder, literal, value)
                    }
                    val value = cv.literal.toULongOrNull()
                        ?: throw ChirSerializationException("unsupported integer literal for codec: '${cv.literal}'")
                    val literal = LiteralValue.createLiteralValue(builder, base, ConstantValueKind.INT)
                    ValueElem.IntLiteral to IntLiteral.createIntLiteral(builder, literal, value)
                }
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

    /**
     * 编码单个表达式或终结指令节点并返回 union tag 与 table offset。
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    private fun encodeExpr(builder: FlatBufferBuilder, e: EncExpr, ctx: EncodeContext): Pair<UByte, Int> {
        val operation = e.node
        return when (operation) {
            is ChirUnaryExpression -> {
                val operator = ChirUnaryOperator.parse(operation.operator)
                    ?: throw ChirSerializationException("unsupported unary operator: ${operation.operator}")
                val ops = uintArrayOf(ctx.valueIdOf(operation.operand.semanticId.value))
                val base = encodeBaseExpression(builder, e, ctx, encodeUnaryExprKind(operator), ops, ctx.typeIdOf(operation.resultType))
                ExpressionElem.UnaryExpression to UnaryExpression.createUnaryExpression(
                    builder,
                    base,
                    encodeOverflowStrategy(operation.overflowStrategy),
                )
            }
            is ChirBinaryExpression -> {
                val operator = ChirBinaryOperator.parse(operation.operator)
                    ?: throw ChirSerializationException("unsupported binary operator: ${operation.operator}")
                val ops = uintArrayOf(ctx.valueIdOf(operation.left.semanticId.value), ctx.valueIdOf(operation.right.semanticId.value))
                val base = encodeBaseExpression(builder, e, ctx, encodeBinaryExprKind(operator), ops, ctx.typeIdOf(operation.resultType))
                ExpressionElem.BinaryExpression to BinaryExpression.createBinaryExpression(
                    builder,
                    base,
                    encodeOverflowStrategy(operation.overflowStrategy),
                )
            }
            is ChirMemoryExpression -> {
                val memoryOperation = ChirMemoryOperation.parse(operation.operation)
                    ?: throw ChirSerializationException("unsupported memory operation: ${operation.operation}")
                val operands = when (memoryOperation) {
                    ChirMemoryOperation.STORE_ELEMENT_BY_NAME -> listOfNotNull(operation.value, operation.address)
                    else -> listOfNotNull(operation.address, operation.value)
                }
                val ops = operands
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
                    ChirMemoryOperation.GET_ELEMENT_BY_NAME -> {
                        val names = operation.memberNames.map(builder::createString).toIntArray()
                        val nameVector = GetElementByName.createNamesVector(builder, names)
                        ExpressionElem.GetElementByName to GetElementByName.createGetElementByName(builder, base, nameVector)
                    }
                    ChirMemoryOperation.STORE_ELEMENT_BY_NAME -> {
                        val names = operation.memberNames.map(builder::createString).toIntArray()
                        val nameVector = StoreElementByName.createNamesVector(builder, names)
                        ExpressionElem.StoreElementByName to StoreElementByName.createStoreElementByName(builder, base, nameVector)
                    }
                }
            }
            is ChirCallExpression -> {
                val encodedTarget = if (operation.virtualMethodContext == null) {
                    if (operation.dispatchReceiver != null) {
                        throw ChirSerializationException("direct APPLY ${operation.semanticId} carries a dispatch receiver")
                    }
                    operation.callee
                } else {
                    operation.dispatchReceiver
                        ?: throw ChirSerializationException("virtual INVOKE ${operation.semanticId} has no dispatch receiver")
                }
                val ops = (listOf(encodedTarget) + operation.arguments)
                    .map { ctx.valueIdOf(it.semanticId.value) }
                    .toUIntArray()
                val kind = if (operation.virtualMethodContext == null) CHIRExprKind.APPLY else CHIRExprKind.INVOKE
                val base = encodeBaseExpression(builder, e, ctx, kind, ops, ctx.typeIdOf(operation.resultType))
                val instantiatedTypeArguments = operation.instantiatedTypeArguments
                    .map(ctx::typeIdOf)
                    .toUIntArray()
                val thisType = operation.thisType?.let(ctx::typeIdOf) ?: 0u
                val virtualMethodContext = operation.virtualMethodContext
                if (virtualMethodContext == null) {
                    val instantiatedTypeArgumentsOffset = if (instantiatedTypeArguments.isEmpty()) {
                        0
                    } else {
                        Apply.createInstantiatedTypeArgsVector(builder, instantiatedTypeArguments)
                    }
                    ExpressionElem.Apply to Apply.createApply(
                        builder,
                        base,
                        operation.isSuperCall,
                        instantiatedTypeArgumentsOffset,
                        thisType,
                    )
                } else {
                    val instantiatedTypeArgumentsOffset = if (instantiatedTypeArguments.isEmpty()) {
                        0
                    } else {
                        Invoke.createInstantiatedTypeArgsVector(builder, instantiatedTypeArguments)
                    }
                    val genericTypeParameters = virtualMethodContext.genericTypeParameters
                        .map(ctx::typeIdOf)
                        .toUIntArray()
                    val genericTypeParametersOffset = if (genericTypeParameters.isEmpty()) {
                        0
                    } else {
                        VirMethodContext.createGenericTypeParamsVector(builder, genericTypeParameters)
                    }
                    val sourceCodeIdentifierOffset = builder.createString(virtualMethodContext.sourceCodeIdentifier)
                    val methodContextOffset = VirMethodContext.createVirMethodContext(
                        builder,
                        sourceCodeIdentifierOffset,
                        ctx.typeIdOf(ChirResolvedTypeRef(virtualMethodContext.originalFunctionType)),
                        genericTypeParametersOffset,
                    )
                    ExpressionElem.Invoke to Invoke.createInvoke(
                        builder,
                        base,
                        instantiatedTypeArgumentsOffset,
                        thisType,
                        methodContextOffset,
                    )
                }
            }
            is ChirTupleExpression -> {
                val ops = operation.elements.map { ctx.valueIdOf(it.semanticId.value) }.toUIntArray()
                val resultType = ctx.typeIdOf(operation.resultType)
                val base = encodeBaseExpression(builder, e, ctx, CHIRExprKind.TUPLE, ops, resultType)
                ExpressionElem.Tuple to Tuple.createTuple(builder, base)
            }
            is ChirFieldExpression -> {
                val operands = uintArrayOf(ctx.valueIdOf(operation.base.semanticId.value))
                val resultType = ctx.typeIdOf(operation.resultType)
                val base = encodeBaseExpression(builder, e, ctx, CHIRExprKind.FIELD, operands, resultType)
                val path = Field.createPathVector(builder, operation.path.toULongArray())
                ExpressionElem.Field to Field.createField(builder, base, path)
            }
            is ChirConstantExpression -> {
                val operands = uintArrayOf(ctx.valueIdOf(operation.value.semanticId.value))
                val resultType = ctx.typeIdOf(operation.resultType)
                val base = encodeBaseExpression(builder, e, ctx, CHIRExprKind.CONSTANT, operands, resultType)
                ExpressionElem.Constant to Constant.createConstant(builder, base)
            }
            is ChirOtherExpression -> {
                operation.debugMetadata?.let { debugMetadata ->
                    val ops = operation.operands.map { ctx.valueIdOf(it.semanticId.value) }.toUIntArray()
                    val resultType = operation.resultType?.let(ctx::typeIdOf) ?: 0u
                    val base = encodeBaseExpression(builder, e, ctx, CHIRExprKind.DEBUGEXPR, ops, resultType)
                    val accurateEnvironmentObjectType = debugMetadata.accurateEnvironmentObjectType
                        ?.let(ctx::typeIdOf)
                        ?: 0u
                    return ExpressionElem.Debug to Debug.createDebug(
                        builder,
                        base,
                        builder.createString(debugMetadata.sourceCodeIdentifier),
                        accurateEnvironmentObjectType,
                    )
                }
                operation.intrinsicKind?.let { intrinsicKind ->
                    val ops = operation.operands.map { ctx.valueIdOf(it.semanticId.value) }.toUIntArray()
                    val resultType = operation.resultType?.let(ctx::typeIdOf) ?: 0u
                    val base = encodeBaseExpression(builder, e, ctx, CHIRExprKind.INTRINSIC, ops, resultType)
                    val typeArguments = operation.intrinsicTypeArguments.map(ctx::typeIdOf).toUIntArray()
                    val typeArgumentsOffset = if (typeArguments.isEmpty()) {
                        0
                    } else {
                        Intrinsic.createInstantiatedTypeArgsVector(builder, typeArguments)
                    }
                    return ExpressionElem.Intrinsic to Intrinsic.createIntrinsic(
                        builder,
                        base,
                        encodeIntrinsicKind(intrinsicKind),
                        typeArgumentsOffset,
                    )
                }
                val otherOperation = ChirOtherOperation.parse(operation.operation)
                    ?: throw ChirSerializationException("unsupported other operation: ${operation.operation}")
                val ops = operation.operands.map { ctx.valueIdOf(it.semanticId.value) }.toUIntArray()
                val resultType = operation.resultType?.let(ctx::typeIdOf) ?: 0u
                val base = encodeBaseExpression(builder, e, ctx, encodeOtherExprKind(otherOperation), ops, resultType)
                when (otherOperation) {
                    ChirOtherOperation.RAW_ARRAY_ALLOCATE -> {
                        val allocatedType = operation.resultType ?: operation.resultLocalValue?.type
                            ?: throw ChirSerializationException("raw array allocation ${operation.semanticId.value} has no result type")
                        val arrayType = when (val type = allocatedType.typeOrNull()) {
                            is ChirRawArrayType -> type
                            is ChirRefType -> type.referencedType.typeOrNull() as? ChirRawArrayType
                            else -> null
                        } ?: throw ChirSerializationException(
                            "raw array allocation ${operation.semanticId.value} does not produce a raw array",
                        )
                        ExpressionElem.RawArrayAllocate to RawArrayAllocate.createRawArrayAllocate(
                            builder,
                            base,
                            ctx.typeIdOf(arrayType.elementType),
                        )
                    }

                    ChirOtherOperation.RAW_ARRAY_LITERAL_INIT -> ExpressionElem.RawArrayLiteralInit to
                        RawArrayLiteralInit.createRawArrayLiteralInit(builder, base)

                    ChirOtherOperation.RAW_ARRAY_INIT_BY_VALUE -> ExpressionElem.RawArrayInitByValue to
                        RawArrayInitByValue.createRawArrayInitByValue(builder, base)

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
                    ChirOtherOperation.CANGJIE_TYPE_CAST_NA,
                    ChirOtherOperation.CANGJIE_TYPE_CAST_CHECKED,
                    ChirOtherOperation.CANGJIE_TYPE_CAST_WRAPPING,
                    ChirOtherOperation.CANGJIE_TYPE_CAST_THROWING,
                    ChirOtherOperation.CANGJIE_TYPE_CAST_SATURATING,
                    -> ExpressionElem.TypeCast to TypeCast.createTypeCast(
                        builder,
                        base,
                        encodeOverflowStrategy(otherOperation.typeCastOverflowStrategy ?: ChirOverflowStrategy.NOT_APPLICABLE),
                    )

                    else -> ExpressionElem.Debug to Debug.createDebug(
                        builder,
                        base,
                        builder.createString(otherOperation.canonicalName),
                        0u,
                    )
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
    /**
     * 编码表达式公共基类字段。
     */
    private fun encodeBaseExpression(
        builder: FlatBufferBuilder,
        e: EncExpr,
        ctx: EncodeContext,
        kind: UByte,
        operands: UIntArray,
        resultType: UInt,
    ): Int {
        val operandsOffset = if (operands.isEmpty()) 0 else Expression.createOperandsVector(builder, operands)
        val resultLocalValue = (e.node as? ChirExpression)?.resultLocalValue
        val resultLocalValueId = resultLocalValue?.let { ctx.valueIdOf(it.semanticId.value) } ?: 0u
        return Expression.createExpression(
            builder,
            0,
            kind,
            e.id,
            operandsOffset,
            0,
            ctx.valueIdOf(e.parentBlockSemanticId),
            resultLocalValueId,
            resultType,
        )
    }

    /**
     * 将一元操作符映射为 FlatBuffers 表达式 kind。
     */
    private fun encodeUnaryExprKind(operator: ChirUnaryOperator): UByte = when (operator) {
        ChirUnaryOperator.INT_NEG,
        ChirUnaryOperator.FLOAT_NEG,
        -> CHIRExprKind.NEG
        ChirUnaryOperator.BIT_NOT -> CHIRExprKind.BITNOT
        ChirUnaryOperator.LOGICAL_NOT -> CHIRExprKind.NOT
        ChirUnaryOperator.IDENTITY -> CHIRExprKind.DEBUGEXPR
    }

    /**
     * 将二元操作符映射为 FlatBuffers 表达式 kind。
     */
    private fun encodeBinaryExprKind(operator: ChirBinaryOperator): UByte = when (operator) {
        ChirBinaryOperator.ADD -> CHIRExprKind.ADD
        ChirBinaryOperator.SUB -> CHIRExprKind.SUB
        ChirBinaryOperator.MUL -> CHIRExprKind.MUL
        ChirBinaryOperator.POWER -> CHIRExprKind.EXP
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

    /**
     * 将内存操作映射为 FlatBuffers 表达式 kind。
     */
    private fun encodeMemoryExprKind(operation: ChirMemoryOperation): UByte = when (operation) {
        ChirMemoryOperation.LOAD -> CHIRExprKind.LOAD
        ChirMemoryOperation.STORE -> CHIRExprKind.STORE
        ChirMemoryOperation.ALLOCA -> CHIRExprKind.ALLOCATE
        ChirMemoryOperation.GET_ELEMENT_PTR,
        ChirMemoryOperation.GET_ELEMENT_PTR_INBOUNDS,
        -> CHIRExprKind.GET_ELEMENT_REF
        ChirMemoryOperation.GET_ELEMENT_BY_NAME -> CHIRExprKind.GET_ELEMENT_BY_NAME
        ChirMemoryOperation.STORE_ELEMENT_BY_NAME -> CHIRExprKind.STORE_ELEMENT_BY_NAME
    }

    /**
     * 将其他表达式操作映射为 FlatBuffers 表达式 kind。
     */
    private fun encodeOtherExprKind(operation: ChirOtherOperation): UByte = when (operation) {
        ChirOtherOperation.SELECT -> CHIRExprKind.IF
        ChirOtherOperation.PHI -> CHIRExprKind.DEBUGEXPR
        ChirOtherOperation.RAW_ARRAY_ALLOCATE -> CHIRExprKind.RAW_ARRAY_ALLOCATE
        ChirOtherOperation.RAW_ARRAY_LITERAL_INIT -> CHIRExprKind.RAW_ARRAY_LITERAL_INIT
        ChirOtherOperation.RAW_ARRAY_INIT_BY_VALUE -> CHIRExprKind.RAW_ARRAY_INIT_BY_VALUE
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
        ChirOtherOperation.CANGJIE_TYPE_CAST_NA,
        ChirOtherOperation.CANGJIE_TYPE_CAST_CHECKED,
        ChirOtherOperation.CANGJIE_TYPE_CAST_WRAPPING,
        ChirOtherOperation.CANGJIE_TYPE_CAST_THROWING,
        ChirOtherOperation.CANGJIE_TYPE_CAST_SATURATING,
        -> CHIRExprKind.TYPECAST
        else -> CHIRExprKind.DEBUGEXPR
    }

    /** 算术和类型转换表达式共用官方 CHIR 溢出策略编码。 */
    private fun encodeOverflowStrategy(strategy: ChirOverflowStrategy): UByte = when (strategy) {
        ChirOverflowStrategy.NOT_APPLICABLE -> OverflowStrategy.NA
        ChirOverflowStrategy.CHECKED -> OverflowStrategy.CHECKED
        ChirOverflowStrategy.WRAPPING -> OverflowStrategy.WRAPPING
        ChirOverflowStrategy.THROWING -> OverflowStrategy.THROWING
        ChirOverflowStrategy.SATURATING -> OverflowStrategy.SATURATING
    }

    private fun decodeOverflowStrategy(strategy: UByte): ChirOverflowStrategy = when (strategy) {
        OverflowStrategy.NA -> ChirOverflowStrategy.NOT_APPLICABLE
        OverflowStrategy.CHECKED -> ChirOverflowStrategy.CHECKED
        OverflowStrategy.WRAPPING -> ChirOverflowStrategy.WRAPPING
        OverflowStrategy.THROWING -> ChirOverflowStrategy.THROWING
        OverflowStrategy.SATURATING -> ChirOverflowStrategy.SATURATING
        else -> throw ChirSerializationException("unknown typecast overflow strategy $strategy")
    }

    private fun ChirTypeRef.typeOrNull(): ChirType? = (this as? ChirResolvedTypeRef)?.type

    /** 将仓内类型化 intrinsic 种类映射到当前 CHIR schema。 */
    private fun encodeIntrinsicKind(kind: ChirIntrinsicKind): UShort {
        if (kind.schemaCode > IntrinsicKind.EXCLUSIVE_SCOPE) {
            throw ChirSerializationException("unsupported intrinsic kind ${kind.schemaCode}")
        }
        return kind.schemaCode
    }

    /** 将当前 CHIR schema 的 intrinsic 种类恢复为仓内类型化模型。 */
    private fun decodeIntrinsicKind(kind: UShort): ChirIntrinsicKind {
        if (kind > IntrinsicKind.EXCLUSIVE_SCOPE) {
            throw ChirSerializationException("unsupported intrinsic kind $kind")
        }
        return ChirIntrinsicKind(kind)
    }

    /**
     * 从 FlatBuffers 根节点解码全部类型引用。
     */
    private fun decodeTypes(root: CHIRPackage): Map<UInt, ChirTypeRef> {
        val result = linkedMapOf<UInt, ChirTypeRef>()

        fun register(base: Type, type: ChirType, label: String) {
            if (base.typeId == 0u || result.containsKey(base.typeId)) {
                throw ChirSerializationException("damaged payload: $label has invalid or duplicate type id ${base.typeId}")
            }
            result[base.typeId] = ChirResolvedTypeRef(type)
        }

        fun requiredBase(base: Type?, label: String): Type = base
            ?: throw ChirSerializationException("damaged payload: $label has no base type")

        fun requiredCustomBase(base: CustomType?, label: String): CustomType = base
            ?: throw ChirSerializationException("damaged payload: $label has no custom type base")

        fun typeArguments(base: Type): List<ChirTypeRef> = List(base.argTysLength) { index ->
            val typeId = base.argTys(index)
            result[typeId]
                ?: throw ChirSerializationException("damaged payload: type ${base.typeId} references non-prior type $typeId")
        }

        fun singleTypeArgument(base: Type, label: String): ChirTypeRef {
            val arguments = typeArguments(base)
            if (arguments.size != 1) {
                throw ChirSerializationException("damaged payload: $label has ${arguments.size} type arguments, expected one")
            }
            return arguments.single()
        }

        fun customDefinition(definitionId: UInt, label: String): CustomTypeDef {
            if (definitionId == 0u || definitionId > root.defsLength.toUInt()) {
                throw ChirSerializationException("damaged payload: $label references missing definition $definitionId")
            }
            val definitionIndex = (definitionId - 1u).toInt()
            val customDefinition = when (root.defsType(definitionIndex)) {
                CustomTypeDefElem.StructDef -> (root.defs(StructDef(), definitionIndex) as? StructDef)?.base
                CustomTypeDefElem.ClassDef -> (root.defs(ClassDef(), definitionIndex) as? ClassDef)?.base
                CustomTypeDefElem.EnumDef -> (root.defs(EnumDef(), definitionIndex) as? EnumDef)?.base
                else -> null
            } ?: throw ChirSerializationException("damaged payload: $label has an invalid custom definition kind")
            if (customDefinition.customTypeDefId != definitionId) {
                throw ChirSerializationException(
                    "damaged payload: $label definition id ${customDefinition.customTypeDefId} does not match $definitionId",
                )
            }
            return customDefinition
        }

        fun customTypeDeclarationId(definitionId: UInt, label: String): ChirTypeDeclarationId {
            val definition = customDefinition(definitionId, label)
            val packageName = definition.packageName
                ?: throw ChirSerializationException("damaged payload: $label definition $definitionId has no package name")
            val declarationName = definition.srcCodeIdentifier
                ?: throw ChirSerializationException("damaged payload: $label definition $definitionId has no source identifier")
            return ChirTypeDeclarationId(packageName, declarationName)
        }

        fun instantiatedMemberTypes(customType: CustomType, label: String): List<ChirTypeRef> =
            List(customType.instantiatedMemberTysLength) { index ->
                val memberTypeId = customType.instantiatedMemberTys(index)
                result[memberTypeId]
                    ?: throw ChirSerializationException(
                        "damaged payload: $label references non-prior member type $memberTypeId",
                    )
            }

        fun enumCases(enumDefinitionId: UInt, enumTypeId: UInt, label: String): List<ChirEnumCaseType> {
            val definitionIndex = (enumDefinitionId - 1u).toInt()
            val definition = root.defs(EnumDef(), definitionIndex) as? EnumDef
                ?: throw ChirSerializationException("damaged payload: $label has no EnumDef table")
            return List(definition.ctorsLength) { caseIndex ->
                val constructor = definition.ctors(caseIndex)
                    ?: throw ChirSerializationException("damaged payload: $label constructor #$caseIndex is missing")
                val constructorTypeId = constructor.funcType
                if (constructorTypeId == 0u || constructorTypeId > root.typesLength.toUInt()) {
                    throw ChirSerializationException(
                        "damaged payload: $label constructor #$caseIndex references invalid function type $constructorTypeId",
                    )
                }
                val constructorTypeIndex = (constructorTypeId - 1u).toInt()
                if (root.typesType(constructorTypeIndex) != TypeElem.FuncType) {
                    throw ChirSerializationException(
                        "damaged payload: $label constructor #$caseIndex does not reference a function type",
                    )
                }
                val functionType = root.types(FuncType(), constructorTypeIndex) as? FuncType
                    ?: throw ChirSerializationException("damaged payload: $label constructor #$caseIndex has no function type table")
                val functionBase = requiredBase(functionType.base, "$label constructor #$caseIndex function type")
                val parameterAndReturnTypeIds = List(functionBase.argTysLength) { functionBase.argTys(it) }
                if (parameterAndReturnTypeIds.isEmpty() || parameterAndReturnTypeIds.last() != enumTypeId) {
                    throw ChirSerializationException(
                        "damaged payload: $label constructor #$caseIndex has an invalid enum return type",
                    )
                }
                val payloadTypes = parameterAndReturnTypeIds.dropLast(1).map { payloadTypeId ->
                    result[payloadTypeId]
                        ?: throw ChirSerializationException(
                            "damaged payload: $label constructor #$caseIndex references non-prior payload type $payloadTypeId",
                        )
                }
                ChirEnumCaseType(
                    name = constructor.identifier
                        ?: throw ChirSerializationException("damaged payload: $label constructor #$caseIndex has no identifier"),
                    payloadTypes = payloadTypes,
                )
            }
        }

        for (i in 0 until root.typesLength) {
            when (root.typesType(i)) {
                TypeElem.IntType -> root.types(IntType(), i)?.let { t ->
                    val base = (t as IntType).base?.base
                        ?: throw ChirSerializationException("damaged payload: int type #$i has no base")
                    register(base, decodePrimitiveType(base.kind), "int type #$i")
                }
                TypeElem.FloatType -> root.types(FloatType(), i)?.let { t ->
                    val base = (t as FloatType).base?.base
                        ?: throw ChirSerializationException("damaged payload: float type #$i has no base")
                    register(base, decodePrimitiveType(base.kind), "float type #$i")
                }
                TypeElem.BooleanType -> root.types(BooleanType(), i)?.let { t ->
                    val base = (t as BooleanType).base
                        ?: throw ChirSerializationException("damaged payload: boolean type #$i has no base")
                    register(base, ChirPrimitiveType.BOOL, "boolean type #$i")
                }
                TypeElem.UnitType -> root.types(UnitType(), i)?.let { t ->
                    val base = (t as UnitType).base
                        ?: throw ChirSerializationException("damaged payload: unit type #$i has no base")
                    register(base, ChirPrimitiveType.UNIT, "unit type #$i")
                }
                TypeElem.NothingType -> root.types(NothingType(), i)?.let { t ->
                    val base = (t as NothingType).base
                        ?: throw ChirSerializationException("damaged payload: nothing type #$i has no base")
                    register(base, ChirPrimitiveType.NOTHING, "nothing type #$i")
                }
                TypeElem.VoidType -> root.types(VoidType(), i)?.let { t ->
                    val base = (t as VoidType).base
                        ?: throw ChirSerializationException("damaged payload: void type #$i has no base")
                    register(base, ChirPrimitiveType.VOID, "void type #$i")
                }
                TypeElem.RuneType -> root.types(RuneType(), i)?.let { t ->
                    val base = (t as RuneType).base
                        ?: throw ChirSerializationException("damaged payload: rune type #$i has no base")
                    register(base, ChirPrimitiveType.RUNE, "rune type #$i")
                }
                TypeElem.TupleType -> root.types(TupleType(), i)?.let { t ->
                    val base = requiredBase((t as TupleType).base, "tuple type #$i")
                    register(base, ChirTupleType(typeArguments(base)), "tuple type #$i")
                }
                TypeElem.StructType -> root.types(StructType(), i)?.let { t ->
                    val custom = requiredCustomBase((t as StructType).base, "struct type #$i")
                    val base = requiredBase(custom.base, "struct type #$i")
                    if (base.kind != CHIRTypeKind.STRUCT) {
                        throw ChirSerializationException("damaged payload: struct type #$i has kind ${base.kind}")
                    }
                    val declarationId = customTypeDeclarationId(custom.customTypeDef, "struct type #$i")
                    register(
                        base,
                        ChirStructType(
                            declarationId = declarationId,
                            fieldTypes = instantiatedMemberTypes(custom, "struct type #$i"),
                            typeArguments = typeArguments(base),
                        ),
                        "struct type #$i",
                    )
                }
                TypeElem.ClassType -> root.types(ClassType(), i)?.let { t ->
                    val classType = t as ClassType
                    val custom = requiredCustomBase(classType.base, "class type #$i")
                    val base = requiredBase(custom.base, "class type #$i")
                    if (base.kind != CHIRTypeKind.CLASS) {
                        throw ChirSerializationException("damaged payload: class type #$i has kind ${base.kind}")
                    }
                    val superTypes = buildList {
                        classType.superClassTy.takeIf { it != 0u }?.let { superClassTypeId ->
                            add(result[superClassTypeId]
                                ?: throw ChirSerializationException("damaged payload: class type #$i references non-prior superclass type $superClassTypeId"))
                        }
                        repeat(custom.implementedInterfaceTysLength) { interfaceIndex ->
                            val interfaceTypeId = custom.implementedInterfaceTys(interfaceIndex)
                            add(result[interfaceTypeId]
                                ?: throw ChirSerializationException("damaged payload: class type #$i references non-prior interface type $interfaceTypeId"))
                        }
                    }
                    register(
                        base,
                        ChirClassType(
                            declarationId = customTypeDeclarationId(custom.customTypeDef, "class type #$i"),
                            fieldTypes = instantiatedMemberTypes(custom, "class type #$i"),
                            superTypes = superTypes,
                            typeArguments = typeArguments(base),
                        ),
                        "class type #$i",
                    )
                }
                TypeElem.EnumType -> root.types(EnumType(), i)?.let { t ->
                    val enumType = t as EnumType
                    val custom = requiredCustomBase(enumType.base, "enum type #$i")
                    val base = requiredBase(custom.base, "enum type #$i")
                    if (base.kind != CHIRTypeKind.ENUM) {
                        throw ChirSerializationException("damaged payload: enum type #$i has kind ${base.kind}")
                    }
                    val definition = customDefinition(custom.customTypeDef, "enum type #$i")
                    val definitionIndex = (custom.customTypeDef - 1u).toInt()
                    if (root.defsType(definitionIndex) != CustomTypeDefElem.EnumDef) {
                        throw ChirSerializationException("damaged payload: enum type #$i references a non-enum definition")
                    }
                    val enumDefinition = root.defs(EnumDef(), definitionIndex) as? EnumDef
                        ?: throw ChirSerializationException("damaged payload: enum type #$i has no EnumDef table")
                    val cases = enumCases(custom.customTypeDef, base.typeId, "enum type #$i")
                    register(
                        base,
                        ChirEnumType(
                            declarationId = customTypeDeclarationId(custom.customTypeDef, "enum type #$i"),
                            cases = cases,
                            typeArguments = typeArguments(base),
                            nonExhaustive = enumDefinition.nonExhaustive,
                        ),
                        "enum type #$i",
                    )
                }
                TypeElem.RawArrayType -> root.types(RawArrayType(), i)?.let { t ->
                    val array = t as RawArrayType
                    val base = requiredBase(array.base, "raw array type #$i")
                    if (array.dims == 0u) {
                        throw ChirSerializationException("damaged payload: raw array type #$i has zero dimensions")
                    }
                    register(
                        base,
                        ChirRawArrayType(singleTypeArgument(base, "raw array type #$i"), array.dims),
                        "raw array type #$i",
                    )
                }
                TypeElem.VArrayType -> root.types(VArrayType(), i)?.let { t ->
                    val array = t as VArrayType
                    val base = requiredBase(array.base, "VArray type #$i")
                    if (array.size < 0L) {
                        throw ChirSerializationException("damaged payload: VArray type #$i has negative size ${array.size}")
                    }
                    register(
                        base,
                        ChirVArrayType(singleTypeArgument(base, "VArray type #$i"), array.size),
                        "VArray type #$i",
                    )
                }
                TypeElem.FuncType -> root.types(FuncType(), i)?.let { t ->
                    val function = t as FuncType
                    val base = requiredBase(function.base, "function type #$i")
                    val arguments = typeArguments(base)
                    if (arguments.isEmpty()) {
                        throw ChirSerializationException("damaged payload: function type #$i has no return type")
                    }
                    val receiver = function.receiverType.takeIf { it != 0u }?.let { receiverId ->
                        result[receiverId]
                            ?: throw ChirSerializationException("damaged payload: function type #$i references non-prior receiver type $receiverId")
                    }
                    register(
                        base,
                        ChirFunctionType(
                            parameterTypes = arguments.dropLast(1),
                            returnType = arguments.last(),
                            receiverType = receiver,
                            throws = function.throws,
                            isCFunctionType = function.isCfuncType,
                            hasVarArg = function.hasVarArg,
                        ),
                        "function type #$i",
                    )
                }
                TypeElem.CStringType -> root.types(CStringType(), i)?.let { t ->
                    val base = requiredBase((t as CStringType).base, "C string type #$i")
                    register(base, ChirCStringType, "C string type #$i")
                }
                TypeElem.CPointerType -> root.types(CPointerType(), i)?.let { t ->
                    val base = requiredBase((t as CPointerType).base, "C pointer type #$i")
                    register(
                        base,
                        ChirCPointerType(singleTypeArgument(base, "C pointer type #$i")),
                        "C pointer type #$i",
                    )
                }
                TypeElem.GenericType -> root.types(GenericType(), i)?.let { t ->
                    val g = t as GenericType
                    val base = g.base
                        ?: throw ChirSerializationException("damaged payload: generic type #$i has no base")
                    val identifier = g.identifier
                        ?: throw ChirSerializationException("damaged payload: generic type #$i has no identifier")
                    val sourceIdentifier = g.srcCodeIdentifier ?: identifier
                    val upperBounds = List(g.upperBoundsLength) { boundIndex ->
                        val boundId = g.upperBounds(boundIndex)
                        result[boundId]
                            ?: throw ChirSerializationException("damaged payload: generic type #$i references non-prior upper bound $boundId")
                    }
                    register(
                        base,
                        ChirGenericType(
                            identifier = sourceIdentifier,
                            upperBounds = upperBounds,
                            semanticIdentity = identifier,
                        ),
                        "generic type #$i",
                    )
                }
                TypeElem.RefType -> root.types(RefType(), i)?.let { t ->
                    val reference = t as RefType
                    val base = requiredBase(reference.base, "reference type #$i")
                    register(
                        base,
                        ChirRefType(singleTypeArgument(base, "reference type #$i"), reference.mutable),
                        "reference type #$i",
                    )
                }
                TypeElem.BoxType -> root.types(BoxType(), i)?.let { t ->
                    val base = requiredBase((t as BoxType).base, "box type #$i")
                    register(base, ChirBoxType(singleTypeArgument(base, "box type #$i")), "box type #$i")
                }
                TypeElem.ThisType -> root.types(ThisType(), i)?.let { t ->
                    val thisType = t as ThisType
                    val base = requiredBase(thisType.base, "this type #$i")
                    if (base.argTysLength != 0) {
                        throw ChirSerializationException("damaged payload: this type #$i has type arguments")
                    }
                    val ownerTypeName = thisType.ownerTypeName
                        ?: throw ChirSerializationException("damaged payload: this type #$i has no owner name")
                    register(base, ChirThisType(ownerTypeName), "this type #$i")
                }
                else -> throw ChirSerializationException(
                    "damaged payload: unsupported type union tag ${root.typesType(i)} at index $i",
                )
            }
        }
        return result
    }

    /**
     * 将 FlatBuffers 类型 kind 解码为 CHIR 原始类型。
     */
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

    /**
     * 从 FlatBuffers 根节点解码值表，并建立函数、参数和基本块索引。
     */
    private fun decodeValues(
        root: CHIRPackage,
        typeById: Map<UInt, ChirTypeRef>,
        exprMetadata: Map<UInt, DecodedExprMetadata>,
    ): DecodedValues {
        val out = DecodedValues()
        fun typeOf(id: UInt) = typeById[id]
            ?: throw ChirSerializationException("damaged payload: value references unknown type $id")
        for (i in 0 until root.valuesLength) {
            when (root.valuesType(i)) {
                ValueElem.BoolLiteral -> root.values(BoolLiteral(), i)?.let { table ->
                    val value = table as BoolLiteral
                    val literal = value.base
                        ?: throw ChirSerializationException("damaged payload: Bool literal value #$i has no literal metadata")
                    val base = literal.base
                        ?: throw ChirSerializationException("damaged payload: Bool literal value #$i has no base")
                    if (literal.literalKind != ConstantValueKind.BOOL) {
                        throw ChirSerializationException("damaged payload: Bool literal value ${base.valueId} has a non-Bool literal kind")
                    }
                    val type = typeOf(base.type)
                    if (type != booleanType) {
                        throw ChirSerializationException("damaged payload: Bool literal value ${base.valueId} has non-Bool type ${type.renderName}")
                    }
                    val identifier = base.identifier
                        ?: throw ChirSerializationException("damaged payload: Bool literal value ${base.valueId} has no identifier")
                    out.values[base.valueId] = ChirConstantValue(
                        semanticId = ChirSemanticId(identifier),
                        type = type,
                        literal = value.val_.toString(),
                    )
                }
                ValueElem.IntLiteral -> root.values(IntLiteral(), i)?.let { t ->
                    val b = (t as IntLiteral).base?.base
                        ?: throw ChirSerializationException("damaged payload: int literal value #$i has no base")
                    val identifier = b.identifier
                        ?: throw ChirSerializationException("damaged payload: int literal value ${b.valueId} has no identifier")
                    out.values[b.valueId] = ChirConstantValue(
                        semanticId = ChirSemanticId(identifier),
                        type = typeOf(b.type),
                        literal = t.val_.toString(),
                    )
                }
                ValueElem.FloatLiteral -> root.values(FloatLiteral(), i)?.let { t ->
                    val literal = (t as FloatLiteral).base
                        ?: throw ChirSerializationException("damaged payload: Float literal value #$i has no literal metadata")
                    val base = literal.base
                        ?: throw ChirSerializationException("damaged payload: Float literal value #$i has no base")
                    if (literal.literalKind != ConstantValueKind.FLOAT) {
                        throw ChirSerializationException("damaged payload: Float literal value ${base.valueId} has a non-Float literal kind")
                    }
                    val type = typeOf(base.type)
                    if ((type as? ChirResolvedTypeRef)?.type !in floatingLiteralTypes) {
                        throw ChirSerializationException("damaged payload: Float literal value ${base.valueId} has non-Float type ${type.renderName}")
                    }
                    val identifier = base.identifier
                        ?: throw ChirSerializationException("damaged payload: Float literal value ${base.valueId} has no identifier")
                    out.values[base.valueId] = ChirConstantValue(
                        semanticId = ChirSemanticId(identifier),
                        type = type,
                        literal = t.val_.toString(),
                    )
                }
                ValueElem.RuneLiteral -> root.values(RuneLiteral(), i)?.let { t ->
                    val literal = (t as RuneLiteral).base
                        ?: throw ChirSerializationException("damaged payload: Rune literal value #$i has no literal metadata")
                    val base = literal.base
                        ?: throw ChirSerializationException("damaged payload: Rune literal value #$i has no base")
                    if (literal.literalKind != ConstantValueKind.RUNE) {
                        throw ChirSerializationException("damaged payload: Rune literal value ${base.valueId} has a non-Rune literal kind")
                    }
                    val type = typeOf(base.type)
                    if (type != runeType) {
                        throw ChirSerializationException("damaged payload: Rune literal value ${base.valueId} has non-Rune type ${type.renderName}")
                    }
                    val identifier = base.identifier
                        ?: throw ChirSerializationException("damaged payload: Rune literal value ${base.valueId} has no identifier")
                    out.values[base.valueId] = ChirConstantValue(
                        semanticId = ChirSemanticId(identifier),
                        type = type,
                        literal = t.val_.toString(),
                    )
                }
                ValueElem.StringLiteral -> root.values(StringLiteral(), i)?.let { value ->
                    val stringLiteral = value as StringLiteral
                    val literal = stringLiteral.base
                        ?: throw ChirSerializationException("damaged payload: String literal value #$i has no literal metadata")
                    val base = literal.base
                        ?: throw ChirSerializationException("damaged payload: String literal value #$i has no base")
                    if (literal.literalKind != ConstantValueKind.STRING) {
                        throw ChirSerializationException("damaged payload: String literal value ${base.valueId} has a non-String literal kind")
                    }
                    val type = typeOf(base.type)
                    val identifier = base.identifier
                        ?: throw ChirSerializationException("damaged payload: String literal value ${base.valueId} has no identifier")
                    val text = stringLiteral.val_
                        ?: throw ChirSerializationException("damaged payload: String literal value ${base.valueId} has no text")
                    out.values[base.valueId] = ChirConstantValue(
                        semanticId = ChirSemanticId(identifier),
                        type = type,
                        literal = text,
                    )
                }
                ValueElem.UnitLiteral -> root.values(UnitLiteral(), i)?.let { value ->
                    val literal = (value as UnitLiteral).base
                        ?: throw ChirSerializationException("damaged payload: Unit literal value #$i has no literal metadata")
                    val b = literal.base
                        ?: throw ChirSerializationException("damaged payload: Unit literal value #$i has no base")
                    if (literal.literalKind != ConstantValueKind.UNIT) {
                        throw ChirSerializationException("damaged payload: Unit literal value ${b.valueId} has a non-Unit literal kind")
                    }
                    val type = typeOf(b.type)
                    if (type != unitType) {
                        throw ChirSerializationException("damaged payload: Unit literal value ${b.valueId} has non-Unit type ${type.renderName}")
                    }
                    val identifier = b.identifier
                        ?: throw ChirSerializationException("damaged payload: Unit literal value ${b.valueId} has no identifier")
                    out.values[b.valueId] = ChirConstantValue(
                        semanticId = ChirSemanticId(identifier),
                        type = type,
                        literal = "unit",
                    )
                }
                ValueElem.NullLiteral -> root.values(NullLiteral(), i)?.let { value ->
                    val literal = (value as NullLiteral).base
                        ?: throw ChirSerializationException("damaged payload: Null literal value #$i has no literal metadata")
                    val base = literal.base
                        ?: throw ChirSerializationException("damaged payload: Null literal value #$i has no base")
                    if (literal.literalKind != ConstantValueKind.NULL) {
                        throw ChirSerializationException("damaged payload: Null literal value ${base.valueId} has a non-Null literal kind")
                    }
                    val identifier = base.identifier
                        ?: throw ChirSerializationException("damaged payload: Null literal value ${base.valueId} has no identifier")
                    out.values[base.valueId] = ChirConstantValue(
                        semanticId = ChirSemanticId(identifier),
                        type = typeOf(base.type),
                        literal = "null",
                        constantKind = ChirConstantKind.NULL,
                    )
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
                    out.functions += DecodedFunc(
                        valueId = b.valueId,
                        semanticId = ChirSemanticId(semanticId),
                        name = name,
                        returnType = typeOf(b.type),
                        moduleSemanticId = ChirSemanticId(moduleSemanticId),
                        moduleName = moduleName,
                        paramValueIds = List(f.paramsLength) { f.params(it) },
                        entryBlockValueId = f.body,
                        genericTypeParameters = List(f.genericTypeParamsLength) { index -> typeOf(f.genericTypeParams(index)) },
                        isStatic = b.attributes and OfficialChirV113Attribute.STATIC.mask != 0uL,
                    )
                }
                ValueElem.Block -> root.values(Block(), i)?.let { t ->
                    val b = t as Block
                    val base = b.base
                        ?: throw ChirSerializationException("damaged payload: block value #$i has no base")
                    val blockType = typeOf(base.type)
                    if (blockType != unitType) {
                        throw ChirSerializationException("damaged payload: block value ${base.valueId} has non-Unit type ${blockType.renderName}")
                    }
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
                    val associatedExpressionId = t.associatedExpr.takeIf { it != 0u }?.let { expressionId ->
                        val semanticId = exprMetadata[expressionId]?.semanticId
                            ?: throw ChirSerializationException(
                                "damaged payload: local value ${b.valueId} references unknown expression $expressionId",
                            )
                        ChirSemanticId(semanticId)
                    }
                    out.values[b.valueId] = ChirLocalValue(
                        semanticId = ChirSemanticId(parts[0]),
                        type = typeOf(b.type),
                        name = parts.getOrElse(1) { parts[0] },
                        associatedExpressionId = associatedExpressionId,
                        isReturnValue = t.isRetVal,
                    )
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

    /**
     * 从 FlatBuffers 根节点解码表达式和终结指令表。
     */
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
        fun resultLocalValue(base: Expression): ChirLocalValue? {
            val valueId = base.resultLocalVar.takeIf { it != 0u } ?: return null
            return values.values[valueId] as? ChirLocalValue
                ?: throw ChirSerializationException(
                    "damaged payload: expression ${base.expressionId} references non-local result value $valueId",
                )
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
                            resultLocalValue = resultLocalValue(base),
                            overflowStrategy = decodeOverflowStrategy((t as UnaryExpression).overflowStrategy),
                        )
                    }
                }
                ExpressionElem.BinaryExpression -> root.exprs(BinaryExpression(), i)?.let { t ->
                    val base = (t as BinaryExpression).base ?: return@let
                    val ops = operands(base)
                    if (ops.size >= 2) {
                        val operation = operationOf(base.expressionId) ?: decodeBinaryExprKind(base.kind)
                        out[base.expressionId] = ChirBinaryExpression(
                            exprSid(base.expressionId),
                            operation,
                            ops[0],
                            ops[1],
                            typeOf(base.resultTy),
                            resultLocalValue = resultLocalValue(base),
                            overflowStrategy = decodeOverflowStrategy((t as BinaryExpression).overflowStrategy),
                        )
                    }
                }
                ExpressionElem.Constant -> root.exprs(Constant(), i)?.let { t ->
                    val base = (t as Constant).base ?: return@let
                    val constant = operands(base).singleOrNull() as? ChirConstantValue
                        ?: throw ChirSerializationException(
                            "damaged payload: constant expression ${base.expressionId} must have one literal operand",
                        )
                    val resultType = typeOf(base.resultTy)
                    val resultLocal = resultLocalValue(base)
                        ?: throw ChirSerializationException(
                            "damaged payload: constant expression ${base.expressionId} has no result local",
                        )
                    if (!ChirTypeRelations.isOfficialChirTypeCompatible(resultType, constant.type) &&
                        constant.literal != "null"
                    ) {
                        throw ChirSerializationException(
                            "damaged payload: constant expression ${base.expressionId} result type differs from its literal",
                        )
                    }
                    out[base.expressionId] = ChirConstantExpression(
                        semanticId = exprSid(base.expressionId),
                        value = constant,
                        resultType = resultType,
                        resultLocalValue = resultLocal,
                    )
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
                            resultLocalValue = resultLocalValue(base),
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
                            base.resultTy.takeIf { it != 0u }?.let(::typeOf),
                            resultLocalValue = resultLocalValue(base),
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
                            resultLocalValue = resultLocalValue(base),
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
                            resultLocalValue = resultLocalValue(base),
                        )
                    }
                }
                ExpressionElem.GetElementByName -> root.exprs(GetElementByName(), i)?.let { t ->
                    val element = t as GetElementByName
                    val base = element.base ?: return@let
                    val ops = operands(base)
                    if (ops.size == 1) {
                        out[base.expressionId] = ChirMemoryExpression(
                            semanticId = exprSid(base.expressionId),
                            operation = operationOf(base.expressionId) ?: ChirMemoryOperation.GET_ELEMENT_BY_NAME.canonicalName,
                            address = ops.single(),
                            resultType = typeOf(base.resultTy),
                            resultLocalValue = resultLocalValue(base),
                            memberNames = List(element.namesLength) { index ->
                                element.names(index)
                                    ?: throw ChirSerializationException("damaged payload: get-element-by-name expression ${base.expressionId} has a missing member name")
                            },
                        )
                    }
                }
                ExpressionElem.StoreElementByName -> root.exprs(StoreElementByName(), i)?.let { t ->
                    val element = t as StoreElementByName
                    val base = element.base ?: return@let
                    val ops = operands(base)
                    if (ops.size == 2) {
                        out[base.expressionId] = ChirMemoryExpression(
                            semanticId = exprSid(base.expressionId),
                            operation = operationOf(base.expressionId) ?: ChirMemoryOperation.STORE_ELEMENT_BY_NAME.canonicalName,
                            address = ops[1],
                            value = ops[0],
                            resultType = typeOf(base.resultTy),
                            resultLocalValue = resultLocalValue(base),
                            memberNames = List(element.namesLength) { index ->
                                element.names(index)
                                    ?: throw ChirSerializationException("damaged payload: store-element-by-name expression ${base.expressionId} has a missing member name")
                            },
                        )
                    }
                }
                ExpressionElem.Apply -> root.exprs(Apply(), i)?.let { t ->
                    val base = (t as Apply).base ?: return@let
                    val ops = operands(base)
                    if (ops.isNotEmpty()) {
                        val instantiatedTypeArguments = (0 until t.instantiatedTypeArgsLength)
                            .map { index -> typeOf(t.instantiatedTypeArgs(index)) }
                        val thisType = t.thisType.takeIf { it != 0u }?.let(::typeOf)
                        out[base.expressionId] = ChirCallExpression(
                            exprSid(base.expressionId),
                            ops[0],
                            ops.drop(1),
                            typeOf(base.resultTy),
                            isSuperCall = t.isSuperCall,
                            instantiatedTypeArguments = instantiatedTypeArguments,
                            thisType = thisType,
                            resultLocalValue = resultLocalValue(base),
                        )
                    }
                }
                ExpressionElem.Invoke -> root.exprs(Invoke(), i)?.let { t ->
                    val invoke = t as Invoke
                    val base = invoke.base ?: return@let
                    if (base.kind != CHIRExprKind.INVOKE) {
                        throw ChirSerializationException("invoke expression ${base.expressionId} has kind ${base.kind}")
                    }
                    val ops = operands(base)
                    if (ops.isEmpty()) {
                        throw ChirSerializationException("invoke expression ${base.expressionId} has no callee operand")
                    }
                    val methodContext = invoke.virMethodCtx
                        ?: throw ChirSerializationException("invoke expression ${base.expressionId} has no virtual method context")
                    val sourceCodeIdentifier = methodContext.srcCodeIdentifier
                        ?.takeIf(String::isNotBlank)
                        ?: throw ChirSerializationException("invoke expression ${base.expressionId} has no source code identifier")
                    val originalFunctionType = ((typeOf(methodContext.originalFuncType) as? ChirResolvedTypeRef)?.type as? ChirFunctionType)
                        ?: throw ChirSerializationException("invoke expression ${base.expressionId} has no original function type")
                    val genericTypeParameters = List(methodContext.genericTypeParamsLength) { index ->
                        val type = typeOf(methodContext.genericTypeParams(index))
                        if ((type as? ChirResolvedTypeRef)?.type !is ChirGenericType) {
                            throw ChirSerializationException(
                                "invoke expression ${base.expressionId} generic type parameter #$index is not generic",
                            )
                        }
                        type
                    }
                    val instantiatedTypeArguments = (0 until invoke.instantiatedTypeArgsLength)
                        .map { index -> typeOf(invoke.instantiatedTypeArgs(index)) }
                    val thisType = invoke.thisType.takeIf { it != 0u }?.let(::typeOf)
                    val virtualMethodContext = ChirVirtualMethodContext(
                        sourceCodeIdentifier = sourceCodeIdentifier,
                        originalFunctionType = originalFunctionType,
                        genericTypeParameters = genericTypeParameters,
                    )
                    out[base.expressionId] = ChirCallExpression(
                        semanticId = exprSid(base.expressionId),
                        callee = ChirImportedFunctionValue(
                            semanticId = ChirSemanticId("${exprSid(base.expressionId).value}:virtual-target"),
                            type = ChirResolvedTypeRef(originalFunctionType),
                            name = sourceCodeIdentifier,
                        ),
                        arguments = ops.drop(1),
                        resultType = typeOf(base.resultTy),
                        isSuperCall = false,
                        instantiatedTypeArguments = instantiatedTypeArguments,
                        thisType = thisType,
                        virtualMethodContext = virtualMethodContext,
                        dispatchReceiver = ops[0],
                        resultLocalValue = resultLocalValue(base),
                    )
                }
                ExpressionElem.RawArrayAllocate -> root.exprs(RawArrayAllocate(), i)?.let { t ->
                    val allocation = t as RawArrayAllocate
                    val base = allocation.base
                        ?: throw ChirSerializationException("raw array allocation expression #$i has no base")
                    if (base.kind != CHIRExprKind.RAW_ARRAY_ALLOCATE) {
                        throw ChirSerializationException("raw array allocation expression ${base.expressionId} has kind ${base.kind}")
                    }
                    val ops = operands(base)
                    if (ops.size != 1 || base.resultTy == 0u) {
                        throw ChirSerializationException("raw array allocation expression ${base.expressionId} has invalid operands or result type")
                    }
                    val resultType = typeOf(base.resultTy)
                    val arrayType = when (val type = resultType.typeOrNull()) {
                        is ChirRawArrayType -> type
                        is ChirRefType -> type.referencedType.typeOrNull() as? ChirRawArrayType
                        else -> null
                    } ?: throw ChirSerializationException(
                        "raw array allocation expression ${base.expressionId} does not produce a raw array",
                    )
                    val elementType = typeOf(allocation.elementType)
                    if (!ChirTypeRelations.areEquivalent(arrayType.elementType, elementType)) {
                        throw ChirSerializationException(
                            "raw array allocation expression ${base.expressionId} element type disagrees with its result type",
                        )
                    }
                    out[base.expressionId] = ChirOtherExpression(
                        semanticId = exprSid(base.expressionId),
                        operation = operationOf(base.expressionId) ?: ChirOtherOperation.RAW_ARRAY_ALLOCATE.canonicalName,
                        operands = ops,
                        resultType = resultType,
                        resultLocalValue = resultLocalValue(base),
                    )
                }
                ExpressionElem.RawArrayLiteralInit -> root.exprs(RawArrayLiteralInit(), i)?.let { t ->
                    val initialization = t as RawArrayLiteralInit
                    val base = initialization.base
                        ?: throw ChirSerializationException("raw array literal init expression #$i has no base")
                    if (base.kind != CHIRExprKind.RAW_ARRAY_LITERAL_INIT) {
                        throw ChirSerializationException("raw array literal init expression ${base.expressionId} has kind ${base.kind}")
                    }
                    val resultType = base.resultTy.takeIf { it != 0u }?.let(::typeOf)
                    out[base.expressionId] = ChirOtherExpression(
                        semanticId = exprSid(base.expressionId),
                        operation = operationOf(base.expressionId) ?: ChirOtherOperation.RAW_ARRAY_LITERAL_INIT.canonicalName,
                        operands = operands(base),
                        resultType = resultType,
                        resultLocalValue = resultLocalValue(base),
                    )
                }
                ExpressionElem.RawArrayInitByValue -> root.exprs(RawArrayInitByValue(), i)?.let { t ->
                    val initialization = t as RawArrayInitByValue
                    val base = initialization.base
                        ?: throw ChirSerializationException("raw array init by value expression #$i has no base")
                    if (base.kind != CHIRExprKind.RAW_ARRAY_INIT_BY_VALUE) {
                        throw ChirSerializationException("raw array init by value expression ${base.expressionId} has kind ${base.kind}")
                    }
                    val resultType = base.resultTy.takeIf { it != 0u }?.let(::typeOf)
                    out[base.expressionId] = ChirOtherExpression(
                        semanticId = exprSid(base.expressionId),
                        operation = operationOf(base.expressionId) ?: ChirOtherOperation.RAW_ARRAY_INIT_BY_VALUE.canonicalName,
                        operands = operands(base),
                        resultType = resultType,
                        resultLocalValue = resultLocalValue(base),
                    )
                }
                ExpressionElem.Intrinsic -> root.exprs(Intrinsic(), i)?.let { t ->
                    val intrinsic = t as Intrinsic
                    val base = intrinsic.base
                        ?: throw ChirSerializationException("intrinsic expression #$i has no base")
                    if (base.kind != CHIRExprKind.INTRINSIC) {
                        throw ChirSerializationException("intrinsic expression ${base.expressionId} has kind ${base.kind}")
                    }
                    val operation = decodeIntrinsicKind(intrinsic.intrinsicKind)
                    val resultType = base.resultTy.takeIf { it != 0u }?.let(::typeOf)
                    val typeArguments = List(intrinsic.instantiatedTypeArgsLength) { index ->
                        typeOf(intrinsic.instantiatedTypeArgs(index))
                    }
                    out[base.expressionId] = ChirOtherExpression(
                        semanticId = exprSid(base.expressionId),
                        operation = operation.canonicalName,
                        operands = operands(base),
                        resultType = resultType,
                        resultLocalValue = resultLocalValue(base),
                        intrinsicKind = operation,
                        intrinsicTypeArguments = typeArguments,
                    )
                }
                ExpressionElem.Tuple -> root.exprs(Tuple(), i)?.let { t ->
                    val base = (t as Tuple).base ?: return@let
                    if (base.kind != CHIRExprKind.TUPLE) {
                        throw ChirSerializationException("tuple expression ${base.expressionId} has kind ${base.kind}")
                    }
                    if (base.resultTy == 0u) {
                        throw ChirSerializationException("tuple expression ${base.expressionId} has no result type")
                    }
                    out[base.expressionId] = ChirTupleExpression(
                        semanticId = exprSid(base.expressionId),
                        elements = operands(base),
                        resultType = typeOf(base.resultTy),
                        resultLocalValue = resultLocalValue(base),
                    )
                }
                ExpressionElem.Field -> root.exprs(Field(), i)?.let { t ->
                    val field = t as Field
                    val base = field.base ?: return@let
                    if (base.kind != CHIRExprKind.FIELD) {
                        throw ChirSerializationException("field expression ${base.expressionId} has kind ${base.kind}")
                    }
                    if (base.resultTy == 0u) {
                        throw ChirSerializationException("field expression ${base.expressionId} has no result type")
                    }
                    val operands = operands(base)
                    if (operands.size != 1) {
                        throw ChirSerializationException("field expression ${base.expressionId} must have one base operand")
                    }
                    out[base.expressionId] = ChirFieldExpression(
                        semanticId = exprSid(base.expressionId),
                        base = operands.single(),
                        path = List(field.pathLength) { index -> field.path(index) },
                        resultType = typeOf(base.resultTy),
                        resultLocalValue = resultLocalValue(base),
                    )
                }
                ExpressionElem.TypeCast -> root.exprs(TypeCast(), i)?.let { t ->
                    val base = (t as TypeCast).base ?: return@let
                    val ops = operands(base)
                    val operation = operationOf(base.expressionId)
                        ?: throw ChirSerializationException("typecast expression ${base.expressionId} is missing operation metadata")
                    val operationKind = ChirOtherOperation.parse(operation)
                        ?: throw ChirSerializationException("typecast expression ${base.expressionId} has unknown operation '$operation'")
                    val wireStrategy = decodeOverflowStrategy((t as TypeCast).overflowStrategy)
                    if (operationKind.typeCastOverflowStrategy != null &&
                        operationKind.typeCastOverflowStrategy != wireStrategy
                    ) {
                        throw ChirSerializationException(
                            "typecast expression ${base.expressionId} has strategy $wireStrategy, " +
                                "but operation ${operationKind.canonicalName} encodes ${operationKind.typeCastOverflowStrategy}",
                        )
                    }
                    if (operationKind.typeCastOverflowStrategy == null &&
                        operationKind.family == ChirOtherOperationFamily.LLVM_CAST &&
                        wireStrategy != ChirOverflowStrategy.NOT_APPLICABLE
                    ) {
                        throw ChirSerializationException(
                            "lowered LLVM cast ${operationKind.canonicalName} has an unexpected overflow strategy $wireStrategy",
                        )
                    }
                    out[base.expressionId] = ChirOtherExpression(
                        exprSid(base.expressionId),
                        operation,
                        ops,
                        typeOf(base.resultTy),
                        resultLocalValue = resultLocalValue(base),
                    )
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
                    val encodedOperation = operationOf(base.expressionId)
                    val sourceCodeIdentifier = t.srcCodeIdentifier
                        ?: throw ChirSerializationException("debug expression ${base.expressionId} is missing operation metadata")
                    val debugMetadata = if (encodedOperation == DEBUG_METADATA_OPERATION) {
                        ChirDebugMetadata(
                            sourceCodeIdentifier = sourceCodeIdentifier,
                            accurateEnvironmentObjectType = t.accurateEnvObjTy.takeIf { it != 0u }?.let(::typeOf),
                        )
                    } else {
                        null
                    }
                    val operation = if (debugMetadata != null) sourceCodeIdentifier else encodedOperation ?: sourceCodeIdentifier
                    val resultType = if (base.resultTy == 0u) null else typeOf(base.resultTy)
                    out[base.expressionId] = ChirOtherExpression(
                        semanticId = exprSid(base.expressionId),
                        operation = operation,
                        operands = ops,
                        resultType = resultType,
                        resultLocalValue = resultLocalValue(base),
                        debugMetadata = debugMetadata,
                    )
                }
            }
        }
        return out
    }

    /**
     * 将 FlatBuffers 一元表达式 kind 解码为 CHIR 操作名。
     */
    private fun decodeUnaryExprKind(kind: UByte): String = when (kind) {
        CHIRExprKind.NEG -> ChirUnaryOperator.INT_NEG.canonicalName
        CHIRExprKind.BITNOT -> ChirUnaryOperator.BIT_NOT.canonicalName
        CHIRExprKind.NOT -> ChirUnaryOperator.LOGICAL_NOT.canonicalName
        else -> throw ChirSerializationException("cannot decode unary expression kind $kind without operation metadata")
    }

    /**
     * 将 FlatBuffers 二元表达式 kind 解码为 CHIR 操作名。
     */
    private fun decodeBinaryExprKind(kind: UByte): String = when (kind) {
        CHIRExprKind.ADD -> ChirBinaryOperator.ADD.canonicalName
        CHIRExprKind.SUB -> ChirBinaryOperator.SUB.canonicalName
        CHIRExprKind.MUL -> ChirBinaryOperator.MUL.canonicalName
        CHIRExprKind.EXP -> ChirBinaryOperator.POWER.canonicalName
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

    /**
     * 序列化阶段登记的类型引用。
     */
    private data class EncType(
        val id: UInt,
        val ref: ChirTypeRef,
        val enumConstructorOwnerTypeId: UInt? = null,
    )

    private data class EncNominalType(
        val definitionId: UInt,
        val typeId: UInt,
        val type: ChirType,
    )

    /**
     * 序列化阶段登记的值节点分类。
     */
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
    /**
     * 序列化阶段登记的值节点及其所属上下文。
     */
    private data class EncValue(
        /**
         * FlatBuffers 值表中的本地编号。
         */
        val id: UInt,

        /**
         * 值节点编码分类。
         */
        val kind: EncValueKind,

        /**
         * 所属模块语义标识。
         */
        val moduleSemanticId: String? = null,

        /**
         * 所属模块名称。
         */
        val moduleName: String? = null,

        /**
         * 所属函数语义标识。
         */
        val ownerFunctionSemanticId: String? = null,

        /**
         * 函数值对应的 CHIR 函数声明。
         */
        val function: ChirFunctionDeclaration? = null,

        /**
         * 参数值对应的 CHIR 变量声明。
         */
        val parameter: org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration? = null,

        /**
         * 基本块值对应的 CHIR 基本块。
         */
        val block: ChirBlock? = null,

        /**
         * 普通值节点。
         */
        val value: ChirValue? = null,
    )

    /**
     * 序列化阶段登记的表达式或终结指令节点。
     */
    private data class EncExpr(val id: UInt, val parentBlockSemanticId: String, val semanticId: String, val node: Any)

    /**
     * 序列化上下文，负责为类型、值和表达式分配稳定编号。
     */
    private class EncodeContext(pkg: ChirPackage) {
        val packageName: String = pkg.name

        private val interfaceClassNames = pkg.allDeclarations
            .filterIsInstance<ChirClassDeclaration>()
            .filter(ChirClassDeclaration::isInterface)
            .flatMap { declaration -> listOf(declaration.name, declaration.typeDeclarationId.qualifiedName) }
            .toSet()

        fun classDefKind(typeName: String): Short =
            if (typeName in interfaceClassNames) ClassDefKind.INTERFACE else ClassDefKind.CLASS

        /**
         * 已登记类型列表。
         */
        val types = mutableListOf<EncType>()

        /**
         * 已登记值列表。
         */
        val values = mutableListOf<EncValue>()

        /**
         * 已登记表达式列表。
         */
        val exprs = mutableListOf<EncExpr>()

        val nominalTypes = mutableListOf<EncNominalType>()

        /**
         * 类型渲染名到本地编号的索引。
         */
        private val typeByName = linkedMapOf<ChirTypeRef, UInt>()

        private val nominalDefinitionIdByTypeId = linkedMapOf<UInt, UInt>()

        private val nominalTypeByIdentity = linkedMapOf<Pair<ChirTypeKind, ChirTypeDeclarationId>, EncNominalType>()

        private val enumConstructorTypeIdByDefinitionAndIndex = linkedMapOf<Pair<UInt, Int>, UInt>()

        private val enumConstructorOwnerTypeIdByTypeId = linkedMapOf<UInt, UInt>()

        /**
         * 值语义标识到本地编号的索引。
         */
        private val valueBySemantic = linkedMapOf<String, UInt>()

        /** 类型表发射前关闭注册；编码阶段只能引用完整收集阶段登记过的类型。 */
        private var typeRegistryFrozen = false

        /**
         * 表达式语义标识到本地编号的索引。
         */
        private val exprBySemantic = linkedMapOf<String, UInt>()
        init {
            // 每个 Block 的 Value.type 都是 Unit，即使函数体没有 Unit 表达式也必须进入类型表。
            typeIdOf(unitType)
            pkg.modules.forEach { m ->
                m.declarations.filterIsInstance<ChirFunctionDeclaration>().forEach { fn ->
                    registerValue(fn.semanticId.value, EncValueKind.Function, m.semanticId.value, m.name, fn.semanticId.value, fn, null, null, null)
                    typeIdOf(fn.returnType)
                    fn.genericTypeParameters.forEach(::typeIdOf)
                    fn.parameters.forEach { p -> registerValue(p.semanticId.value, EncValueKind.Parameter, m.semanticId.value, m.name, fn.semanticId.value, fn, p, null, null); typeIdOf(p.type) }
                    fn.blocks.forEach { b ->
                        registerValue(b.semanticId.value, EncValueKind.Block, m.semanticId.value, m.name, fn.semanticId.value, fn, null, b, null)
                        b.expressions.forEach { x -> registerExpr(x.semanticId.value, b.semanticId.value, x); collectValues(x) }
                        b.terminator?.let { terminator ->
                            registerExpr(terminator.semanticId.value, b.semanticId.value, terminator)
                            collectValues(terminator)
                        }
                    }
                }
            }
            types.toList().forEach { encodedType ->
                val enumType = (encodedType.ref as? ChirResolvedTypeRef)?.type as? ChirEnumType ?: return@forEach
                val definitionId = nominalDefinitionIdByTypeId[encodedType.id]
                    ?: throw ChirSerializationException("enum type #${encodedType.id} has no nominal definition id")
                enumType.cases.forEachIndexed { caseIndex, enumCase ->
                    val functionType = ChirResolvedTypeRef(
                        ChirFunctionType(
                            parameterTypes = enumCase.payloadTypes,
                            returnType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT),
                        ),
                    )
                    val functionTypeId = (types.size + 1).toUInt()
                    types += EncType(functionTypeId, functionType, encodedType.id)
                    enumConstructorTypeIdByDefinitionAndIndex[definitionId to caseIndex] = functionTypeId
                    enumConstructorOwnerTypeIdByTypeId[functionTypeId] = encodedType.id
                }
            }
            typeRegistryFrozen = true
        }
        /**
         * 返回类型引用的本地编号，不存在时自动登记。
         */
        fun typeIdOf(type: ChirTypeRef): UInt {
            typeByName[type]?.let { return it }
            if (typeRegistryFrozen) {
                throw ChirSerializationException(
                    "type registry is frozen; type '${type.renderName}' was not collected before serialization",
                )
            }
            typeChildren(type).forEach(::typeIdOf)
            val id = (types.size + 1).toUInt()
            typeByName[type] = id
            types += EncType(id, type)
            (type as? ChirResolvedTypeRef)?.type
                ?.takeIf { it is ChirStructType || it is ChirClassType || it is ChirEnumType }
                ?.let { nominalType ->
                    val (declarationId, typeArguments) = when (nominalType) {
                        is ChirStructType -> nominalType.declarationId to nominalType.typeArguments
                        is ChirClassType -> nominalType.declarationId to nominalType.typeArguments
                        is ChirEnumType -> nominalType.declarationId to nominalType.typeArguments
                        else -> error("nominal type classification changed during registration")
                    }
                    val name = declarationId.qualifiedName
                    if (typeArguments.isNotEmpty()) {
                        throw ChirSerializationException(
                            "generic nominal type '$name' cannot be serialized without its declaration-level type parameters",
                        )
                    }
                    val identity = nominalType.kind to declarationId
                    val prior = nominalTypeByIdentity[identity]
                    val nominalDefinition = if (prior == null) {
                        EncNominalType((nominalTypes.size + 1).toUInt(), id, nominalType).also { definition ->
                            nominalTypes += definition
                            nominalTypeByIdentity[identity] = definition
                        }
                    } else {
                        if (prior.type != nominalType) {
                            throw ChirSerializationException(
                                "nominal type '$name' has conflicting structural layouts in one CHIR package",
                            )
                        }
                        prior
                    }
                    nominalDefinitionIdByTypeId[id] = nominalDefinition.definitionId
                }
            return id
        }

        private fun typeChildren(typeRef: ChirTypeRef): List<ChirTypeRef> {
            val type = (typeRef as? ChirResolvedTypeRef)?.type ?: return emptyList()
            return when (type) {
                is ChirPrimitiveType, is ChirNamedType,
                is ChirThisType, is ChirCStringType,
                -> emptyList()

                is ChirTupleType -> type.elementTypes
                is ChirFunctionType -> type.parameterTypes + listOfNotNull(type.receiverType, type.returnType)
                is ChirStructType -> type.typeArguments + type.fieldTypes
                is ChirClassType -> type.typeArguments + type.superTypes + type.fieldTypes
                is ChirEnumType -> type.typeArguments + type.cases.flatMap { it.payloadTypes }
                is ChirRawArrayType -> listOf(type.elementType)
                is ChirVArrayType -> listOf(type.elementType)
                is ChirCPointerType -> listOf(type.pointeeType)
                is ChirGenericType -> type.upperBounds
                is ChirRefType -> listOf(type.referencedType)
                is ChirBoxType -> listOf(type.boxedType)
            }
        }

        fun nominalDefinitionId(typeId: UInt): UInt = nominalDefinitionIdByTypeId[typeId]
            ?: throw ChirSerializationException("nominal type #$typeId has no definition id")

        fun enumConstructorTypeId(definitionId: UInt, caseIndex: Int): UInt =
            enumConstructorTypeIdByDefinitionAndIndex[definitionId to caseIndex]
                ?: throw ChirSerializationException("enum definition #$definitionId has no constructor type #$caseIndex")

        fun enumConstructorOwnerTypeId(typeId: UInt): UInt? = enumConstructorOwnerTypeIdByTypeId[typeId]

        /**
         * 按值语义标识读取本地编号。
         */
        fun valueIdOf(semantic: String): UInt = valueBySemantic[semantic] ?: throw ChirSerializationException("missing value id: $semantic")

        /**
         * 按表达式语义标识读取本地编号。
         */
        fun exprIdOf(semantic: String): UInt = exprBySemantic[semantic] ?: throw ChirSerializationException("missing expr id: $semantic")

        /**
         * 登记一个值节点。
         */
        private fun registerValue(semantic: String, kind: EncValueKind, moduleSid: String?, moduleName: String?, ownerFn: String?, fn: ChirFunctionDeclaration?, p: org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration?, b: ChirBlock?, value: ChirValue?) {
            val existingId = valueBySemantic[semantic]
            if (existingId != null) {
                val index = existingId.toInt() - 1
                val existing = values[index]
                if (existing.kind != kind) {
                    throw ChirSerializationException("semantic value id '$semantic' is registered with conflicting kinds")
                }
                val previousLocal = existing.value as? ChirLocalValue
                val nextLocal = value as? ChirLocalValue
                if (previousLocal != null && nextLocal != null) {
                    if (previousLocal.type != nextLocal.type) {
                        throw ChirSerializationException("local value '$semantic' is registered with conflicting types")
                    }
                    if (previousLocal.name.isNotBlank() && nextLocal.name.isNotBlank() && previousLocal.name != nextLocal.name) {
                        throw ChirSerializationException("local value '$semantic' is registered with conflicting names")
                    }
                    val associatedExpressionId = when {
                        previousLocal.associatedExpressionId == null -> nextLocal.associatedExpressionId
                        nextLocal.associatedExpressionId == null -> previousLocal.associatedExpressionId
                        previousLocal.associatedExpressionId == nextLocal.associatedExpressionId -> previousLocal.associatedExpressionId
                        else -> throw ChirSerializationException("local value '$semantic' has conflicting defining expressions")
                    }
                    values[index] = existing.copy(
                        value = previousLocal.copy(
                            name = previousLocal.name.ifBlank { nextLocal.name },
                            attributes = previousLocal.attributes + nextLocal.attributes,
                            annotations = (previousLocal.annotations + nextLocal.annotations).distinct(),
                            userIds = previousLocal.userIds + nextLocal.userIds,
                            associatedExpressionId = associatedExpressionId,
                            isReturnValue = previousLocal.isReturnValue || nextLocal.isReturnValue,
                        ),
                    )
                }
                return
            }
            val id = (valueBySemantic.size + 1).toUInt()
            valueBySemantic[semantic] = id
            values += EncValue(id, kind, moduleSid, moduleName, ownerFn, fn, p, b, value)
        }

        /**
         * 登记一个表达式或终结指令节点。
         */
        private fun registerExpr(semantic: String, parentBlockSemanticId: String, node: Any) {
            if (exprBySemantic.containsKey(semantic)) return
            val id = (exprBySemantic.size + 1).toUInt()
            exprBySemantic[semantic] = id
            exprs += EncExpr(id, parentBlockSemanticId, semantic, node)
        }

        /**
         * 收集表达式或终结指令引用到的值节点和类型引用。
         */
        private fun collectValues(node: Any) {
            val resultLocalValue = (node as? ChirExpression)?.resultLocalValue
            if (resultLocalValue != null) {
                registerValue(
                    resultLocalValue.semanticId.value,
                    EncValueKind.Local,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    resultLocalValue,
                )
                typeIdOf(resultLocalValue.type)
            }
            val valuesToAdd = when (node) {
                is ChirUnaryExpression -> listOf(node.operand)
                is ChirBinaryExpression -> listOf(node.left, node.right)
                is ChirMemoryExpression -> listOfNotNull(node.address, node.value)
                is ChirCallExpression -> {
                    node.instantiatedTypeArguments.forEach(::typeIdOf)
                    node.thisType?.let(::typeIdOf)
                    node.virtualMethodContext?.let { context ->
                        typeIdOf(ChirResolvedTypeRef(context.originalFunctionType))
                        context.genericTypeParameters.forEach(::typeIdOf)
                    }
                    if (node.virtualMethodContext == null) {
                        if (node.dispatchReceiver != null) {
                            throw ChirSerializationException("direct APPLY ${node.semanticId} carries a dispatch receiver")
                        }
                        listOf(node.callee) + node.arguments
                    } else {
                        listOf(
                            node.dispatchReceiver
                                ?: throw ChirSerializationException("virtual INVOKE ${node.semanticId} has no dispatch receiver"),
                        ) + node.arguments
                    }
                }
                is ChirTupleExpression -> node.elements
                is ChirFieldExpression -> listOf(node.base)
                is ChirConstantExpression -> listOf(node.value)
                is ChirOtherExpression -> {
                    node.intrinsicTypeArguments.forEach(::typeIdOf)
                    node.debugMetadata?.accurateEnvironmentObjectType?.let(::typeIdOf)
                    node.operands
                }
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

    /**
     * 反序列化阶段累积的值表和辅助索引。
     */
    private class DecodedValues {
        /**
         * 值编号到 CHIR 值节点的映射。
         */
        val values = linkedMapOf<UInt, ChirValue>()

        /**
         * 解码出的函数值记录。
         */
        val functions = mutableListOf<DecodedFunc>()

        /**
         * 参数值编号到参数记录的映射。
         */
        val parameters = linkedMapOf<UInt, DecodedParam>()

        /**
         * 基本块值编号到基本块记录的映射。
         */
        val blocks = linkedMapOf<UInt, DecodedBlock>()

        /**
         * 基本块值编号到语义标识的映射。
         */
        val blockSemanticByValueId = linkedMapOf<UInt, ChirSemanticId>()

        /**
         * 函数值编号到语义标识的映射。
         */
        val functionSemanticByValueId = linkedMapOf<UInt, ChirSemanticId>()
    }

    /**
     * 反序列化阶段的函数值记录。
     */
    private data class DecodedFunc(
        val valueId: UInt,
        val semanticId: ChirSemanticId,
        val name: String,
        val returnType: ChirTypeRef,
        val moduleSemanticId: ChirSemanticId,
        val moduleName: String,
        val paramValueIds: List<UInt>,
        val entryBlockValueId: UInt,
        val genericTypeParameters: List<ChirTypeRef>,
        val isStatic: Boolean,
    )

    /**
     * 反序列化阶段的参数值记录。
     */
    private data class DecodedParam(val semanticId: ChirSemanticId, val name: String, val type: ChirTypeRef, val mutable: Boolean)

    /**
     * 反序列化阶段的基本块值记录。
     */
    private data class DecodedBlock(val semanticId: ChirSemanticId, val name: String, val parentFunctionValueId: UInt, val exprIds: List<UInt>)

    /**
     * 表达式元数据记录。
     */
    private data class DecodedExprMetadata(val semanticId: String, val operation: String?)

    /**
     * 拆分值标识符中的语义标识和显示名称。
     */
    private fun valueIdentifierParts(identifier: String): List<String> {
        val parts = identifier.split("|", limit = 2)
        if (parts[0].isBlank()) {
            throw ChirSerializationException("damaged payload: value identifier has blank semantic id")
        }
        return parts
    }

    /**
     * 解析包路径字段中附带的包语义标识和表达式元数据。
     */
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

    /**
     * 获取需要写入表达式元数据的操作名称。
     */
    private fun expressionOperationName(node: Any): String? = when (node) {
        is ChirUnaryExpression -> node.operator
        is ChirBinaryExpression -> node.operator
        is ChirMemoryExpression -> node.operation
        is ChirOtherExpression -> if (node.debugMetadata == null) node.operation else DEBUG_METADATA_OPERATION
        else -> null
    }

}


