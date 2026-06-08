package org.cangnova.cangjie.jvm.codegen.pipeline

import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirThrowTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirEnumDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirStructDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirNamedType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirRawArrayType
import org.cangnova.cangjie.chir.core.type.ChirRefType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirImportedFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirImportedVariableValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.jvm.codegen.api.ChirJvmCodegenInput
import org.cangnova.cangjie.jvm.codegen.api.JvmClassFileArtifact
import org.cangnova.cangjie.jvm.codegen.api.writeJar
import org.cangnova.cangjie.jvm.codegen.context.JvmAbiAttributes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.objectweb.asm.ClassReader
import org.objectweb.asm.util.CheckClassAdapter
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Path
import java.util.jar.JarFile

class DefaultChirToJvmCodeGeneratorTest {
    @Test
    fun `generates verifier-loadable class and executable static function`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:sum"),
            name = "sum",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:add"),
                            operator = "+",
                            left = ChirConstantValue(ChirSemanticId("const:one"), intType, "1"),
                            right = ChirConstantValue(ChirSemanticId("const:two"), intType, "2"),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirLocalValue(ChirSemanticId("local:sum"), intType, "expr_add"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(function))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(3, generatedClass.getMethod("sum").invoke(null))
    }

    @Test
    fun `generates same-facade static function calls`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val callee = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:callee"),
            name = "callee",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:callee"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:callee:return"),
                        returnValue = ChirConstantValue(ChirSemanticId("const:forty"), intType, "40"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:callee"),
        )
        val caller = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:caller"),
            name = "caller",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:caller"),
                    name = "entry",
                    expressions = listOf(
                        ChirCallExpression(
                            semanticId = ChirSemanticId("expr:call"),
                            callee = ChirFunctionValue(
                                semanticId = ChirSemanticId("value:callee"),
                                type = ChirResolvedTypeRef(ChirFunctionType(emptyList(), intType)),
                                name = "callee",
                            ),
                            arguments = emptyList(),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:caller:return"),
                        returnValue = ChirLocalValue(ChirSemanticId("local:call"), intType, "expr_call"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:caller"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(callee, caller))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(40, generatedClass.getMethod("caller").invoke(null))
    }

    @Test
    fun `lowers CHIR memory local slots to JVM locals`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val refIntType = ChirResolvedTypeRef(ChirRefType(intType, mutable = true))
        val localAddress = ChirLocalValue(ChirSemanticId("addr:x"), refIntType, "x")
        val loadResult = ChirLocalValue(ChirSemanticId("expr:load-x"), intType, "expr_load_x")
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:local"),
            name = "localValue",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:alloca-x"),
                            operation = "alloca",
                            address = localAddress,
                        ),
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:store-initial-x"),
                            operation = "store",
                            address = localAddress,
                            value = ChirConstantValue(ChirSemanticId("const:five"), intType, "5"),
                        ),
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:store-updated-x"),
                            operation = "store",
                            address = localAddress,
                            value = ChirConstantValue(ChirSemanticId("const:nine"), intType, "9"),
                        ),
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:load-x"),
                            operation = "load",
                            address = localAddress,
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = loadResult,
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(function))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(9, generatedClass.getMethod("localValue").invoke(null))
    }

    @Test
    fun `lowers CHIR unsigned integer operations to JVM intrinsics`() {
        val uintType = ChirResolvedTypeRef(ChirPrimitiveType.UINT32)
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val unsignedGreater = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:unsignedGreater"),
            name = "unsignedGreater",
            returnType = boolType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:unsignedGreater"),
                    name = "entry",
                    expressions = listOf(
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:ugt"),
                            operator = "ugt",
                            left = ChirConstantValue(ChirSemanticId("const:minusOne"), uintType, "-1"),
                            right = ChirConstantValue(ChirSemanticId("const:one"), uintType, "1"),
                            resultType = boolType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:unsignedGreater:return"),
                        returnValue = ChirLocalValue(ChirSemanticId("expr:ugt"), boolType, "expr_ugt"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:unsignedGreater"),
        )
        val unsignedDiv = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:unsignedDiv"),
            name = "unsignedDiv",
            returnType = uintType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:unsignedDiv"),
                    name = "entry",
                    expressions = listOf(
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:udiv"),
                            operator = "udiv",
                            left = ChirConstantValue(ChirSemanticId("const:minusTwo"), uintType, "-2"),
                            right = ChirConstantValue(ChirSemanticId("const:two"), uintType, "2"),
                            resultType = uintType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:unsignedDiv:return"),
                        returnValue = ChirLocalValue(ChirSemanticId("expr:udiv"), uintType, "expr_udiv"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:unsignedDiv"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(unsignedGreater, unsignedDiv))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(true, generatedClass.getMethod("unsignedGreater").invoke(null))
        assertEquals(Int.MAX_VALUE, generatedClass.getMethod("unsignedDiv").invoke(null))
    }

    @Test
    fun `lowers CHIR unary bitnot and select expressions`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val bitNot = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:bitNot"),
            name = "bitNot",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:bitNot"),
                    name = "entry",
                    expressions = listOf(
                        ChirUnaryExpression(
                            semanticId = ChirSemanticId("expr:bitnot"),
                            operator = "not",
                            operand = ChirConstantValue(ChirSemanticId("const:zero"), intType, "0"),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:bitNot:return"),
                        returnValue = ChirLocalValue(ChirSemanticId("expr:bitnot"), intType, "expr_bitnot"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:bitNot"),
        )
        val select = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:selectValue"),
            name = "selectValue",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:selectValue"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:select"),
                            operation = "select",
                            operands = listOf(
                                ChirConstantValue(ChirSemanticId("const:true"), boolType, "true"),
                                ChirConstantValue(ChirSemanticId("const:seven"), intType, "7"),
                                ChirConstantValue(ChirSemanticId("const:nine"), intType, "9"),
                            ),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:selectValue:return"),
                        returnValue = ChirLocalValue(ChirSemanticId("expr:select"), intType, "expr_select"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:selectValue"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(bitNot, select))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(-1, generatedClass.getMethod("bitNot").invoke(null))
        assertEquals(7, generatedClass.getMethod("selectValue").invoke(null))
    }

    @Test
    fun `lowers imported JVM functions and static fields from explicit ABI attributes`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val stringType = ChirResolvedTypeRef(ChirNamedType("String"))
        val maxCall = ChirCallExpression(
            semanticId = ChirSemanticId("expr:math:max"),
            callee = ChirImportedFunctionValue(
                semanticId = ChirSemanticId("import:java.lang.Math.max"),
                type = ChirResolvedTypeRef(ChirFunctionType(listOf(intType, intType), intType)),
                name = "Math.max",
                attributes = jvmImport("java/lang/Math", "max", "static"),
            ),
            arguments = listOf(
                ChirConstantValue(ChirSemanticId("const:three"), intType, "3"),
                ChirConstantValue(ChirSemanticId("const:nine"), intType, "9"),
            ),
            resultType = intType,
        )
        val stringLengthCall = ChirCallExpression(
            semanticId = ChirSemanticId("expr:string:length"),
            callee = ChirImportedFunctionValue(
                semanticId = ChirSemanticId("import:java.lang.String.length"),
                type = ChirResolvedTypeRef(
                    ChirFunctionType(
                        parameterTypes = emptyList(),
                        returnType = intType,
                        receiverType = stringType,
                    ),
                ),
                name = "String.length",
                attributes = jvmImport("java/lang/String", "length", "virtual"),
            ),
            arguments = listOf(ChirConstantValue(ChirSemanticId("const:string"), stringType, "cangjie")),
            resultType = intType,
        )
        val integerMaxValue = ChirImportedVariableValue(
            semanticId = ChirSemanticId("import:java.lang.Integer.MAX_VALUE"),
            type = intType,
            name = "Integer.MAX_VALUE",
            attributes = setOf(
                ChirStringAttribute(JvmAbiAttributes.OWNER, "java/lang/Integer"),
                ChirStringAttribute(JvmAbiAttributes.NAME, "MAX_VALUE"),
            ),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(
            inputWith(
                functionReturningValue("maxValue", intType, listOf(maxCall), ChirLocalValue(ChirSemanticId("expr:math:max"), intType, "expr_math_max")),
                functionReturningValue(
                    "stringLength",
                    intType,
                    listOf(stringLengthCall),
                    ChirLocalValue(ChirSemanticId("expr:string:length"), intType, "expr_string_length"),
                ),
                functionReturningValue("integerMaxValue", intType, emptyList(), integerMaxValue),
            ),
        )
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(9, generatedClass.getMethod("maxValue").invoke(null))
        assertEquals(7, generatedClass.getMethod("stringLength").invoke(null))
        assertEquals(Int.MAX_VALUE, generatedClass.getMethod("integerMaxValue").invoke(null))
    }

    @Test
    fun `generates CHIR class struct and enum declarations as JVM classes`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val memberFunction = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:box:answer"),
            name = "answer",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:box:answer"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:box:answer:return"),
                        returnValue = ChirConstantValue(ChirSemanticId("const:box:answer"), intType, "42"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:box:answer"),
        )
        val box = DefaultChirClassDeclaration(
            semanticId = ChirSemanticId("class:Box"),
            name = "Box",
            memberDeclarations = listOf(
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("field:Box.value"),
                    name = "value",
                    type = intType,
                    mutable = true,
                ),
                memberFunction,
            ),
        )
        val pair = DefaultChirStructDeclaration(
            semanticId = ChirSemanticId("struct:Pair"),
            name = "Pair",
            fieldDeclarations = listOf(
                DefaultChirVariableDeclaration(ChirSemanticId("field:Pair.left"), "left", intType, mutable = false),
                DefaultChirVariableDeclaration(ChirSemanticId("field:Pair.right"), "right", intType, mutable = false),
            ),
        )
        val color = DefaultChirEnumDeclaration(
            semanticId = ChirSemanticId("enum:Color"),
            name = "Color",
            cases = listOf("RED", "GREEN"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWithDeclarations(box, pair, color))
        val artifactsByName = output.classes.associateBy { it.internalName }

        assertTrue("demo/pkg/Box" in artifactsByName.keys)
        assertTrue("demo/pkg/Pair" in artifactsByName.keys)
        assertTrue("demo/pkg/Color" in artifactsByName.keys)
        output.classes.forEach(::verifyClass)

        val loader = GeneratedClassLoader()
        val boxClass = loader.define(requireNotNull(artifactsByName["demo/pkg/Box"]))
        val pairClass = loader.define(requireNotNull(artifactsByName["demo/pkg/Pair"]))
        val colorClass = loader.define(requireNotNull(artifactsByName["demo/pkg/Color"]))

        val boxInstance = boxClass.getConstructor().newInstance()
        val valueField = boxClass.getField("value")
        valueField.setInt(boxInstance, 9)
        assertEquals(9, valueField.getInt(boxInstance))
        assertEquals(42, boxClass.getMethod("answer").invoke(boxInstance))
        assertNotNull(pairClass.getField("left"))
        assertNotNull(pairClass.getField("right"))
        val values = colorClass.getMethod("values").invoke(null) as Array<*>
        assertEquals(listOf("RED", "GREEN"), values.map { (it as Enum<*>).name })
        assertEquals("GREEN", (colorClass.getMethod("valueOf", String::class.java).invoke(null, "GREEN") as Enum<*>).name)
    }

    @Test
    fun `lowers CHIR object construction and instance field access`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val boxType = ChirResolvedTypeRef(ChirNamedType("demo.pkg.Box"))
        val boxLocal = ChirLocalValue(ChirSemanticId("expr:new-box"), boxType, "expr_new_box")
        val box = DefaultChirClassDeclaration(
            semanticId = ChirSemanticId("class:Box"),
            name = "Box",
            memberDeclarations = listOf(
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("field:Box.value"),
                    name = "value",
                    type = intType,
                    mutable = true,
                ),
            ),
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:boxField"),
            name = "boxField",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:boxField"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:new-box"),
                            operation = "jvm.new",
                            operands = emptyList(),
                            resultType = boxType,
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:set-value"),
                            operation = "jvm.putField",
                            operands = listOf(
                                boxLocal,
                                ChirConstantValue(ChirSemanticId("const:thirteen"), intType, "13"),
                            ),
                            attributes = jvmField("value"),
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:get-value"),
                            operation = "jvm.getField",
                            operands = listOf(boxLocal),
                            resultType = intType,
                            attributes = jvmField("value"),
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:boxField:return"),
                        returnValue = ChirLocalValue(ChirSemanticId("expr:get-value"), intType, "expr_get_value"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:boxField"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWithDeclarations(box, function))
        val artifactsByName = output.classes.associateBy { it.internalName }

        output.classes.forEach(::verifyClass)
        val loader = GeneratedClassLoader()
        loader.define(requireNotNull(artifactsByName["demo/pkg/Box"]))
        val facadeClass = loader.define(requireNotNull(artifactsByName["demo/pkg/DemoCj"]))
        assertEquals(13, facadeClass.getMethod("boxField").invoke(null))
    }

    @Test
    fun `lowers CHIR raw array allocation element access and length`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val arrayType = ChirResolvedTypeRef(ChirRawArrayType(intType))
        val arrayLocal = ChirLocalValue(ChirSemanticId("expr:new-array"), arrayType, "expr_new_array")
        val loadedLocal = ChirLocalValue(ChirSemanticId("expr:array-load"), intType, "expr_array_load")
        val lengthLocal = ChirLocalValue(ChirSemanticId("expr:array-length"), intType, "expr_array_length")
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:arrayRoundTrip"),
            name = "arrayRoundTrip",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:arrayRoundTrip"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:new-array"),
                            operation = "jvm.newArray",
                            operands = listOf(ChirConstantValue(ChirSemanticId("const:size"), intType, "3")),
                            resultType = arrayType,
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:array-store"),
                            operation = "jvm.arrayStore",
                            operands = listOf(
                                arrayLocal,
                                ChirConstantValue(ChirSemanticId("const:index"), intType, "1"),
                                ChirConstantValue(ChirSemanticId("const:stored"), intType, "77"),
                            ),
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:array-load"),
                            operation = "jvm.arrayLoad",
                            operands = listOf(
                                arrayLocal,
                                ChirConstantValue(ChirSemanticId("const:load-index"), intType, "1"),
                            ),
                            resultType = intType,
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:array-length"),
                            operation = "jvm.arrayLength",
                            operands = listOf(arrayLocal),
                            resultType = intType,
                        ),
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:sum-array-result"),
                            operator = "+",
                            left = loadedLocal,
                            right = lengthLocal,
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:arrayRoundTrip:return"),
                        returnValue = ChirLocalValue(ChirSemanticId("expr:sum-array-result"), intType, "expr_sum_array_result"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:arrayRoundTrip"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(function))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(80, generatedClass.getMethod("arrayRoundTrip").invoke(null))
    }

    @Test
    fun `lowers CHIR JVM type checks and throw terminator`() {
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
        val runtimeExceptionType = ChirResolvedTypeRef(ChirNamedType("java.lang.RuntimeException"))
        val exceptionLocal = ChirLocalValue(ChirSemanticId("expr:new-runtime-exception"), runtimeExceptionType, "expr_new_runtime_exception")
        val isThrowable = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:isThrowable"),
            name = "isThrowable",
            returnType = boolType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:isThrowable"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:new-runtime-exception"),
                            operation = "jvm.new",
                            operands = emptyList(),
                            resultType = runtimeExceptionType,
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:instanceof-throwable"),
                            operation = "jvm.instanceOf",
                            operands = listOf(exceptionLocal),
                            resultType = boolType,
                            attributes = jvmType("java/lang/Throwable"),
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:isThrowable:return"),
                        returnValue = ChirLocalValue(ChirSemanticId("expr:instanceof-throwable"), boolType, "expr_instanceof_throwable"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:isThrowable"),
        )
        val throwRuntimeException = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:throwRuntimeException"),
            name = "throwRuntimeException",
            returnType = unitType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:throwRuntimeException"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:new-runtime-exception"),
                            operation = "jvm.new",
                            operands = emptyList(),
                            resultType = runtimeExceptionType,
                        ),
                    ),
                    terminator = ChirThrowTerminator(
                        semanticId = ChirSemanticId("term:throwRuntimeException:throw"),
                        exceptionValue = exceptionLocal,
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:throwRuntimeException"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(isThrowable, throwRuntimeException))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(true, generatedClass.getMethod("isThrowable").invoke(null))
        val thrown = assertThrows(java.lang.reflect.InvocationTargetException::class.java) {
            generatedClass.getMethod("throwRuntimeException").invoke(null)
        }
        assertTrue(thrown.cause is RuntimeException)
    }

    @Test
    fun `generates Java main bridge and jar manifest`(@TempDir tempDir: Path) {
        val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
        val main = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:main"),
            name = "main",
            returnType = unitType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(ChirSemanticId("term:return")),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(main))
        val artifact = output.classes.single()

        assertNotNull(output.mainClassInternalName)
        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        generatedClass.getMethod("main", Array<String>::class.java).invoke(null, arrayOf<String>())

        val jar = output.writeJar(tempDir.resolve("main.jar"))
        JarFile(jar.toFile()).use { jarFile ->
            assertEquals(output.mainClassInternalName!!.replace('/', '.'), jarFile.manifest.mainAttributes.getValue("Main-Class"))
            assertTrue(jarFile.getEntry(artifact.relativePath) != null)
        }
    }

    private fun inputWith(vararg functions: DefaultChirFunctionDeclaration): ChirJvmCodegenInput {
        return ChirJvmCodegenInput(
            chirPackage = ChirPackage(
                semanticId = ChirSemanticId("pkg:demo"),
                name = "demo.pkg",
                modules = listOf(
                    ChirModule(
                        semanticId = ChirSemanticId("mod:demo"),
                        name = "demo",
                        declarations = functions.toList(),
                    ),
                ),
            ),
        )
    }

    private fun functionReturningValue(
        name: String,
        returnType: ChirResolvedTypeRef,
        expressions: List<org.cangnova.cangjie.chir.core.expression.ChirExpression>,
        returnValue: org.cangnova.cangjie.chir.core.value.ChirValue,
    ): DefaultChirFunctionDeclaration {
        return DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:$name"),
            name = name,
            returnType = returnType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:$name"),
                    name = "entry",
                    expressions = expressions,
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:$name:return"),
                        returnValue = returnValue,
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:$name"),
        )
    }

    private fun jvmImport(
        owner: String,
        name: String,
        invokeKind: String,
    ): Set<ChirStringAttribute> =
        setOf(
            ChirStringAttribute(JvmAbiAttributes.OWNER, owner),
            ChirStringAttribute(JvmAbiAttributes.NAME, name),
            ChirStringAttribute(JvmAbiAttributes.INVOKE_KIND, invokeKind),
        )

    private fun jvmField(name: String): Set<ChirStringAttribute> =
        setOf(ChirStringAttribute(JvmAbiAttributes.NAME, name))

    private fun jvmType(typeInternalName: String): Set<ChirStringAttribute> =
        setOf(ChirStringAttribute(JvmAbiAttributes.TYPE, typeInternalName))

    private fun inputWithDeclarations(vararg declarations: ChirDeclaration): ChirJvmCodegenInput {
        return ChirJvmCodegenInput(
            chirPackage = ChirPackage(
                semanticId = ChirSemanticId("pkg:demo"),
                name = "demo.pkg",
                modules = listOf(
                    ChirModule(
                        semanticId = ChirSemanticId("mod:demo"),
                        name = "demo",
                        declarations = declarations.toList(),
                    ),
                ),
            ),
        )
    }

    private fun verifyClass(artifact: JvmClassFileArtifact) {
        val output = StringWriter()
        CheckClassAdapter.verify(ClassReader(artifact.bytes), false, PrintWriter(output))
        assertTrue(output.toString().isBlank(), output.toString())
    }

    private class GeneratedClassLoader : ClassLoader(DefaultChirToJvmCodeGeneratorTest::class.java.classLoader) {
        fun define(artifact: JvmClassFileArtifact): Class<*> {
            return defineClass(artifact.internalName.replace('/', '.'), artifact.bytes, 0, artifact.bytes.size)
        }
    }
}
