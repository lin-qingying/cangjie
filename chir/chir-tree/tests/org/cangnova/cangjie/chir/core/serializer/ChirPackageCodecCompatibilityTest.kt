package org.cangnova.cangjie.chir.core.serializer

import PackageFormat.CHIRPackage
import PackageFormat.Block
import PackageFormat.GenericType
import PackageFormat.RawArrayType
import PackageFormat.TypeElem
import PackageFormat.ValueElem
import PackageFormat.VArrayType
import com.google.flatbuffers.FlatBufferBuilder
import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirFieldExpression
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirConstantExpression
import org.cangnova.cangjie.chir.core.expression.ChirDebugMetadata
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirTupleExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.testkit.ChirTestAssertions
import org.cangnova.cangjie.chir.core.testkit.ChirTestFixtures
import org.cangnova.cangjie.chir.core.type.ChirEnumCaseType
import org.cangnova.cangjie.chir.core.type.ChirEnumType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirCStringType
import org.cangnova.cangjie.chir.core.type.ChirRawArrayType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirStructType
import org.cangnova.cangjie.chir.core.type.ChirTypeDeclarationId
import org.cangnova.cangjie.chir.core.type.ChirTupleType
import org.cangnova.cangjie.chir.core.type.ChirTypeRelations
import org.cangnova.cangjie.chir.core.type.ChirVArrayType
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirConstantKind
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.chir.core.value.ChirParameterValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

/**
 * 校验 CHIR 包编解码器与当前 FlatBuffers schema 的兼容性边界。
 *
 * 该测试覆盖正常往返、schema 版本拒绝和损坏载荷拒绝，确保序列化入口不会静默接受不兼容数据。
 */
class ChirPackageCodecCompatibilityTest {

    /**
     * 校验当前 schema 下序列化再反序列化能够保持语义等价。
     *
     * 该用例通过测试工具断言样本包的结构、标识和关键字段在往返后不发生漂移。
     */
    @Test
    fun `round trip keeps semantic equivalence for current schema`() {
        ChirTestAssertions.assertCodecRoundTrip(ChirTestFixtures.codecPackage())
    }

    @Test
    fun `round trip preserves null and string literal kinds with identical text`() {
        val source = ChirTestFixtures.codecPackage()
        val module = source.modules.single()
        val function = module.declarations.single() as DefaultChirFunctionDeclaration
        val stringType = ChirResolvedTypeRef(ChirCStringType)
        val expressions = listOf(
            ChirConstantExpression(
                semanticId = ChirSemanticId("expr:string-text-null"),
                value = ChirConstantValue(
                    semanticId = ChirSemanticId("const:string-text-null"),
                    type = stringType,
                    literal = "null",
                ),
                resultType = stringType,
                resultLocalValue = ChirLocalValue(
                    semanticId = ChirSemanticId("value:string-text-null"),
                    type = stringType,
                    name = "string-text-null",
                    associatedExpressionId = ChirSemanticId("expr:string-text-null"),
                ),
            ),
            ChirConstantExpression(
                semanticId = ChirSemanticId("expr:null-literal"),
                value = ChirConstantValue(
                    semanticId = ChirSemanticId("const:null-literal"),
                    type = stringType,
                    literal = "null",
                    constantKind = ChirConstantKind.NULL,
                ),
                resultType = stringType,
                resultLocalValue = ChirLocalValue(
                    semanticId = ChirSemanticId("value:null-literal"),
                    type = stringType,
                    name = "null-literal",
                    associatedExpressionId = ChirSemanticId("expr:null-literal"),
                ),
            ),
        )
        val packageWithLiterals = source.copy(
            modules = listOf(
                module.copy(
                    declarations = listOf(
                        function.copy(
                            blocks = function.blocks.map { block ->
                                if (block.semanticId == function.entryBlockId) {
                                    block.copy(expressions = block.expressions + expressions)
                                } else {
                                    block
                                }
                            },
                        ),
                    ),
                ),
            ),
        )

        ChirTestAssertions.assertCodecRoundTrip(packageWithLiterals)
        val restoredFunction = (ChirPackageCodec.deserialize(ChirPackageCodec.serialize(packageWithLiterals))
            .modules.single().declarations.single() as DefaultChirFunctionDeclaration)
        val restoredConstants = restoredFunction.blocks
            .flatMap { it.expressions }
            .filterIsInstance<ChirConstantExpression>()
            .associateBy { it.semanticId }

        assertEquals(ChirConstantKind.UNSPECIFIED, (restoredConstants.getValue(expressions[0].semanticId).value as ChirConstantValue).constantKind)
        assertEquals(ChirConstantKind.NULL, (restoredConstants.getValue(expressions[1].semanticId).value as ChirConstantValue).constantKind)
    }

    @Test
    fun `round trip preserves static member function metadata`() {
        val source = ChirTestFixtures.codecPackage()
        val module = source.modules.single()
        val function = module.declarations.single() as DefaultChirFunctionDeclaration
        val staticPackage = source.copy(
            modules = listOf(module.copy(declarations = listOf(function.copy(isStatic = true)))),
        )

        ChirTestAssertions.assertCodecRoundTrip(staticPackage)
        val restored = ChirPackageCodec.deserialize(ChirPackageCodec.serialize(staticPackage))
        val restoredFunction = restored.modules.single().declarations.single() as DefaultChirFunctionDeclaration
        assertTrue(restoredFunction.isStatic)
    }

    @Test
    fun `round trip preserves debug metadata and unterminated dead blocks`() {
        val source = ChirTestFixtures.codecPackage()
        val module = source.modules.single()
        val function = module.declarations.single() as DefaultChirFunctionDeclaration
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val debugExpression = ChirOtherExpression(
            semanticId = ChirSemanticId("expr:debug-metadata"),
            operation = "local",
            operands = listOf(ChirConstantValue(ChirSemanticId("const:debug"), intType, "1")),
            debugMetadata = ChirDebugMetadata("local", intType),
        )
        val constantExpressionId = ChirSemanticId("expr:constant-materialization")
        val constantExpression = ChirConstantExpression(
            semanticId = constantExpressionId,
            value = ChirConstantValue(ChirSemanticId("const:materialized"), intType, "17"),
            resultType = intType,
            resultLocalValue = ChirLocalValue(
                semanticId = ChirSemanticId("value:constant-materialized"),
                type = intType,
                name = "constant-materialized",
                associatedExpressionId = constantExpressionId,
            ),
        )
        val deadBlock = ChirBlock(
            semanticId = ChirSemanticId("block:unterminated-dead"),
            name = "unterminated-dead",
            expressions = emptyList(),
            terminator = null,
        )
        val updatedFunction = function.copy(
            blocks = function.blocks.map { block ->
                if (block.semanticId == function.entryBlockId) {
                    block.copy(expressions = block.expressions + debugExpression + constantExpression)
                } else {
                    block
                }
            } + deadBlock,
        )
        val packageWithDebugMetadata = source.copy(
            modules = listOf(module.copy(declarations = listOf(updatedFunction))),
        )

        ChirTestAssertions.assertCodecRoundTrip(packageWithDebugMetadata)
        val restored = ChirPackageCodec.deserialize(ChirPackageCodec.serialize(packageWithDebugMetadata))
        val restoredFunction = restored.modules.single().declarations.single() as DefaultChirFunctionDeclaration
        val restoredDebug = restoredFunction.blocks.single { it.semanticId == function.entryBlockId }
            .expressions.single { it.semanticId == debugExpression.semanticId } as ChirOtherExpression
        assertEquals(debugExpression.debugMetadata, restoredDebug.debugMetadata)
        val restoredConstant = restoredFunction.blocks.single { it.semanticId == function.entryBlockId }
            .expressions.single { it.semanticId == constantExpressionId } as ChirConstantExpression
        assertEquals(constantExpression.value, restoredConstant.value)
        assertEquals(constantExpression.resultLocalValue, restoredConstant.resultLocalValue)
        assertEquals(null, restoredFunction.blocks.single { it.semanticId == deadBlock.semanticId }.terminator)
    }

    @Test
    fun `round trip preserves floating point and rune constants`() {
        val source = ChirTestFixtures.codecPackage()
        val module = source.modules.single()
        val function = module.declarations.single() as DefaultChirFunctionDeclaration
        val floatType = ChirResolvedTypeRef(ChirPrimitiveType.FLOAT64)
        val runeType = ChirResolvedTypeRef(ChirPrimitiveType.RUNE)
        val floatExpressionId = ChirSemanticId("expr:float-literal")
        val runeExpressionId = ChirSemanticId("expr:rune-literal")
        val expressions = listOf(
            ChirConstantExpression(
                semanticId = floatExpressionId,
                value = ChirConstantValue(ChirSemanticId("const:float-literal"), floatType, "-1.25e+12"),
                resultType = floatType,
                resultLocalValue = ChirLocalValue(
                    semanticId = ChirSemanticId("value:float-literal"),
                    type = floatType,
                    name = "float-literal",
                    associatedExpressionId = floatExpressionId,
                ),
            ),
            ChirConstantExpression(
                semanticId = runeExpressionId,
                value = ChirConstantValue(ChirSemanticId("const:rune-literal"), runeType, "128512"),
                resultType = runeType,
                resultLocalValue = ChirLocalValue(
                    semanticId = ChirSemanticId("value:rune-literal"),
                    type = runeType,
                    name = "rune-literal",
                    associatedExpressionId = runeExpressionId,
                ),
            ),
        )
        val updatedFunction = function.copy(
            blocks = function.blocks.map { block ->
                if (block.semanticId == function.entryBlockId) {
                    block.copy(expressions = block.expressions + expressions)
                } else {
                    block
                }
            },
        )
        val packageWithNumericConstants = source.copy(
            modules = listOf(module.copy(declarations = listOf(updatedFunction))),
        )

        ChirTestAssertions.assertCodecRoundTrip(packageWithNumericConstants)
        val restored = ChirPackageCodec.deserialize(ChirPackageCodec.serialize(packageWithNumericConstants))
        val restoredConstants = (restored.modules.single().declarations.single() as DefaultChirFunctionDeclaration)
            .blocks.flatMap { it.expressions }.filterIsInstance<ChirConstantExpression>()

        val restoredById = restoredConstants.associateBy { it.semanticId }
        val restoredFloat = assertInstanceOf(ChirConstantExpression::class.java, restoredById.getValue(floatExpressionId))
        val restoredRune = assertInstanceOf(ChirConstantExpression::class.java, restoredById.getValue(runeExpressionId))
        assertEquals(floatType, restoredFloat.value.type)
        assertEquals(
            "-1.25e+12".toDouble().toBits(),
            restoredFloat.value.literal.toDouble().toBits(),
        )
        assertEquals(runeType, restoredRune.value.type)
        assertEquals(128512u, restoredRune.value.literal.toUInt())
    }

    /**
     * 官方 CHIR 的表达式 ID 与结果局部值 ID 属于不同域，编解码不得把二者折叠。
     */
    @Test
    fun `round trip preserves independent expression and result value identities`() {
        val original = ChirTestFixtures.codecPackage()
        val module = original.modules.single()
        val function = module.declarations.single() as DefaultChirFunctionDeclaration
        val block = function.blocks.first { candidate -> candidate.expressions.any { it is ChirCallExpression } }
        val call = block.expressions.filterIsInstance<ChirCallExpression>().single()
        val resultLocal = ChirLocalValue(
            semanticId = ChirSemanticId("value:call-result"),
            type = call.resultType,
            name = "call-result",
            associatedExpressionId = call.semanticId,
        )
        val updatedBlock = block.copy(
            expressions = block.expressions.map { expression ->
                if (expression.semanticId == call.semanticId) call.copy(resultLocalValue = resultLocal) else expression
            },
        )
        val updatedFunction = function.copy(
            blocks = function.blocks.map { if (it.semanticId == block.semanticId) updatedBlock else it },
        )
        val packageWithIndependentIds = original.copy(
            modules = listOf(module.copy(declarations = listOf(updatedFunction))),
        )

        val decoded = ChirPackageCodec.deserialize(ChirPackageCodec.serialize(packageWithIndependentIds))
        val decodedCall = (decoded.modules.single().declarations.single() as DefaultChirFunctionDeclaration)
            .blocks.first { candidate -> candidate.expressions.any { it is ChirCallExpression } }
            .expressions.filterIsInstance<ChirCallExpression>().single()

        assertEquals(call.semanticId, decodedCall.semanticId)
        assertEquals(resultLocal, decodedCall.resultLocalValue)
        assertFalse(decodedCall.semanticId == decodedCall.resultLocalValue?.semanticId)
    }

    /**
     * 校验类型表和定义表往返保留结构体字段布局与枚举选择器语义。
     */
    @Test
    fun `round trip preserves nominal field layouts and enum selector types`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val structType = ChirResolvedTypeRef(
            ChirStructType(ChirTypeDeclarationId("test", "Record"), fieldTypes = listOf(intType, boolType)),
        )
        val enumType = ChirResolvedTypeRef(
            ChirEnumType(
                declarationId = ChirTypeDeclarationId("test", "OptionLike"),
                cases = listOf(
                    ChirEnumCaseType("None"),
                    ChirEnumCaseType("Some", listOf(intType)),
                ),
            ),
        )
        val structField = fieldFunction(
            functionId = "fn:record-field",
            functionName = "recordField",
            parameterType = structType,
            parameterName = "record",
            fieldIndex = 1uL,
            resultType = boolType,
        )
        val enumSelector = fieldFunction(
            functionId = "fn:option-selector",
            functionName = "optionSelector",
            parameterType = enumType,
            parameterName = "option",
            fieldIndex = 0uL,
            resultType = boolType,
        )
        val chirPackage = ChirPackage(
            semanticId = ChirSemanticId("pkg:nominal-codec"),
            name = "nominalCodec",
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:nominal-codec"),
                    name = "nominalCodec",
                    declarations = listOf(structField, enumSelector),
                ),
            ),
        )

        val bytes = ChirPackageCodec.serialize(chirPackage)
        val decoded = ChirPackageCodec.deserialize(bytes)
        val decodedFunctions = decoded.modules.single().declarations.filterIsInstance<DefaultChirFunctionDeclaration>()
        val decodedStructField = decodedFunctions.single { it.name == "recordField" }
            .blocks.single().expressions.single() as ChirFieldExpression
        val decodedEnumSelector = decodedFunctions.single { it.name == "optionSelector" }
            .blocks.single().expressions.single() as ChirFieldExpression
        val decodedStruct = assertInstanceOf(ChirStructType::class.java, (decodedStructField.base.type as ChirResolvedTypeRef).type)
        val decodedEnum = assertInstanceOf(ChirEnumType::class.java, (decodedEnumSelector.base.type as ChirResolvedTypeRef).type)

        assertEquals(listOf(intType, boolType), decodedStruct.fieldTypes)
        assertEquals(listOf(ChirEnumCaseType("None"), ChirEnumCaseType("Some", listOf(intType))), decodedEnum.cases)
        assertFalse(decodedEnum.nonExhaustive)
        assertEquals(ChirPrimitiveType.BOOL, decodedEnum.selectorType)
        assertEquals(
            boolType,
            ChirTypeRelations.projectFieldType(decodedEnumSelector.base.type, decodedEnumSelector.path)?.resultType,
        )
    }

    /**
     * 官方 CHIR 将 VArray 长度存为 int64、RawArray 维数存为 uint32；仓内模型和 codec 必须保留原宽度。
     */
    @Test
    fun `round trip preserves 64 bit VArray size`() {
        val size = Int.MAX_VALUE.toLong() + 1L
        val dimensions = UInt.MAX_VALUE
        val varrayType = ChirResolvedTypeRef(
            ChirVArrayType(ChirResolvedTypeRef(ChirPrimitiveType.INT32), size),
        )
        val rawArrayType = ChirResolvedTypeRef(
            ChirRawArrayType(ChirResolvedTypeRef(ChirPrimitiveType.INT32), dimensions),
        )
        val aggregateType = ChirResolvedTypeRef(ChirTupleType(listOf(varrayType, rawArrayType)))
        val functionId = ChirSemanticId("fn:large-varray")
        val parameterId = ChirSemanticId("fn:large-varray:param")
        val blockId = ChirSemanticId("fn:large-varray:block")
        val function = DefaultChirFunctionDeclaration(
            semanticId = functionId,
            name = "largeVArray",
            returnType = aggregateType,
            parameters = listOf(
                DefaultChirVariableDeclaration(parameterId, "value", aggregateType, mutable = false),
            ),
            blocks = listOf(
                ChirBlock(
                    semanticId = blockId,
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("fn:large-varray:return"),
                        returnValue = ChirParameterValue(parameterId, aggregateType, "value", functionId),
                    ),
                ),
            ),
            entryBlockId = blockId,
        )
        val chirPackage = ChirPackage(
            semanticId = ChirSemanticId("pkg:large-varray"),
            name = "largeVArray",
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:large-varray"),
                    name = "largeVArray",
                    declarations = listOf(function),
                ),
            ),
        )

        val bytes = ChirPackageCodec.serialize(chirPackage)
        val root = CHIRPackage.getRootAsCHIRPackage(ByteBuffer.wrap(bytes))
        val varrayTypeIndex = (0 until root.typesLength)
            .single { root.typesType(it) == TypeElem.VArrayType }
        val encodedVArray = root.types(VArrayType(), varrayTypeIndex) as VArrayType
        val rawArrayTypeIndex = (0 until root.typesLength)
            .single { root.typesType(it) == TypeElem.RawArrayType }
        val encodedRawArray = root.types(RawArrayType(), rawArrayTypeIndex) as RawArrayType
        val decoded = ChirPackageCodec.deserialize(bytes)
        val decodedType = decoded.modules.single().declarations
            .filterIsInstance<DefaultChirFunctionDeclaration>()
            .single()
            .returnType as ChirResolvedTypeRef
        val decodedTuple = decodedType.type as ChirTupleType

        assertEquals(size, encodedVArray.size)
        assertEquals(dimensions, encodedRawArray.dims)
        assertEquals(size, ((decodedTuple.elementTypes[0] as ChirResolvedTypeRef).type as ChirVArrayType).size)
        assertEquals(
            dimensions,
            ((decodedTuple.elementTypes[1] as ChirResolvedTypeRef).type as ChirRawArrayType).dimensions,
        )
    }

    /**
     * 校验 UnitLiteral 可作为 tuple operand 经当前 CHIR schema 往返。
     */
    @Test
    fun `round trip preserves Unit tuple operands`() {
        val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val tupleType = ChirResolvedTypeRef(ChirTupleType(listOf(unitType, intType)))
        val tupleId = ChirSemanticId("expr:unit-tuple")
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:unit-tuple"),
            name = "unitTuple",
            returnType = tupleType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:unit-tuple"),
                    name = "entry",
                    expressions = listOf(
                        ChirTupleExpression(
                            semanticId = tupleId,
                            elements = listOf(
                                ChirConstantValue(ChirSemanticId("const:unit-tuple:unit"), unitType, "unit"),
                                ChirConstantValue(ChirSemanticId("const:unit-tuple:int"), intType, "42"),
                            ),
                            resultType = tupleType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:unit-tuple:return"),
                        returnValue = ChirLocalValue(tupleId, tupleType, "tuple"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:unit-tuple"),
        )
        val chirPackage = ChirPackage(
            semanticId = ChirSemanticId("pkg:unit-tuple"),
            name = "unitTuple",
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:unit-tuple"),
                    name = "unitTuple",
                    declarations = listOf(function),
                ),
            ),
        )

        val decoded = ChirPackageCodec.deserialize(ChirPackageCodec.serialize(chirPackage))
        val decodedTuple = decoded.modules.single().declarations
            .filterIsInstance<DefaultChirFunctionDeclaration>()
            .single()
            .blocks.single().expressions.filterIsInstance<ChirTupleExpression>().single()

        assertEquals(
            listOf(
                ChirConstantValue(ChirSemanticId("const:unit-tuple:unit"), unitType, "unit"),
                ChirConstantValue(ChirSemanticId("const:unit-tuple:int"), intType, "42"),
            ),
            decodedTuple.elements,
        )
        assertEquals(tupleType, decodedTuple.resultType)
    }

    /**
     * 校验 Bool 使用独立的 BoolLiteral union，并且没有显式 Unit 常量时 Block 仍引用已登记的 Unit 类型。
     */
    @Test
    fun `round trip preserves Bool literals and block Unit type registration`() {
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val tupleType = ChirResolvedTypeRef(ChirTupleType(listOf(boolType, boolType)))
        val tupleId = ChirSemanticId("expr:bool-tuple")
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:bool-tuple"),
            name = "boolTuple",
            returnType = tupleType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:bool-tuple"),
                    name = "entry",
                    expressions = listOf(
                        ChirTupleExpression(
                            semanticId = tupleId,
                            elements = listOf(
                                ChirConstantValue(ChirSemanticId("const:bool-tuple:true"), boolType, "true"),
                                ChirConstantValue(ChirSemanticId("const:bool-tuple:false"), boolType, "false"),
                            ),
                            resultType = tupleType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:bool-tuple:return"),
                        returnValue = ChirLocalValue(tupleId, tupleType, "tuple"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:bool-tuple"),
        )
        val chirPackage = ChirPackage(
            semanticId = ChirSemanticId("pkg:bool-tuple"),
            name = "boolTuple",
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:bool-tuple"),
                    name = "boolTuple",
                    declarations = listOf(function),
                ),
            ),
        )

        val bytes = ChirPackageCodec.serialize(chirPackage)
        val decoded = ChirPackageCodec.deserialize(bytes)
        val decodedTuple = decoded.modules.single().declarations
            .filterIsInstance<DefaultChirFunctionDeclaration>()
            .single()
            .blocks.single().expressions.filterIsInstance<ChirTupleExpression>().single()

        assertEquals(
            listOf(
                ChirConstantValue(ChirSemanticId("const:bool-tuple:true"), boolType, "true"),
                ChirConstantValue(ChirSemanticId("const:bool-tuple:false"), boolType, "false"),
            ),
            decodedTuple.elements,
        )
        assertEquals(tupleType, decodedTuple.resultType)

        val root = CHIRPackage.getRootAsCHIRPackage(ByteBuffer.wrap(bytes))
        val blockIndex = (0 until root.valuesLength).single { root.valuesType(it) == ValueElem.Block }
        val block = root.values(Block(), blockIndex) as Block
        val booleanTypeIndex = (0 until root.typesLength).single { root.typesType(it) == TypeElem.BooleanType }
        assertTrue(block.base!!.mutateType((booleanTypeIndex + 1).toUInt()))
        assertThrows(ChirSerializationException::class.java) {
            ChirPackageCodec.deserialize(bytes)
        }
    }

    /**
     * 校验泛型上界只写入 GenericType.upperBounds，不会误写到 Type.argTys。
     */
    @Test
    fun `serialize stores generic upper bounds in their schema field`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val genericType = ChirResolvedTypeRef(
            org.cangnova.cangjie.chir.core.type.ChirGenericType(
                identifier = "T",
                upperBounds = listOf(intType),
            ),
        )
        val parameterId = ChirSemanticId("fn:generic-bound:param")
        val functionId = ChirSemanticId("fn:generic-bound")
        val function = DefaultChirFunctionDeclaration(
            semanticId = functionId,
            name = "genericBound",
            returnType = genericType,
            parameters = listOf(
                DefaultChirVariableDeclaration(parameterId, "value", genericType, mutable = false),
            ),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:generic-bound"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:generic-bound:return"),
                        returnValue = ChirParameterValue(parameterId, genericType, "value", functionId),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:generic-bound"),
        )
        val chirPackage = ChirPackage(
            semanticId = ChirSemanticId("pkg:generic-bound"),
            name = "genericBound",
            modules = listOf(
                ChirModule(ChirSemanticId("mod:generic-bound"), "genericBound", listOf(function)),
            ),
        )

        val bytes = ChirPackageCodec.serialize(chirPackage)
        val root = CHIRPackage.getRootAsCHIRPackage(ByteBuffer.wrap(bytes))
        val genericIndex = (0 until root.typesLength).single { root.typesType(it) == TypeElem.GenericType }
        val generic = root.types(GenericType(), genericIndex) as GenericType

        assertEquals(0, generic.base?.argTysLength)
        assertEquals(1, generic.upperBoundsLength)
    }

    /**
     * 校验反序列化会拒绝高于当前支持版本的 schema。
     *
     * 该用例直接篡改 FlatBuffers 根节点版本字段，固定版本兼容策略的失败行为。
     */
    @Test
    fun `deserialize rejects unsupported schema version`() {
        val bytes = ChirPackageCodec.serialize(ChirTestFixtures.codecPackage())
        val root = CHIRPackage.getRootAsCHIRPackage(ByteBuffer.wrap(bytes))
        root.mutatePhase((ChirSerializationSchema.CURRENT_VERSION + 1).toUByte())

        assertThrows(ChirSerializationException::class.java) {
            ChirPackageCodec.deserialize(bytes)
        }
    }

    /**
     * 校验反序列化会拒绝结构损坏的载荷。
     *
     * 该用例构造字段不完整的包文本，确保解码失败会以 `ChirSerializationException` 暴露。
     */
    @Test
    fun `deserialize rejects damaged payload`() {
        val builder = FlatBufferBuilder(256)
        val nameOffset = builder.createString("bad")
        val pathOffset = builder.createString("S|1\nP|pkg:bad|bad\nM|mod:bad|bad\nF|broken")
        val packageOffset = CHIRPackage.createCHIRPackage(
            builder = builder,
            nameOffset = nameOffset,
            pathOffset = pathOffset,
            pkgAccessLevel = 0u,
            typesTypeOffset = 0,
            typesOffset = 0,
            valuesTypeOffset = 0,
            valuesOffset = 0,
            exprsTypeOffset = 0,
            exprsOffset = 0,
            defsTypeOffset = 0,
            defsOffset = 0,
            packageInitFunc = 0u,
            phase = ChirSerializationSchema.CURRENT_VERSION.toUByte(),
            packageLiteralInitFunc = 0u,
            maxImportedValueId = 0u,
            maxImportedStructId = 0u,
            maxImportedClassId = 0u,
            maxImportedEnumId = 0u,
            maxImportedExtendId = 0u,
        )
        CHIRPackage.finishCHIRPackageBuffer(builder, packageOffset)

        assertThrows(ChirSerializationException::class.java) {
            ChirPackageCodec.deserialize(builder.sizedByteArray())
        }
    }

    private fun fieldFunction(
        functionId: String,
        functionName: String,
        parameterType: ChirResolvedTypeRef,
        parameterName: String,
        fieldIndex: ULong,
        resultType: ChirResolvedTypeRef,
    ): DefaultChirFunctionDeclaration {
        val functionSemanticId = ChirSemanticId(functionId)
        val parameterSemanticId = ChirSemanticId("$functionId:param")
        val fieldSemanticId = ChirSemanticId("$functionId:field")
        val parameter = DefaultChirVariableDeclaration(
            semanticId = parameterSemanticId,
            name = parameterName,
            type = parameterType,
            mutable = false,
        )
        val field = ChirFieldExpression(
            semanticId = fieldSemanticId,
            base = ChirParameterValue(
                semanticId = parameterSemanticId,
                type = parameterType,
                name = parameterName,
                ownerFunctionId = functionSemanticId,
            ),
            path = listOf(fieldIndex),
            resultType = resultType,
        )
        return DefaultChirFunctionDeclaration(
            semanticId = functionSemanticId,
            name = functionName,
            returnType = resultType,
            parameters = listOf(parameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("$functionId:block"),
                    name = "entry",
                    expressions = listOf(field),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("$functionId:return"),
                        returnValue = ChirLocalValue(fieldSemanticId, resultType, "field"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("$functionId:block"),
        )
    }
}
