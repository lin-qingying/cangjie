package org.cangnova.cangjie.codegen.parity

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirThrowTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.model.ChirPackageMembers
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType.BOOL
import org.cangnova.cangjie.chir.core.type.ChirCPointerType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirImportedFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.codegen.api.ChirCodegenInput
import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.pipeline.DefaultChirToLlvmCodeGenerator
import junit.framework.TestCase
import java.io.File
import java.nio.charset.StandardCharsets

abstract class AbstractCodegenParityTestCase : TestCase() {
    private val comparator = LlvmIrParityComparator()

    protected fun runTest(testDataFilePath: String) {
        val fixtureFile = resolveTestDataPath(testDataFilePath)
        require(fixtureFile.exists()) { "fixture not found: ${fixtureFile.path}" }
        require(fixtureFile.extension == "json" && fixtureFile.name.endsWith(".chir.json")) {
            "fixture must be *.chir.json: ${fixtureFile.path}"
        }

        val expectedFile = File(fixtureFile.parentFile, "${fixtureFile.nameWithoutExtension.removeSuffix(".chir")}.txt")
        require(expectedFile.exists()) { "missing golden file: ${expectedFile.path}" }

        val generated = generateFixtureIr(fixtureFile.readText(StandardCharsets.UTF_8))
        val expected = expectedFile.readText(StandardCharsets.UTF_8)
        val result = comparator.compare(expected, generated)
        assertTrue(comparator.formatFirstDiffReport(result), result.matches)
    }

    protected fun assertAllFilesPresentByMetadata(testDataRootRelativePath: String) {
        val dir = resolveTestDataPath(testDataRootRelativePath)
        require(dir.isDirectory) { "testData dir not found: ${dir.path}" }
        val missingGolden = dir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".chir.json") }
            .filterNot { File(it.parentFile, "${it.nameWithoutExtension.removeSuffix(".chir")}.txt").exists() }
            .map { it.path }
            .toList()
        check(missingGolden.isEmpty()) {
            "Missing .txt golden files for parity fixtures:\n${missingGolden.joinToString("\n")}"
        }
    }

    protected fun resolveTestDataPath(path: String): File {
        val direct = File(path)
        if (direct.isAbsolute) return direct
        if (direct.exists()) return direct

        var cursor = File(System.getProperty("user.dir", ".")).absoluteFile
        while (true) {
            val candidate = cursor.resolve(path)
            if (candidate.exists()) return candidate
            val parent = cursor.parentFile ?: break
            cursor = parent
        }
        return direct
    }

    protected fun generateFixtureIr(fixtureText: String): String {
        val chirPackage = buildChirPackageFromFixture(fixtureText)
        return DefaultChirToLlvmCodeGenerator()
            .generate(
                ChirCodegenInput(
                    chirPackage = chirPackage,
                    options = CodegenOptions(
                        enabled = true,
                        verifyBeforeWrite = true,
                        emitBitcode = false,
                        emitComments = false,
                        emitModuleHeader = false,
                        emitRuntimeDeclarations = false,
                    ),
                ),
            )
            .modules
            .single()
            .ir
    }

    private fun buildChirPackageFromFixture(fixtureText: String): ChirPackage {
        val scenario = optionalStringField(fixtureText, "scenario") ?: "return"
        val packageName = requiredStringField(fixtureText, "package")
        val moduleName = requiredStringField(fixtureText, "module")
        val functionName = requiredStringField(fixtureText, "function")
        return when (scenario) {
            "return" -> buildReturnFixturePackage(fixtureText, packageName, moduleName, functionName)
            "branch_phi" -> buildBranchPhiFixturePackage(packageName, moduleName, functionName)
            "imported_call" -> buildImportedCallFixturePackage(packageName, moduleName, functionName)
            "throw_unwind" -> buildThrowUnwindFixturePackage(packageName, moduleName, functionName)
            "runtime_globals" -> buildRuntimeGlobalsFixturePackage(packageName, moduleName, functionName)
            "float_cmp" -> buildFloatCompareFixturePackage(packageName, moduleName, functionName)
            "cast_chain" -> buildCastChainFixturePackage(packageName, moduleName, functionName)
            "memory_gep_align" -> buildMemoryGepAlignFixturePackage(packageName, moduleName, functionName)
            "tail_call_cc" -> buildTailCallCcFixturePackage(packageName, moduleName, functionName)
            "unary_alias" -> buildUnaryAliasFixturePackage(packageName, moduleName, functionName)
            "select_i32" -> buildSelectI32FixturePackage(packageName, moduleName, functionName)
            "bitwise_shift_chain" -> buildBitwiseShiftChainFixturePackage(packageName, moduleName, functionName)
            "ptr_int_cast_chain" -> buildPtrIntCastChainFixturePackage(packageName, moduleName, functionName)
            "unary_logic_not" -> buildUnaryLogicNotFixturePackage(packageName, moduleName, functionName)
            "unsigned_chain" -> buildUnsignedChainFixturePackage(packageName, moduleName, functionName)
            "float_cast_chain" -> buildFloatCastChainFixturePackage(packageName, moduleName, functionName)
            "unary_misc" -> buildUnaryMiscFixturePackage(packageName, moduleName, functionName)
            "void_call" -> buildVoidCallFixturePackage(packageName, moduleName, functionName)
            "gep_spaced_inbounds" -> buildGepSpacedInboundsFixturePackage(packageName, moduleName, functionName)
            "operation_matrix" -> buildOperationMatrixFixturePackage(packageName, moduleName, functionName)
            else -> error("unsupported scenario '$scenario' in chir fixture")
        }
    }

    private fun buildReturnFixturePackage(
        fixtureText: String,
        packageName: String,
        moduleName: String,
        functionName: String,
    ): ChirPackage {
        val literal = requiredStringField(fixtureText, "operand")

        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:$functionName"),
            name = functionName,
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
                            semanticId = ChirSemanticId("const:return"),
                            type = intType,
                            literal = literal,
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        return ChirPackage(
            semanticId = ChirSemanticId("pkg:$packageName"),
            name = packageName,
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:$moduleName"),
                    name = moduleName,
                    declarations = listOf(function),
                ),
            ),
        )
    }

    private fun buildBranchPhiFixturePackage(packageName: String, moduleName: String, functionName: String): ChirPackage {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val i1Type = ChirResolvedTypeRef(BOOL)
        val thenBlockId = ChirSemanticId("block:then")
        val elseBlockId = ChirSemanticId("block:else")
        val mergeBlockId = ChirSemanticId("block:merge")
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:$functionName"),
            name = functionName,
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirConditionalBranchTerminator(
                        semanticId = ChirSemanticId("term:entry:br"),
                        condition = ChirConstantValue(
                            semanticId = ChirSemanticId("const:cond"),
                            type = i1Type,
                            literal = "true",
                        ),
                        trueTargetBlockId = thenBlockId,
                        falseTargetBlockId = elseBlockId,
                    ),
                ),
                ChirBlock(
                    semanticId = thenBlockId,
                    name = "then",
                    expressions = emptyList(),
                    terminator = ChirBranchTerminator(
                        semanticId = ChirSemanticId("term:then:br"),
                        targetBlockId = mergeBlockId,
                    ),
                ),
                ChirBlock(
                    semanticId = elseBlockId,
                    name = "else",
                    expressions = emptyList(),
                    terminator = ChirBranchTerminator(
                        semanticId = ChirSemanticId("term:else:br"),
                        targetBlockId = mergeBlockId,
                    ),
                ),
                ChirBlock(
                    semanticId = mergeBlockId,
                    name = "merge",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:phi"),
                            operation = "phi",
                            operands = listOf(
                                ChirConstantValue(
                                    semanticId = ChirSemanticId("const:one"),
                                    type = intType,
                                    literal = "1",
                                    attributes = setOf(org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute("pred", thenBlockId.value)),
                                ),
                                ChirConstantValue(
                                    semanticId = ChirSemanticId("const:two"),
                                    type = intType,
                                    literal = "2",
                                    attributes = setOf(org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute("pred", elseBlockId.value)),
                                ),
                            ),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:merge:return"),
                        returnValue = ChirLocalValue(
                            semanticId = ChirSemanticId("expr:phi"),
                            type = intType,
                            name = "expr_phi",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        return simplePackage(packageName, moduleName, function)
    }

    private fun buildImportedCallFixturePackage(packageName: String, moduleName: String, functionName: String): ChirPackage {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val importedFunction = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:ext_add"),
            name = "ext_add",
            returnType = intType,
            parameters = listOf(
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("arg:x"),
                    name = "x",
                    type = intType,
                    mutable = false,
                ),
            ),
            blocks = emptyList(),
            entryBlockId = ChirSemanticId("block:none"),
            attributes = setOf(
                org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute("cc", "fastcc"),
                org.cangnova.cangjie.chir.core.attribute.ChirBooleanAttribute("nounwind", enabled = true),
            ),
        )
        val callExpr = ChirCallExpression(
            semanticId = ChirSemanticId("expr:call"),
            callee = ChirImportedFunctionValue(
                semanticId = ChirSemanticId("imported:ext_add"),
                type = ChirResolvedTypeRef(
                    ChirFunctionType(
                        parameterTypes = listOf(intType),
                        returnType = intType,
                    ),
                ),
                name = "ext_add",
                attributes = setOf(org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute("cc", "fastcc")),
            ),
            arguments = listOf(
                ChirConstantValue(
                    semanticId = ChirSemanticId("const:one"),
                    type = intType,
                    literal = "1",
                ),
            ),
            resultType = intType,
        )
        val caller = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:$functionName"),
            name = functionName,
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(callExpr),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirLocalValue(
                            semanticId = ChirSemanticId("expr:call"),
                            type = intType,
                            name = "expr_call",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        return ChirPackage(
            semanticId = ChirSemanticId("pkg:$packageName"),
            name = packageName,
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:$moduleName"),
                    name = moduleName,
                    declarations = listOf(caller),
                ),
            ),
            members = ChirPackageMembers(importedFunctions = listOf(importedFunction)),
        )
    }

    private fun buildThrowUnwindFixturePackage(packageName: String, moduleName: String, functionName: String): ChirPackage {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val ptrType = ChirResolvedTypeRef(ChirCPointerType(ChirResolvedTypeRef(ChirPrimitiveType.INT8)))
        val unwindBlock = ChirSemanticId("block:unwind")
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:$functionName"),
            name = functionName,
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirThrowTerminator(
                        semanticId = ChirSemanticId("term:throw"),
                        exceptionValue = ChirConstantValue(
                            semanticId = ChirSemanticId("const:exc"),
                            type = ptrType,
                            literal = "null",
                        ),
                        unwindTargetBlockId = unwindBlock,
                    ),
                ),
                ChirBlock(
                    semanticId = unwindBlock,
                    name = "unwind",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirConstantValue(
                            semanticId = ChirSemanticId("const:ret"),
                            type = intType,
                            literal = "0",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        return simplePackage(packageName, moduleName, function)
    }

    private fun buildRuntimeGlobalsFixturePackage(packageName: String, moduleName: String, functionName: String): ChirPackage {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val initId = ChirSemanticId("fn:init")
        val literalInitId = ChirSemanticId("fn:literal_init")
        val initFunction = DefaultChirFunctionDeclaration(
            semanticId = initId,
            name = "init",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:init"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:init:return"),
                        returnValue = ChirConstantValue(
                            semanticId = ChirSemanticId("const:init"),
                            type = intType,
                            literal = "0",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:init"),
        )
        val literalInitFunction = DefaultChirFunctionDeclaration(
            semanticId = literalInitId,
            name = "literal_init",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:literal"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:literal:return"),
                        returnValue = ChirConstantValue(
                            semanticId = ChirSemanticId("const:literal"),
                            type = intType,
                            literal = "0",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:literal"),
        )
        val mainFunction = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:$functionName"),
            name = functionName,
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
                            semanticId = ChirSemanticId("const:ret"),
                            type = intType,
                            literal = "0",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        return ChirPackage(
            semanticId = ChirSemanticId("pkg:$packageName"),
            name = packageName,
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:$moduleName"),
                    name = moduleName,
                    declarations = listOf(mainFunction, initFunction, literalInitFunction),
                ),
            ),
            members = ChirPackageMembers(
                globalVariables = listOf(
                    DefaultChirVariableDeclaration(
                        semanticId = ChirSemanticId("global:mutable"),
                        name = "g_mut",
                        type = intType,
                        mutable = true,
                    ),
                    DefaultChirVariableDeclaration(
                        semanticId = ChirSemanticId("global:const"),
                        name = "g_const",
                        type = intType,
                        mutable = false,
                        attributes = setOf(org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute("initializer", "7")),
                    ),
                ),
            ),
            packageInitFunctionId = initId,
            packageLiteralInitFunctionId = literalInitId,
        )
    }

    private fun buildFloatCompareFixturePackage(packageName: String, moduleName: String, functionName: String): ChirPackage {
        val f64 = ChirResolvedTypeRef(ChirPrimitiveType.FLOAT64)
        val bool = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:$functionName"),
            name = functionName,
            returnType = bool,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:cmp"),
                            operator = "fgt",
                            left = ChirConstantValue(ChirSemanticId("const:left"), f64, "1.5"),
                            right = ChirConstantValue(ChirSemanticId("const:right"), f64, "0.5"),
                            resultType = bool,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirLocalValue(
                            semanticId = ChirSemanticId("expr:cmp"),
                            type = bool,
                            name = "expr_cmp",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        return simplePackage(packageName, moduleName, function)
    }

    private fun buildCastChainFixturePackage(packageName: String, moduleName: String, functionName: String): ChirPackage {
        val i32 = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val i64 = ChirResolvedTypeRef(ChirPrimitiveType.INT64)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:$functionName"),
            name = functionName,
            returnType = i64,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:zext"),
                            operation = "zext",
                            operands = listOf(
                                ChirConstantValue(
                                    semanticId = ChirSemanticId("const:one"),
                                    type = i32,
                                    literal = "1",
                                ),
                            ),
                            resultType = i64,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirLocalValue(
                            semanticId = ChirSemanticId("expr:zext"),
                            type = i64,
                            name = "expr_zext",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        return simplePackage(packageName, moduleName, function)
    }

    private fun buildMemoryGepAlignFixturePackage(packageName: String, moduleName: String, functionName: String): ChirPackage {
        val i32 = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val i64 = ChirResolvedTypeRef(ChirPrimitiveType.INT64)
        val ptrI32 = ChirResolvedTypeRef(org.cangnova.cangjie.chir.core.type.ChirCPointerType(i32))
        val allocaLocal = ChirLocalValue(
            semanticId = ChirSemanticId("expr:alloca"),
            type = ptrI32,
            name = "expr_alloca",
            attributes = setOf(org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute("align", "16")),
        )
        val loadLocal = ChirLocalValue(
            semanticId = ChirSemanticId("expr:load"),
            type = i32,
            name = "expr_load",
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:$functionName"),
            name = functionName,
            returnType = i32,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:alloca"),
                            operation = "alloca",
                            address = ChirConstantValue(ChirSemanticId("const:count"), i64, "1"),
                            resultType = ptrI32,
                        ),
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:store"),
                            operation = "store",
                            address = allocaLocal,
                            value = ChirConstantValue(ChirSemanticId("const:value"), i32, "7"),
                        ),
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:gep"),
                            operation = "getelementptr.inbounds",
                            address = allocaLocal,
                            value = ChirConstantValue(ChirSemanticId("const:index"), i32, "0"),
                            resultType = ptrI32,
                        ),
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:load"),
                            operation = "load",
                            address = allocaLocal,
                            resultType = i32,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = loadLocal,
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        return simplePackage(packageName, moduleName, function)
    }

    private fun buildTailCallCcFixturePackage(packageName: String, moduleName: String, functionName: String): ChirPackage {
        val i32 = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val imported = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:ext_fast"),
            name = "ext_fast",
            returnType = i32,
            parameters = listOf(
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("arg:x"),
                    name = "x",
                    type = i32,
                    mutable = false,
                ),
            ),
            blocks = emptyList(),
            entryBlockId = ChirSemanticId("block:none"),
            attributes = setOf(
                org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute("cc", "fastcc"),
            ),
        )
        val call = ChirCallExpression(
            semanticId = ChirSemanticId("expr:call"),
            callee = ChirImportedFunctionValue(
                semanticId = ChirSemanticId("imported:ext_fast"),
                type = ChirResolvedTypeRef(
                    ChirFunctionType(parameterTypes = listOf(i32), returnType = i32),
                ),
                name = "ext_fast",
                attributes = setOf(
                    org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute("cc", "fastcc"),
                    org.cangnova.cangjie.chir.core.attribute.ChirBooleanAttribute("tail", enabled = true),
                ),
            ),
            arguments = listOf(ChirConstantValue(ChirSemanticId("const:one"), i32, "1")),
            resultType = i32,
        )
        val caller = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:$functionName"),
            name = functionName,
            returnType = i32,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(call),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirLocalValue(
                            semanticId = ChirSemanticId("expr:call"),
                            type = i32,
                            name = "expr_call",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        return ChirPackage(
            semanticId = ChirSemanticId("pkg:$packageName"),
            name = packageName,
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:$moduleName"),
                    name = moduleName,
                    declarations = listOf(caller),
                ),
            ),
            members = ChirPackageMembers(importedFunctions = listOf(imported)),
        )
    }

    private fun buildUnaryAliasFixturePackage(packageName: String, moduleName: String, functionName: String): ChirPackage {
        val i32 = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:$functionName"),
            name = functionName,
            returnType = i32,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirUnaryExpression(
                            semanticId = ChirSemanticId("expr:neg"),
                            operator = "ineg",
                            operand = ChirConstantValue(
                                semanticId = ChirSemanticId("const:one"),
                                type = i32,
                                literal = "1",
                            ),
                            resultType = i32,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirLocalValue(
                            semanticId = ChirSemanticId("expr:neg"),
                            type = i32,
                            name = "expr_neg",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        return simplePackage(packageName, moduleName, function)
    }

    private fun buildSelectI32FixturePackage(packageName: String, moduleName: String, functionName: String): ChirPackage {
        val i32 = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val i1 = ChirResolvedTypeRef(BOOL)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:$functionName"),
            name = functionName,
            returnType = i32,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:select"),
                            operation = "select",
                            operands = listOf(
                                ChirConstantValue(
                                    semanticId = ChirSemanticId("const:cond"),
                                    type = i1,
                                    literal = "true",
                                ),
                                ChirConstantValue(
                                    semanticId = ChirSemanticId("const:lhs"),
                                    type = i32,
                                    literal = "1",
                                ),
                                ChirConstantValue(
                                    semanticId = ChirSemanticId("const:rhs"),
                                    type = i32,
                                    literal = "2",
                                ),
                            ),
                            resultType = i32,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirLocalValue(
                            semanticId = ChirSemanticId("expr:select"),
                            type = i32,
                            name = "expr_select",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        return simplePackage(packageName, moduleName, function)
    }

    private fun buildBitwiseShiftChainFixturePackage(packageName: String, moduleName: String, functionName: String): ChirPackage {
        val i32 = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:$functionName"),
            name = functionName,
            returnType = i32,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:and"),
                            operator = "and",
                            left = ChirConstantValue(ChirSemanticId("const:and_lhs"), i32, "6"),
                            right = ChirConstantValue(ChirSemanticId("const:and_rhs"), i32, "3"),
                            resultType = i32,
                        ),
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:xor"),
                            operator = "xor",
                            left = ChirLocalValue(ChirSemanticId("expr:and"), i32, "expr_and"),
                            right = ChirConstantValue(ChirSemanticId("const:xor_rhs"), i32, "1"),
                            resultType = i32,
                        ),
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:shl"),
                            operator = "shl",
                            left = ChirLocalValue(ChirSemanticId("expr:xor"), i32, "expr_xor"),
                            right = ChirConstantValue(ChirSemanticId("const:shl_rhs"), i32, "1"),
                            resultType = i32,
                        ),
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:lshr"),
                            operator = "lshr",
                            left = ChirLocalValue(ChirSemanticId("expr:shl"), i32, "expr_shl"),
                            right = ChirConstantValue(ChirSemanticId("const:lshr_rhs"), i32, "1"),
                            resultType = i32,
                        ),
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:or"),
                            operator = "or",
                            left = ChirLocalValue(ChirSemanticId("expr:lshr"), i32, "expr_lshr"),
                            right = ChirConstantValue(ChirSemanticId("const:or_rhs"), i32, "8"),
                            resultType = i32,
                        ),
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:ashr"),
                            operator = "ashr",
                            left = ChirConstantValue(ChirSemanticId("const:ashr_lhs"), i32, "-8"),
                            right = ChirConstantValue(ChirSemanticId("const:ashr_rhs"), i32, "1"),
                            resultType = i32,
                        ),
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:add"),
                            operator = "add",
                            left = ChirLocalValue(ChirSemanticId("expr:or"), i32, "expr_or"),
                            right = ChirLocalValue(ChirSemanticId("expr:ashr"), i32, "expr_ashr"),
                            resultType = i32,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirLocalValue(
                            semanticId = ChirSemanticId("expr:add"),
                            type = i32,
                            name = "expr_add",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        return simplePackage(packageName, moduleName, function)
    }

    private fun buildPtrIntCastChainFixturePackage(packageName: String, moduleName: String, functionName: String): ChirPackage {
        val i8 = ChirResolvedTypeRef(ChirPrimitiveType.INT8)
        val i32 = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val i64 = ChirResolvedTypeRef(ChirPrimitiveType.INT64)
        val ptrI32 = ChirResolvedTypeRef(org.cangnova.cangjie.chir.core.type.ChirCPointerType(i32))
        val ptrI8 = ChirResolvedTypeRef(org.cangnova.cangjie.chir.core.type.ChirCPointerType(i8))
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:$functionName"),
            name = functionName,
            returnType = i64,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:alloca"),
                            operation = "alloca",
                            address = ChirConstantValue(ChirSemanticId("const:count"), i64, "1"),
                            resultType = ptrI32,
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:ptrtoint"),
                            operation = "ptrtoint",
                            operands = listOf(ChirLocalValue(ChirSemanticId("expr:alloca"), ptrI32, "expr_alloca")),
                            resultType = i64,
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:inttoptr"),
                            operation = "inttoptr",
                            operands = listOf(ChirLocalValue(ChirSemanticId("expr:ptrtoint"), i64, "expr_ptrtoint")),
                            resultType = ptrI32,
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:bitcast"),
                            operation = "bitcast",
                            operands = listOf(ChirLocalValue(ChirSemanticId("expr:inttoptr"), ptrI32, "expr_inttoptr")),
                            resultType = ptrI8,
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:ptrtoint2"),
                            operation = "ptrtoint",
                            operands = listOf(ChirLocalValue(ChirSemanticId("expr:bitcast"), ptrI8, "expr_bitcast")),
                            resultType = i64,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirLocalValue(
                            semanticId = ChirSemanticId("expr:ptrtoint2"),
                            type = i64,
                            name = "expr_ptrtoint2",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        return simplePackage(packageName, moduleName, function)
    }

    private fun buildUnaryLogicNotFixturePackage(packageName: String, moduleName: String, functionName: String): ChirPackage {
        val i1 = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:$functionName"),
            name = functionName,
            returnType = i1,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirUnaryExpression(
                            semanticId = ChirSemanticId("expr:not"),
                            operator = "logical_not",
                            operand = ChirConstantValue(
                                semanticId = ChirSemanticId("const:true"),
                                type = i1,
                                literal = "true",
                            ),
                            resultType = i1,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirLocalValue(
                            semanticId = ChirSemanticId("expr:not"),
                            type = i1,
                            name = "expr_not",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        return simplePackage(packageName, moduleName, function)
    }

    private fun buildUnsignedChainFixturePackage(packageName: String, moduleName: String, functionName: String): ChirPackage {
        val i32 = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val i1 = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:$functionName"),
            name = functionName,
            returnType = i1,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:udiv"),
                            operator = "udiv",
                            left = ChirConstantValue(ChirSemanticId("const:lhs"), i32, "9"),
                            right = ChirConstantValue(ChirSemanticId("const:rhs"), i32, "2"),
                            resultType = i32,
                        ),
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:urem"),
                            operator = "urem",
                            left = ChirConstantValue(ChirSemanticId("const:lhs2"), i32, "9"),
                            right = ChirConstantValue(ChirSemanticId("const:rhs2"), i32, "2"),
                            resultType = i32,
                        ),
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:add"),
                            operator = "+",
                            left = ChirLocalValue(ChirSemanticId("expr:udiv"), i32, "expr_udiv"),
                            right = ChirLocalValue(ChirSemanticId("expr:urem"), i32, "expr_urem"),
                            resultType = i32,
                        ),
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:cmp"),
                            operator = "uge",
                            left = ChirLocalValue(ChirSemanticId("expr:add"), i32, "expr_add"),
                            right = ChirConstantValue(ChirSemanticId("const:cmp"), i32, "5"),
                            resultType = i1,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirLocalValue(
                            semanticId = ChirSemanticId("expr:cmp"),
                            type = i1,
                            name = "expr_cmp",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        return simplePackage(packageName, moduleName, function)
    }

    private fun buildFloatCastChainFixturePackage(packageName: String, moduleName: String, functionName: String): ChirPackage {
        val f32 = ChirResolvedTypeRef(ChirPrimitiveType.FLOAT32)
        val f64 = ChirResolvedTypeRef(ChirPrimitiveType.FLOAT64)
        val i32 = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:$functionName"),
            name = functionName,
            returnType = f64,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:sitofp"),
                            operation = "sitofp",
                            operands = listOf(ChirConstantValue(ChirSemanticId("const:int"), i32, "3")),
                            resultType = f32,
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:fpext"),
                            operation = "fpext",
                            operands = listOf(ChirLocalValue(ChirSemanticId("expr:sitofp"), f32, "expr_sitofp")),
                            resultType = f64,
                        ),
                        ChirUnaryExpression(
                            semanticId = ChirSemanticId("expr:fneg"),
                            operator = "fneg",
                            operand = ChirLocalValue(ChirSemanticId("expr:fpext"), f64, "expr_fpext"),
                            resultType = f64,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirLocalValue(
                            semanticId = ChirSemanticId("expr:fneg"),
                            type = f64,
                            name = "expr_fneg",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        return simplePackage(packageName, moduleName, function)
    }

    private fun buildUnaryMiscFixturePackage(packageName: String, moduleName: String, functionName: String): ChirPackage {
        val i32 = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:$functionName"),
            name = functionName,
            returnType = i32,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirUnaryExpression(
                            semanticId = ChirSemanticId("expr:not"),
                            operator = "bitnot",
                            operand = ChirConstantValue(ChirSemanticId("const:v"), i32, "10"),
                            resultType = i32,
                        ),
                        ChirUnaryExpression(
                            semanticId = ChirSemanticId("expr:id"),
                            operator = "identity",
                            operand = ChirLocalValue(ChirSemanticId("expr:not"), i32, "expr_not"),
                            resultType = i32,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirLocalValue(
                            semanticId = ChirSemanticId("expr:id"),
                            type = i32,
                            name = "expr_id",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        return simplePackage(packageName, moduleName, function)
    }

    private fun buildVoidCallFixturePackage(packageName: String, moduleName: String, functionName: String): ChirPackage {
        val i32 = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val unit = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
        val imported = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:ext_log"),
            name = "ext_log",
            returnType = unit,
            parameters = listOf(
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("arg:x"),
                    name = "x",
                    type = i32,
                    mutable = false,
                ),
            ),
            blocks = emptyList(),
            entryBlockId = ChirSemanticId("block:none"),
        )
        val call = ChirCallExpression(
            semanticId = ChirSemanticId("expr:call_void"),
            callee = ChirImportedFunctionValue(
                semanticId = ChirSemanticId("imported:ext_log"),
                type = ChirResolvedTypeRef(
                    ChirFunctionType(parameterTypes = listOf(i32), returnType = unit),
                ),
                name = "ext_log",
            ),
            arguments = listOf(ChirConstantValue(ChirSemanticId("const:one"), i32, "1")),
            resultType = unit,
        )
        val caller = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:$functionName"),
            name = functionName,
            returnType = i32,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(call),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirConstantValue(
                            semanticId = ChirSemanticId("const:zero"),
                            type = i32,
                            literal = "0",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        return ChirPackage(
            semanticId = ChirSemanticId("pkg:$packageName"),
            name = packageName,
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:$moduleName"),
                    name = moduleName,
                    declarations = listOf(caller),
                ),
            ),
            members = ChirPackageMembers(importedFunctions = listOf(imported)),
        )
    }

    private fun buildGepSpacedInboundsFixturePackage(packageName: String, moduleName: String, functionName: String): ChirPackage {
        val i32 = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val i64 = ChirResolvedTypeRef(ChirPrimitiveType.INT64)
        val ptrI32 = ChirResolvedTypeRef(org.cangnova.cangjie.chir.core.type.ChirCPointerType(i32))
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:$functionName"),
            name = functionName,
            returnType = i32,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:alloca"),
                            operation = "alloca",
                            address = ChirConstantValue(ChirSemanticId("const:count"), i64, "1"),
                            resultType = ptrI32,
                        ),
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:gep"),
                            operation = "getelementptr inbounds",
                            address = ChirLocalValue(ChirSemanticId("expr:alloca"), ptrI32, "expr_alloca"),
                            value = ChirConstantValue(ChirSemanticId("const:index"), i32, "0"),
                            resultType = ptrI32,
                        ),
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:store"),
                            operation = "store",
                            address = ChirLocalValue(ChirSemanticId("expr:gep"), ptrI32, "expr_gep"),
                            value = ChirConstantValue(ChirSemanticId("const:value"), i32, "11"),
                        ),
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:load"),
                            operation = "load",
                            address = ChirLocalValue(ChirSemanticId("expr:gep"), ptrI32, "expr_gep"),
                            resultType = i32,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirLocalValue(
                            semanticId = ChirSemanticId("expr:load"),
                            type = i32,
                            name = "expr_load",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        return simplePackage(packageName, moduleName, function)
    }

    private fun buildOperationMatrixFixturePackage(packageName: String, moduleName: String, functionName: String): ChirPackage {
        val i32 = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val i64 = ChirResolvedTypeRef(ChirPrimitiveType.INT64)
        val i1 = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val f32 = ChirResolvedTypeRef(ChirPrimitiveType.FLOAT32)
        val f64 = ChirResolvedTypeRef(ChirPrimitiveType.FLOAT64)
        val expressions = listOf(
            ChirUnaryExpression(
                semanticId = ChirSemanticId("expr:neg"),
                operator = "neg",
                operand = ChirConstantValue(ChirSemanticId("const:neg"), i32, "3"),
                resultType = i32,
            ),
            ChirUnaryExpression(
                semanticId = ChirSemanticId("expr:fneg"),
                operator = "fneg",
                operand = ChirConstantValue(ChirSemanticId("const:fneg"), f64, "1.0"),
                resultType = f64,
            ),
            ChirUnaryExpression(
                semanticId = ChirSemanticId("expr:not"),
                operator = "bitnot",
                operand = ChirConstantValue(ChirSemanticId("const:not"), i32, "0"),
                resultType = i32,
            ),
            ChirUnaryExpression(
                semanticId = ChirSemanticId("expr:lnot"),
                operator = "lnot",
                operand = ChirConstantValue(ChirSemanticId("const:lnot"), i1, "true"),
                resultType = i1,
            ),
            ChirBinaryExpression(
                semanticId = ChirSemanticId("expr:add"),
                operator = "add",
                left = ChirConstantValue(ChirSemanticId("const:add_l"), i32, "1"),
                right = ChirConstantValue(ChirSemanticId("const:add_r"), i32, "2"),
                resultType = i32,
            ),
            ChirBinaryExpression(
                semanticId = ChirSemanticId("expr:udiv"),
                operator = "udiv",
                left = ChirConstantValue(ChirSemanticId("const:udiv_l"), i32, "8"),
                right = ChirConstantValue(ChirSemanticId("const:udiv_r"), i32, "2"),
                resultType = i32,
            ),
            ChirBinaryExpression(
                semanticId = ChirSemanticId("expr:eq"),
                operator = "eq",
                left = ChirConstantValue(ChirSemanticId("const:eq_l"), i32, "1"),
                right = ChirConstantValue(ChirSemanticId("const:eq_r"), i32, "1"),
                resultType = i1,
            ),
            ChirBinaryExpression(
                semanticId = ChirSemanticId("expr:ult"),
                operator = "ult",
                left = ChirConstantValue(ChirSemanticId("const:ult_l"), i32, "1"),
                right = ChirConstantValue(ChirSemanticId("const:ult_r"), i32, "2"),
                resultType = i1,
            ),
            ChirBinaryExpression(
                semanticId = ChirSemanticId("expr:fgt"),
                operator = "fgt",
                left = ChirConstantValue(ChirSemanticId("const:fgt_l"), f64, "2.0"),
                right = ChirConstantValue(ChirSemanticId("const:fgt_r"), f64, "1.0"),
                resultType = i1,
            ),
            ChirOtherExpression(
                semanticId = ChirSemanticId("expr:zext"),
                operation = "zext",
                operands = listOf(ChirConstantValue(ChirSemanticId("const:zext"), i32, "1")),
                resultType = i64,
            ),
            ChirOtherExpression(
                semanticId = ChirSemanticId("expr:trunc"),
                operation = "trunc",
                operands = listOf(ChirConstantValue(ChirSemanticId("const:trunc"), i64, "9")),
                resultType = i32,
            ),
            ChirOtherExpression(
                semanticId = ChirSemanticId("expr:sitofp"),
                operation = "sitofp",
                operands = listOf(ChirConstantValue(ChirSemanticId("const:sitofp"), i32, "7")),
                resultType = f32,
            ),
            ChirOtherExpression(
                semanticId = ChirSemanticId("expr:fptosi"),
                operation = "fptosi",
                operands = listOf(ChirConstantValue(ChirSemanticId("const:fptosi"), f32, "7.0")),
                resultType = i32,
            ),
            ChirOtherExpression(
                semanticId = ChirSemanticId("expr:select"),
                operation = "select",
                operands = listOf(
                    ChirConstantValue(ChirSemanticId("const:sel_c"), i1, "true"),
                    ChirConstantValue(ChirSemanticId("const:sel_l"), i32, "5"),
                    ChirConstantValue(ChirSemanticId("const:sel_r"), i32, "6"),
                ),
                resultType = i32,
            ),
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:$functionName"),
            name = functionName,
            returnType = i32,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = expressions,
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirLocalValue(
                            semanticId = ChirSemanticId("expr:select"),
                            type = i32,
                            name = "expr_select",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        return simplePackage(packageName, moduleName, function)
    }

    private fun simplePackage(packageName: String, moduleName: String, function: DefaultChirFunctionDeclaration): ChirPackage {
        return ChirPackage(
            semanticId = ChirSemanticId("pkg:$packageName"),
            name = packageName,
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:$moduleName"),
                    name = moduleName,
                    declarations = listOf(function),
                ),
            ),
        )
    }

    private fun requiredStringField(text: String, fieldName: String): String {
        val regex = """"$fieldName"\s*:\s*"([^"]+)"""".toRegex()
        return regex.find(text)?.groupValues?.get(1)
            ?: error("missing required field '$fieldName' in chir fixture")
    }

    private fun optionalStringField(text: String, fieldName: String): String? {
        val regex = """"$fieldName"\s*:\s*"([^"]+)"""".toRegex()
        return regex.find(text)?.groupValues?.get(1)
    }
}
