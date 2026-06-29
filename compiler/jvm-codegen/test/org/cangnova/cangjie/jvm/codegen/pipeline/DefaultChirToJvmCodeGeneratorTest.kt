package org.cangnova.cangjie.jvm.codegen.pipeline

import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirThrowTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirUnwindTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirEnumDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirExtendDeclaration
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
import org.cangnova.cangjie.chir.core.model.ChirPackageMembers
import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.type.ChirCPointerType
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirNamedType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirRawArrayType
import org.cangnova.cangjie.chir.core.type.ChirRefType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirVArrayType
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirGlobalValue
import org.cangnova.cangjie.chir.core.value.ChirImportedFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirImportedVariableValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.chir.core.value.ChirParameterValue
import org.cangnova.cangjie.jvm.codegen.api.ChirJvmCodegenInput
import org.cangnova.cangjie.jvm.codegen.api.JvmClassFileArtifact
import org.cangnova.cangjie.jvm.codegen.api.writeJar
import org.cangnova.cangjie.jvm.codegen.context.JvmAbiAttributes
import org.cangnova.cangjie.jvm.codegen.diagnostics.JvmCodegenException
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
import java.lang.invoke.MethodHandle
import java.lang.reflect.Modifier
import java.nio.file.Path
import java.util.jar.JarFile

/**
 * `DefaultChirToJvmCodeGenerator` 的 JVM classfile 生成契约测试。
 *
 * 测试直接构造 CHIR 输入，验证生成的 class 能通过 ASM verifier、可被自定义 classloader 加载，
 * 并覆盖 ABI 属性、调用、内存、数组、异常、phi、类型声明、包 facade、main bridge 与 jar manifest。
 */
class DefaultChirToJvmCodeGeneratorTest {
    /**
     * 验证最小静态函数能生成可校验、可加载并可执行的 JVM class。
     */
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

    /**
     * 验证同一个 module facade 内的静态函数调用会解析到同一生成类上的目标方法。
     */
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

    /**
     * 验证本地函数与全局字段的显式 JVM ABI 名称会覆盖默认命名策略。
     */
    @Test
    fun `uses explicit JVM ABI names on local declarations`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val callee = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:abiNamedCallee"),
            name = "abiNamedCallee",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:abiNamedCallee"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:abiNamedCallee:return"),
                        returnValue = ChirConstantValue(ChirSemanticId("const:abiNamedCallee"), intType, "73"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:abiNamedCallee"),
            attributes = setOf(ChirStringAttribute(JvmAbiAttributes.NAME, "abi_named_callee")),
        )
        val caller = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:abiNamedCaller"),
            name = "abiNamedCaller",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:abiNamedCaller"),
                    name = "entry",
                    expressions = listOf(
                        ChirCallExpression(
                            semanticId = ChirSemanticId("expr:abi-named-call"),
                            callee = ChirFunctionValue(
                                semanticId = ChirSemanticId("value:abiNamedCallee"),
                                type = ChirResolvedTypeRef(ChirFunctionType(emptyList(), intType)),
                                name = "abiNamedCallee",
                            ),
                            arguments = emptyList(),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:abiNamedCaller:return"),
                        returnValue = ChirLocalValue(ChirSemanticId("expr:abi-named-call"), intType, "expr_abi_named_call"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:abiNamedCaller"),
        )
        val global = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("global:abiField"),
            name = "abiField",
            type = intType,
            mutable = true,
            attributes = setOf(ChirStringAttribute(JvmAbiAttributes.NAME, "abi_field")),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWithDeclarations(global, callee, caller))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(73, generatedClass.getMethod("abi_named_callee").invoke(null))
        assertEquals(73, generatedClass.getMethod("abiNamedCaller").invoke(null))
        generatedClass.getField("abi_field").setInt(null, 31)
        assertEquals(31, generatedClass.getField("abi_field").getInt(null))
    }

    /**
     * 验证跨模块显式 owner ABI 的本地静态 JVM 函数调用会指向正确 facade。
     */
    @Test
    fun `calls local static JVM functions through explicit owner ABI across modules`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val callee = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:crossModuleAnswer"),
            name = "answer",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:crossModuleAnswer"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:crossModuleAnswer:return"),
                        returnValue = ChirConstantValue(ChirSemanticId("const:crossModuleAnswer"), intType, "66"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:crossModuleAnswer"),
        )
        val caller = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:crossModuleCaller"),
            name = "crossModuleCaller",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:crossModuleCaller"),
                    name = "entry",
                    expressions = listOf(
                        ChirCallExpression(
                            semanticId = ChirSemanticId("expr:cross-module-call"),
                            callee = ChirFunctionValue(
                                semanticId = ChirSemanticId("value:crossModuleAnswer"),
                                type = ChirResolvedTypeRef(ChirFunctionType(emptyList(), intType)),
                                name = "answer",
                                attributes = setOf(ChirStringAttribute(JvmAbiAttributes.OWNER, "demo/pkg/AlphaCj")),
                            ),
                            arguments = emptyList(),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:crossModuleCaller:return"),
                        returnValue = ChirLocalValue(
                            ChirSemanticId("expr:cross-module-call"),
                            intType,
                            "expr_cross_module_call",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:crossModuleCaller"),
        )
        val input = ChirJvmCodegenInput(
            chirPackage = ChirPackage(
                semanticId = ChirSemanticId("pkg:crossModule"),
                name = "demo.pkg",
                modules = listOf(
                    ChirModule(
                        semanticId = ChirSemanticId("mod:alpha"),
                        name = "alpha",
                        declarations = listOf(callee),
                    ),
                    ChirModule(
                        semanticId = ChirSemanticId("mod:beta"),
                        name = "beta",
                        declarations = listOf(caller),
                    ),
                ),
            ),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(input)
        val artifactsByName = output.classes.associateBy { it.internalName }

        val loader = GeneratedClassLoader(artifactsByName)
        verifyClasses(output.classes, loader)
        loader.load(requireNotNull(artifactsByName["demo/pkg/AlphaCj"]))
        val betaClass = loader.load(requireNotNull(artifactsByName["demo/pkg/BetaCj"]))
        assertEquals(66, betaClass.getMethod("crossModuleCaller").invoke(null))
    }

    /**
     * 验证生成器在遇到重复 JVM class artifact 时报告错误，而不是静默覆盖。
     */
    @Test
    fun `rejects duplicate JVM class artifacts instead of silently dropping one`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val first = functionReturningValue(
            name = "firstCollisionValue",
            returnType = intType,
            expressions = emptyList(),
            returnValue = ChirConstantValue(ChirSemanticId("const:firstCollisionValue"), intType, "1"),
        )
        val second = functionReturningValue(
            name = "secondCollisionValue",
            returnType = intType,
            expressions = emptyList(),
            returnValue = ChirConstantValue(ChirSemanticId("const:secondCollisionValue"), intType, "2"),
        )
        val input = ChirJvmCodegenInput(
            chirPackage = ChirPackage(
                semanticId = ChirSemanticId("pkg:duplicateJvmArtifacts"),
                name = "demo.pkg",
                modules = listOf(
                    ChirModule(
                        semanticId = ChirSemanticId("mod:duplicateJvmArtifacts:lower"),
                        name = "demo",
                        declarations = listOf(first),
                    ),
                    ChirModule(
                        semanticId = ChirSemanticId("mod:duplicateJvmArtifacts:upper"),
                        name = "Demo",
                        declarations = listOf(second),
                    ),
                ),
            ),
        )

        val exception = assertThrows(JvmCodegenException::class.java) {
            DefaultChirToJvmCodeGenerator().generate(input)
        }
        assertTrue(requireNotNull(exception.message).contains("duplicate JVM class artifact 'demo/pkg/DemoCj'"))
    }

    /**
     * 验证 module facade 内重复 JVM 方法签名会被前置诊断。
     */
    @Test
    fun `rejects duplicate JVM method signatures in module facade`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val first = functionReturningValue(
            name = "firstMethodCollision",
            returnType = intType,
            expressions = emptyList(),
            returnValue = ChirConstantValue(ChirSemanticId("const:firstMethodCollision"), intType, "1"),
        ).copy(attributes = setOf(ChirStringAttribute(JvmAbiAttributes.NAME, "collision")))
        val second = functionReturningValue(
            name = "secondMethodCollision",
            returnType = intType,
            expressions = emptyList(),
            returnValue = ChirConstantValue(ChirSemanticId("const:secondMethodCollision"), intType, "2"),
        ).copy(attributes = setOf(ChirStringAttribute(JvmAbiAttributes.NAME, "collision")))

        val exception = assertThrows(JvmCodegenException::class.java) {
            DefaultChirToJvmCodeGenerator().generate(inputWith(first, second))
        }
        assertTrue(requireNotNull(exception.message).contains("duplicate JVM method 'collision()I' in class 'demo/pkg/DemoCj'"))
    }

    /**
     * 验证类型 classfile 内重复 JVM 字段签名会被前置诊断。
     */
    @Test
    fun `rejects duplicate JVM field signatures in generated type class`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val firstField = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("field:DuplicateFields.first"),
            name = "first",
            type = intType,
            mutable = true,
            attributes = setOf(ChirStringAttribute(JvmAbiAttributes.NAME, "slot")),
        )
        val secondField = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("field:DuplicateFields.second"),
            name = "second",
            type = intType,
            mutable = true,
            attributes = setOf(ChirStringAttribute(JvmAbiAttributes.NAME, "slot")),
        )
        val duplicateFields = DefaultChirStructDeclaration(
            semanticId = ChirSemanticId("struct:DuplicateFields"),
            name = "DuplicateFields",
            fieldDeclarations = listOf(firstField, secondField),
        )

        val exception = assertThrows(JvmCodegenException::class.java) {
            DefaultChirToJvmCodeGenerator().generate(inputWithDeclarations(duplicateFields))
        }
        assertTrue(requireNotNull(exception.message).contains("duplicate JVM field 'slotI' in class 'demo/pkg/DuplicateFields'"))
    }

    /**
     * 验证本地声明和调用可以使用显式 JVM descriptor 覆盖 CHIR 推导 descriptor。
     */
    @Test
    fun `uses explicit JVM ABI descriptors on local declarations and calls`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val stringType = ChirResolvedTypeRef(ChirNamedType("String"))
        val abiDescriptor = "(Ljava/lang/CharSequence;)I"
        val parameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:abiDescriptorCallee.value"),
            name = "value",
            type = stringType,
            mutable = false,
        )
        val parameterValue = ChirParameterValue(
            semanticId = parameter.semanticId,
            type = stringType,
            name = parameter.name,
            ownerFunctionId = ChirSemanticId("fn:abiDescriptorCallee"),
        )
        val callee = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:abiDescriptorCallee"),
            name = "abiDescriptorCallee",
            returnType = intType,
            parameters = listOf(parameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:abiDescriptorCallee"),
                    name = "entry",
                    expressions = listOf(
                        ChirCallExpression(
                            semanticId = ChirSemanticId("expr:abi-descriptor-param-length"),
                            callee = ChirImportedFunctionValue(
                                semanticId = ChirSemanticId("import:abiDescriptor:String.length"),
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
                            arguments = listOf(parameterValue),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:abiDescriptorCallee:return"),
                        returnValue = ChirLocalValue(
                            ChirSemanticId("expr:abi-descriptor-param-length"),
                            intType,
                            "expr_abi_descriptor_param_length",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:abiDescriptorCallee"),
            attributes = setOf(ChirStringAttribute(JvmAbiAttributes.DESCRIPTOR, abiDescriptor)),
        )
        val caller = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:abiDescriptorCaller"),
            name = "abiDescriptorCaller",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:abiDescriptorCaller"),
                    name = "entry",
                    expressions = listOf(
                        ChirCallExpression(
                            semanticId = ChirSemanticId("expr:abi-descriptor-call"),
                            callee = ChirFunctionValue(
                                semanticId = ChirSemanticId("value:abiDescriptorCallee"),
                                type = ChirResolvedTypeRef(ChirFunctionType(listOf(stringType), intType)),
                                name = "abiDescriptorCallee",
                                attributes = setOf(ChirStringAttribute(JvmAbiAttributes.DESCRIPTOR, abiDescriptor)),
                            ),
                            arguments = listOf(ChirConstantValue(ChirSemanticId("const:abi-descriptor-arg"), stringType, "text")),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:abiDescriptorCaller:return"),
                        returnValue = ChirLocalValue(
                            ChirSemanticId("expr:abi-descriptor-call"),
                            intType,
                            "expr_abi_descriptor_call",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:abiDescriptorCaller"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(callee, caller))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(4, generatedClass.getMethod("abiDescriptorCallee", CharSequence::class.java).invoke(null, "text"))
        assertEquals(4, generatedClass.getMethod("abiDescriptorCaller").invoke(null))
    }

    /**
     * 验证本地调用实参会按显式 JVM ABI descriptor 的 carrier 类型进行适配。
     */
    @Test
    fun `adapts explicit JVM ABI descriptor argument carrier on local calls`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val charSequenceType = ChirResolvedTypeRef(ChirNamedType("java.lang.CharSequence"))
        val abiDescriptor = "(Ljava/lang/String;)I"
        val calleeParameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:abiArgumentDescriptorCallee.value"),
            name = "value",
            type = charSequenceType,
            mutable = false,
        )
        val callee = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:abiArgumentDescriptorCallee"),
            name = "abiArgumentDescriptorCallee",
            returnType = intType,
            parameters = listOf(calleeParameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:abiArgumentDescriptorCallee"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:abiArgumentDescriptorCallee:return"),
                        returnValue = ChirConstantValue(
                            ChirSemanticId("const:abi-argument-descriptor-result"),
                            intType,
                            "17",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:abiArgumentDescriptorCallee"),
            attributes = setOf(ChirStringAttribute(JvmAbiAttributes.DESCRIPTOR, abiDescriptor)),
        )
        val callerParameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:abiArgumentDescriptorCaller.value"),
            name = "value",
            type = charSequenceType,
            mutable = false,
        )
        val callerParameterValue = ChirParameterValue(
            semanticId = callerParameter.semanticId,
            type = charSequenceType,
            name = callerParameter.name,
            ownerFunctionId = ChirSemanticId("fn:abiArgumentDescriptorCaller"),
        )
        val caller = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:abiArgumentDescriptorCaller"),
            name = "abiArgumentDescriptorCaller",
            returnType = intType,
            parameters = listOf(callerParameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:abiArgumentDescriptorCaller"),
                    name = "entry",
                    expressions = listOf(
                        ChirCallExpression(
                            semanticId = ChirSemanticId("expr:abi-argument-descriptor-call"),
                            callee = ChirFunctionValue(
                                semanticId = ChirSemanticId("value:abiArgumentDescriptorCallee"),
                                type = ChirResolvedTypeRef(ChirFunctionType(listOf(charSequenceType), intType)),
                                name = "abiArgumentDescriptorCallee",
                                attributes = setOf(ChirStringAttribute(JvmAbiAttributes.DESCRIPTOR, abiDescriptor)),
                            ),
                            arguments = listOf(callerParameterValue),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:abiArgumentDescriptorCaller:return"),
                        returnValue = ChirLocalValue(
                            ChirSemanticId("expr:abi-argument-descriptor-call"),
                            intType,
                            "expr_abi_argument_descriptor_call",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:abiArgumentDescriptorCaller"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(callee, caller))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(17, generatedClass.getMethod("abiArgumentDescriptorCallee", String::class.java).invoke(null, "text"))
        assertEquals(17, generatedClass.getMethod("abiArgumentDescriptorCaller", CharSequence::class.java).invoke(null, "text"))
    }

    /**
     * 验证显式 JVM ABI descriptor 的返回 carrier 会适配回 CHIR 结果类型。
     */
    @Test
    fun `adapts explicit JVM ABI descriptor return carrier to CHIR result type`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val stringType = ChirResolvedTypeRef(ChirNamedType("String"))
        val abiDescriptor = "()Ljava/lang/CharSequence;"
        val callValue = ChirLocalValue(
            semanticId = ChirSemanticId("expr:abi-return-descriptor-call"),
            type = stringType,
            name = "expr_abi_return_descriptor_call",
        )
        val lengthValue = ChirLocalValue(
            semanticId = ChirSemanticId("expr:abi-return-descriptor-length"),
            type = intType,
            name = "expr_abi_return_descriptor_length",
        )
        val callee = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:abiReturnDescriptorCallee"),
            name = "abiReturnDescriptorCallee",
            returnType = stringType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:abiReturnDescriptorCallee"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:abiReturnDescriptorCallee:return"),
                        returnValue = ChirConstantValue(
                            ChirSemanticId("const:abi-return-descriptor-value"),
                            stringType,
                            "cangjie",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:abiReturnDescriptorCallee"),
            attributes = setOf(ChirStringAttribute(JvmAbiAttributes.DESCRIPTOR, abiDescriptor)),
        )
        val caller = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:abiReturnDescriptorCaller"),
            name = "abiReturnDescriptorCaller",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:abiReturnDescriptorCaller"),
                    name = "entry",
                    expressions = listOf(
                        ChirCallExpression(
                            semanticId = ChirSemanticId("expr:abi-return-descriptor-call"),
                            callee = ChirFunctionValue(
                                semanticId = ChirSemanticId("value:abiReturnDescriptorCallee"),
                                type = ChirResolvedTypeRef(ChirFunctionType(emptyList(), stringType)),
                                name = "abiReturnDescriptorCallee",
                                attributes = setOf(ChirStringAttribute(JvmAbiAttributes.DESCRIPTOR, abiDescriptor)),
                            ),
                            arguments = emptyList(),
                            resultType = stringType,
                        ),
                        ChirCallExpression(
                            semanticId = ChirSemanticId("expr:abi-return-descriptor-length"),
                            callee = ChirImportedFunctionValue(
                                semanticId = ChirSemanticId("import:abiReturnDescriptor:String.length"),
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
                            arguments = listOf(callValue),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:abiReturnDescriptorCaller:return"),
                        returnValue = lengthValue,
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:abiReturnDescriptorCaller"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(callee, caller))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals("cangjie", generatedClass.getMethod("abiReturnDescriptorCallee").invoke(null))
        assertEquals(7, generatedClass.getMethod("abiReturnDescriptorCaller").invoke(null))
    }

    /**
     * 验证函数返回值会按显式 JVM descriptor 的返回 carrier 适配。
     */
    @Test
    fun `adapts explicit JVM ABI descriptor carrier on function returns`() {
        val charSequenceType = ChirResolvedTypeRef(ChirNamedType("java.lang.CharSequence"))
        val abiDescriptor = "(Ljava/lang/CharSequence;)Ljava/lang/String;"
        val parameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:abiReturnValueDescriptor.value"),
            name = "value",
            type = charSequenceType,
            mutable = false,
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:abiReturnValueDescriptor"),
            name = "abiReturnValueDescriptor",
            returnType = charSequenceType,
            parameters = listOf(parameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:abiReturnValueDescriptor"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:abiReturnValueDescriptor:return"),
                        returnValue = ChirParameterValue(
                            semanticId = parameter.semanticId,
                            type = charSequenceType,
                            name = parameter.name,
                            ownerFunctionId = ChirSemanticId("fn:abiReturnValueDescriptor"),
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:abiReturnValueDescriptor"),
            attributes = setOf(ChirStringAttribute(JvmAbiAttributes.DESCRIPTOR, abiDescriptor)),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(function))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals("text", generatedClass.getMethod("abiReturnValueDescriptor", CharSequence::class.java).invoke(null, "text"))
    }

    /**
     * 验证静态字段读写会按显式 JVM descriptor 的字段 carrier 适配。
     */
    @Test
    fun `adapts explicit JVM ABI descriptor carrier on static fields`() {
        val charSequenceType = ChirResolvedTypeRef(ChirNamedType("java.lang.CharSequence"))
        val stringDescriptor = "Ljava/lang/String;"
        val global = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("global:abiStaticField.slot"),
            name = "slot",
            type = charSequenceType,
            mutable = true,
            attributes = jvmField("slot", descriptor = stringDescriptor),
        )
        val parameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:abiStaticFieldRoundTrip.value"),
            name = "value",
            type = charSequenceType,
            mutable = false,
        )
        val parameterValue = ChirParameterValue(
            semanticId = parameter.semanticId,
            type = charSequenceType,
            name = parameter.name,
            ownerFunctionId = ChirSemanticId("fn:abiStaticFieldRoundTrip"),
        )
        val fieldValue = ChirLocalValue(
            semanticId = ChirSemanticId("expr:abi-static-field-get"),
            type = charSequenceType,
            name = "expr_abi_static_field_get",
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:abiStaticFieldRoundTrip"),
            name = "abiStaticFieldRoundTrip",
            returnType = charSequenceType,
            parameters = listOf(parameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:abiStaticFieldRoundTrip"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:abi-static-field-put"),
                            operation = "jvm.putStatic",
                            operands = listOf(parameterValue),
                            attributes = setOf(
                                ChirStringAttribute(JvmAbiAttributes.OWNER, "demo/pkg/DemoCj"),
                                ChirStringAttribute(JvmAbiAttributes.NAME, "slot"),
                                ChirStringAttribute(JvmAbiAttributes.DESCRIPTOR, stringDescriptor),
                            ),
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:abi-static-field-get"),
                            operation = "jvm.getStatic",
                            operands = emptyList(),
                            resultType = charSequenceType,
                            attributes = setOf(
                                ChirStringAttribute(JvmAbiAttributes.OWNER, "demo/pkg/DemoCj"),
                                ChirStringAttribute(JvmAbiAttributes.NAME, "slot"),
                                ChirStringAttribute(JvmAbiAttributes.DESCRIPTOR, stringDescriptor),
                            ),
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:abiStaticFieldRoundTrip:return"),
                        returnValue = fieldValue,
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:abiStaticFieldRoundTrip"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWithDeclarations(global, function))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(String::class.java, generatedClass.getField("slot").type)
        assertEquals("text", generatedClass.getMethod("abiStaticFieldRoundTrip", CharSequence::class.java).invoke(null, "text"))
        assertEquals("text", generatedClass.getField("slot").get(null))
    }

    /**
     * 验证显式 JVM ABI descriptor 下 primitive carrier 的装箱与拆箱路径。
     */
    @Test
    fun `boxes and unboxes primitive carriers for explicit JVM ABI descriptors`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val objectDescriptor = "Ljava/lang/Object;"
        val primitiveReturnDescriptor = "(I)$objectDescriptor"
        val objectArgumentDescriptor = "($objectDescriptor)I"
        val identityParameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:primitiveReturnAsObject.value"),
            name = "value",
            type = intType,
            mutable = false,
        )
        val primitiveReturnAsObject = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:primitiveReturnAsObject"),
            name = "primitiveReturnAsObject",
            returnType = intType,
            parameters = listOf(identityParameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:primitiveReturnAsObject"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:primitiveReturnAsObject:return"),
                        returnValue = ChirParameterValue(
                            semanticId = identityParameter.semanticId,
                            type = intType,
                            name = identityParameter.name,
                            ownerFunctionId = ChirSemanticId("fn:primitiveReturnAsObject"),
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:primitiveReturnAsObject"),
            attributes = setOf(ChirStringAttribute(JvmAbiAttributes.DESCRIPTOR, primitiveReturnDescriptor)),
        )
        val objectArgumentParameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:objectArgumentCallee.value"),
            name = "value",
            type = intType,
            mutable = false,
        )
        val objectArgumentParameterValue = ChirParameterValue(
            semanticId = objectArgumentParameter.semanticId,
            type = intType,
            name = objectArgumentParameter.name,
            ownerFunctionId = ChirSemanticId("fn:objectArgumentCallee"),
        )
        val objectArgumentCallee = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:objectArgumentCallee"),
            name = "objectArgumentCallee",
            returnType = intType,
            parameters = listOf(objectArgumentParameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:objectArgumentCallee"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:objectArgumentCallee:return"),
                        returnValue = objectArgumentParameterValue,
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:objectArgumentCallee"),
            attributes = setOf(ChirStringAttribute(JvmAbiAttributes.DESCRIPTOR, objectArgumentDescriptor)),
        )
        val unboxedCall = ChirLocalValue(
            semanticId = ChirSemanticId("expr:primitive-return-as-object-call"),
            type = intType,
            name = "expr_primitive_return_as_object_call",
        )
        val boxedArgumentCall = ChirLocalValue(
            semanticId = ChirSemanticId("expr:object-argument-call"),
            type = intType,
            name = "expr_object_argument_call",
        )
        val caller = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:primitiveCarrierBridge"),
            name = "primitiveCarrierBridge",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:primitiveCarrierBridge"),
                    name = "entry",
                    expressions = listOf(
                        ChirCallExpression(
                            semanticId = ChirSemanticId("expr:primitive-return-as-object-call"),
                            callee = ChirFunctionValue(
                                semanticId = ChirSemanticId("value:primitiveReturnAsObject"),
                                type = ChirResolvedTypeRef(ChirFunctionType(listOf(intType), intType)),
                                name = "primitiveReturnAsObject",
                                attributes = setOf(ChirStringAttribute(JvmAbiAttributes.DESCRIPTOR, primitiveReturnDescriptor)),
                            ),
                            arguments = listOf(ChirConstantValue(ChirSemanticId("const:primitive-carrier-arg"), intType, "42")),
                            resultType = intType,
                        ),
                        ChirCallExpression(
                            semanticId = ChirSemanticId("expr:object-argument-call"),
                            callee = ChirFunctionValue(
                                semanticId = ChirSemanticId("value:objectArgumentCallee"),
                                type = ChirResolvedTypeRef(ChirFunctionType(listOf(intType), intType)),
                                name = "objectArgumentCallee",
                                attributes = setOf(ChirStringAttribute(JvmAbiAttributes.DESCRIPTOR, objectArgumentDescriptor)),
                            ),
                            arguments = listOf(unboxedCall),
                            resultType = intType,
                        ),
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:primitive-carrier-sum"),
                            operator = "+",
                            left = unboxedCall,
                            right = boxedArgumentCall,
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:primitiveCarrierBridge:return"),
                        returnValue = ChirLocalValue(
                            ChirSemanticId("expr:primitive-carrier-sum"),
                            intType,
                            "expr_primitive_carrier_sum",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:primitiveCarrierBridge"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(primitiveReturnAsObject, objectArgumentCallee, caller))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(42, generatedClass.getMethod("primitiveReturnAsObject", Int::class.javaPrimitiveType).invoke(null, 42))
        assertEquals(58, generatedClass.getMethod("objectArgumentCallee", Any::class.java).invoke(null, 58))
        assertEquals(84, generatedClass.getMethod("primitiveCarrierBridge").invoke(null))
    }

    /**
     * 验证 CHIR ref memory alloca/load/store 会降低为 JVM local slot 读写。
     */
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

    /**
     * 验证 CHIR 无符号整数除法、取余和比较会降低为 JVM/运行时支持的 intrinsic。
     */
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

    /**
     * 验证 CHIR 浮点比较遵循 JVM NaN 有序比较语义。
     */
    @Test
    fun `lowers CHIR floating comparisons with JVM NaN ordered semantics`() {
        data class FloatingOperandCase(
            val namePrefix: String,
            val type: ChirResolvedTypeRef,
            val nanLiteral: String,
            val oneLiteral: String,
        )

        data class FloatingComparisonCase(
            val operator: String,
            val suffix: String,
            val expected: Boolean,
        )

        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val operandCases = listOf(
            FloatingOperandCase("float", ChirResolvedTypeRef(ChirPrimitiveType.FLOAT32), "NaN", "1.0"),
            FloatingOperandCase("double", ChirResolvedTypeRef(ChirPrimitiveType.FLOAT64), "NaN", "1.0"),
        )
        val comparisonCases = listOf(
            FloatingComparisonCase("feq", "FEQ", false),
            FloatingComparisonCase("fne", "FNE", true),
            FloatingComparisonCase("flt", "FLT", false),
            FloatingComparisonCase("fle", "FLE", false),
            FloatingComparisonCase("fgt", "FGT", false),
            FloatingComparisonCase("fge", "FGE", false),
            FloatingComparisonCase("==", "EQ", false),
            FloatingComparisonCase("!=", "NE", true),
            FloatingComparisonCase("<", "LT", false),
            FloatingComparisonCase("<=", "LE", false),
            FloatingComparisonCase(">", "GT", false),
            FloatingComparisonCase(">=", "GE", false),
        )
        val expectedByFunctionName = linkedMapOf<String, Boolean>()
        val functions = operandCases.flatMap { operandCase ->
            comparisonCases.map { comparisonCase ->
                val functionName = "${operandCase.namePrefix}${comparisonCase.suffix}Nan"
                expectedByFunctionName[functionName] = comparisonCase.expected
                functionReturningValue(
                    name = functionName,
                    returnType = boolType,
                    expressions = listOf(
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:$functionName"),
                            operator = comparisonCase.operator,
                            left = ChirConstantValue(ChirSemanticId("const:$functionName:nan"), operandCase.type, operandCase.nanLiteral),
                            right = ChirConstantValue(ChirSemanticId("const:$functionName:one"), operandCase.type, operandCase.oneLiteral),
                            resultType = boolType,
                        ),
                    ),
                    returnValue = ChirLocalValue(ChirSemanticId("expr:$functionName"), boolType, "expr_$functionName"),
                )
            }
        }

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(*functions.toTypedArray()))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        expectedByFunctionName.forEach { (functionName, expected) ->
            assertEquals(expected, generatedClass.getMethod(functionName).invoke(null), functionName)
        }
    }

    /**
     * 验证无符号整数最大值字面量会按 JVM carrier 正确物化。
     */
    @Test
    fun `materializes CHIR unsigned integer max literals as JVM carrier values`() {
        val uint32Type = ChirResolvedTypeRef(ChirPrimitiveType.UINT32)
        val uint64Type = ChirResolvedTypeRef(ChirPrimitiveType.UINT64)
        val uint32Max = functionReturningValue(
            name = "uint32Max",
            returnType = uint32Type,
            expressions = emptyList(),
            returnValue = ChirConstantValue(ChirSemanticId("const:uint32:max:literal"), uint32Type, "4294967295"),
        )
        val uint64Max = functionReturningValue(
            name = "uint64Max",
            returnType = uint64Type,
            expressions = emptyList(),
            returnValue = ChirConstantValue(ChirSemanticId("const:uint64:max:literal"), uint64Type, "18446744073709551615"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(uint32Max, uint64Max))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(-1, generatedClass.getMethod("uint32Max").invoke(null))
        assertEquals(-1L, generatedClass.getMethod("uint64Max").invoke(null))
    }

    /**
     * 验证 CHIR 无符号数值转换中的零扩展语义。
     */
    @Test
    fun `lowers CHIR unsigned numeric casts with zero extension`() {
        val uint8Type = ChirResolvedTypeRef(ChirPrimitiveType.UINT8)
        val uint32Type = ChirResolvedTypeRef(ChirPrimitiveType.UINT32)
        val uint64Type = ChirResolvedTypeRef(ChirPrimitiveType.UINT64)
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val longType = ChirResolvedTypeRef(ChirPrimitiveType.INT64)
        val doubleType = ChirResolvedTypeRef(ChirPrimitiveType.FLOAT64)
        val zextByte = functionReturningValue(
            name = "zextByte",
            returnType = intType,
            expressions = listOf(
                ChirOtherExpression(
                    semanticId = ChirSemanticId("expr:zext-byte"),
                    operation = "zext",
                    operands = listOf(ChirConstantValue(ChirSemanticId("const:uint8:max"), uint8Type, "255")),
                    resultType = intType,
                ),
            ),
            returnValue = ChirLocalValue(ChirSemanticId("expr:zext-byte"), intType, "expr_zext_byte"),
        )
        val zextInt = functionReturningValue(
            name = "zextInt",
            returnType = longType,
            expressions = listOf(
                ChirOtherExpression(
                    semanticId = ChirSemanticId("expr:zext-int"),
                    operation = "zext",
                    operands = listOf(ChirConstantValue(ChirSemanticId("const:uint32:max"), uint32Type, "-1")),
                    resultType = longType,
                ),
            ),
            returnValue = ChirLocalValue(ChirSemanticId("expr:zext-int"), longType, "expr_zext_int"),
        )
        val uintToDouble = functionReturningValue(
            name = "uintToDouble",
            returnType = doubleType,
            expressions = listOf(
                ChirOtherExpression(
                    semanticId = ChirSemanticId("expr:uint-to-double"),
                    operation = "uitofp",
                    operands = listOf(ChirConstantValue(ChirSemanticId("const:uint32:max:double"), uint32Type, "-1")),
                    resultType = doubleType,
                ),
            ),
            returnValue = ChirLocalValue(ChirSemanticId("expr:uint-to-double"), doubleType, "expr_uint_to_double"),
        )
        val ulongToDouble = functionReturningValue(
            name = "ulongToDouble",
            returnType = doubleType,
            expressions = listOf(
                ChirOtherExpression(
                    semanticId = ChirSemanticId("expr:ulong-to-double"),
                    operation = "uitofp",
                    operands = listOf(ChirConstantValue(ChirSemanticId("const:uint64:max:double"), uint64Type, "-1")),
                    resultType = doubleType,
                ),
            ),
            returnValue = ChirLocalValue(ChirSemanticId("expr:ulong-to-double"), doubleType, "expr_ulong_to_double"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(zextByte, zextInt, uintToDouble, ulongToDouble))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(255, generatedClass.getMethod("zextByte").invoke(null))
        assertEquals(4_294_967_295L, generatedClass.getMethod("zextInt").invoke(null))
        assertEquals(4_294_967_295.0, generatedClass.getMethod("uintToDouble").invoke(null) as Double, 0.0)
        assertEquals(18_446_744_073_709_551_615.0, generatedClass.getMethod("ulongToDouble").invoke(null) as Double, 0.0)
    }

    /**
     * 验证浮点到无符号整数转换会通过 JVM unsigned runtime 辅助方法降低。
     */
    @Test
    fun `lowers CHIR floating point to unsigned integer casts through JVM runtime`() {
        val uint8Type = ChirResolvedTypeRef(ChirPrimitiveType.UINT8)
        val uint32Type = ChirResolvedTypeRef(ChirPrimitiveType.UINT32)
        val uint64Type = ChirResolvedTypeRef(ChirPrimitiveType.UINT64)
        val doubleType = ChirResolvedTypeRef(ChirPrimitiveType.FLOAT64)
        val byteFromDouble = functionReturningValue(
            name = "byteFromDouble",
            returnType = uint8Type,
            expressions = listOf(
                ChirOtherExpression(
                    semanticId = ChirSemanticId("expr:fptoui-byte"),
                    operation = "fptoui",
                    operands = listOf(ChirConstantValue(ChirSemanticId("const:double:256"), doubleType, "256.0")),
                    resultType = uint8Type,
                ),
            ),
            returnValue = ChirLocalValue(ChirSemanticId("expr:fptoui-byte"), uint8Type, "expr_fptoui_byte"),
        )
        val intFromDouble = functionReturningValue(
            name = "intFromDouble",
            returnType = uint32Type,
            expressions = listOf(
                ChirOtherExpression(
                    semanticId = ChirSemanticId("expr:fptoui-int"),
                    operation = "fptoui",
                    operands = listOf(ChirConstantValue(ChirSemanticId("const:double:uint32-max"), doubleType, "4294967295.0")),
                    resultType = uint32Type,
                ),
            ),
            returnValue = ChirLocalValue(ChirSemanticId("expr:fptoui-int"), uint32Type, "expr_fptoui_int"),
        )
        val longFromDouble = functionReturningValue(
            name = "longFromDouble",
            returnType = uint64Type,
            expressions = listOf(
                ChirOtherExpression(
                    semanticId = ChirSemanticId("expr:fptoui-long"),
                    operation = "fptoui",
                    operands = listOf(ChirConstantValue(ChirSemanticId("const:double:uint64-high-bit"), doubleType, "9223372036854775808.0")),
                    resultType = uint64Type,
                ),
            ),
            returnValue = ChirLocalValue(ChirSemanticId("expr:fptoui-long"), uint64Type, "expr_fptoui_long"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(byteFromDouble, intFromDouble, longFromDouble))
        val artifactsByName = output.classes.associateBy { it.internalName }

        assertTrue("org/cangnova/cangjie/jvm/runtime/CjJvmUnsignedRuntime" in artifactsByName.keys)
        val loader = GeneratedClassLoader(artifactsByName)
        verifyClasses(output.classes, loader)
        val generatedClass = loader.load(requireNotNull(artifactsByName["demo/pkg/DemoCj"]))
        assertEquals(0.toByte(), generatedClass.getMethod("byteFromDouble").invoke(null))
        assertEquals(-1, generatedClass.getMethod("intFromDouble").invoke(null))
        assertEquals(Long.MIN_VALUE, generatedClass.getMethod("longFromDouble").invoke(null))
    }

    /**
     * 验证一元 bitnot 与 select 表达式的 JVM 降低结果。
     */
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

    /**
     * 验证导入 JVM 函数和静态字段会按显式 ABI 属性生成访问字节码。
     */
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

    /**
     * 验证显式 ABI 属性描述的导入 JVM 构造器调用会生成 new/invokespecial 序列。
     */
    @Test
    fun `lowers imported JVM constructor calls from explicit ABI attributes`() {
        val stringType = ChirResolvedTypeRef(ChirNamedType("String"))
        val runtimeExceptionType = ChirResolvedTypeRef(ChirNamedType("java.lang.RuntimeException"))
        val constructorCall = ChirCallExpression(
            semanticId = ChirSemanticId("expr:runtime-exception:init"),
            callee = ChirImportedFunctionValue(
                semanticId = ChirSemanticId("import:java.lang.RuntimeException.init"),
                type = ChirResolvedTypeRef(ChirFunctionType(listOf(stringType), runtimeExceptionType)),
                name = "RuntimeException.<init>",
                attributes = jvmImport("java/lang/RuntimeException", "<init>", "constructor"),
            ),
            arguments = listOf(ChirConstantValue(ChirSemanticId("const:constructor-message"), stringType, "constructed")),
            resultType = runtimeExceptionType,
        )
        val function = functionReturningValue(
            name = "newRuntimeException",
            returnType = runtimeExceptionType,
            expressions = listOf(constructorCall),
            returnValue = ChirLocalValue(
                ChirSemanticId("expr:runtime-exception:init"),
                runtimeExceptionType,
                "expr_runtime_exception_init",
            ),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(function))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        val exception = generatedClass.getMethod("newRuntimeException").invoke(null) as RuntimeException
        assertEquals("constructed", exception.message)
    }

    /**
     * 验证 CHIR 函数值会物化为 MethodHandle 并支持动态调用。
     */
    @Test
    fun `materializes CHIR function values as JVM method handles and invokes them dynamically`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val functionType = ChirResolvedTypeRef(ChirFunctionType(emptyList(), intType))
        val answerA = functionReturningValue(
            "answerA",
            intType,
            emptyList(),
            ChirConstantValue(ChirSemanticId("const:answerA"), intType, "11"),
        )
        val answerB = functionReturningValue(
            "answerB",
            intType,
            emptyList(),
            ChirConstantValue(ChirSemanticId("const:answerB"), intType, "29"),
        )
        val selectHandle = ChirLocalValue(ChirSemanticId("expr:select-function"), functionType, "expr_select_function")
        val dynamicCall = ChirLocalValue(ChirSemanticId("expr:dynamic-call"), intType, "expr_dynamic_call")
        val caller = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:callSelected"),
            name = "callSelected",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:callSelected"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:select-function"),
                            operation = "select",
                            operands = listOf(
                                ChirConstantValue(ChirSemanticId("const:false"), boolType, "false"),
                                ChirFunctionValue(ChirSemanticId("value:answerA"), functionType, "answerA"),
                                ChirFunctionValue(ChirSemanticId("value:answerB"), functionType, "answerB"),
                            ),
                            resultType = functionType,
                        ),
                        ChirCallExpression(
                            semanticId = ChirSemanticId("expr:dynamic-call"),
                            callee = selectHandle,
                            arguments = emptyList(),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:callSelected:return"),
                        returnValue = dynamicCall,
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:callSelected"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(answerA, answerB, caller))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(29, generatedClass.getMethod("callSelected").invoke(null))
    }

    /**
     * 验证导入 JVM 函数值会按 ABI 属性物化为 MethodHandle。
     */
    @Test
    fun `materializes imported JVM function values as method handles`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val functionType = ChirResolvedTypeRef(ChirFunctionType(listOf(intType, intType), intType))
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:maxHandle"),
            name = "maxHandle",
            returnType = functionType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:maxHandle"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:maxHandle:return"),
                        returnValue = ChirImportedFunctionValue(
                            semanticId = ChirSemanticId("import:java.lang.Math.max.handle"),
                            type = functionType,
                            name = "Math.max",
                            attributes = jvmImport("java/lang/Math", "max", "static"),
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:maxHandle"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(function))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        val handle = generatedClass.getMethod("maxHandle").invoke(null) as MethodHandle
        assertEquals(17, handle.invokeWithArguments(3, 17))
    }

    /**
     * 验证导入 JVM 构造器函数值会物化为构造器 MethodHandle。
     */
    @Test
    fun `materializes imported JVM constructor values as method handles`() {
        val stringType = ChirResolvedTypeRef(ChirNamedType("String"))
        val runtimeExceptionType = ChirResolvedTypeRef(ChirNamedType("java.lang.RuntimeException"))
        val functionType = ChirResolvedTypeRef(ChirFunctionType(listOf(stringType), runtimeExceptionType))
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:runtimeExceptionConstructorHandle"),
            name = "runtimeExceptionConstructorHandle",
            returnType = functionType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:runtimeExceptionConstructorHandle"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:runtimeExceptionConstructorHandle:return"),
                        returnValue = ChirImportedFunctionValue(
                            semanticId = ChirSemanticId("import:java.lang.RuntimeException.init.handle"),
                            type = functionType,
                            name = "RuntimeException.<init>",
                            attributes = jvmImport("java/lang/RuntimeException", "<init>", "constructor"),
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:runtimeExceptionConstructorHandle"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(function))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        val handle = generatedClass.getMethod("runtimeExceptionConstructorHandle").invoke(null) as MethodHandle
        val exception = handle.invokeWithArguments("from-handle") as RuntimeException
        assertEquals("from-handle", exception.message)
    }

    /**
     * 验证带 receiver 的 CHIR 函数值会降低为 JVM 实例方法调用。
     */
    @Test
    fun `invokes CHIR receiver function values as JVM instance methods`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val boxType = ChirResolvedTypeRef(ChirNamedType("demo.pkg.Box"))
        val boxLocal = ChirLocalValue(ChirSemanticId("expr:new-receiver-box"), boxType, "expr_new_receiver_box")
        val answer = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:Box.answer"),
            name = "answer",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:Box.answer"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:Box.answer:return"),
                        returnValue = ChirConstantValue(ChirSemanticId("const:Box.answer"), intType, "42"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:Box.answer"),
        )
        val box = DefaultChirClassDeclaration(
            semanticId = ChirSemanticId("class:ReceiverBox"),
            name = "Box",
            memberDeclarations = listOf(answer),
        )
        val caller = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:callReceiver"),
            name = "callReceiver",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:callReceiver"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:new-receiver-box"),
                            operation = "jvm.new",
                            operands = emptyList(),
                            resultType = boxType,
                        ),
                        ChirCallExpression(
                            semanticId = ChirSemanticId("expr:receiver-call"),
                            callee = ChirFunctionValue(
                                semanticId = ChirSemanticId("value:Box.answer"),
                                type = ChirResolvedTypeRef(
                                    ChirFunctionType(
                                        parameterTypes = emptyList(),
                                        returnType = intType,
                                        receiverType = boxType,
                                    ),
                                ),
                                name = "answer",
                            ),
                            arguments = listOf(boxLocal),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:callReceiver:return"),
                        returnValue = ChirLocalValue(ChirSemanticId("expr:receiver-call"), intType, "expr_receiver_call"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:callReceiver"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWithDeclarations(box, caller))
        val artifactsByName = output.classes.associateBy { it.internalName }

        output.classes.forEach(::verifyClass)
        val loader = GeneratedClassLoader()
        loader.define(requireNotNull(artifactsByName["demo/pkg/Box"]))
        val facadeClass = loader.define(requireNotNull(artifactsByName["demo/pkg/DemoCj"]))
        assertEquals(42, facadeClass.getMethod("callReceiver").invoke(null))
    }

    /**
     * 验证带 receiver 的 CHIR 函数值可物化为实例 MethodHandle 并动态调用。
     */
    @Test
    fun `materializes CHIR receiver function values as JVM method handles and invokes them dynamically`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val boxType = ChirResolvedTypeRef(ChirNamedType("demo.pkg.Box"))
        val receiverFunctionType = ChirResolvedTypeRef(
            ChirFunctionType(
                parameterTypes = emptyList(),
                returnType = intType,
                receiverType = boxType,
            ),
        )
        val boxLocal = ChirLocalValue(ChirSemanticId("expr:receiver-handle-box"), boxType, "expr_receiver_handle_box")
        val handleLocal = ChirLocalValue(ChirSemanticId("expr:receiver-handle-select"), receiverFunctionType, "expr_receiver_handle_select")
        val dynamicCall = ChirLocalValue(ChirSemanticId("expr:receiver-handle-call"), intType, "expr_receiver_handle_call")
        val answer = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:Box.answerHandleTarget"),
            name = "answerHandleTarget",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:Box.answerHandleTarget"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:Box.answerHandleTarget:return"),
                        returnValue = ChirConstantValue(ChirSemanticId("const:Box.answerHandleTarget"), intType, "51"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:Box.answerHandleTarget"),
        )
        val box = DefaultChirClassDeclaration(
            semanticId = ChirSemanticId("class:Box.receiverHandle"),
            name = "Box",
            memberDeclarations = listOf(answer),
        )
        val caller = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:callReceiverHandle"),
            name = "callReceiverHandle",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:callReceiverHandle"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:receiver-handle-box"),
                            operation = "jvm.new",
                            operands = emptyList(),
                            resultType = boxType,
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:receiver-handle-select"),
                            operation = "select",
                            operands = listOf(
                                ChirConstantValue(ChirSemanticId("const:receiver-handle-select"), boolType, "true"),
                                ChirFunctionValue(
                                    semanticId = ChirSemanticId("value:Box.answerHandleTarget"),
                                    type = receiverFunctionType,
                                    name = "answerHandleTarget",
                                ),
                                ChirFunctionValue(
                                    semanticId = ChirSemanticId("value:Box.answerHandleTarget.fallback"),
                                    type = receiverFunctionType,
                                    name = "answerHandleTarget",
                                ),
                            ),
                            resultType = receiverFunctionType,
                        ),
                        ChirCallExpression(
                            semanticId = ChirSemanticId("expr:receiver-handle-call"),
                            callee = handleLocal,
                            arguments = listOf(boxLocal),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:callReceiverHandle:return"),
                        returnValue = dynamicCall,
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:callReceiverHandle"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWithDeclarations(box, caller))
        val artifactsByName = output.classes.associateBy { it.internalName }

        val loader = GeneratedClassLoader(artifactsByName)
        verifyClasses(output.classes, loader)
        val facadeClass = loader.load(requireNotNull(artifactsByName["demo/pkg/DemoCj"]))
        assertEquals(51, facadeClass.getMethod("callReceiverHandle").invoke(null))
    }

    /**
     * 验证导入 virtual JVM MethodHandle 的动态调用路径。
     */
    @Test
    fun `invokes imported virtual JVM method handles dynamically`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val stringType = ChirResolvedTypeRef(ChirNamedType("String"))
        val functionType = ChirResolvedTypeRef(
            ChirFunctionType(
                parameterTypes = emptyList(),
                returnType = intType,
                receiverType = stringType,
            ),
        )
        val handleLocal = ChirLocalValue(ChirSemanticId("expr:string-length-handle"), functionType, "expr_string_length_handle")
        val callResult = ChirLocalValue(ChirSemanticId("expr:string-length-dynamic-call"), intType, "expr_string_length_dynamic_call")
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:dynamicStringLength"),
            name = "dynamicStringLength",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:dynamicStringLength"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:string-length-handle"),
                            operation = "select",
                            operands = listOf(
                                ChirConstantValue(ChirSemanticId("const:select-length"), ChirResolvedTypeRef(ChirPrimitiveType.BOOL), "true"),
                                ChirImportedFunctionValue(
                                    semanticId = ChirSemanticId("import:java.lang.String.length.handle"),
                                    type = functionType,
                                    name = "String.length",
                                    attributes = jvmImport("java/lang/String", "length", "virtual", descriptor = "()I"),
                                ),
                                ChirImportedFunctionValue(
                                    semanticId = ChirSemanticId("import:java.lang.String.length.handle.fallback"),
                                    type = functionType,
                                    name = "String.length",
                                    attributes = jvmImport("java/lang/String", "length", "virtual", descriptor = "()I"),
                                ),
                            ),
                            resultType = functionType,
                        ),
                        ChirCallExpression(
                            semanticId = ChirSemanticId("expr:string-length-dynamic-call"),
                            callee = handleLocal,
                            arguments = listOf(ChirConstantValue(ChirSemanticId("const:dynamic-string"), stringType, "cangjie")),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:dynamicStringLength:return"),
                        returnValue = callResult,
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:dynamicStringLength"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(function))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(7, generatedClass.getMethod("dynamicStringLength").invoke(null))
    }

    /**
     * 验证 CHIR class、struct 与 enum 声明会生成可加载的 JVM class。
     */
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
                    attributes = setOf(ChirStringAttribute(JvmAbiAttributes.NAME, "abi_value")),
                ),
                memberFunction,
            ),
        )
        val pair = DefaultChirStructDeclaration(
            semanticId = ChirSemanticId("struct:Pair"),
            name = "Pair",
            fieldDeclarations = listOf(
                DefaultChirVariableDeclaration(ChirSemanticId("field:Pair.left"), "left", intType, mutable = false),
            ),
            memberDeclarations = listOf(
                DefaultChirVariableDeclaration(ChirSemanticId("field:Pair.right"), "right", intType, mutable = false),
            ),
        )
        val color = DefaultChirEnumDeclaration(
            semanticId = ChirSemanticId("enum:Color"),
            name = "Color",
            cases = listOf("RED", "GREEN"),
            memberDeclarations = listOf(
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("field:Color.rgb"),
                    name = "rgb",
                    type = intType,
                    mutable = true,
                ),
                DefaultChirFunctionDeclaration(
                    semanticId = ChirSemanticId("fn:Color.code"),
                    name = "code",
                    returnType = intType,
                    parameters = emptyList(),
                    blocks = listOf(
                        ChirBlock(
                            semanticId = ChirSemanticId("block:Color.code"),
                            name = "entry",
                            expressions = emptyList(),
                            terminator = ChirReturnTerminator(
                                semanticId = ChirSemanticId("term:Color.code:return"),
                                returnValue = ChirConstantValue(ChirSemanticId("const:Color.code"), intType, "5"),
                            ),
                        ),
                    ),
                    entryBlockId = ChirSemanticId("block:Color.code"),
                ),
            ),
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
        val valueField = boxClass.getField("abi_value")
        assertEquals(false, Modifier.isFinal(valueField.modifiers))
        valueField.setInt(boxInstance, 9)
        assertEquals(9, valueField.getInt(boxInstance))
        assertEquals(42, boxClass.getMethod("answer").invoke(boxInstance))
        assertTrue(Modifier.isFinal(pairClass.getField("left").modifiers))
        assertTrue(Modifier.isFinal(pairClass.getField("right").modifiers))
        val values = colorClass.getMethod("values").invoke(null) as Array<*>
        assertEquals(listOf("RED", "GREEN"), values.map { (it as Enum<*>).name })
        assertEquals("GREEN", (colorClass.getMethod("valueOf", String::class.java).invoke(null, "GREEN") as Enum<*>).name)
        val red = values[0] as Enum<*>
        colorClass.getField("rgb").setInt(red, 0xCC0000)
        assertEquals(0xCC0000, colorClass.getField("rgb").getInt(red))
        assertEquals(5, colorClass.getMethod("code").invoke(red))
    }

    /**
     * 验证嵌套 CHIR 自定义类型成员声明会生成为独立 JVM class artifact。
     */
    @Test
    fun `generates nested CHIR custom type member declarations as JVM classes`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val nested = DefaultChirStructDeclaration(
            semanticId = ChirSemanticId("struct:Outer.Nested"),
            name = "demo.pkg.Outer.Nested",
            fieldDeclarations = listOf(
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("field:Outer.Nested.value"),
                    name = "value",
                    type = intType,
                    mutable = false,
                ),
            ),
        )
        val outer = DefaultChirClassDeclaration(
            semanticId = ChirSemanticId("class:Outer"),
            name = "Outer",
            memberDeclarations = listOf(nested),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWithDeclarations(outer))
        val artifactsByName = output.classes.associateBy { it.internalName }

        assertTrue("demo/pkg/Outer" in artifactsByName.keys)
        assertTrue("demo/pkg/Outer/Nested" in artifactsByName.keys)
        output.classes.forEach(::verifyClass)
        val loader = GeneratedClassLoader()
        val nestedClass = loader.define(requireNotNull(artifactsByName["demo/pkg/Outer/Nested"]))
        assertTrue(Modifier.isFinal(nestedClass.getField("value").modifiers))
    }

    /**
     * 验证 CHIR 对象构造、实例字段读取和实例字段写入的 JVM 降低。
     */
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

    /**
     * 验证 CHIR 成员构造器声明会生成 JVM `<init>` 方法。
     */
    @Test
    fun `lowers CHIR member constructor declarations to JVM init methods`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
        val boxType = ChirResolvedTypeRef(ChirNamedType("demo.pkg.Box"))
        val constructorId = ChirSemanticId("fn:Box.init")
        val constructorParameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("param:Box.init.initial"),
            name = "initial",
            type = intType,
            mutable = false,
        )
        val constructor = DefaultChirFunctionDeclaration(
            semanticId = constructorId,
            name = "init",
            returnType = unitType,
            parameters = listOf(constructorParameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:Box.init"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:constructor-set-value"),
                            operation = "jvm.putField",
                            operands = listOf(
                                ChirLocalValue(ChirSemanticId("value:Box.this"), boxType, "this"),
                                ChirParameterValue(
                                    semanticId = constructorParameter.semanticId,
                                    type = intType,
                                    name = "initial",
                                    ownerFunctionId = constructorId,
                                ),
                            ),
                            attributes = jvmField("value"),
                        ),
                    ),
                    terminator = ChirReturnTerminator(ChirSemanticId("term:Box.init:return")),
                ),
            ),
            entryBlockId = ChirSemanticId("block:Box.init"),
            attributes = setOf(ChirStringAttribute(JvmAbiAttributes.NAME, "<init>")),
        )
        val box = DefaultChirClassDeclaration(
            semanticId = ChirSemanticId("class:BoxWithConstructor"),
            name = "Box",
            memberDeclarations = listOf(
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("field:Box.value"),
                    name = "value",
                    type = intType,
                    mutable = true,
                ),
                constructor,
            ),
        )
        val boxLocal = ChirLocalValue(ChirSemanticId("expr:new-box-with-constructor"), boxType, "expr_new_box_with_constructor")
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:boxConstructorValue"),
            name = "boxConstructorValue",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:boxConstructorValue"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:new-box-with-constructor"),
                            operation = "jvm.new",
                            operands = listOf(ChirConstantValue(ChirSemanticId("const:constructor-value"), intType, "21")),
                            resultType = boxType,
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:get-constructor-value"),
                            operation = "jvm.getField",
                            operands = listOf(boxLocal),
                            resultType = intType,
                            attributes = jvmField("value"),
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:boxConstructorValue:return"),
                        returnValue = ChirLocalValue(
                            ChirSemanticId("expr:get-constructor-value"),
                            intType,
                            "expr_get_constructor_value",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:boxConstructorValue"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWithDeclarations(box, function))
        val artifactsByName = output.classes.associateBy { it.internalName }

        val loader = GeneratedClassLoader(artifactsByName)
        verifyClasses(output.classes, loader)
        val boxClass = loader.load(requireNotNull(artifactsByName["demo/pkg/Box"]))
        val facadeClass = loader.load(requireNotNull(artifactsByName["demo/pkg/DemoCj"]))
        assertThrows(NoSuchMethodException::class.java) { boxClass.getConstructor() }
        val constructorReflection = boxClass.getConstructor(Int::class.javaPrimitiveType)
        assertEquals(21, boxClass.getField("value").getInt(constructorReflection.newInstance(21)))
        assertEquals(21, facadeClass.getMethod("boxConstructorValue").invoke(null))
    }

    /**
     * 验证 class 成员访问 module 全局变量和顶层函数时使用 facade owner。
     */
    @Test
    fun `lowers class member access to module globals and top level functions through facade owner`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val boxType = ChirResolvedTypeRef(ChirNamedType("demo.pkg.Box"))
        val global = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("global:counter"),
            name = "counter",
            type = intType,
            mutable = true,
        )
        val topAnswer = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:topAnswer"),
            name = "topAnswer",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:topAnswer"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:topAnswer:return"),
                        returnValue = ChirConstantValue(ChirSemanticId("const:topAnswer"), intType, "8"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:topAnswer"),
        )
        val member = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:Box.sumGlobalAndTop"),
            name = "sumGlobalAndTop",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:Box.sumGlobalAndTop"),
                    name = "entry",
                    expressions = listOf(
                        ChirCallExpression(
                            semanticId = ChirSemanticId("expr:member-call-top"),
                            callee = ChirFunctionValue(
                                semanticId = ChirSemanticId("value:topAnswer"),
                                type = ChirResolvedTypeRef(ChirFunctionType(emptyList(), intType)),
                                name = "topAnswer",
                            ),
                            arguments = emptyList(),
                            resultType = intType,
                        ),
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:member-sum"),
                            operator = "+",
                            left = ChirLocalValue(ChirSemanticId("expr:member-call-top"), intType, "expr_member_call_top"),
                            right = ChirGlobalValue(ChirSemanticId("value:counter"), intType, "counter"),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:Box.sumGlobalAndTop:return"),
                        returnValue = ChirLocalValue(ChirSemanticId("expr:member-sum"), intType, "expr_member_sum"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:Box.sumGlobalAndTop"),
        )
        val box = DefaultChirClassDeclaration(
            semanticId = ChirSemanticId("class:BoxWithFacadeAccess"),
            name = "Box",
            memberDeclarations = listOf(member),
        )
        val caller = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:callMemberFacadeAccess"),
            name = "callMemberFacadeAccess",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:callMemberFacadeAccess"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:new-facade-access-box"),
                            operation = "jvm.new",
                            operands = emptyList(),
                            resultType = boxType,
                        ),
                        ChirCallExpression(
                            semanticId = ChirSemanticId("expr:call-member-facade-access"),
                            callee = ChirFunctionValue(
                                semanticId = ChirSemanticId("value:Box.sumGlobalAndTop"),
                                type = ChirResolvedTypeRef(
                                    ChirFunctionType(
                                        parameterTypes = emptyList(),
                                        returnType = intType,
                                        receiverType = boxType,
                                    ),
                                ),
                                name = "sumGlobalAndTop",
                            ),
                            arguments = listOf(
                                ChirLocalValue(
                                    ChirSemanticId("expr:new-facade-access-box"),
                                    boxType,
                                    "expr_new_facade_access_box",
                                ),
                            ),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:callMemberFacadeAccess:return"),
                        returnValue = ChirLocalValue(
                            ChirSemanticId("expr:call-member-facade-access"),
                            intType,
                            "expr_call_member_facade_access",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:callMemberFacadeAccess"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWithDeclarations(global, topAnswer, box, caller))
        val artifactsByName = output.classes.associateBy { it.internalName }

        val loader = GeneratedClassLoader(artifactsByName)
        verifyClasses(output.classes, loader)
        loader.load(requireNotNull(artifactsByName["demo/pkg/Box"]))
        val facadeClass = loader.load(requireNotNull(artifactsByName["demo/pkg/DemoCj"]))
        facadeClass.getField("counter").setInt(null, 4)
        assertEquals(12, facadeClass.getMethod("callMemberFacadeAccess").invoke(null))
    }

    /**
     * 验证 CHIR 引用相等与不等会降低为 JVM 引用比较。
     */
    @Test
    fun `lowers CHIR reference equality to JVM reference comparison`() {
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val boxType = ChirResolvedTypeRef(ChirNamedType("demo.pkg.Box"))
        val firstBox = ChirLocalValue(ChirSemanticId("expr:new-first-box"), boxType, "expr_new_first_box")
        val secondBox = ChirLocalValue(ChirSemanticId("expr:new-second-box"), boxType, "expr_new_second_box")
        val box = DefaultChirClassDeclaration(
            semanticId = ChirSemanticId("class:EqualityBox"),
            name = "Box",
        )
        val sameReference = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:sameReference"),
            name = "sameReference",
            returnType = boolType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:sameReference"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:new-first-box"),
                            operation = "jvm.new",
                            operands = emptyList(),
                            resultType = boxType,
                        ),
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:same-reference"),
                            operator = "==",
                            left = firstBox,
                            right = firstBox,
                            resultType = boolType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:sameReference:return"),
                        returnValue = ChirLocalValue(ChirSemanticId("expr:same-reference"), boolType, "expr_same_reference"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:sameReference"),
        )
        val distinctReferences = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:distinctReferences"),
            name = "distinctReferences",
            returnType = boolType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:distinctReferences"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:new-first-box"),
                            operation = "jvm.new",
                            operands = emptyList(),
                            resultType = boxType,
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:new-second-box"),
                            operation = "jvm.new",
                            operands = emptyList(),
                            resultType = boxType,
                        ),
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:distinct-references"),
                            operator = "!=",
                            left = firstBox,
                            right = secondBox,
                            resultType = boolType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:distinctReferences:return"),
                        returnValue = ChirLocalValue(ChirSemanticId("expr:distinct-references"), boolType, "expr_distinct_references"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:distinctReferences"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWithDeclarations(box, sameReference, distinctReferences))
        val artifactsByName = output.classes.associateBy { it.internalName }

        val loader = GeneratedClassLoader(artifactsByName)
        verifyClasses(output.classes, loader)
        loader.load(requireNotNull(artifactsByName["demo/pkg/Box"]))
        val facadeClass = loader.load(requireNotNull(artifactsByName["demo/pkg/DemoCj"]))
        assertEquals(true, facadeClass.getMethod("sameReference").invoke(null))
        assertEquals(true, facadeClass.getMethod("distinctReferences").invoke(null))
    }

    /**
     * 验证 CHIR raw array 分配、元素读写和 length intrinsic 的 JVM 降低。
     */
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

    /**
     * 验证对象数组元素访问时引用 carrier 的适配逻辑。
     */
    @Test
    fun `adapts reference carriers for CHIR object array element access`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val objectType = ChirResolvedTypeRef(ChirNamedType("java.lang.Object"))
        val stringType = ChirResolvedTypeRef(ChirNamedType("String"))
        val arrayType = ChirResolvedTypeRef(ChirRawArrayType(objectType))
        val arrayLocal = ChirLocalValue(ChirSemanticId("expr:new-object-array"), arrayType, "expr_new_object_array")
        val loadedLocal = ChirLocalValue(ChirSemanticId("expr:object-array-load"), stringType, "expr_object_array_load")
        val lengthLocal = ChirLocalValue(ChirSemanticId("expr:object-array-length"), intType, "expr_object_array_length")
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:objectArrayStringLength"),
            name = "objectArrayStringLength",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:objectArrayStringLength"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:new-object-array"),
                            operation = "jvm.newArray",
                            operands = listOf(ChirConstantValue(ChirSemanticId("const:object-array-size"), intType, "1")),
                            resultType = arrayType,
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:object-array-store"),
                            operation = "jvm.arrayStore",
                            operands = listOf(
                                arrayLocal,
                                ChirConstantValue(ChirSemanticId("const:object-array-store-index"), intType, "0"),
                                ChirConstantValue(ChirSemanticId("const:object-array-string"), stringType, "text"),
                            ),
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:object-array-load"),
                            operation = "jvm.arrayLoad",
                            operands = listOf(
                                arrayLocal,
                                ChirConstantValue(ChirSemanticId("const:object-array-load-index"), intType, "0"),
                            ),
                            resultType = stringType,
                        ),
                        ChirCallExpression(
                            semanticId = ChirSemanticId("expr:object-array-length"),
                            callee = ChirImportedFunctionValue(
                                semanticId = ChirSemanticId("import:objectArray:String.length"),
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
                            arguments = listOf(loadedLocal),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:objectArrayStringLength:return"),
                        returnValue = lengthLocal,
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:objectArrayStringLength"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(function))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(4, generatedClass.getMethod("objectArrayStringLength").invoke(null))
    }

    /**
     * 验证多维 varray 分配与嵌套元素访问的 JVM 降低。
     */
    @Test
    fun `lowers CHIR multidimensional varray allocation and nested element access`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val rowType = ChirResolvedTypeRef(ChirVArrayType(intType))
        val matrixType = ChirResolvedTypeRef(ChirVArrayType(intType, rank = 2))
        val matrixLocal = ChirLocalValue(ChirSemanticId("expr:new-matrix"), matrixType, "expr_new_matrix")
        val rowLocal = ChirLocalValue(ChirSemanticId("expr:matrix-row"), rowType, "expr_matrix_row")
        val loadedLocal = ChirLocalValue(ChirSemanticId("expr:matrix-load"), intType, "expr_matrix_load")
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:matrixRoundTrip"),
            name = "matrixRoundTrip",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:matrixRoundTrip"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:new-matrix"),
                            operation = "jvm.newArray",
                            operands = listOf(
                                ChirConstantValue(ChirSemanticId("const:matrix-rows"), intType, "2"),
                                ChirConstantValue(ChirSemanticId("const:matrix-cols"), intType, "3"),
                            ),
                            resultType = matrixType,
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:matrix-row"),
                            operation = "jvm.arrayLoad",
                            operands = listOf(
                                matrixLocal,
                                ChirConstantValue(ChirSemanticId("const:matrix-row-index"), intType, "1"),
                            ),
                            resultType = rowType,
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:matrix-store"),
                            operation = "jvm.arrayStore",
                            operands = listOf(
                                rowLocal,
                                ChirConstantValue(ChirSemanticId("const:matrix-col-index"), intType, "2"),
                                ChirConstantValue(ChirSemanticId("const:matrix-value"), intType, "34"),
                            ),
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:matrix-load"),
                            operation = "jvm.arrayLoad",
                            operands = listOf(
                                rowLocal,
                                ChirConstantValue(ChirSemanticId("const:matrix-load-index"), intType, "2"),
                            ),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:matrixRoundTrip:return"),
                        returnValue = loadedLocal,
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:matrixRoundTrip"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(function))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(34, generatedClass.getMethod("matrixRoundTrip").invoke(null))
    }

    /**
     * 验证 long carrier 可作为数组大小和下标输入并被适配为 JVM int。
     */
    @Test
    fun `lowers CHIR long array size and index operands`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val longType = ChirResolvedTypeRef(ChirPrimitiveType.INT64)
        val arrayType = ChirResolvedTypeRef(ChirRawArrayType(intType))
        val arrayLocal = ChirLocalValue(ChirSemanticId("expr:new-long-index-array"), arrayType, "expr_new_long_index_array")
        val loadedLocal = ChirLocalValue(ChirSemanticId("expr:long-index-array-load"), intType, "expr_long_index_array_load")
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:longArrayIndex"),
            name = "longArrayIndex",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:longArrayIndex"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:new-long-index-array"),
                            operation = "jvm.newArray",
                            operands = listOf(ChirConstantValue(ChirSemanticId("const:long-array-size"), longType, "4")),
                            resultType = arrayType,
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:long-index-array-store"),
                            operation = "jvm.arrayStore",
                            operands = listOf(
                                arrayLocal,
                                ChirConstantValue(ChirSemanticId("const:long-array-index"), longType, "2"),
                                ChirConstantValue(ChirSemanticId("const:long-array-value"), intType, "91"),
                            ),
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:long-index-array-load"),
                            operation = "jvm.arrayLoad",
                            operands = listOf(
                                arrayLocal,
                                ChirConstantValue(ChirSemanticId("const:long-array-load-index"), longType, "2"),
                            ),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:longArrayIndex:return"),
                        returnValue = loadedLocal,
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:longArrayIndex"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(function))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(91, generatedClass.getMethod("longArrayIndex").invoke(null))
    }

    /**
     * 验证对象 null 常量会生成为 null 引用，同时字符串字面量 `null` 不被重写。
     */
    @Test
    fun `lowers CHIR object null constants without rewriting string null literals`() {
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val stringType = ChirResolvedTypeRef(ChirNamedType("String"))
        val boxType = ChirResolvedTypeRef(ChirNamedType("demo.pkg.Box"))
        val stringLength = ChirLocalValue(ChirSemanticId("expr:string-null-length"), intType, "expr_string_null_length")
        val box = DefaultChirClassDeclaration(
            semanticId = ChirSemanticId("class:NullBox"),
            name = "Box",
        )
        val isNullBox = functionReturningValue(
            name = "isNullBox",
            returnType = boolType,
            expressions = listOf(
                ChirBinaryExpression(
                    semanticId = ChirSemanticId("expr:null-box-equality"),
                    operator = "==",
                    left = ChirConstantValue(ChirSemanticId("const:null-box-left"), boxType, "null"),
                    right = ChirConstantValue(ChirSemanticId("const:null-box-right"), boxType, "nullptr"),
                    resultType = boolType,
                ),
            ),
            returnValue = ChirLocalValue(ChirSemanticId("expr:null-box-equality"), boolType, "expr_null_box_equality"),
        )
        val stringNullLength = functionReturningValue(
            name = "stringNullLength",
            returnType = intType,
            expressions = listOf(
                ChirCallExpression(
                    semanticId = ChirSemanticId("expr:string-null-length"),
                    callee = ChirImportedFunctionValue(
                        semanticId = ChirSemanticId("import:String.length.nullLiteral"),
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
                    arguments = listOf(ChirConstantValue(ChirSemanticId("const:string-null-literal"), stringType, "null")),
                    resultType = intType,
                ),
            ),
            returnValue = stringLength,
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWithDeclarations(box, isNullBox, stringNullLength))
        val artifactsByName = output.classes.associateBy { it.internalName }

        val loader = GeneratedClassLoader(artifactsByName)
        verifyClasses(output.classes, loader)
        loader.load(requireNotNull(artifactsByName["demo/pkg/Box"]))
        val facadeClass = loader.load(requireNotNull(artifactsByName["demo/pkg/DemoCj"]))
        assertEquals(true, facadeClass.getMethod("isNullBox").invoke(null))
        assertEquals(4, facadeClass.getMethod("stringNullLength").invoke(null))
    }

    /**
     * 验证 JVM checkcast、instanceof 与 throw 终结符的降低。
     */
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

    /**
     * 验证 CHIR phi 表达式会在 JVM 控制流边上发射赋值。
     */
    @Test
    fun `lowers CHIR phi assignments on JVM control-flow edges`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val thenBlockId = ChirSemanticId("block:then")
        val elseBlockId = ChirSemanticId("block:else")
        val mergeBlockId = ChirSemanticId("block:merge")
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:phiValue"),
            name = "phiValue",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirConditionalBranchTerminator(
                        semanticId = ChirSemanticId("term:entry:branch"),
                        condition = ChirConstantValue(ChirSemanticId("const:true"), boolType, "true"),
                        trueTargetBlockId = thenBlockId,
                        falseTargetBlockId = elseBlockId,
                    ),
                ),
                ChirBlock(
                    semanticId = thenBlockId,
                    name = "then",
                    expressions = emptyList(),
                    terminator = ChirBranchTerminator(ChirSemanticId("term:then:branch"), mergeBlockId),
                ),
                ChirBlock(
                    semanticId = elseBlockId,
                    name = "else",
                    expressions = emptyList(),
                    terminator = ChirBranchTerminator(ChirSemanticId("term:else:branch"), mergeBlockId),
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
                                    semanticId = ChirSemanticId("const:phi:then"),
                                    type = intType,
                                    literal = "41",
                                    attributes = setOf(ChirStringAttribute("pred", thenBlockId.value)),
                                ),
                                ChirConstantValue(
                                    semanticId = ChirSemanticId("const:phi:else"),
                                    type = intType,
                                    literal = "9",
                                    attributes = setOf(ChirStringAttribute("pred", elseBlockId.value)),
                                ),
                            ),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:merge:return"),
                        returnValue = ChirLocalValue(ChirSemanticId("expr:phi"), intType, "expr_phi"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(function))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(41, generatedClass.getMethod("phiValue").invoke(null))
    }

    /**
     * 验证 throw 的 unwind 边会降低为 JVM exception handler 控制流。
     */
    @Test
    fun `lowers CHIR throw unwind edge to JVM exception handler`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val runtimeExceptionType = ChirResolvedTypeRef(ChirNamedType("java.lang.RuntimeException"))
        val entryBlockId = ChirSemanticId("block:throwUnwind:entry")
        val handlerBlockId = ChirSemanticId("block:throwUnwind:handler")
        val exceptionLocal = ChirLocalValue(
            semanticId = ChirSemanticId("expr:throwUnwind:new-runtime-exception"),
            type = runtimeExceptionType,
            name = "expr_throwUnwind_new_runtime_exception",
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:throwUnwind"),
            name = "throwUnwind",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = entryBlockId,
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:throwUnwind:new-runtime-exception"),
                            operation = "jvm.new",
                            operands = emptyList(),
                            resultType = runtimeExceptionType,
                        ),
                    ),
                    terminator = ChirThrowTerminator(
                        semanticId = ChirSemanticId("term:throwUnwind:throw"),
                        exceptionValue = exceptionLocal,
                        unwindTargetBlockId = handlerBlockId,
                    ),
                ),
                ChirBlock(
                    semanticId = handlerBlockId,
                    name = "handler",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:throwUnwind:phi"),
                            operation = "phi",
                            operands = listOf(
                                ChirConstantValue(
                                    semanticId = ChirSemanticId("const:throwUnwind:handled"),
                                    type = intType,
                                    literal = "77",
                                    attributes = setOf(ChirStringAttribute("pred", entryBlockId.value)),
                                ),
                            ),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:throwUnwind:return"),
                        returnValue = ChirLocalValue(
                            semanticId = ChirSemanticId("expr:throwUnwind:phi"),
                            type = intType,
                            name = "expr_throwUnwind_phi",
                        ),
                    ),
                ),
            ),
            entryBlockId = entryBlockId,
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(function))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(77, generatedClass.getMethod("throwUnwind").invoke(null))
    }

    /**
     * 验证 CHIR unwind terminator 会降低为普通 JVM 控制流跳转。
     */
    @Test
    fun `lowers CHIR unwind terminator to JVM control-flow edge`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val entryBlockId = ChirSemanticId("block:unwindTerminator:entry")
        val handlerBlockId = ChirSemanticId("block:unwindTerminator:handler")
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:unwindTerminator"),
            name = "unwindTerminator",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = entryBlockId,
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirUnwindTerminator(
                        semanticId = ChirSemanticId("term:unwindTerminator:unwind"),
                        targetBlockId = handlerBlockId,
                    ),
                ),
                ChirBlock(
                    semanticId = handlerBlockId,
                    name = "handler",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:unwindTerminator:phi"),
                            operation = "phi",
                            operands = listOf(
                                ChirConstantValue(
                                    semanticId = ChirSemanticId("const:unwindTerminator:handled"),
                                    type = intType,
                                    literal = "93",
                                    attributes = setOf(ChirStringAttribute("pred", entryBlockId.value)),
                                ),
                            ),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:unwindTerminator:return"),
                        returnValue = ChirLocalValue(
                            semanticId = ChirSemanticId("expr:unwindTerminator:phi"),
                            type = intType,
                            name = "expr_unwindTerminator_phi",
                        ),
                    ),
                ),
            ),
            entryBlockId = entryBlockId,
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(function))
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(93, generatedClass.getMethod("unwindTerminator").invoke(null))
    }

    /**
     * 验证 CHIR C pointer memory 与指针/整数互转的 JVM runtime 降低。
     */
    @Test
    fun `lowers CHIR C pointer memory and pointer integer round trip`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val int64Type = ChirResolvedTypeRef(ChirPrimitiveType.INT64)
        val pointerType = ChirResolvedTypeRef(ChirCPointerType(intType))
        val pointer = ChirLocalValue(ChirSemanticId("expr:alloca-pointer"), pointerType, "expr_alloca_pointer")
        val address = ChirLocalValue(ChirSemanticId("expr:ptrtoint"), int64Type, "expr_ptrtoint")
        val restoredPointer = ChirLocalValue(ChirSemanticId("expr:inttoptr"), pointerType, "expr_inttoptr")
        val loaded = ChirLocalValue(ChirSemanticId("expr:pointer-load"), intType, "expr_pointer_load")
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:pointerRoundTrip"),
            name = "pointerRoundTrip",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:pointer"),
                    name = "entry",
                    expressions = listOf(
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:alloca-pointer"),
                            operation = "alloca",
                            address = ChirConstantValue(ChirSemanticId("const:pointer:size"), intType, "4"),
                            resultType = pointerType,
                        ),
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:pointer-store"),
                            operation = "store",
                            address = pointer,
                            value = ChirConstantValue(ChirSemanticId("const:pointer:value"), intType, "123"),
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:ptrtoint"),
                            operation = "ptrtoint",
                            operands = listOf(pointer),
                            resultType = int64Type,
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:inttoptr"),
                            operation = "inttoptr",
                            operands = listOf(address),
                            resultType = pointerType,
                        ),
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:pointer-load"),
                            operation = "load",
                            address = restoredPointer,
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:pointer:return"),
                        returnValue = loaded,
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:pointer"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(function))
        val artifactsByName = output.classes.associateBy { it.internalName }

        output.classes.forEach(::verifyClass)
        val generatedClass = GeneratedClassLoader().define(requireNotNull(artifactsByName["demo/pkg/DemoCj"]))
        assertEquals(123, generatedClass.getMethod("pointerRoundTrip").invoke(null))
    }

    /**
     * 验证指针 GEP 会按 pointee 元素字节宽度移动 ByteBuffer 位置。
     */
    @Test
    fun `lowers CHIR pointer gep by pointee element size`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val pointerType = ChirResolvedTypeRef(ChirCPointerType(intType))
        val pointer = ChirLocalValue(ChirSemanticId("expr:alloca-gep-pointer"), pointerType, "expr_alloca_gep_pointer")
        val nextPointer = ChirLocalValue(ChirSemanticId("expr:pointer-gep-next"), pointerType, "expr_pointer_gep_next")
        val baseValue = ChirLocalValue(ChirSemanticId("expr:pointer-gep-base-load"), intType, "expr_pointer_gep_base_load")
        val nextValue = ChirLocalValue(ChirSemanticId("expr:pointer-gep-next-load"), intType, "expr_pointer_gep_next_load")
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:pointerGep"),
            name = "pointerGep",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:pointerGep"),
                    name = "entry",
                    expressions = listOf(
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:alloca-gep-pointer"),
                            operation = "alloca",
                            address = ChirConstantValue(ChirSemanticId("const:gep-pointer:size"), intType, "8"),
                            resultType = pointerType,
                        ),
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:pointer-gep-next"),
                            operation = "gep",
                            address = pointer,
                            value = ChirConstantValue(ChirSemanticId("const:pointer-gep-index"), intType, "1"),
                            resultType = pointerType,
                        ),
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:pointer-gep-base-store"),
                            operation = "store",
                            address = pointer,
                            value = ChirConstantValue(ChirSemanticId("const:pointer-gep-base-value"), intType, "11"),
                        ),
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:pointer-gep-next-store"),
                            operation = "store",
                            address = nextPointer,
                            value = ChirConstantValue(ChirSemanticId("const:pointer-gep-next-value"), intType, "22"),
                        ),
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:pointer-gep-base-load"),
                            operation = "load",
                            address = pointer,
                            resultType = intType,
                        ),
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:pointer-gep-next-load"),
                            operation = "load",
                            address = nextPointer,
                            resultType = intType,
                        ),
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:pointer-gep-sum"),
                            operator = "+",
                            left = baseValue,
                            right = nextValue,
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:pointerGep:return"),
                        returnValue = ChirLocalValue(ChirSemanticId("expr:pointer-gep-sum"), intType, "expr_pointer_gep_sum"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:pointerGep"),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWith(function))
        val artifactsByName = output.classes.associateBy { it.internalName }

        output.classes.forEach(::verifyClass)
        val generatedClass = GeneratedClassLoader().define(requireNotNull(artifactsByName["demo/pkg/DemoCj"]))
        assertEquals(33, generatedClass.getMethod("pointerGep").invoke(null))
    }

    /**
     * 验证 facade 全局变量和 extend 声明的 JVM classfile 生成。
     */
    @Test
    fun `lowers CHIR facade globals and extend declarations`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val global = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("global:counter"),
            name = "counter",
            type = intType,
            mutable = true,
        )
        val readGlobal = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:readGlobal"),
            name = "readGlobal",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:readGlobal"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:readGlobal:return"),
                        returnValue = ChirGlobalValue(ChirSemanticId("value:counter"), intType, "counter"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:readGlobal"),
        )
        val extensionFunction = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:extensionAnswer"),
            name = "extensionAnswer",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:extensionAnswer"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:extensionAnswer:return"),
                        returnValue = ChirConstantValue(ChirSemanticId("const:extensionAnswer"), intType, "64"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:extensionAnswer"),
        )
        val extend = DefaultChirExtendDeclaration(
            semanticId = ChirSemanticId("extend:IntExtensions"),
            name = "IntExtensions",
            targetType = intType,
            memberDeclarations = listOf(
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("extend-field:IntExtensions.cache"),
                    name = "cache",
                    type = intType,
                    mutable = true,
                ),
                extensionFunction,
            ),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(inputWithDeclarations(global, readGlobal, extend))
        val artifactsByName = output.classes.associateBy { it.internalName }

        output.classes.forEach(::verifyClass)
        val loader = GeneratedClassLoader()
        val facadeClass = loader.define(requireNotNull(artifactsByName["demo/pkg/DemoCj"]))
        val extendClass = loader.define(requireNotNull(artifactsByName["demo/pkg/IntExtensions"]))
        facadeClass.getField("counter").setInt(null, 37)
        assertEquals(37, facadeClass.getMethod("readGlobal").invoke(null))
        extendClass.getField("cache").setInt(null, 12)
        assertEquals(12, extendClass.getField("cache").getInt(null))
        assertEquals(64, extendClass.getMethod("extensionAnswer").invoke(null))
    }

    /**
     * 验证 CHIR package init 函数会生成 JVM class initializer。
     */
    @Test
    fun `generates JVM class initializer from CHIR package init function`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
        val global = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("global:initialized"),
            name = "initialized",
            type = intType,
            mutable = true,
        )
        val initializer = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:packageInit"),
            name = "packageInit",
            returnType = unitType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:packageInit"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:init-put-static"),
                            operation = "jvm.putStatic",
                            operands = listOf(ChirConstantValue(ChirSemanticId("const:init-value"), intType, "45")),
                            attributes = setOf(
                                ChirStringAttribute(JvmAbiAttributes.OWNER, "demo/pkg/DemoCj"),
                                ChirStringAttribute(JvmAbiAttributes.NAME, "initialized"),
                            ),
                        ),
                    ),
                    terminator = ChirReturnTerminator(ChirSemanticId("term:packageInit:return")),
                ),
            ),
            entryBlockId = ChirSemanticId("block:packageInit"),
        )
        val readInitialized = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:readInitialized"),
            name = "readInitialized",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:readInitialized"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:readInitialized:return"),
                        returnValue = ChirGlobalValue(ChirSemanticId("value:initialized"), intType, "initialized"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:readInitialized"),
        )
        val input = ChirJvmCodegenInput(
            chirPackage = ChirPackage(
                semanticId = ChirSemanticId("pkg:init"),
                name = "demo.pkg",
                modules = listOf(
                    ChirModule(
                        semanticId = ChirSemanticId("mod:demo"),
                        name = "demo",
                        declarations = listOf(global, initializer, readInitialized),
                    ),
                ),
                packageInitFunctionId = initializer.semanticId,
            ),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(input)
        val artifact = output.classes.single()

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        assertEquals(45, generatedClass.getMethod("readInitialized").invoke(null))
    }

    /**
     * 验证 CHIR package members 会生成包级 JVM facade。
     */
    @Test
    fun `generates JVM package facade for CHIR package members`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val packageGlobal = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("pkg-global:shared"),
            name = "shared",
            type = intType,
            mutable = true,
        )
        val packageFunction = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("pkg-fn:readShared"),
            name = "readShared",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:readShared"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:readShared:return"),
                        returnValue = ChirGlobalValue(ChirSemanticId("value:shared"), intType, "shared"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:readShared"),
        )
        val input = ChirJvmCodegenInput(
            chirPackage = ChirPackage(
                semanticId = ChirSemanticId("pkg:members"),
                name = "demo.pkg",
                modules = emptyList(),
                members = ChirPackageMembers(
                    globalVariables = listOf(packageGlobal),
                    globalFunctions = listOf(packageFunction),
                ),
            ),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(input)
        val artifact = output.classes.single { it.internalName == "demo/pkg/PkgPackageCj" }

        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        generatedClass.getField("shared").setInt(null, 88)
        assertEquals(88, generatedClass.getMethod("readShared").invoke(null))
    }

    /**
     * 验证包级 main 函数会生成 Java main bridge，并在 jar manifest 中登记入口类。
     */
    @Test
    fun `generates Java main bridge and jar manifest for package-level main`(@TempDir tempDir: Path) {
        val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
        val main = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("pkg-fn:main"),
            name = "main",
            returnType = unitType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:pkg-main"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(ChirSemanticId("term:pkg-main:return")),
                ),
            ),
            entryBlockId = ChirSemanticId("block:pkg-main"),
        )
        val input = ChirJvmCodegenInput(
            chirPackage = ChirPackage(
                semanticId = ChirSemanticId("pkg:main"),
                name = "demo.pkg",
                modules = emptyList(),
                members = ChirPackageMembers(globalFunctions = listOf(main)),
            ),
        )

        val output = DefaultChirToJvmCodeGenerator().generate(input)
        val artifact = output.classes.single { it.internalName == "demo/pkg/PkgPackageCj" }

        assertEquals("demo/pkg/PkgPackageCj", output.mainClassInternalName)
        verifyClass(artifact)
        val generatedClass = GeneratedClassLoader().define(artifact)
        generatedClass.getMethod("main", Array<String>::class.java).invoke(null, arrayOf<String>())

        val jar = output.writeJar(tempDir.resolve("package-main.jar"))
        JarFile(jar.toFile()).use { jarFile ->
            assertEquals("demo.pkg.PkgPackageCj", jarFile.manifest.mainAttributes.getValue("Main-Class"))
            assertTrue(jarFile.getEntry(artifact.relativePath) != null)
        }
    }

    /**
     * 验证多个可生成 Java main bridge 的模块会被拒绝，避免 manifest 入口不确定。
     */
    @Test
    fun `rejects multiple generated Java main bridges`() {
        fun mainFunction(moduleName: String): DefaultChirFunctionDeclaration {
            return DefaultChirFunctionDeclaration(
                semanticId = ChirSemanticId("fn:$moduleName:main"),
                name = "main",
                returnType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT),
                parameters = emptyList(),
                blocks = listOf(
                    ChirBlock(
                        semanticId = ChirSemanticId("block:$moduleName:main"),
                        name = "entry",
                        expressions = emptyList(),
                        terminator = ChirReturnTerminator(ChirSemanticId("term:$moduleName:main:return")),
                    ),
                ),
                entryBlockId = ChirSemanticId("block:$moduleName:main"),
            )
        }
        val input = ChirJvmCodegenInput(
            chirPackage = ChirPackage(
                semanticId = ChirSemanticId("pkg:multiple-main"),
                name = "demo.pkg",
                modules = listOf(
                    ChirModule(
                        semanticId = ChirSemanticId("mod:alpha-main"),
                        name = "alpha",
                        declarations = listOf(mainFunction("alpha")),
                    ),
                    ChirModule(
                        semanticId = ChirSemanticId("mod:beta-main"),
                        name = "beta",
                        declarations = listOf(mainFunction("beta")),
                    ),
                ),
            ),
        )

        val exception = assertThrows(JvmCodegenException::class.java) {
            DefaultChirToJvmCodeGenerator().generate(input)
        }
        val message = requireNotNull(exception.message)
        assertTrue(message.contains("multiple JVM main classes generated for CHIR package 'demo.pkg'"))
        assertTrue(message.contains("demo/pkg/AlphaCj"))
        assertTrue(message.contains("demo/pkg/BetaCj"))
    }

    /**
     * 验证默认输入下的 Java main bridge 和 jar manifest 生成路径。
     */
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

    /**
     * 构造只包含单个 demo 模块和给定函数列表的 JVM codegen 输入。
     */
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

    /**
     * 构造一个无参、单入口块、返回指定 value 的 CHIR 函数。
     */
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

    /**
     * 构造导入 JVM 函数所需的 owner/name/invokeKind/descriptor ABI 属性集合。
     */
    private fun jvmImport(
        owner: String,
        name: String,
        invokeKind: String,
        descriptor: String? = null,
    ): Set<ChirStringAttribute> =
        listOfNotNull(
            ChirStringAttribute(JvmAbiAttributes.OWNER, owner),
            ChirStringAttribute(JvmAbiAttributes.NAME, name),
            ChirStringAttribute(JvmAbiAttributes.INVOKE_KIND, invokeKind),
            descriptor?.let { ChirStringAttribute(JvmAbiAttributes.DESCRIPTOR, it) },
        ).toSet()

    /**
     * 构造 JVM 字段名称与可选 descriptor ABI 属性集合。
     */
    private fun jvmField(name: String, descriptor: String? = null): Set<ChirStringAttribute> =
        listOfNotNull(
            ChirStringAttribute(JvmAbiAttributes.NAME, name),
            descriptor?.let { ChirStringAttribute(JvmAbiAttributes.DESCRIPTOR, it) },
        ).toSet()

    /**
     * 构造 JVM 类型 internal name ABI 属性集合。
     */
    private fun jvmType(typeInternalName: String): Set<ChirStringAttribute> =
        setOf(ChirStringAttribute(JvmAbiAttributes.TYPE, typeInternalName))

    /**
     * 构造只包含单个 demo 模块和任意 CHIR 声明列表的 JVM codegen 输入。
     */
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

    /**
     * 使用 ASM CheckClassAdapter 验证单个生成 classfile。
     */
    private fun verifyClass(artifact: JvmClassFileArtifact) {
        val output = StringWriter()
        CheckClassAdapter.verify(ClassReader(artifact.bytes), false, PrintWriter(output))
        assertTrue(output.toString().isBlank(), output.toString())
    }

    /**
     * 使用指定 classloader 上下文验证一组生成 classfile。
     */
    private fun verifyClasses(artifacts: List<JvmClassFileArtifact>, classLoader: ClassLoader) {
        artifacts.forEach { artifact ->
            val output = StringWriter()
            CheckClassAdapter.verify(ClassReader(artifact.bytes), classLoader, false, PrintWriter(output))
            assertTrue(output.toString().isBlank(), output.toString())
        }
    }

    /**
     * 测试专用 classloader，用于从内存中的 `JvmClassFileArtifact` 定义和加载生成类。
     */
    private class GeneratedClassLoader(
        /**
         * 以 JVM internal name 索引的待加载 class artifact。
         */
        private val artifactsByName: Map<String, JvmClassFileArtifact> = emptyMap(),
    ) : ClassLoader(DefaultChirToJvmCodeGeneratorTest::class.java.classLoader) {
        /**
         * 直接把 artifact 字节定义为 JVM class。
         */
        fun define(artifact: JvmClassFileArtifact): Class<*> {
            return defineClass(artifact.internalName.replace('/', '.'), artifact.bytes, 0, artifact.bytes.size)
        }

        /**
         * 通过标准 classloader 解析流程加载 artifact 对应的类名。
         */
        fun load(artifact: JvmClassFileArtifact): Class<*> {
            return loadClass(artifact.internalName.replace('/', '.'))
        }

        /**
         * 当父加载器找不到类时，从 artifact 映射中按 internal name 定义生成类。
         */
        override fun findClass(name: String): Class<*> {
            val artifact = artifactsByName[name.replace('.', '/')] ?: return super.findClass(name)
            return define(artifact)
        }
    }
}
