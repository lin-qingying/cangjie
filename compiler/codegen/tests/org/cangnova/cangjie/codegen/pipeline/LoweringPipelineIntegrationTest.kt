package org.cangnova.cangjie.codegen.pipeline

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirStructDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryOperation
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherOperation
import org.cangnova.cangjie.chir.core.expression.ChirTupleExpression
import org.cangnova.cangjie.chir.core.expression.ChirFieldExpression
import org.cangnova.cangjie.chir.core.expression.ChirIntrinsicKind
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirRefType
import org.cangnova.cangjie.chir.core.type.ChirCPointerType
import org.cangnova.cangjie.chir.core.type.ChirClassType
import org.cangnova.cangjie.chir.core.type.ChirEnumCaseType
import org.cangnova.cangjie.chir.core.type.ChirEnumType
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirGenericType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirRawArrayType
import org.cangnova.cangjie.chir.core.type.ChirTupleType
import org.cangnova.cangjie.chir.core.type.ChirStructType
import org.cangnova.cangjie.chir.core.type.ChirTypeDeclarationId
import org.cangnova.cangjie.chir.core.declaration.ChirTypeDeclaration
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirConstantKind
import org.cangnova.cangjie.chir.core.value.ChirFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.chir.core.value.ChirParameterValue
import org.cangnova.cangjie.codegen.api.ChirCodegenInput
import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private const val GENERIC_TYPE_INFO_RUNTIME_SYMBOL = "CJ_MCC_GetOrCreateTypeInfo"

/**
 * CHIR 到 LLVM lowering pipeline 的集成测试。
 */
class LoweringPipelineIntegrationTest {
    /** 验证 String 字面量以官方 aggregate 布局和模块级 UTF-8 backing storage 发射。 */
    @Test
    fun `lowers string literals as official aggregate constants`() {
        val stringId = ChirTypeDeclarationId("std.core", "String")
        val stringType = ChirResolvedTypeRef(ChirStructType(stringId))
        val literal = ChirConstantValue(ChirSemanticId("const:string-literal"), stringType, "hello")
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:string-literal"),
            name = "stringLiteral",
            returnType = stringType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:string-literal"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:string-literal:return"),
                        returnValue = literal,
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:string-literal"),
        )

        val ir = generateIr(
            function,
            typeDefinitions = listOf(
                DefaultChirStructDeclaration(
                    semanticId = ChirSemanticId("type:string-literal"),
                    name = stringId.qualifiedName,
                    typeDeclarationId = stringId,
                ),
            ),
        )

        assertTrue(
            "%struct.std.core_String = type { ptr addrspace(1), i32, i32 }" in ir,
            ir,
        )
        assertTrue("c\"hello\"" in ir, ir)
        assertTrue(Regex("ret %struct\\.std\\.core_String \\{ ptr addrspace\\(1\\) addrspacecast").containsMatchIn(ir), ir)
        assertTrue("ret %struct.std.core_String hello" !in ir, ir)
    }

    /** String 的运行时字段使用 GC 地址空间，按 CHIR RawArray 类型投影时恢复其 p0 ABI。 */
    @Test
    fun `lowers string backing array field to its CHIR pointer ABI`() {
        val stringId = ChirTypeDeclarationId("std.core", "String")
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val byteArrayType = ChirResolvedTypeRef(
            ChirRawArrayType(ChirResolvedTypeRef(ChirPrimitiveType.UINT8)),
        )
        val stringType = ChirResolvedTypeRef(
            ChirStructType(stringId, fieldTypes = listOf(byteArrayType, intType, intType)),
        )
        val functionId = ChirSemanticId("fn:string-data-field")
        val parameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:string-data-field"),
            name = "stringValue",
            type = stringType,
            mutable = false,
        )
        val field = ChirFieldExpression(
            semanticId = ChirSemanticId("field:string-data-field"),
            base = ChirParameterValue(parameter.semanticId, stringType, parameter.name, ownerFunctionId = functionId),
            path = listOf(0uL),
            resultType = byteArrayType,
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = functionId,
            name = "stringDataField",
            returnType = byteArrayType,
            parameters = listOf(parameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:string-data-field"),
                    name = "entry",
                    expressions = listOf(field),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:string-data-field:return"),
                        returnValue = ChirLocalValue(field.semanticId, byteArrayType, "data"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:string-data-field"),
        )

        val ir = generateIr(
            function,
            typeDefinitions = listOf(
                DefaultChirStructDeclaration(
                    semanticId = ChirSemanticId("type:string-data-field"),
                    name = stringId.qualifiedName,
                    typeDeclarationId = stringId,
                    fieldDeclarations = listOf(
                        DefaultChirVariableDeclaration(
                            semanticId = ChirSemanticId("field:string-data-field:data"),
                            name = "data",
                            type = byteArrayType,
                            mutable = false,
                        ),
                        DefaultChirVariableDeclaration(
                            semanticId = ChirSemanticId("field:string-data-field:offset"),
                            name = "offset",
                            type = intType,
                            mutable = false,
                        ),
                        DefaultChirVariableDeclaration(
                            semanticId = ChirSemanticId("field:string-data-field:length"),
                            name = "length",
                            type = intType,
                            mutable = false,
                        ),
                    ),
                ),
            ),
        )

        assertTrue(Regex("extractvalue %struct\\.std\\.core_String %stringValue, 0").containsMatchIn(ir), ir)
        assertTrue(Regex("addrspacecast ptr addrspace\\(1\\) %[^ ]+ to ptr").containsMatchIn(ir), ir)
    }

    /** String 聚合构造必须按运行时 identified type 的 GC 指针字段布局转换 RawArray 值。 */
    @Test
    fun `lowers string tuple construction through its runtime field ABI`() {
        val stringId = ChirTypeDeclarationId("std.core", "String")
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val byteArrayType = ChirResolvedTypeRef(ChirRawArrayType(ChirResolvedTypeRef(ChirPrimitiveType.UINT8)))
        val stringType = ChirResolvedTypeRef(
            ChirStructType(stringId, fieldTypes = listOf(byteArrayType, intType, intType)),
        )
        val functionId = ChirSemanticId("fn:string-tuple-abi")
        val bytes = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:string-tuple-abi:bytes"),
            name = "bytes",
            type = byteArrayType,
            mutable = false,
        )
        val tupleId = ChirSemanticId("expr:string-tuple-abi")
        val tuple = ChirTupleExpression(
            semanticId = tupleId,
            elements = listOf(
                ChirParameterValue(bytes.semanticId, byteArrayType, bytes.name, functionId),
                ChirConstantValue(ChirSemanticId("const:string-tuple-abi:start"), intType, "0"),
                ChirConstantValue(ChirSemanticId("const:string-tuple-abi:length"), intType, "0"),
            ),
            resultType = stringType,
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = functionId,
            name = "constructString",
            returnType = stringType,
            parameters = listOf(bytes),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:string-tuple-abi"),
                    name = "entry",
                    expressions = listOf(tuple),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:string-tuple-abi:return"),
                        returnValue = ChirLocalValue(tupleId, stringType, "string"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:string-tuple-abi"),
        )

        val ir = generateIr(function)

        assertTrue("addrspacecast ptr %bytes to ptr addrspace(1)" in ir, ir)
        assertTrue(
            Regex("insertvalue %struct\\.std\\.core_String .*ptr addrspace\\(1\\) %[^,]+, 0").containsMatchIn(ir),
            ir,
        )
    }

    /** String 的具名字段读写统一转换 CHIR RawArray 的 p0 与运行时 p1 字段 ABI。 */
    @Test
    fun `lowers string backing array named loads and stores through its runtime field ABI`() {
        val stringId = ChirTypeDeclarationId("std.core", "String")
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
        val byteArrayType = ChirResolvedTypeRef(ChirRawArrayType(ChirResolvedTypeRef(ChirPrimitiveType.UINT8)))
        val stringType = ChirResolvedTypeRef(
            ChirStructType(stringId, fieldTypes = listOf(byteArrayType, intType, intType)),
        )
        val functionId = ChirSemanticId("fn:string-named-memory-abi")
        val stringParameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:string-named-memory-abi:string"),
            name = "stringValue",
            type = ChirResolvedTypeRef(ChirRefType(stringType)),
            mutable = false,
        )
        val bytesParameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:string-named-memory-abi:bytes"),
            name = "bytes",
            type = byteArrayType,
            mutable = false,
        )
        val readId = ChirSemanticId("expr:string-named-memory-abi:read")
        val read = ChirMemoryExpression(
            semanticId = readId,
            operation = ChirMemoryOperation.GET_ELEMENT_BY_NAME.canonicalName,
            address = ChirParameterValue(
                stringParameter.semanticId,
                stringParameter.type,
                stringParameter.name,
                functionId,
            ),
            resultType = byteArrayType,
            memberNames = listOf("myData"),
        )
        val store = ChirMemoryExpression(
            semanticId = ChirSemanticId("expr:string-named-memory-abi:store"),
            operation = ChirMemoryOperation.STORE_ELEMENT_BY_NAME.canonicalName,
            address = ChirParameterValue(
                stringParameter.semanticId,
                stringParameter.type,
                stringParameter.name,
                functionId,
            ),
            value = ChirParameterValue(
                bytesParameter.semanticId,
                bytesParameter.type,
                bytesParameter.name,
                functionId,
            ),
            resultType = unitType,
            memberNames = listOf("myData"),
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = functionId,
            name = "readAndWriteStringData",
            returnType = byteArrayType,
            parameters = listOf(stringParameter, bytesParameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:string-named-memory-abi"),
                    name = "entry",
                    expressions = listOf(read, store),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:string-named-memory-abi:return"),
                        returnValue = ChirLocalValue(readId, byteArrayType, "data"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:string-named-memory-abi"),
        )
        val stringDeclaration = DefaultChirStructDeclaration(
            semanticId = ChirSemanticId("type:string-named-memory-abi"),
            name = stringId.qualifiedName,
            typeDeclarationId = stringId,
            fieldDeclarations = listOf(
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("field:string-named-memory-abi:data"),
                    name = "myData",
                    type = byteArrayType,
                    mutable = false,
                ),
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("field:string-named-memory-abi:start"),
                    name = "start",
                    type = intType,
                    mutable = false,
                ),
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("field:string-named-memory-abi:length"),
                    name = "length",
                    type = intType,
                    mutable = false,
                ),
            ),
        )

        val ir = generateIr(function, typeDefinitions = listOf(stringDeclaration))

        assertTrue("load ptr addrspace(1), ptr" in ir, ir)
        assertTrue(Regex("addrspacecast ptr addrspace\\(1\\) %[^ ]+ to ptr").containsMatchIn(ir), ir)
        assertTrue("addrspacecast ptr %bytes to ptr addrspace(1)" in ir, ir)
        assertTrue(Regex("store ptr addrspace\\(1\\) %[^,]+, ptr").containsMatchIn(ir), ir)
    }

    /** NullLiteral 和文本恰为 `null` 的 StringLiteral 必须保留不同的 CHIR 常量语义。 */
    @Test
    fun `distinguishes null string values from string literal text`() {
        val stringId = ChirTypeDeclarationId("std.core", "String")
        val stringType = ChirResolvedTypeRef(ChirStructType(stringId))

        fun function(name: String, id: String, constantKind: ChirConstantKind) = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:$id"),
            name = name,
            returnType = stringType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:$id"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:$id:return"),
                        returnValue = ChirConstantValue(
                            semanticId = ChirSemanticId("const:$id"),
                            type = stringType,
                            literal = "null",
                            constantKind = constantKind,
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:$id"),
        )

        val stringLiteralIr = generateIr(function("stringText", "string-text-null", ChirConstantKind.UNSPECIFIED))
        val nullLiteralIr = generateIr(function("nullValue", "null-string-value", ChirConstantKind.NULL))

        assertTrue("c\"null\"" in stringLiteralIr, stringLiteralIr)
        assertTrue("ret %struct.std.core_String zeroinitializer" !in stringLiteralIr, stringLiteralIr)
        assertTrue("ret %struct.std.core_String zeroinitializer" in nullLiteralIr, nullLiteralIr)
        assertTrue("c\"null\"" !in nullLiteralIr, nullLiteralIr)
    }

    /** CHIR NullLiteral 表示目标类型的零值，LLVM 整数常量必须使用数值零。 */
    @Test
    fun `lowers typed null literal to its official zero value`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT64)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:typed-null"),
            name = "typedNull",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:typed-null"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:typed-null:return"),
                        returnValue = ChirConstantValue(ChirSemanticId("const:typed-null"), intType, "null"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:typed-null"),
        )

        val ir = generateIr(function)

        assertTrue("ret i64 0" in ir, ir)
        assertTrue("i64 null" !in ir, ir)
    }

    /** 验证 class 对象字段访问先跳过对象头中的 TypeInfo 指针。 */
    @Test
    fun `lowers class member access from object payload`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val classId = ChirTypeDeclarationId("pkg", "Holder")
        val classType = ChirResolvedTypeRef(ChirClassType(classId, fieldTypes = listOf(intType)))
        val classReferenceType = ChirResolvedTypeRef(ChirRefType(classType))
        val parameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:class-member:holder"),
            name = "holder",
            type = classReferenceType,
            mutable = false,
        )
        val readId = ChirSemanticId("expr:class-member:value")
        val read = ChirMemoryExpression(
            semanticId = readId,
            operation = ChirMemoryOperation.GET_ELEMENT_BY_NAME.canonicalName,
            address = ChirParameterValue(
                parameter.semanticId,
                parameter.type,
                parameter.name,
                ownerFunctionId = ChirSemanticId("fn:class-member"),
            ),
            resultType = intType,
            memberNames = listOf("value"),
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:class-member"),
            name = "readClassMember",
            returnType = intType,
            parameters = listOf(parameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:class-member"),
                    name = "entry",
                    expressions = listOf(read),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:class-member:return"),
                        returnValue = ChirLocalValue(readId, intType, "value"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:class-member"),
        )
        val ir = generateIr(
            function,
            typeDefinitions = listOf(
                DefaultChirClassDeclaration(
                    semanticId = ChirSemanticId("class:Holder"),
                    name = "pkg:Holder",
                    typeDeclarationId = classId,
                    memberDeclarations = listOf(
                        DefaultChirVariableDeclaration(
                            semanticId = ChirSemanticId("field:Holder.value"),
                            name = "value",
                            type = intType,
                            mutable = true,
                        ),
                    ),
                ),
            ),
        )

        val payloadOffset = ir.indexOf("getelementptr ptr, ptr %holder, i32 1")
        val fieldAddress = ir.indexOf("getelementptr %class.pkg_Holder", payloadOffset)
        assertTrue(payloadOffset >= 0, ir)
        assertTrue(fieldAddress > payloadOffset, ir)
    }

    /** 验证有相同 LLVM 指针表示的类引用转换直接复用源值。 */
    @Test
    fun `lowers class reference casts with identical pointer ABI`() {
        val sourceId = ChirTypeDeclarationId("pkg", "Source")
        val targetId = ChirTypeDeclarationId("pkg", "Target")
        val sourceType = ChirResolvedTypeRef(ChirRefType(ChirResolvedTypeRef(ChirClassType(sourceId))))
        val targetType = ChirResolvedTypeRef(ChirRefType(ChirResolvedTypeRef(ChirClassType(targetId))))
        val parameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:class-cast:value"),
            name = "value",
            type = sourceType,
            mutable = false,
        )
        val castId = ChirSemanticId("expr:class-cast:result")
        val cast = ChirOtherExpression(
            semanticId = castId,
            operation = ChirOtherOperation.CANGJIE_TYPE_CAST_NA.canonicalName,
            operands = listOf(
                ChirParameterValue(
                    parameter.semanticId,
                    parameter.type,
                    parameter.name,
                    ownerFunctionId = ChirSemanticId("fn:class-cast"),
                ),
            ),
            resultType = targetType,
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:class-cast"),
            name = "castClassReference",
            returnType = targetType,
            parameters = listOf(parameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:class-cast"),
                    name = "entry",
                    expressions = listOf(cast),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:class-cast:return"),
                        returnValue = ChirLocalValue(castId, targetType, "target"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:class-cast"),
        )
        val ir = generateIr(
            function,
            typeDefinitions = listOf(
                DefaultChirClassDeclaration(
                    semanticId = ChirSemanticId("class:Source"),
                    name = "pkg:Source",
                    typeDeclarationId = sourceId,
                ),
                DefaultChirClassDeclaration(
                    semanticId = ChirSemanticId("class:Target"),
                    name = "pkg:Target",
                    typeDeclarationId = targetId,
                ),
            ),
        )

        assertTrue(ir.contains("select i1 true, ptr %value, ptr %value"), ir)
    }

    /** 验证官方 bool 到整数转换执行零扩展。 */
    @Test
    fun `lowers bool to integer cast with zero extension`() {
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val int64Type = ChirResolvedTypeRef(ChirPrimitiveType.INT64)
        val parameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:bool-cast:value"),
            name = "value",
            type = boolType,
            mutable = false,
        )
        val castId = ChirSemanticId("expr:bool-cast:result")
        val cast = ChirOtherExpression(
            semanticId = castId,
            operation = ChirOtherOperation.CANGJIE_TYPE_CAST_NA.canonicalName,
            operands = listOf(
                ChirParameterValue(
                    parameter.semanticId,
                    parameter.type,
                    parameter.name,
                    ownerFunctionId = ChirSemanticId("fn:bool-cast"),
                ),
            ),
            resultType = int64Type,
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:bool-cast"),
            name = "castBoolToInt64",
            returnType = int64Type,
            parameters = listOf(parameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:bool-cast"),
                    name = "entry",
                    expressions = listOf(cast),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:bool-cast:return"),
                        returnValue = ChirLocalValue(castId, int64Type, "integer"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:bool-cast"),
        )

        val ir = generateIr(function)

        assertTrue(ir.contains("zext i1 %value to i64"), ir)
    }

    /** 验证 throwing 转换的源整数域完全可由目标表示时直接使用无溢出的扩展。 */
    @Test
    fun `lowers non-overflowing unsigned widening throwing cast`() {
        val uint32Type = ChirResolvedTypeRef(ChirPrimitiveType.UINT32)
        val int64Type = ChirResolvedTypeRef(ChirPrimitiveType.INT64)
        val functionId = ChirSemanticId("fn:unsigned-widening-cast")
        val parameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:unsigned-widening-cast:value"),
            name = "value",
            type = uint32Type,
            mutable = false,
        )
        val castId = ChirSemanticId("expr:unsigned-widening-cast:result")
        val cast = ChirOtherExpression(
            semanticId = castId,
            operation = ChirOtherOperation.CANGJIE_TYPE_CAST_THROWING.canonicalName,
            operands = listOf(
                ChirParameterValue(
                    parameter.semanticId,
                    parameter.type,
                    parameter.name,
                    ownerFunctionId = functionId,
                ),
            ),
            resultType = int64Type,
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = functionId,
            name = "widenUInt32ToInt64",
            returnType = int64Type,
            parameters = listOf(parameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:unsigned-widening-cast"),
                    name = "entry",
                    expressions = listOf(cast),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:unsigned-widening-cast:return"),
                        returnValue = ChirLocalValue(castId, int64Type, "widened"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:unsigned-widening-cast"),
        )

        val ir = generateIr(function)

        assertTrue(ir.contains("zext i32 %value to i64"), ir)
    }

    /** 验证有溢出可能的 throwing 整数转换检查完整源域并抛出 OverflowException。 */
    @Test
    fun `lowers throwing integer cast with range checks and overflow exception`() {
        val int64Type = ChirResolvedTypeRef(ChirPrimitiveType.INT64)
        val uint32Type = ChirResolvedTypeRef(ChirPrimitiveType.UINT32)
        val functionId = ChirSemanticId("fn:throwing-narrowing-cast")
        val parameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:throwing-narrowing-cast:value"),
            name = "value",
            type = int64Type,
            mutable = false,
        )
        val castId = ChirSemanticId("expr:throwing-narrowing-cast:result")
        val cast = ChirOtherExpression(
            semanticId = castId,
            operation = ChirOtherOperation.CANGJIE_TYPE_CAST_THROWING.canonicalName,
            operands = listOf(
                ChirParameterValue(
                    parameter.semanticId,
                    parameter.type,
                    parameter.name,
                    ownerFunctionId = functionId,
                ),
            ),
            resultType = uint32Type,
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = functionId,
            name = "throwingInt64ToUInt32",
            returnType = uint32Type,
            parameters = listOf(parameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:throwing-narrowing-cast"),
                    name = "entry",
                    expressions = listOf(cast),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:throwing-narrowing-cast:return"),
                        returnValue = ChirLocalValue(castId, uint32Type, "narrowed"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:throwing-narrowing-cast"),
        )

        val ir = generateIr(function)

        assertTrue(ir.contains("icmp sge i65"), ir)
        assertTrue(ir.contains("icmp sle i65"), ir)
        assertTrue(ir.contains("trunc i64 %value to i32"), ir)
        assertTrue(ir.contains("declare ptr addrspace(1) @\"rt\$CreateOverflowException_msg\"(ptr addrspace(1))"), ir)
        assertTrue(
            ir.contains("call ptr addrspace(1) @\"rt\$CreateOverflowException_msg\"(ptr addrspace(1) addrspacecast"),
            ir,
        )
        assertTrue(ir.contains("call void @llvm.cj.throw.exception"), ir)
        assertTrue(ir.contains("RawArray<UInt8>.ti"), ir)
        assertTrue(ir.contains("result would be greater than UInt32.Max."), ir)
    }

    /** 验证 RawArray intrinsic 按声明布局读取元素并按 TypeInfo 计算泛型元素尺寸。 */
    @Test
    fun `lowers generic raw array get intrinsic using runtime element size`() {
        val genericType = ChirResolvedTypeRef(
            ChirGenericType(
                identifier = "T",
                declarationIndex = 0,
                semanticIdentity = "fn:rawarray-get:T",
            ),
        )
        val arrayType = ChirResolvedTypeRef(ChirRawArrayType(genericType))
        val indexType = ChirResolvedTypeRef(ChirPrimitiveType.INT64)
        val arrayParameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:rawarray-get:array"),
            name = "array",
            type = arrayType,
            mutable = false,
        )
        val indexParameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:rawarray-get:index"),
            name = "index",
            type = indexType,
            mutable = false,
        )
        val getId = ChirSemanticId("expr:rawarray-get:value")
        val get = ChirOtherExpression(
            semanticId = getId,
            operation = ChirIntrinsicKind.ARRAY_GET_UNCHECKED.canonicalName,
            operands = listOf(
                ChirParameterValue(
                    arrayParameter.semanticId,
                    arrayParameter.type,
                    arrayParameter.name,
                    ownerFunctionId = ChirSemanticId("fn:rawarray-get"),
                ),
                ChirParameterValue(
                    indexParameter.semanticId,
                    indexParameter.type,
                    indexParameter.name,
                    ownerFunctionId = ChirSemanticId("fn:rawarray-get"),
                ),
            ),
            resultType = genericType,
            intrinsicKind = ChirIntrinsicKind.ARRAY_GET_UNCHECKED,
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:rawarray-get"),
            name = "getGenericArrayElement",
            returnType = genericType,
            parameters = listOf(arrayParameter, indexParameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:rawarray-get"),
                    name = "entry",
                    expressions = listOf(get),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:rawarray-get:return"),
                        returnValue = ChirLocalValue(getId, genericType, "element"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:rawarray-get"),
            genericTypeParameters = listOf(genericType),
        )

        val ir = generateIr(function)

        assertTrue(ir.contains("getelementptr %TypeInfo"), ir)
        assertTrue(ir.contains("mul i64"), ir)
        assertTrue(ir.contains("getelementptr i8, ptr %array"), ir)
        assertTrue(ir.contains("load ptr, ptr"), ir)
    }

    /** 验证官方 RAW_ARRAY_REFEQ 比较数组引用身份，而不是遍历元素内容。 */
    @Test
    fun `lowers raw array reference equality as pointer identity`() {
        val elementType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val arrayType = ChirResolvedTypeRef(ChirRawArrayType(elementType))
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val functionId = ChirSemanticId("fn:rawarray-ref-eq")
        val leftParameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:rawarray-ref-eq:left"),
            name = "left",
            type = arrayType,
            mutable = false,
        )
        val rightParameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:rawarray-ref-eq:right"),
            name = "right",
            type = arrayType,
            mutable = false,
        )
        val expressionId = ChirSemanticId("expr:rawarray-ref-eq:result")
        val expression = ChirOtherExpression(
            semanticId = expressionId,
            operation = ChirIntrinsicKind.RAW_ARRAY_REFEQ.canonicalName,
            operands = listOf(leftParameter, rightParameter).map { parameter ->
                ChirParameterValue(
                    parameter.semanticId,
                    parameter.type,
                    parameter.name,
                    ownerFunctionId = functionId,
                )
            },
            resultType = boolType,
            intrinsicKind = ChirIntrinsicKind.RAW_ARRAY_REFEQ,
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = functionId,
            name = "rawArrayReferenceEquals",
            returnType = boolType,
            parameters = listOf(leftParameter, rightParameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:rawarray-ref-eq"),
                    name = "entry",
                    expressions = listOf(expression),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:rawarray-ref-eq:return"),
                        returnValue = ChirLocalValue(expressionId, boolType, "sameArray"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:rawarray-ref-eq"),
        )

        val ir = generateIr(function)

        assertTrue(ir.contains("icmp eq ptr %left, %right"), ir)
        assertTrue(!ir.contains("getelementptr i8, ptr %left"), ir)
    }

    /** 验证任意名义泛型 TypeInfo 都按 TypeTemplate 与声明顺序递归构造。 */
    @Test
    fun `lowers nested generic nominal TypeInfo arguments in declaration order`() {
        val typeT = genericType("T", "fn:generic-type-info:T", 0)
        val typeU = genericType("U", "fn:generic-type-info:U", 1)
        val arrayOfT = nominalClassType("std.core", "Array", typeT)
        val arrayOfU = nominalClassType("std.core", "Array", typeU)
        val pairOfTU = nominalClassType("typeinfo", "Pair", typeT, typeU)
        val pairOfUT = nominalClassType("typeinfo", "Pair", typeU, typeT)
        val envelopeOfPairTU = nominalClassType("typeinfo", "Envelope", pairOfTU)
        val envelopeOfPairUT = nominalClassType("typeinfo", "Envelope", pairOfUT)

        val sinkId = ChirSemanticId("fn:generic-type-info:sink")
        val sinkTypeA = genericType("A", "fn:generic-type-info:sink:A", 0)
        val sinkTypeB = genericType("B", "fn:generic-type-info:sink:B", 1)
        val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
        val sink = DefaultChirFunctionDeclaration(
            semanticId = sinkId,
            name = "consumeGenericValues",
            returnType = unitType,
            parameters = listOf(
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("param:generic-type-info:sink:first"),
                    name = "first",
                    type = sinkTypeA,
                    mutable = false,
                ),
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("param:generic-type-info:sink:second"),
                    name = "second",
                    type = sinkTypeB,
                    mutable = false,
                ),
            ),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:generic-type-info:sink"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:generic-type-info:sink:return"),
                        returnValue = ChirConstantValue(
                            semanticId = ChirSemanticId("const:generic-type-info:sink:return"),
                            type = unitType,
                            literal = "unit",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:generic-type-info:sink"),
            genericTypeParameters = listOf(sinkTypeA, sinkTypeB),
        )

        val callerId = ChirSemanticId("fn:generic-type-info:caller")
        val arrayTParameter = genericParameter("arrayT", arrayOfT, callerId)
        val envelopeTUParameter = genericParameter("envelopeTU", envelopeOfPairTU, callerId)
        val envelopeUTParameter = genericParameter("envelopeUT", envelopeOfPairUT, callerId)
        val arrayUParameter = genericParameter("arrayU", arrayOfU, callerId)
        val sinkFunctionType = ChirResolvedTypeRef(ChirFunctionType(listOf(sinkTypeA, sinkTypeB), unitType))
        val sinkValue = ChirFunctionValue(
            semanticId = sinkId,
            type = sinkFunctionType,
            name = "consumeGenericValues",
        )
        val forwardCall = ChirCallExpression(
            semanticId = ChirSemanticId("expr:generic-type-info:forward"),
            callee = sinkValue,
            arguments = listOf(
                parameterValue(arrayTParameter, callerId),
                parameterValue(envelopeTUParameter, callerId),
            ),
            resultType = unitType,
            instantiatedTypeArguments = listOf(arrayOfT, envelopeOfPairTU),
        )
        val reverseCall = ChirCallExpression(
            semanticId = ChirSemanticId("expr:generic-type-info:reverse"),
            callee = sinkValue,
            arguments = listOf(
                parameterValue(envelopeUTParameter, callerId),
                parameterValue(arrayUParameter, callerId),
            ),
            resultType = unitType,
            instantiatedTypeArguments = listOf(envelopeOfPairUT, arrayOfU),
        )
        val caller = DefaultChirFunctionDeclaration(
            semanticId = callerId,
            name = "exerciseGenericNominalTypeInfo",
            returnType = unitType,
            parameters = listOf(arrayTParameter, envelopeTUParameter, envelopeUTParameter, arrayUParameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:generic-type-info:caller"),
                    name = "entry",
                    expressions = listOf(forwardCall, reverseCall),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:generic-type-info:caller:return"),
                        returnValue = ChirConstantValue(
                            semanticId = ChirSemanticId("const:generic-type-info:caller:return"),
                            type = unitType,
                            literal = "unit",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:generic-type-info:caller"),
            genericTypeParameters = listOf(typeT, typeU),
        )

        val ir = generateIr(
            sink,
            caller,
            typeDefinitions = listOf(
                DefaultChirClassDeclaration(
                    semanticId = ChirSemanticId("class:generic-type-info:Pair"),
                    name = "typeinfo:Pair",
                    typeDeclarationId = ChirTypeDeclarationId("typeinfo", "Pair"),
                    typeParameters = listOf("A", "B"),
                    typeParameterTypes = listOf(
                        genericType("A", "class:generic-type-info:Pair:A", 0),
                        genericType("B", "class:generic-type-info:Pair:B", 1),
                    ),
                ),
                DefaultChirClassDeclaration(
                    semanticId = ChirSemanticId("class:generic-type-info:Envelope"),
                    name = "typeinfo:Envelope",
                    typeDeclarationId = ChirTypeDeclarationId("typeinfo", "Envelope"),
                    typeParameters = listOf("T"),
                    typeParameterTypes = listOf(genericType("T", "class:generic-type-info:Envelope:T", 0)),
                ),
            ),
        )

        val typeTemplateCalls = Regex(
            """(?m)^\s*(%[^\s]+) = call ptr @$GENERIC_TYPE_INFO_RUNTIME_SYMBOL\(ptr @"([^"]+)", i32 (\d+), ptr ([^\)]+)\)$""",
        ).findAll(ir).map { match ->
            TypeTemplateCall(
                result = match.groupValues[1],
                template = match.groupValues[2],
                argumentCount = match.groupValues[3].toInt(),
                arguments = match.groupValues[4],
                endOffset = match.range.last + 1,
            )
        }.toList()

        assertEquals(
            listOf(
                TypeTemplateCallShape("std.core:Array.tt", 1),
                TypeTemplateCallShape("typeinfo:Pair.tt", 2),
                TypeTemplateCallShape("typeinfo:Envelope.tt", 1),
                TypeTemplateCallShape("typeinfo:Pair.tt", 2),
                TypeTemplateCallShape("typeinfo:Envelope.tt", 1),
                TypeTemplateCallShape("std.core:Array.tt", 1),
            ),
            typeTemplateCalls.map { TypeTemplateCallShape(it.template, it.argumentCount) },
            ir,
        )
        assertTrue(ir.contains("@\"std.core:Array.tt\" = external global %TypeTemplate"), ir)
        assertTrue(ir.contains("@\"typeinfo:Pair.tt\" = external global %TypeTemplate"), ir)
        assertTrue(ir.contains("@\"typeinfo:Envelope.tt\" = external global %TypeTemplate"), ir)

        val pairCalls = typeTemplateCalls.filter { it.template == "typeinfo:Pair.tt" }
        assertOrderedTypeInfoArguments(ir, pairCalls[0], listOf("%ti.T.0", "%ti.U.1"))
        assertOrderedTypeInfoArguments(ir, pairCalls[1], listOf("%ti.U.1", "%ti.T.0"))
        val envelopeCalls = typeTemplateCalls.filter { it.template == "typeinfo:Envelope.tt" }
        assertNestedTypeInfoArgument(ir, typeTemplateCalls, envelopeCalls[0], "typeinfo:Pair.tt")
        assertNestedTypeInfoArgument(ir, typeTemplateCalls, envelopeCalls[1], "typeinfo:Pair.tt")
    }

    /** 验证跨 owner 静态泛型成员调用复用同一动态 TypeInfo，且 SSA 临时量不重名。 */
    @Test
    fun `reuses dynamic owner typeinfo for static generic member call`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val ownerTypeId = ChirTypeDeclarationId("typeinfo", "Other")
        val ownerGeneric = genericType("OwnerT", "class:static-generic-owner:OwnerT", 0)
        val staticFunctionId = ChirSemanticId("fn:static-generic-owner:member")
        val staticFunction = DefaultChirFunctionDeclaration(
            semanticId = staticFunctionId,
            name = "staticMember",
            returnType = intType,
            parameters = emptyList(),
            blocks = emptyList(),
            entryBlockId = null,
            hasBody = false,
            isImported = true,
            isStatic = true,
        )
        val callerId = ChirSemanticId("fn:static-generic-owner:caller")
        val callerTypeParameter = genericType("T", "$callerId:T", 0)
        val callId = ChirSemanticId("expr:static-generic-owner:call")
        val call = ChirCallExpression(
            semanticId = callId,
            callee = ChirFunctionValue(
                semanticId = staticFunctionId,
                type = ChirResolvedTypeRef(ChirFunctionType(emptyList(), intType)),
                name = "staticMember",
            ),
            arguments = emptyList(),
            resultType = intType,
            thisType = nominalClassType("typeinfo", "Other", callerTypeParameter),
        )
        val caller = DefaultChirFunctionDeclaration(
            semanticId = callerId,
            name = "callStaticGenericMember",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:static-generic-owner:caller"),
                    name = "entry",
                    expressions = listOf(call),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:static-generic-owner:return"),
                        returnValue = ChirLocalValue(callId, intType, "result"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:static-generic-owner:caller"),
            genericTypeParameters = listOf(callerTypeParameter),
        )
        val otherType = DefaultChirClassDeclaration(
            semanticId = ChirSemanticId("class:static-generic-owner:Other"),
            name = "Other",
            typeDeclarationId = ownerTypeId,
            typeParameters = listOf("OwnerT"),
            memberDeclarations = listOf(staticFunction),
            typeParameterTypes = listOf(ownerGeneric),
        )

        val ir = generateIr(caller, typeDefinitions = listOf(otherType))

        assertEquals(
            1,
            Regex("call ptr @$GENERIC_TYPE_INFO_RUNTIME_SYMBOL\\(ptr @\\\"typeinfo:Other.tt\\\"")
                .findAll(ir)
                .count(),
            ir,
        )
        val callLine = ir.lineSequence().single { it.contains("call i32 @") && !it.contains(GENERIC_TYPE_INFO_RUNTIME_SYMBOL) }
        val typeInfoArguments = Regex("ptr (%[^, )]+)").findAll(callLine).map { it.groupValues[1] }.toList()
        assertEquals(2, typeInfoArguments.size, callLine)
        assertEquals(typeInfoArguments.first(), typeInfoArguments.last(), callLine)

        val definitions = Regex("(?m)^\\s*(%[^\\s=]+)\\s*=").findAll(ir).map { it.groupValues[1] }.toList()
        assertEquals(definitions.size, definitions.toSet().size, ir)
    }

    /** 验证两个 32 字节向量 intrinsic 共享 RawArray 布局并保留官方返回约定。 */
    @Test
    fun `lowers official 32 byte vector intrinsics`() {
        val byteType = ChirResolvedTypeRef(ChirPrimitiveType.UINT8)
        val arrayType = ChirResolvedTypeRef(ChirRawArrayType(byteType))
        val int64Type = ChirResolvedTypeRef(ChirPrimitiveType.INT64)

        fun vectorFunction(
            functionName: String,
            kind: ChirIntrinsicKind,
            parameterTypes: List<org.cangnova.cangjie.chir.core.type.ChirTypeRef>,
        ): DefaultChirFunctionDeclaration {
            val functionId = ChirSemanticId("fn:$functionName")
            val parameters = parameterTypes.mapIndexed { index, type ->
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("param:$functionName:$index"),
                    name = "arg$index",
                    type = type,
                    mutable = false,
                )
            }
            val intrinsicId = ChirSemanticId("expr:$functionName:intrinsic")
            val intrinsic = ChirOtherExpression(
                semanticId = intrinsicId,
                operation = kind.canonicalName,
                operands = parameters.map { parameter ->
                    ChirParameterValue(
                        parameter.semanticId,
                        parameter.type,
                        parameter.name,
                        ownerFunctionId = functionId,
                    )
                },
                resultType = int64Type,
                intrinsicKind = kind,
            )
            return DefaultChirFunctionDeclaration(
                semanticId = functionId,
                name = functionName,
                returnType = int64Type,
                parameters = parameters,
                blocks = listOf(
                    ChirBlock(
                        semanticId = ChirSemanticId("block:$functionName"),
                        name = "entry",
                        expressions = listOf(intrinsic),
                        terminator = ChirReturnTerminator(
                            semanticId = ChirSemanticId("term:$functionName:return"),
                            returnValue = ChirLocalValue(intrinsicId, int64Type, "offset"),
                        ),
                    ),
                ),
                entryBlockId = ChirSemanticId("block:$functionName"),
            )
        }

        val compare = vectorFunction(
            functionName = "compareVectorBytes",
            kind = ChirIntrinsicKind.VECTOR_COMPARE_32,
            parameterTypes = listOf(arrayType, int64Type, arrayType, int64Type),
        )
        val findByte = vectorFunction(
            functionName = "findVectorByte",
            kind = ChirIntrinsicKind.VECTOR_INDEX_BYTE_32,
            parameterTypes = listOf(arrayType, int64Type, byteType),
        )
        val ir = generateIr(compare, findByte)

        assertTrue(ir.contains("declare i32 @llvm.cttz.i32(i32, i1 immarg)"), ir)
        assertTrue(ir.contains("icmp ne <32 x i8>"), ir)
        assertTrue(ir.contains("icmp eq <32 x i8>"), ir)
        assertTrue(ir.contains("shufflevector <32 x i8>"), ir)
    }

    /** 验证官方数组 copy-to intrinsic 的标量、引用、聚合引用和泛型布局分派。 */
    @Test
    fun `lowers official raw array copy to intrinsic by element layout`() {
        val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
        val indexType = ChirResolvedTypeRef(ChirPrimitiveType.INT64)
        val int32Type = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val referenceType = ChirResolvedTypeRef(
            ChirRefType(
                ChirResolvedTypeRef(
                    ChirClassType(ChirTypeDeclarationId("pkg", "CopyElement")),
                ),
            ),
        )
        val aggregateReferenceType = ChirResolvedTypeRef(
            ChirTupleType(listOf(int32Type, referenceType)),
        )
        val genericType = ChirResolvedTypeRef(
            ChirGenericType(
                identifier = "T",
                declarationIndex = 0,
                semanticIdentity = "fn:copyGeneric:T",
            ),
        )

        fun copyFunction(
            functionName: String,
            elementType: ChirTypeRef,
            genericParameters: List<ChirTypeRef> = emptyList(),
        ): DefaultChirFunctionDeclaration {
            val functionId = ChirSemanticId("fn:$functionName")
            val arrayType = ChirResolvedTypeRef(ChirRawArrayType(elementType))
            val parameterTypes = listOf(arrayType, arrayType, indexType, indexType, indexType)
            val parameters = parameterTypes.mapIndexed { index, type ->
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("param:$functionName:$index"),
                    name = "arg$index",
                    type = type,
                    mutable = false,
                )
            }
            val copy = ChirOtherExpression(
                semanticId = ChirSemanticId("expr:$functionName:copy"),
                operation = ChirIntrinsicKind.ARRAY_BUILT_IN_COPY_TO.canonicalName,
                operands = parameters.map { parameter ->
                    ChirParameterValue(
                        parameter.semanticId,
                        parameter.type,
                        parameter.name,
                        ownerFunctionId = functionId,
                    )
                },
                resultType = unitType,
                intrinsicKind = ChirIntrinsicKind.ARRAY_BUILT_IN_COPY_TO,
            )
            return DefaultChirFunctionDeclaration(
                semanticId = functionId,
                name = functionName,
                returnType = unitType,
                parameters = parameters,
                blocks = listOf(
                    ChirBlock(
                        semanticId = ChirSemanticId("block:$functionName"),
                        name = "entry",
                        expressions = listOf(copy),
                        terminator = ChirReturnTerminator(
                            semanticId = ChirSemanticId("term:$functionName:return"),
                            returnValue = null,
                        ),
                    ),
                ),
                entryBlockId = ChirSemanticId("block:$functionName"),
                genericTypeParameters = genericParameters,
            )
        }

        val ir = generateIr(
            copyFunction("copyScalarArray", int32Type),
            copyFunction("copyReferenceArray", referenceType),
            copyFunction("copyAggregateArray", aggregateReferenceType),
            copyFunction("copyGenericArray", genericType, listOf(genericType)),
        )

        assertTrue(ir.contains("call void @llvm.memmove.p1.p1.i64"), ir)
        assertTrue(ir.contains("call void @llvm.cj.array.copy.ref.i64"), ir)
        assertTrue(ir.contains("call void @llvm.cj.array.copy.struct.i64"), ir)
        assertTrue(ir.contains("call void @llvm.cj.array.copy.generic.i64"), ir)
        assertTrue(ir.contains("rawarray.copy.condition"), ir)
    }

    /** 验证 RawArray 按值初始化的循环存储和引用写屏障。 */
    @Test
    fun `lowers raw array initialization with scalar stores and reference barriers`() {
        val int32Type = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val int64Type = ChirResolvedTypeRef(ChirPrimitiveType.INT64)
        val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
        val classId = ChirTypeDeclarationId("pkg", "Element")
        val referenceType = ChirResolvedTypeRef(
            ChirRefType(ChirResolvedTypeRef(ChirClassType(classId))),
        )

        fun initializationFunction(functionName: String, elementType: ChirTypeRef): DefaultChirFunctionDeclaration {
            val functionId = ChirSemanticId("fn:$functionName")
            val arrayType = ChirResolvedTypeRef(ChirRawArrayType(elementType))
            val parameters = listOf(
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("param:$functionName:array"),
                    name = "array",
                    type = arrayType,
                    mutable = false,
                ),
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("param:$functionName:count"),
                    name = "count",
                    type = int64Type,
                    mutable = false,
                ),
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("param:$functionName:value"),
                    name = "value",
                    type = elementType,
                    mutable = false,
                ),
            )
            val init = ChirOtherExpression(
                semanticId = ChirSemanticId("expr:$functionName:init"),
                operation = ChirOtherOperation.RAW_ARRAY_INIT_BY_VALUE.canonicalName,
                operands = parameters.map { parameter ->
                    ChirParameterValue(
                        parameter.semanticId,
                        parameter.type,
                        parameter.name,
                        ownerFunctionId = functionId,
                    )
                },
                resultType = unitType,
            )
            return DefaultChirFunctionDeclaration(
                semanticId = functionId,
                name = functionName,
                returnType = unitType,
                parameters = parameters,
                blocks = listOf(
                    ChirBlock(
                        semanticId = ChirSemanticId("block:$functionName"),
                        name = "entry",
                        expressions = listOf(init),
                        terminator = ChirReturnTerminator(
                            semanticId = ChirSemanticId("term:$functionName:return"),
                            returnValue = null,
                        ),
                    ),
                ),
                entryBlockId = ChirSemanticId("block:$functionName"),
            )
        }

        val ir = generateIr(
            initializationFunction("fillScalarArray", int32Type),
            initializationFunction("fillReferenceArray", referenceType),
            typeDefinitions = listOf(
                DefaultChirClassDeclaration(
                    semanticId = ChirSemanticId("class:Element"),
                    name = "pkg:Element",
                    typeDeclarationId = classId,
                ),
            ),
        )

        assertTrue(ir.contains("icmp slt i64"), ir)
        assertTrue(ir.contains("store i32 %value, ptr"), ir)
        assertTrue(ir.contains("call void @llvm.cj.gcwrite.ref"), ir)
    }

    /**
     * 验证等宽有符号到无符号转换保留官方 LLVM ABI 中相同的整数位模式。
     */
    @Test
    fun `lowers equal width signed to unsigned type cast without changing bits`() {
        val int64Type = ChirResolvedTypeRef(ChirPrimitiveType.INT64)
        val uint64Type = ChirResolvedTypeRef(ChirPrimitiveType.UINT64)
        val parameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:integer-cast:value"),
            name = "value",
            type = int64Type,
            mutable = false,
        )
        val castId = ChirSemanticId("expr:integer-cast:int64-to-uint64")
        val cast = ChirOtherExpression(
            semanticId = castId,
            operation = ChirOtherOperation.CANGJIE_TYPE_CAST_NA.canonicalName,
            operands = listOf(
                ChirParameterValue(
                    semanticId = parameter.semanticId,
                    type = parameter.type,
                    name = parameter.name,
                    ownerFunctionId = ChirSemanticId("fn:integer-cast"),
                ),
            ),
            resultType = uint64Type,
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:integer-cast"),
            name = "castInt64ToUInt64",
            returnType = uint64Type,
            parameters = listOf(parameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:integer-cast"),
                    name = "entry",
                    expressions = listOf(cast),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:integer-cast:return"),
                        returnValue = ChirLocalValue(castId, uint64Type, "unsignedValue"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:integer-cast"),
        )
        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = ChirPackage(
                    semanticId = ChirSemanticId("pkg:integer-cast"),
                    name = "integer_cast",
                    modules = listOf(
                        ChirModule(
                            semanticId = ChirSemanticId("mod:integer-cast"),
                            name = "main",
                            declarations = listOf(function),
                        ),
                    ),
                ),
                options = CodegenOptions(
                    enabled = true,
                    validateChirBeforeLowering = true,
                    verifyBeforeWrite = true,
                    emitBitcode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )

        val ir = output.modules.single().ir
        assertTrue(ir.contains("select i1 true, i64 %value, i64 %value"), ir)
    }

    /**
     * 验证泛型值遵循指针 ABI，并可无转换地作为其 class/interface 引用视图传递。
     */
    @Test
    fun `lowers generic to class reference cast through pointer ABI`() {
        val genericType = ChirResolvedTypeRef(
            ChirGenericType(
                identifier = "T",
                declarationIndex = 0,
                semanticIdentity = "fn:generic-cast:T",
            ),
        )
        val classId = ChirTypeDeclarationId("pkg", "Comparable")
        val classType = ChirResolvedTypeRef(
            ChirClassType(
                declarationId = classId,
                typeArguments = listOf(genericType),
            ),
        )
        val classReferenceType = ChirResolvedTypeRef(ChirRefType(classType, mutable = true))
        val parameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:generic-cast:value"),
            name = "value",
            type = genericType,
            mutable = false,
        )
        val castId = ChirSemanticId("expr:generic-cast:to-class-reference")
        val cast = ChirOtherExpression(
            semanticId = castId,
            operation = ChirOtherOperation.CANGJIE_TYPE_CAST_NA.canonicalName,
            operands = listOf(
                ChirParameterValue(
                    semanticId = parameter.semanticId,
                    type = parameter.type,
                    name = parameter.name,
                    ownerFunctionId = ChirSemanticId("fn:generic-cast"),
                ),
            ),
            resultType = classReferenceType,
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:generic-cast"),
            name = "castGenericToClassReference",
            returnType = classReferenceType,
            parameters = listOf(parameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:generic-cast"),
                    name = "entry",
                    expressions = listOf(cast),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:generic-cast:return"),
                        returnValue = ChirLocalValue(castId, classReferenceType, "classReference"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:generic-cast"),
            genericTypeParameters = listOf(genericType),
        )
        val input = ChirPackage(
            semanticId = ChirSemanticId("pkg:generic-cast"),
            name = "pkg",
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:generic-cast"),
                    name = "main",
                    declarations = listOf(function),
                ),
            ),
            typeDefinitions = listOf(
                DefaultChirClassDeclaration(
                    semanticId = ChirSemanticId("class:Comparable"),
                    name = "pkg:Comparable",
                    typeDeclarationId = classId,
                    typeParameters = listOf("T"),
                ),
            ),
        )

        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = input,
                options = CodegenOptions(
                    enabled = true,
                    validateChirBeforeLowering = true,
                    verifyBeforeWrite = true,
                    emitBitcode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )

        val ir = output.modules.single().ir
        assertTrue(ir.contains("define ptr @castGenericToClassReference(ptr %value, ptr %ti.T.0)"), ir)
        assertTrue(ir.contains("select i1 true, ptr %value, ptr %value"), ir)
        assertTrue(!ir.contains("%generic.T = type opaque"), ir)
    }

    /**
     * 验证按名 CHIR 字段路径使用声明布局降低嵌套读取和字段写入。
     */
    @Test
    fun `lowers named member paths through declared aggregate layouts`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
        val innerId = ChirTypeDeclarationId("pkg", "Inner")
        val outerId = ChirTypeDeclarationId("pkg", "Outer")
        val innerType = ChirResolvedTypeRef(ChirStructType(innerId, fieldTypes = listOf(intType)))
        val outerType = ChirResolvedTypeRef(ChirStructType(outerId, fieldTypes = listOf(innerType, boolType)))
        val outerReferenceType = ChirResolvedTypeRef(ChirRefType(outerType))
        val parameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:member-path:outer"),
            name = "record",
            type = outerReferenceType,
            mutable = false,
        )
        val readId = ChirSemanticId("expr:member-path:read")
        val read = ChirMemoryExpression(
            semanticId = readId,
            operation = ChirMemoryOperation.GET_ELEMENT_BY_NAME.canonicalName,
            address = ChirParameterValue(
                parameter.semanticId,
                parameter.type,
                parameter.name,
                ownerFunctionId = ChirSemanticId("fn:member-path"),
            ),
            resultType = intType,
            memberNames = listOf("inner", "value"),
        )
        val store = ChirMemoryExpression(
            semanticId = ChirSemanticId("expr:member-path:store"),
            operation = ChirMemoryOperation.STORE_ELEMENT_BY_NAME.canonicalName,
            address = ChirParameterValue(
                parameter.semanticId,
                parameter.type,
                parameter.name,
                ownerFunctionId = ChirSemanticId("fn:member-path"),
            ),
            value = ChirConstantValue(ChirSemanticId("const:member-path:enabled"), boolType, "true"),
            resultType = unitType,
            memberNames = listOf("enabled"),
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:member-path"),
            name = "readAndEnable",
            returnType = intType,
            parameters = listOf(parameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:member-path"),
                    name = "entry",
                    expressions = listOf(read, store),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:member-path:return"),
                        returnValue = ChirLocalValue(readId, intType, "memberValue"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:member-path"),
        )
        val input = ChirPackage(
            semanticId = ChirSemanticId("pkg:member-path"),
            name = "pkg",
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:member-path"),
                    name = "main",
                    declarations = listOf(function),
                ),
            ),
            typeDefinitions = listOf(
                DefaultChirStructDeclaration(
                    semanticId = ChirSemanticId("struct:Inner"),
                    name = "pkg:Inner",
                    typeDeclarationId = innerId,
                    fieldDeclarations = listOf(
                        DefaultChirVariableDeclaration(
                            semanticId = ChirSemanticId("field:Inner.value"),
                            name = "value",
                            type = intType,
                            mutable = true,
                        ),
                    ),
                ),
                DefaultChirStructDeclaration(
                    semanticId = ChirSemanticId("struct:Outer"),
                    name = "pkg:Outer",
                    typeDeclarationId = outerId,
                    fieldDeclarations = listOf(
                        DefaultChirVariableDeclaration(
                            semanticId = ChirSemanticId("field:Outer.inner"),
                            name = "inner",
                            type = innerType,
                            mutable = true,
                        ),
                        DefaultChirVariableDeclaration(
                            semanticId = ChirSemanticId("field:Outer.enabled"),
                            name = "enabled",
                            type = boolType,
                            mutable = true,
                        ),
                    ),
                ),
            ),
        )
        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = input,
                options = CodegenOptions(
                    enabled = true,
                    validateChirBeforeLowering = true,
                    verifyBeforeWrite = true,
                    emitBitcode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )

        val ir = output.modules.single().ir
        assertTrue(ir.contains("getelementptr %struct.pkg_Outer, ptr %record, i32 0, i32 0"), ir)
        assertTrue(ir.contains("getelementptr %struct.pkg_Inner"), ir)
        assertTrue(ir.contains("load i32, ptr"), ir)
        assertTrue(ir.contains("getelementptr %struct.pkg_Outer, ptr %record, i32 0, i32 1"), ir)
        assertTrue(ir.contains("store i1 1, ptr"), ir)
    }

    /**
     * 验证有序 tuple 聚合按结果类型 lowering，且返回值引用绑定到表达式 SSA 身份。
     */
    @Test
    fun `lowers tuple aggregate elements in order and resolves expression result by identity`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val tupleType = ChirResolvedTypeRef(ChirTupleType(listOf(intType, boolType)))
        val tupleId = ChirSemanticId("expr:tuple")
        val tupleExpression = ChirTupleExpression(
            semanticId = tupleId,
            elements = listOf(
                ChirConstantValue(ChirSemanticId("const:tuple:first"), intType, "7"),
                ChirConstantValue(ChirSemanticId("const:tuple:second"), boolType, "true"),
            ),
            resultType = tupleType,
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:tuple"),
            name = "tuple",
            returnType = tupleType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:tuple"),
                    name = "entry",
                    expressions = listOf(tupleExpression),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:tuple:return"),
                        returnValue = ChirLocalValue(tupleId, tupleType, "official-local-6"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:tuple"),
        )
        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = ChirPackage(
                    semanticId = ChirSemanticId("pkg:tuple"),
                    name = "tuple",
                    modules = listOf(
                        ChirModule(
                            semanticId = ChirSemanticId("mod:tuple"),
                            name = "tuple",
                            declarations = listOf(function),
                        ),
                    ),
                ),
                options = CodegenOptions(
                    enabled = true,
                    validateChirBeforeLowering = true,
                    verifyBeforeWrite = true,
                    emitBitcode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )

        val ir = output.modules.single().ir
        assertEquals(2, ir.lines().count { "insertvalue" in it }, ir)
        assertTrue(ir.contains("insertvalue { i32, i1 } undef, i32 7, 0"), ir)
        assertTrue(ir.contains("insertvalue { i32, i1 }"), ir)
        assertTrue(ir.contains("ret { i32, i1 } %expr_tuple"), ir)
        assertTrue("official_local_6" !in ir, ir)
    }

    /**
     * 验证 Unit 在值位置使用零大小结构体，在函数 ABI 返回位置使用 void，并可经过栈与字段投影。
     */
    @Test
    fun `lowers Unit aggregate values separately from void function returns`() {
        val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val unitPointerType = ChirResolvedTypeRef(ChirCPointerType(unitType))
        val tupleType = ChirResolvedTypeRef(ChirTupleType(listOf(unitType, intType)))
        val allocaId = ChirSemanticId("expr:unit:alloca")
        val loadId = ChirSemanticId("expr:unit:load")
        val loadValueId = ChirSemanticId("value:unit:load")
        val loadValue = ChirLocalValue(
            semanticId = loadValueId,
            type = unitType,
            name = "loadedUnit",
            associatedExpressionId = loadId,
        )
        val firstTupleId = ChirSemanticId("expr:unit:first-tuple")
        val fieldId = ChirSemanticId("expr:unit:field")
        val fieldTupleId = ChirSemanticId("expr:unit:field-tuple")
        val callId = ChirSemanticId("expr:unit:call")
        val callTupleId = ChirSemanticId("expr:unit:call-tuple")
        val alloca = ChirMemoryExpression(
            semanticId = allocaId,
            operation = "alloca",
            address = ChirConstantValue(ChirSemanticId("const:unit:count"), intType, "1"),
            resultType = unitPointerType,
        )
        val address = ChirLocalValue(allocaId, unitPointerType, "unitSlot")
        val store = ChirMemoryExpression(
            semanticId = ChirSemanticId("expr:unit:store"),
            operation = "store",
            address = address,
            value = ChirConstantValue(ChirSemanticId("const:unit:value"), unitType, "zeroinitializer"),
        )
        val load = ChirMemoryExpression(
            semanticId = loadId,
            operation = "load",
            address = address,
            resultType = unitType,
            resultLocalValue = loadValue,
        )
        val firstTuple = ChirTupleExpression(
            semanticId = firstTupleId,
            elements = listOf(
                loadValue,
                ChirConstantValue(ChirSemanticId("const:unit:first-int"), intType, "7"),
            ),
            resultType = tupleType,
        )
        val unitField = ChirFieldExpression(
            semanticId = fieldId,
            base = ChirLocalValue(firstTupleId, tupleType, "firstTuple"),
            path = listOf(0uL),
            resultType = unitType,
        )
        val fieldTuple = ChirTupleExpression(
            semanticId = fieldTupleId,
            elements = listOf(
                ChirLocalValue(fieldId, unitType, "projectedUnit"),
                ChirConstantValue(ChirSemanticId("const:unit:field-int"), intType, "8"),
            ),
            resultType = tupleType,
        )
        val unitFunctionId = ChirSemanticId("fn:unit-return")
        val unitFunction = DefaultChirFunctionDeclaration(
            semanticId = unitFunctionId,
            name = "unitReturn",
            returnType = unitType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:unit-return"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:unit-return:return"),
                        returnValue = ChirConstantValue(ChirSemanticId("const:unit:return"), unitType, "unit"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:unit-return"),
        )
        val unitCall = ChirCallExpression(
            semanticId = callId,
            callee = ChirFunctionValue(
                semanticId = unitFunctionId,
                type = ChirResolvedTypeRef(ChirFunctionType(emptyList(), unitType)),
                name = "unitReturn",
            ),
            arguments = emptyList(),
            resultType = unitType,
        )
        val callTuple = ChirTupleExpression(
            semanticId = callTupleId,
            elements = listOf(
                ChirLocalValue(callId, unitType, "calledUnit"),
                ChirConstantValue(ChirSemanticId("const:unit:call-int"), intType, "9"),
            ),
            resultType = tupleType,
        )
        val aggregateFunction = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:unit-aggregate"),
            name = "unitAggregate",
            returnType = tupleType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:unit-aggregate"),
                    name = "entry",
                    expressions = listOf(alloca, store, load, firstTuple, unitField, fieldTuple, unitCall, callTuple),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:unit-aggregate:return"),
                        returnValue = ChirLocalValue(callTupleId, tupleType, "callTuple"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:unit-aggregate"),
        )
        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = ChirPackage(
                    semanticId = ChirSemanticId("pkg:unit-aggregate"),
                    name = "unitAggregate",
                    modules = listOf(
                        ChirModule(
                            semanticId = ChirSemanticId("mod:unit-aggregate"),
                            name = "unitAggregate",
                            declarations = listOf(unitFunction, aggregateFunction),
                        ),
                    ),
                ),
                options = CodegenOptions(
                    enabled = true,
                    validateChirBeforeLowering = true,
                    verifyBeforeWrite = true,
                    emitBitcode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )

        val ir = output.modules.single().ir
        assertTrue(ir.contains("%cangjie.unit = type { }"), ir)
        assertTrue(ir.contains("define { %cangjie.unit, i32 } @unitAggregate()"), ir)
        assertTrue(ir.contains("define void @unitReturn()"), ir)
        assertTrue(ir.contains("alloca %cangjie.unit"), ir)
        assertTrue(ir.contains("store %cangjie.unit zeroinitializer"), ir)
        assertTrue(ir.contains("load %cangjie.unit"), ir)
        assertTrue(ir.contains("extractvalue { %cangjie.unit, i32 } %expr_unit_first_tuple, 0"), ir)
        assertTrue(ir.contains("%expr_unit_field_tuple.intermediate.0 = insertvalue { %cangjie.unit, i32 } undef, %cangjie.unit %expr_unit_field, 0"), ir)
        assertTrue(ir.contains("call void @unitReturn()"), ir)
        assertTrue(ir.contains("%expr_unit_call_tuple.intermediate.0 = insertvalue { %cangjie.unit, i32 } undef, %cangjie.unit zeroinitializer, 0"), ir)
        assertTrue(!ir.contains("{ void, i32 }"), ir)
        assertTrue(ir.contains("ret void"), ir)
    }

    /**
     * 验证普通枚举保留 UInt32 选择器，Option-like 枚举才将标签转换为 Bool。
     */
    @Test
    fun `lowers enum selector according to enum definition`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val uintType = ChirResolvedTypeRef(ChirPrimitiveType.UINT32)
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val ordinaryEnumType = ChirResolvedTypeRef(
            ChirEnumType(
                declarationId = ChirTypeDeclarationId("test", "Ordinary"),
                cases = listOf(ChirEnumCaseType("First"), ChirEnumCaseType("Second"), ChirEnumCaseType("Third")),
            ),
        )
        val optionLikeEnumType = ChirResolvedTypeRef(
            ChirEnumType(
                declarationId = ChirTypeDeclarationId("test", "OptionLike"),
                cases = listOf(
                    ChirEnumCaseType("None"),
                    ChirEnumCaseType("Some", listOf(intType)),
                ),
            ),
        )
        val ordinaryFunction = enumSelectorFunction(
            functionId = "fn:ordinary-selector",
            functionName = "ordinarySelector",
            enumType = ordinaryEnumType,
            selectorType = uintType,
        )
        val optionLikeFunction = enumSelectorFunction(
            functionId = "fn:optionlike-selector",
            functionName = "optionLikeSelector",
            enumType = optionLikeEnumType,
            selectorType = boolType,
        )
        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = ChirPackage(
                    semanticId = ChirSemanticId("pkg:enum-selector"),
                    name = "enumSelector",
                    modules = listOf(
                        ChirModule(
                            semanticId = ChirSemanticId("mod:enum-selector"),
                            name = "enumSelector",
                            declarations = listOf(ordinaryFunction, optionLikeFunction),
                        ),
                    ),
                ),
                options = CodegenOptions(
                    enabled = true,
                    validateChirBeforeLowering = true,
                    verifyBeforeWrite = true,
                    emitBitcode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )

        val ir = output.modules.single().ir
        val ordinaryBody = ir.substringAfter("define i32 @ordinarySelector").substringBefore("}")
        val optionLikeBody = ir.substringAfter("define i1 @optionLikeSelector").substringBefore("}")
        assertTrue(ordinaryBody.contains("select i1 true, i32 %value, i32 %value"), ir)
        assertTrue(!ordinaryBody.contains("extractvalue"), ir)
        assertTrue(!ordinaryBody.contains("trunc i32"), ir)
        assertTrue(optionLikeBody.contains("extractvalue %enum.test_OptionLike"), ir)
        assertTrue(optionLikeBody.contains("trunc i32"), ir)
    }

    /**
     * 验证聚合值字段路径 lowering 为 LLVM extractvalue。
     */
    @Test
    fun `lowers field projection from tuple aggregate`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val tupleType = ChirResolvedTypeRef(ChirTupleType(listOf(intType, boolType)))
        val tupleId = ChirSemanticId("expr:field:tuple")
        val fieldId = ChirSemanticId("expr:field:value")
        val tuple = ChirTupleExpression(
            semanticId = tupleId,
            elements = listOf(
                ChirConstantValue(ChirSemanticId("const:field:first"), intType, "7"),
                ChirConstantValue(ChirSemanticId("const:field:second"), boolType, "true"),
            ),
            resultType = tupleType,
        )
        val field = ChirFieldExpression(
            semanticId = fieldId,
            base = ChirLocalValue(tupleId, tupleType, "tuple"),
            path = listOf(0uL),
            resultType = intType,
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:field-projection"),
            name = "fieldProjection",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:field-projection"),
                    name = "entry",
                    expressions = listOf(tuple, field),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:field-projection:return"),
                        returnValue = ChirLocalValue(fieldId, intType, "field"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:field-projection"),
        )
        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = ChirPackage(
                    semanticId = ChirSemanticId("pkg:field-projection"),
                    name = "fieldProjection",
                    modules = listOf(
                        ChirModule(
                            semanticId = ChirSemanticId("mod:field-projection"),
                            name = "fieldProjection",
                            declarations = listOf(function),
                        ),
                    ),
                ),
                options = CodegenOptions(
                    enabled = true,
                    validateChirBeforeLowering = true,
                    verifyBeforeWrite = true,
                    emitBitcode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )

        val ir = output.modules.single().ir
        assertTrue(ir.contains("extractvalue { i32, i1 } %expr_field_tuple, 0"), ir)
        assertTrue(ir.contains("ret i32 %expr_field_value"), ir)
    }

    /**
     * 验证开启 trace 时 pipeline 会输出 pass 追踪行。
     */
    @Test
    fun `emits lowering trace when enabled`() {
        val generator = DefaultChirToLlvmCodeGenerator()
        val output = generator.generate(
            ChirCodegenInput(
                chirPackage = validPackage(),
                options = CodegenOptions(
                    enabled = true,
                    emitLoweringTrace = true,
                    verifyBeforeWrite = true,
                    validateChirBeforeLowering = true,
                    emitBitcode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )

        assertTrue(output.loweringTrace.any { it.contains("symbol-collection") }, output.loweringTrace.joinToString("\n"))
        assertTrue(output.loweringTrace.any { it.contains("type-mapping") }, output.loweringTrace.joinToString("\n"))
        assertTrue(output.loweringTrace.any { it.contains("control-flow-contract") }, output.loweringTrace.joinToString("\n"))
    }

    /**
     * 验证控制流 pass 会拒绝无效 CFG。
     */
    @Test
    fun `control-flow pass rejects invalid cfg`() {
        val generator = DefaultChirToLlvmCodeGenerator()
        val error = assertThrows<IllegalArgumentException> {
            generator.generate(
                ChirCodegenInput(
                    chirPackage = invalidCfgPackage(),
                    options = CodegenOptions(
                        enabled = true,
                        validateChirBeforeLowering = false,
                        emitLoweringTrace = true,
                        verifyBeforeWrite = true,
                        emitBitcode = false,
                        emitComments = false,
                        emitModuleHeader = false,
                        emitRuntimeDeclarations = false,
                    ),
                ),
            )
        }

        assertTrue(error.message?.contains("control-flow contract") == true, error.message)
    }

    /**
     * 构造控制流合法的测试 package。
     */
    private fun validPackage(): ChirPackage {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:main"),
            name = "main",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirConstantValue(
                            semanticId = ChirSemanticId("const:zero"),
                            type = intType,
                            literal = "0",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        return ChirPackage(
            semanticId = ChirSemanticId("pkg:valid"),
            name = "valid",
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:valid"),
                    name = "valid",
                    declarations = listOf(function),
                ),
            ),
        )
    }

    private fun enumSelectorFunction(
        functionId: String,
        functionName: String,
        enumType: ChirResolvedTypeRef,
        selectorType: ChirResolvedTypeRef,
    ): DefaultChirFunctionDeclaration {
        val functionSemanticId = ChirSemanticId(functionId)
        val parameterSemanticId = ChirSemanticId("$functionId:parameter")
        val selectorSemanticId = ChirSemanticId("$functionId:selector")
        val parameter = DefaultChirVariableDeclaration(
            semanticId = parameterSemanticId,
            name = "value",
            type = enumType,
            mutable = false,
        )
        val selector = ChirFieldExpression(
            semanticId = selectorSemanticId,
            base = ChirParameterValue(
                semanticId = parameterSemanticId,
                type = enumType,
                name = "value",
                ownerFunctionId = functionSemanticId,
            ),
            path = listOf(0uL),
            resultType = selectorType,
        )
        return DefaultChirFunctionDeclaration(
            semanticId = functionSemanticId,
            name = functionName,
            returnType = selectorType,
            parameters = listOf(parameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("$functionId:block"),
                    name = "entry",
                    expressions = listOf(selector),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("$functionId:return"),
                        returnValue = ChirLocalValue(selectorSemanticId, selectorType, "selector"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("$functionId:block"),
        )
    }

    /** 校验负 RawArray 长度抛异常时传给 runtime 的尺寸符合对象头与实例对齐 ABI。 */
    @Test
    fun `allocates negative array size exception using aligned instance size and typeinfo pointer`() {
        val lengthType = ChirResolvedTypeRef(ChirPrimitiveType.INT64)
        val arrayType = ChirResolvedTypeRef(
            ChirRawArrayType(ChirResolvedTypeRef(ChirPrimitiveType.INT32)),
        )
        val allocationId = ChirSemanticId("expr:rawarray:allocate")
        val allocation = ChirOtherExpression(
            semanticId = allocationId,
            operation = ChirOtherOperation.RAW_ARRAY_ALLOCATE.canonicalName,
            operands = listOf(ChirConstantValue(ChirSemanticId("const:rawarray:length"), lengthType, "-1")),
            resultType = arrayType,
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:rawarrayAllocate"),
            name = "rawarrayAllocate",
            returnType = arrayType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:rawarrayAllocate"),
                    name = "entry",
                    expressions = listOf(allocation),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:rawarrayAllocate:return"),
                        returnValue = ChirLocalValue(allocationId, arrayType, "array"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:rawarrayAllocate"),
        )
        val output = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = ChirPackage(
                    semanticId = ChirSemanticId("pkg:rawarrayAllocate"),
                    name = "rawarrayAllocate",
                    modules = listOf(
                        ChirModule(
                            semanticId = ChirSemanticId("mod:rawarrayAllocate"),
                            name = "rawarrayAllocate",
                            declarations = listOf(function),
                        ),
                    ),
                ),
                options = CodegenOptions(
                    emitBitcode = false,
                    verifyBeforeWrite = false,
                    emitComments = false,
                    emitModuleHeader = false,
                ),
            ),
        )

        val ir = output.modules.single().ir
        assertTrue("zext i32 " in ir, ir)
        assertTrue("and i64 " in ir, ir)
        assertTrue("ptrtoint ptr getelementptr (ptr, ptr null, i32 1) to i64" in ir, ir)
        assertTrue(Regex("call ptr @MCC_NewObject\\(ptr [^,]+, i32 %[^)]+\\)").containsMatchIn(ir), ir)
    }

    /**
     * 构造入口块缺失的非法 CFG package。
     */
    private fun generateIr(
        vararg functions: DefaultChirFunctionDeclaration,
        typeDefinitions: List<ChirTypeDeclaration> = emptyList(),
    ): String = DefaultChirToLlvmCodeGenerator().generate(
        ChirCodegenInput(
            chirPackage = ChirPackage(
                semanticId = ChirSemanticId("pkg:lowering-integration"),
                name = "lowering_integration",
                modules = listOf(
                    ChirModule(
                        semanticId = ChirSemanticId("mod:lowering-integration"),
                        name = "main",
                        declarations = functions.toList(),
                    ),
                ),
                typeDefinitions = typeDefinitions,
            ),
            options = CodegenOptions(
                enabled = true,
                validateChirBeforeLowering = true,
                verifyBeforeWrite = true,
                emitBitcode = false,
                emitComments = false,
                emitModuleHeader = false,
                emitRuntimeDeclarations = false,
            ),
        ),
    ).modules.single().ir

    private fun genericType(identifier: String, semanticIdentity: String, declarationIndex: Int) =
        ChirResolvedTypeRef(
            ChirGenericType(
                identifier = identifier,
                declarationIndex = declarationIndex,
                semanticIdentity = semanticIdentity,
            ),
        )

    private fun nominalClassType(
        packageName: String,
        declarationName: String,
        vararg typeArguments: ChirTypeRef,
    ) = ChirResolvedTypeRef(
        ChirClassType(
            declarationId = ChirTypeDeclarationId(packageName, declarationName),
            typeArguments = typeArguments.toList(),
        ),
    )

    private fun genericParameter(
        name: String,
        type: ChirTypeRef,
        ownerFunctionId: ChirSemanticId,
    ) = DefaultChirVariableDeclaration(
        semanticId = ChirSemanticId("${ownerFunctionId.value}:param:$name"),
        name = name,
        type = type,
        mutable = false,
    )

    private fun parameterValue(
        parameter: DefaultChirVariableDeclaration,
        ownerFunctionId: ChirSemanticId,
    ) = ChirParameterValue(
        semanticId = parameter.semanticId,
        type = parameter.type,
        name = parameter.name,
        ownerFunctionId = ownerFunctionId,
    )

    private fun assertOrderedTypeInfoArguments(
        ir: String,
        call: TypeTemplateCall,
        expectedArguments: List<String>,
    ) {
        val arrayDeclaration = "alloca [${expectedArguments.size} x ptr]"
        val arrayStart = ir.lastIndexOf(arrayDeclaration, call.endOffset)
        assertTrue(arrayStart >= 0, "TypeInfo argument array missing before ${call.template}:\n$ir")
        val argumentArrayBlock = ir.substring(arrayStart, call.endOffset)
        expectedArguments.forEachIndexed { index, expectedArgument ->
            val elementAddress = Regex(
                """(?m)^\s*(%[^\s]+) = getelementptr inbounds \[${expectedArguments.size} x ptr\], ptr [^,]+, i64 0, i64 $index$""",
            ).find(argumentArrayBlock)?.groupValues?.get(1)?.trim()
            assertTrue(
                elementAddress != null,
                "TypeInfo argument $index address missing for ${call.template}:\n$argumentArrayBlock",
            )
            assertTrue(
                argumentArrayBlock.contains("store ptr $expectedArgument, ptr $elementAddress"),
                "TypeInfo argument $index for ${call.template} must be $expectedArgument in declaration order:\n$argumentArrayBlock",
            )
        }
    }

    private fun assertNestedTypeInfoArgument(
        ir: String,
        calls: List<TypeTemplateCall>,
        outerCall: TypeTemplateCall,
        innerTemplate: String,
    ) {
        val innerCall = calls.last { it.template == innerTemplate && it.endOffset < outerCall.endOffset }
        val outerArrayStart = ir.lastIndexOf("alloca [${outerCall.argumentCount} x ptr]", outerCall.endOffset)
        assertTrue(outerArrayStart > innerCall.endOffset, "nested TypeInfo must be lowered before its enclosing TypeInfo:\n$ir")
        val outerArgumentArray = ir.substring(outerArrayStart, outerCall.endOffset)
        assertTrue(
            outerArgumentArray.contains("store ptr ${innerCall.result}, ptr "),
            "${outerCall.template} must receive TypeInfo produced by nested ${innerCall.template}:\n$outerArgumentArray",
        )
        assertTrue(
            outerCall.arguments in outerArgumentArray,
            "${outerCall.template} must consume its own ordered TypeInfo argument array:\n$outerArgumentArray",
        )
    }

    private data class TypeTemplateCall(
        val result: String,
        val template: String,
        val argumentCount: Int,
        val arguments: String,
        val endOffset: Int,
    )

    private data class TypeTemplateCallShape(
        val template: String,
        val argumentCount: Int,
    )

    /**
     * 构造入口块缺失的非法 CFG package。
     */
    private fun invalidCfgPackage(): ChirPackage {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:broken"),
            name = "broken",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirBranchTerminator(
                        semanticId = ChirSemanticId("term:branch"),
                        targetBlockId = ChirSemanticId("block:missing"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        return ChirPackage(
            semanticId = ChirSemanticId("pkg:broken"),
            name = "broken",
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:broken"),
                    name = "broken",
                    declarations = listOf(function),
                ),
            ),
        )
    }
}
