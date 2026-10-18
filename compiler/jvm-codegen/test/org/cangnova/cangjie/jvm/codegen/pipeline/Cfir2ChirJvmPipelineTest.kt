package org.cangnova.cangjie.jvm.codegen.pipeline

import org.cangnova.cangjie.chir.cfir2chir.DefaultCfir2ChirConverter

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.common.CfirModuleCapabilities
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildClass
import org.cangnova.cangjie.cfir.declarations.builder.buildFieldVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOpKind
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builder.buildAssignment
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildArrayLiteral
import org.cangnova.cangjie.cfir.expressions.builder.buildBinaryOp
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildSubscriptExpression
import org.cangnova.cangjie.cfir.expressions.buildThisReceiverExpression
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildThisReference
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.jvm.codegen.api.ChirJvmCodegenInput
import org.cangnova.cangjie.jvm.codegen.api.JvmClassFileArtifact
import org.cangnova.cangjie.jvm.codegen.pipeline.DefaultChirToJvmCodeGenerator
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.isCommon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * 覆盖 CFIR -> CHIR -> JVM 的最小端到端管线。
 */
class Cfir2ChirJvmPipelineTest {
    /**
     * 验证解析后的 CFIR 函数可以降级并生成为可执行 JVM 静态方法。
     */
    @Test
    fun `lowers resolved CFIR function to executable JVM method`() {
        val function = functionReturningInt("answer", 42)
        val file = fileWith(function)

        val chirPackage = DefaultCfir2ChirConverter().convert(listOf(file))
        val output = DefaultChirToJvmCodeGenerator().generate(ChirJvmCodegenInput(chirPackage))
        val generatedClass = GeneratedClassLoader().define(output.classes.single())

        assertEquals(42, generatedClass.getMethod("answer").invoke(null))
    }

    /**
     * 验证 cfir2chir 会在 body lowering 前预注册所有函数 header，从而支持前向调用。
     */
    @Test
    fun `registers all function headers before lowering forward calls`() {
        val callee = functionReturningInt("callee", 7)
        val caller = functionCalling("caller", callee)
        val file = fileWith(caller, callee)

        val chirPackage = DefaultCfir2ChirConverter().convert(listOf(file))
        val output = DefaultChirToJvmCodeGenerator().generate(ChirJvmCodegenInput(chirPackage))
        val generatedClass = GeneratedClassLoader().define(output.classes.single())

        assertEquals(7, generatedClass.getMethod("caller").invoke(null))
    }

    /**
     * 验证 CFIR 局部变量赋值会通过 CHIR memory 表达式降级为 JVM local。
     */
    @Test
    fun `lowers CFIR local variable assignment through CHIR memory to JVM locals`() {
        val function = functionWithLocalVariableAssignment()
        val file = fileWith(function)

        val chirPackage = DefaultCfir2ChirConverter().convert(listOf(file))
        val output = DefaultChirToJvmCodeGenerator().generate(ChirJvmCodegenInput(chirPackage))
        val generatedClass = GeneratedClassLoader().define(output.classes.single())

        assertEquals(2, generatedClass.getMethod("localAssignment").invoke(null))
    }

    /**
     * 验证 CFIR `&&`/`||` 短路二元操作会经 CHIR 控制流降级为可执行的 JVM 字节码。
     */
    @Test
    fun `lowers CFIR short-circuit binary ops to executable JVM methods`() {
        val andFunction = boolResultFunction("andResult", CfirBinaryOpKind.AND, left = false, right = true)
        val orFunction = boolResultFunction("orResult", CfirBinaryOpKind.OR, left = true, right = false)
        val file = fileWith(andFunction, orFunction)

        val chirPackage = DefaultCfir2ChirConverter().convert(listOf(file))
        val output = DefaultChirToJvmCodeGenerator().generate(ChirJvmCodegenInput(chirPackage))
        val generatedClass = GeneratedClassLoader().define(output.classes.single())

        // `false && true` 短路到 false；`true || false` 短路到 true。
        assertEquals(false, generatedClass.getMethod("andResult").invoke(null))
        assertEquals(true, generatedClass.getMethod("orResult").invoke(null))
    }

    /**
     * 构造返回指定 `&&`/`||` 短路结果的测试函数。
     */
    private fun boolResultFunction(
        name: String,
        kind: CfirBinaryOpKind,
        left: Boolean,
        right: Boolean,
    ): CfirNamedFunction {
        val symbol = CfirNamedFunctionSymbol(CallableId(PackageName, Name.identifier(name)))
        return buildNamedFunction {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            returnTypeRef = buildResolvedTypeRef {
                coneType = ConePrimitiveType.BOOLEAN
            }
            this.symbol = symbol
            this.name = Name.identifier(name)
            isMut = false
            body = buildBlock {
                coneTypeOrNull = ConePrimitiveType.BOOLEAN
                statements += buildBinaryOp {
                    coneTypeOrNull = ConePrimitiveType.BOOLEAN
                    this.kind = kind
                    this.left = boolLiteral(left)
                    this.right = boolLiteral(right)
                }
            }
        }
    }

    /**
     * 构造 Bool 字面量表达式。
     */
    private fun boolLiteral(value: Boolean) = buildLiteralExpression {
        kind = CfirLiteralKind.BOOLEAN
        this.value = value
        coneTypeOrNull = ConePrimitiveType.BOOLEAN
    }

    /**
     * 验证成员函数链（this 调用成员函数）能端到端降级为可执行 JVM 实例方法。
     *
     * `Box.answer()` 返回 42；`Box.answerTwice()` 内部通过 this 调用 `answer()`。
     * cfir2chir 把 this 降级为 `this` 局部值、成员调用降级为 receiver-first 调用，
     * JVM 后端生成实例方法并用 `invokevirtual` 执行。
     */
    @Test
    fun `lowers member function chain through this to executable JVM instance methods`() {
        val boxType = ConeClassLikeType(ConeClassLikeLookupTagImpl(BoxClassId))
        val answer = memberFunction(
            name = "answer",
            receiverType = boxType,
            body = intLiteral(42),
        )
        val answerTwice = memberFunction(
            name = "answerTwice",
            receiverType = boxType,
            body = buildFunctionCall {
                coneTypeOrNull = ConePrimitiveType.INT32
                calleeReference = buildResolvedNamedReference {
                    name = answer.name
                    resolvedSymbol = answer.symbol
                }
                dispatchReceiver = buildThisReceiverExpression {
                    coneTypeOrNull = boxType
                    calleeReference = buildThisReference {
                        boundSymbol = CfirClassSymbol(BoxClassId)
                        isImplicit = true
                    }
                }
                argumentList = buildArgumentList()
                origin = CfirFunctionCallOrigin.Regular
            },
        )
        val box = buildClass {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            scopeProvider = UnusedScopeProvider
            status = CfirDeclarationStatusImpl()
            symbol = CfirClassSymbol(BoxClassId)
            name = Name.identifier("Box")
            declarations += answer
            declarations += answerTwice
        }
        val file = fileWith(box)

        val chirPackage = DefaultCfir2ChirConverter().convert(listOf(file))
        val output = DefaultChirToJvmCodeGenerator().generate(ChirJvmCodegenInput(chirPackage))
        val artifactsByName = output.classes.associateBy { it.internalName }
        val boxArtifact = requireNotNull(artifactsByName["sample/Box"])

        val generatedClass = GeneratedClassLoader().define(boxArtifact)
        val instance = generatedClass.getConstructor().newInstance()
        assertEquals(42, generatedClass.getMethod("answer").invoke(instance))
        assertEquals(42, generatedClass.getMethod("answerTwice").invoke(instance))
    }

    /**
     * 验证成员字段读写能端到端降级为可执行的 JVM 实例字段访问。
     *
     * `Box.setValue(n)` 写 `this.value = n`；`Box.getValue()` 读 `this.value`。
     * cfir2chir 把字段读写降级为 `jvm.putfield`/`jvm.getfield`，
     * JVM 后端发射真实的 getfield/putfield 字节码。
     */
    @Test
    fun `lowers member field access through this to executable JVM instance fields`() {
        val boxType = ConeClassLikeType(ConeClassLikeLookupTagImpl(BoxClassId))
        val setValueSymbol = CfirNamedFunctionSymbol(CallableId(PackageName, Name.identifier("setValue")))
        val valueField = buildFieldVariable {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            symbol = CfirFieldVariableSymbol(CallableId(PackageName, Name.identifier("value")))
            name = Name.identifier("value")
            returnTypeRef = intTypeRef()
            initializer = null
            isVar = true
        }
        val thisReceiver = buildThisReceiverExpression {
            coneTypeOrNull = boxType
            calleeReference = buildThisReference {
                boundSymbol = CfirClassSymbol(BoxClassId)
                isImplicit = true
            }
        }
        val valueParameter = buildValueParameter {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            isNamed = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            symbol = CfirValueParameterSymbol(CallableId(PackageName, Name.identifier("n")))
            containingDeclarationSymbol = setValueSymbol
            name = Name.identifier("n")
            returnTypeRef = intTypeRef()
            defaultValue = null
        }
        val setValue = buildNamedFunction {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = boxType
            status = CfirDeclarationStatusImpl()
            returnTypeRef = buildResolvedTypeRef { coneType = ConePrimitiveType.UNIT }
            symbol = setValueSymbol
            name = Name.identifier("setValue")
            isMut = false
            valueParameters += valueParameter
            body = buildBlock {
                coneTypeOrNull = ConePrimitiveType.UNIT
                statements += buildAssignment {
                    coneTypeOrNull = ConePrimitiveType.UNIT
                    lValue = buildNamedAccessExpression {
                        coneTypeOrNull = ConePrimitiveType.INT32
                        calleeReference = buildResolvedNamedReference {
                            name = valueField.name
                            resolvedSymbol = valueField.symbol
                        }
                        dispatchReceiver = thisReceiver
                    }
                    rValue = buildNamedAccessExpression {
                        coneTypeOrNull = ConePrimitiveType.INT32
                        calleeReference = buildResolvedNamedReference {
                            name = valueParameter.name
                            resolvedSymbol = valueParameter.symbol
                        }
                    }
                }
            }
        }
        val getValue = buildNamedFunction {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = boxType
            status = CfirDeclarationStatusImpl()
            returnTypeRef = intTypeRef()
            symbol = CfirNamedFunctionSymbol(CallableId(PackageName, Name.identifier("getValue")))
            name = Name.identifier("getValue")
            isMut = false
            body = buildBlock {
                coneTypeOrNull = ConePrimitiveType.INT32
                statements += buildNamedAccessExpression {
                    coneTypeOrNull = ConePrimitiveType.INT32
                    calleeReference = buildResolvedNamedReference {
                        name = valueField.name
                        resolvedSymbol = valueField.symbol
                    }
                    dispatchReceiver = buildThisReceiverExpression {
                        coneTypeOrNull = boxType
                        calleeReference = buildThisReference {
                            boundSymbol = CfirClassSymbol(BoxClassId)
                            isImplicit = true
                        }
                    }
                }
            }
        }
        val box = buildClass {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            scopeProvider = UnusedScopeProvider
            status = CfirDeclarationStatusImpl()
            symbol = CfirClassSymbol(BoxClassId)
            name = Name.identifier("Box")
            declarations += valueField
            declarations += setValue
            declarations += getValue
        }
        val file = fileWith(box)

        val chirPackage = DefaultCfir2ChirConverter().convert(listOf(file))
        val output = DefaultChirToJvmCodeGenerator().generate(ChirJvmCodegenInput(chirPackage))
        val artifactsByName = output.classes.associateBy { it.internalName }
        val boxArtifact = requireNotNull(artifactsByName["sample/Box"])

        val generatedClass = GeneratedClassLoader().define(boxArtifact)
        val instance = generatedClass.getConstructor().newInstance()
        generatedClass
            .getMethod("setValue", Int::class.javaPrimitiveType)
            .invoke(instance, 99)
        assertEquals(99, generatedClass.getMethod("getValue").invoke(instance))
        assertEquals(99, generatedClass.getField("value").getInt(instance))
    }

    /**
     * 验证 VArray 字面量能端到端降级为可执行的 JVM 数组分配与填充。
     *
     * `makeArray()` 创建 `[10, 20, 30]` 并直接返回该数组；
     * 反射拿到 `int[]` 后逐元素校验真实 JVM 数组内容。
     */
    @Test
    fun `lowers varray literal to executable JVM array allocation and stores`() {
        val intArrayType = ConeVArrayType(ConePrimitiveType.INT32, size = 3)
        val arrayLiteral = buildArrayLiteral {
            coneTypeOrNull = intArrayType
            elements += intLiteral(10)
            elements += intLiteral(20)
            elements += intLiteral(30)
        }

        val function = buildNamedFunction {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            returnTypeRef = buildResolvedTypeRef { coneType = intArrayType }
            symbol = CfirNamedFunctionSymbol(CallableId(PackageName, Name.identifier("makeArray")))
            name = Name.identifier("makeArray")
            isMut = false
            body = buildBlock {
                coneTypeOrNull = intArrayType
                statements += arrayLiteral
            }
        }
        val file = fileWith(function)

        val chirPackage = DefaultCfir2ChirConverter().convert(listOf(file))
        val output = DefaultChirToJvmCodeGenerator().generate(ChirJvmCodegenInput(chirPackage))

        val generatedClass = GeneratedClassLoader().define(output.classes.single())
        val result = generatedClass.getMethod("makeArray").invoke(null)
        val intArray = result as IntArray
        assertEquals(3, intArray.size)
        assertEquals(10, intArray[0])
        assertEquals(20, intArray[1])
        assertEquals(30, intArray[2])
    }

    /**
     * 验证数组完整生命周期能端到端降级为可执行 JVM 字节码。
     *
     * `arrayRoundTrip()` 创建 `[10, 20, 30]`，把 `a[1]` 改写为 77，返回 `a[1]`。
     * 覆盖数组字面量分配、下标赋值与下标读取三条降级路径；
     * 改写成功则返回 77 而不是初始值 20。
     */
    @Test
    fun `lowers array literal subscript store and load round trip to executable JVM bytecode`() {
        val intArrayType = ConeVArrayType(ConePrimitiveType.INT32, size = 3)

        val arraySymbol = CfirFieldVariableSymbol(CallableId(PackageName, Name.identifier("a")))
        val arrayVariable = buildFieldVariable {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            symbol = arraySymbol
            name = Name.identifier("a")
            returnTypeRef = buildResolvedTypeRef { coneType = intArrayType }
            initializer = buildArrayLiteral {
                coneTypeOrNull = intArrayType
                elements += intLiteral(10)
                elements += intLiteral(20)
                elements += intLiteral(30)
            }
            isVar = true
        }
        val arrayAccess = buildNamedAccessExpression {
            coneTypeOrNull = intArrayType
            calleeReference = buildResolvedNamedReference {
                name = arrayVariable.name
                resolvedSymbol = arraySymbol
            }
        }
        val loadAtIndex = fun(index: Int) = buildSubscriptExpression {
            coneTypeOrNull = ConePrimitiveType.INT32
            receiver = arrayAccess
            indices += intLiteral(index)
        }

        val function = buildNamedFunction {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            returnTypeRef = intTypeRef()
            symbol = CfirNamedFunctionSymbol(CallableId(PackageName, Name.identifier("arrayRoundTrip")))
            name = Name.identifier("arrayRoundTrip")
            isMut = false
            body = buildBlock {
                coneTypeOrNull = ConePrimitiveType.INT32
                statements += arrayVariable
                statements += buildAssignment {
                    coneTypeOrNull = ConePrimitiveType.UNIT
                    lValue = loadAtIndex(1)
                    rValue = intLiteral(77)
                }
                statements += loadAtIndex(1)
            }
        }
        val file = fileWith(function)

        val chirPackage = DefaultCfir2ChirConverter().convert(listOf(file))
        val output = DefaultChirToJvmCodeGenerator().generate(ChirJvmCodegenInput(chirPackage))

        val generatedClass = GeneratedClassLoader().define(output.classes.single())
        assertEquals(77, generatedClass.getMethod("arrayRoundTrip").invoke(null))
    }

    /**
     * 验证局部函数声明能端到端降级：函数体进入 module 辅助声明，
     * 函数引用物化为可调用的 JVM MethodHandle。
     *
     * `useLocal()` 声明局部函数 `localAnswer`（返回 5）并返回其引用；
     * 测试反射拿 MethodHandle 后动态调用验证目标真实存在。
     */
    @Test
    fun `lowers local function declaration to callable method handle end to end`() {
        val functionType = ConeFunctionType(emptyList(), ConePrimitiveType.INT32)
        val localAnswer = buildNamedFunction {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            returnTypeRef = intTypeRef()
            symbol = CfirNamedFunctionSymbol(CallableId(PackageName, Name.identifier("localAnswer")))
            name = Name.identifier("localAnswer")
            isMut = false
            body = buildBlock {
                coneTypeOrNull = ConePrimitiveType.INT32
                statements += intLiteral(5)
            }
        }
        val useLocal = buildNamedFunction {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            returnTypeRef = buildResolvedTypeRef { coneType = functionType }
            symbol = CfirNamedFunctionSymbol(CallableId(PackageName, Name.identifier("useLocal")))
            name = Name.identifier("useLocal")
            isMut = false
            body = buildBlock {
                coneTypeOrNull = functionType
                statements += localAnswer
                statements += buildNamedAccessExpression {
                    coneTypeOrNull = functionType
                    calleeReference = buildResolvedNamedReference {
                        name = localAnswer.name
                        resolvedSymbol = localAnswer.symbol
                    }
                }
            }
        }
        val file = fileWith(useLocal)

        val chirPackage = DefaultCfir2ChirConverter().convert(listOf(file))
        val output = DefaultChirToJvmCodeGenerator().generate(ChirJvmCodegenInput(chirPackage))
        val artifactsByName = output.classes.associateBy { it.internalName }
        val facadeArtifact = requireNotNull(artifactsByName["sample/SampleCj"])

        val generatedClass = GeneratedClassLoader().define(facadeArtifact)
        val handle = generatedClass.getMethod("useLocal").invoke(null) as java.lang.invoke.MethodHandle
        assertEquals(5, handle.invoke())
    }

    /**
     * 构造带 dispatch receiver 的成员函数。
     */
    private fun memberFunction(
        name: String,
        receiverType: ConeSimpleCangJieType,
        body: CfirExpression,
    ): CfirNamedFunction {
        val symbol = CfirNamedFunctionSymbol(CallableId(PackageName, Name.identifier(name)))
        return buildNamedFunction {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = receiverType
            status = CfirDeclarationStatusImpl()
            returnTypeRef = intTypeRef()
            this.symbol = symbol
            this.name = Name.identifier(name)
            isMut = false
            this.body = buildBlock {
                coneTypeOrNull = ConePrimitiveType.INT32
                statements += body
            }
        }
    }

    /**
     * 构造返回指定 Int32 常量的测试函数。
     */
    private fun functionReturningInt(name: String, value: Int): CfirNamedFunction {
        val symbol = CfirNamedFunctionSymbol(CallableId(PackageName, Name.identifier(name)))
        return buildNamedFunction {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            returnTypeRef = intTypeRef()
            this.symbol = symbol
            this.name = Name.identifier(name)
            isMut = false
            body = buildBlock {
                coneTypeOrNull = ConePrimitiveType.INT32
                statements += buildLiteralExpression {
                    kind = CfirLiteralKind.INT
                    this.value = value
                    coneTypeOrNull = ConePrimitiveType.INT32
                }
            }
        }
    }

    /**
     * 构造包含局部变量初始化、赋值和读取的测试函数。
     */
    private fun functionWithLocalVariableAssignment(): CfirNamedFunction {
        val symbol = CfirNamedFunctionSymbol(CallableId(PackageName, Name.identifier("localAssignment")))
        val local = localIntVariable("x", 1)
        return buildNamedFunction {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            returnTypeRef = intTypeRef()
            this.symbol = symbol
            name = Name.identifier("localAssignment")
            isMut = false
            body = buildBlock {
                coneTypeOrNull = ConePrimitiveType.INT32
                statements += local
                statements += buildAssignment {
                    coneTypeOrNull = ConePrimitiveType.UNIT
                    lValue = namedAccess(local)
                    rValue = intLiteral(2)
                }
                statements += namedAccess(local)
            }
        }
    }

    /**
     * 构造调用另一个测试函数的命名函数。
     */
    private fun functionCalling(name: String, callee: CfirNamedFunction): CfirNamedFunction {
        val symbol = CfirNamedFunctionSymbol(CallableId(PackageName, Name.identifier(name)))
        return buildNamedFunction {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            returnTypeRef = intTypeRef()
            this.symbol = symbol
            this.name = Name.identifier(name)
            isMut = false
            body = buildBlock {
                coneTypeOrNull = ConePrimitiveType.INT32
                statements += buildFunctionCall {
                    coneTypeOrNull = ConePrimitiveType.INT32
                    calleeReference = buildResolvedNamedReference {
                        this.name = callee.name
                        resolvedSymbol = callee.symbol
                    }
                    argumentList = buildArgumentList()
                    origin = CfirFunctionCallOrigin.Regular
                }
            }
        }
    }

    /**
     * 构造包含指定函数声明的测试 CFIR 文件。
     */
    private fun fileWith(vararg declarations: CfirDeclaration): CfirFile {
        return buildFile {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            name = "sample.cj"
            packageDirective = buildPackageDirective {
                packageFqName = PackageName
                isMacroPackage = false
            }
            this.declarations += declarations
        }
    }

    /**
     * 构造带 Int32 初始化值的可变局部变量。
     */
    private fun localIntVariable(name: String, initialValue: Int): CfirFieldVariable {
        val variableName = Name.identifier(name)
        return buildFieldVariable {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            initializer = intLiteral(initialValue)
            isVar = true
            symbol = CfirFieldVariableSymbol(CallableId(PackageName, variableName))
            returnTypeRef = intTypeRef()
            this.name = variableName
        }
    }

    /**
     * 构造指向指定局部变量 symbol 的命名访问表达式。
     */
    private fun namedAccess(variable: CfirFieldVariable) = buildNamedAccessExpression {
        coneTypeOrNull = ConePrimitiveType.INT32
        calleeReference = buildResolvedNamedReference {
            name = variable.name
            resolvedSymbol = variable.symbol
        }
    }

    /**
     * 构造 Int32 字面量表达式。
     */
    private fun intLiteral(value: Int) = buildLiteralExpression {
        kind = CfirLiteralKind.INT
        this.value = value
        coneTypeOrNull = ConePrimitiveType.INT32
    }

    /**
     * 构造 Int32 resolved type ref。
     */
    private fun intTypeRef() = buildResolvedTypeRef {
        coneType = ConePrimitiveType.INT32
    }

    /**
     * JVM 管线测试使用的最小 source session。
     */
    private object TestSession : CfirSession(Kind.Source)

    /**
     * JVM 管线测试使用的最小 module data。
     */
    private object TestModuleData : CfirModuleData() {
        /**
         * 测试模块名称。
         */
        override val name: Name = Name.identifier("cfir2chir-test")
        /**
         * 测试模块没有普通依赖。
         */
        override val dependencies: List<CfirModuleData> = emptyList()
        /**
         * 测试模块没有 refinement 依赖。
         */
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        /**
         * 测试模块没有传递 refinement 依赖。
         */
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        /**
         * 测试模块使用默认仓颉平台。
         */
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        /**
         * 测试模块使用默认 CFIR 平台。
         */
        override val platform: CfirPlatform = CfirPlatform.DEFAULT
        /**
         * 测试模块是否为 common 模块。
         */
        override val isCommon: Boolean = targetPlatform.isCommon()
        /**
         * 测试模块不声明额外 capability。
         */
        override val capabilities: CfirModuleCapabilities = CfirModuleCapabilities.Empty
        /**
         * 测试模块稳定名称。
         */
        override val stableModuleName: String = "cfir2chir-test"
        /**
         * 测试模块绑定的 CFIR session。
         */
        override val session: CfirSession
            get() = TestSession

        init {
            bindSession(TestSession)
        }
    }

    /**
     * JVM 管线测试使用的空 scope provider：cfir2chir 不查询成员作用域，任何调用都是编程错误。
     */
    private object UnusedScopeProvider : CfirScopeProvider() {
        /**
         * use-site 成员 scope 查询在测试 fixture 中被禁止。
         */
        override fun getUseSiteMemberScope(
            klass: org.cangnova.cangjie.cfir.declarations.CfirClass,
            useSiteSession: CfirSession,
            scopeSession: ScopeSession,
        ): org.cangnova.cangjie.cfir.scopes.CfirTypeScope = error("scope provider must not be used by JVM pipeline tests")

        /**
         * typealias 构造器 scope 查询在测试 fixture 中被禁止。
         */
        override fun getTypealiasConstructorScope(
            typeAlias: org.cangnova.cangjie.cfir.declarations.CfirTypeAlias,
            useSiteSession: CfirSession,
            scopeSession: ScopeSession,
        ): org.cangnova.cangjie.cfir.scopes.CfirScope = error("scope provider must not be used by JVM pipeline tests")

        /**
         * declaration-site 成员 scope 查询在测试 fixture 中被禁止。
         */
        override fun getDeclarationSiteMemberScope(
            klass: org.cangnova.cangjie.cfir.declarations.CfirClass,
            useSiteSession: CfirSession,
            scopeSession: ScopeSession,
        ): org.cangnova.cangjie.cfir.scopes.CfirTypeScope = error("scope provider must not be used by JVM pipeline tests")
    }

    /**
     * 将生成的 JVM class bytes 直接定义到当前测试 class loader。
     */
    private class GeneratedClassLoader : ClassLoader(Cfir2ChirJvmPipelineTest::class.java.classLoader) {
        /**
         * 定义单个 JVM class file artifact 并返回加载后的 Class。
         */
        fun define(artifact: JvmClassFileArtifact): Class<*> {
            return defineClass(artifact.internalName.replace('/', '.'), artifact.bytes, 0, artifact.bytes.size)
        }
    }

    private companion object {
        val PackageName = FqName("sample")
        val BoxClassId = ClassId(PackageName, Name.identifier("Box"))
    }
}
