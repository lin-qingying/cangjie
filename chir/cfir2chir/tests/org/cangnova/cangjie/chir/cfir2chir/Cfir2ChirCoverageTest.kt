package org.cangnova.cangjie.chir.cfir2chir

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.common.CfirModuleCapabilities
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.builder.CfirCodeFragmentBuilder
import org.cangnova.cangjie.cfir.declarations.builder.CfirAnonymousFunctionBuilder
import org.cangnova.cangjie.cfir.declarations.builder.CfirClassBuilder
import org.cangnova.cangjie.cfir.declarations.builder.CfirExtendBuilder
import org.cangnova.cangjie.cfir.declarations.builder.CfirFieldVariableBuilder
import org.cangnova.cangjie.cfir.declarations.builder.CfirFileBuilder
import org.cangnova.cangjie.cfir.declarations.builder.CfirInterfaceBuilder
import org.cangnova.cangjie.cfir.declarations.builder.CfirNamedFunctionBuilder
import org.cangnova.cangjie.cfir.declarations.builder.CfirStructBuilder
import org.cangnova.cangjie.cfir.declarations.builder.CfirTypeAliasBuilder
import org.cangnova.cangjie.cfir.declarations.builder.buildAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildClass
import org.cangnova.cangjie.cfir.declarations.builder.buildCodeFragment
import org.cangnova.cangjie.cfir.declarations.builder.buildExtend
import org.cangnova.cangjie.cfir.declarations.builder.buildFieldVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildInterface
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.builder.buildStruct
import org.cangnova.cangjie.cfir.declarations.builder.buildTypeAlias
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOpKind
import org.cangnova.cangjie.cfir.expressions.CfirComparisonOp
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirSmartcastStability
import org.cangnova.cangjie.cfir.expressions.CfirCatch
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperationKind
import org.cangnova.cangjie.cfir.expressions.InaccessibleReceiverKind
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildArrayLiteral
import org.cangnova.cangjie.cfir.expressions.builder.buildAssignment
import org.cangnova.cangjie.cfir.expressions.builder.buildAugmentedAssignment
import org.cangnova.cangjie.cfir.expressions.builder.buildAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildBinaryOp
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildCatch
import org.cangnova.cangjie.cfir.expressions.builder.buildComparisonExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.buildThrowExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildTryExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildIfExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildIfAvailableExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedArgumentExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildRangeExpression
import org.cangnova.cangjie.cfir.expressions.buildInaccessibleReceiverExpression
import org.cangnova.cangjie.cfir.expressions.buildSmartCastExpression
import org.cangnova.cangjie.cfir.expressions.buildThisReceiverExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildStringInterpolation
import org.cangnova.cangjie.cfir.expressions.builder.buildSubscriptExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildSuperReceiverExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildTupleLiteral
import org.cangnova.cangjie.cfir.expressions.builder.buildTypeConversion
import org.cangnova.cangjie.cfir.expressions.builder.buildTypeOperator
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildSuperReference
import org.cangnova.cangjie.cfir.references.builder.buildThisReference
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCodeFragmentSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPrimitiveTypeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirStructSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.types.ConeAnyType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.declarations.builder.buildPatternBindingVariable
import org.cangnova.cangjie.cfir.patterns.builder.buildCatchPattern
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.controlflow.ChirCatchDispatchEntry
import org.cangnova.cangjie.chir.core.controlflow.ChirThrowTerminator
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.declaration.ChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirExtendDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirInterfaceDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirStructDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirTypeDefinitionKind
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.type.ChirClassType
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirNamedType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirTupleType
import org.cangnova.cangjie.chir.core.type.ChirVArrayType
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.isCommon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 覆盖 CFIR 到 CHIR 转换中尚未接入真实 testData 的关键 lowering 分支。
 */
class Cfir2ChirCoverageTest {
    /**
     * 验证带值 if 表达式会生成 then/else block 和 phi 操作。
     */
    @Test
    fun `lowers value if expression into CHIR phi`() {
        val condition = buildComparisonExpression {
            coneTypeOrNull = ConePrimitiveType.BOOLEAN
            operation = CfirComparisonOp.LT
            left = intLiteral(1)
            right = intLiteral(2)
        }
        val function = functionReturningExpression(
            name = "choose",
            returnType = ConePrimitiveType.INT32,
            expression = buildIfExpression {
                coneTypeOrNull = ConePrimitiveType.INT32
                this.condition = condition
                thenBranch = blockOf(ConePrimitiveType.INT32, intLiteral(10))
                elseBranch = intLiteral(20)
            },
        )

        val chirFunction = convertSingleFunction(function)

        assertTrue(chirFunction.blocks.any { it.name == "then" })
        assertTrue(chirFunction.blocks.any { it.name == "else" })
        assertTrue(chirFunction.otherOperations().contains(Cfir2ChirOperation.PHI.canonicalName))
    }

    /**
     * 验证非 Unit if 分支 fallthrough 缺值时不会静默生成错误 CHIR。
     */
    @Test
    fun `rejects non-unit if branch that falls through without value`() {
        val condition = buildComparisonExpression {
            coneTypeOrNull = ConePrimitiveType.BOOLEAN
            operation = CfirComparisonOp.LT
            left = intLiteral(1)
            right = intLiteral(2)
        }
        val function = functionReturningExpression(
            name = "badIf",
            returnType = ConePrimitiveType.INT32,
            expression = buildIfExpression {
                coneTypeOrNull = ConePrimitiveType.INT32
                this.condition = condition
                thenBranch = blockOf(ConePrimitiveType.UNIT)
                elseBranch = intLiteral(20)
            },
        )

        assertThrows(Cfir2ChirConversionException::class.java) {
            convertSingleFunction(function)
        }
    }

    /**
     * 验证 `&&` 短路二元操作会降级为"条件分支 + 显式布尔常量 + phi"控制流。
     */
    @Test
    fun `lowers short-circuit AND into explicit control flow with phi`() {
        val function = functionReturningExpression(
            name = "andShortCircuit",
            returnType = ConePrimitiveType.BOOLEAN,
            expression = buildBinaryOp {
                coneTypeOrNull = ConePrimitiveType.BOOLEAN
                kind = CfirBinaryOpKind.AND
                left = boolLiteral(false)
                right = boolLiteral(true)
            },
        )

        val chirFunction = convertSingleFunction(function)

        // 不再保留为 cfir.binary_op 占位操作。
        assertTrue(Cfir2ChirOperation.CFIR_BINARY_OP.canonicalName !in chirFunction.otherOperations())
        // rhs 求值块与 merge 块存在，短路边携带显式 false 常量，merge 处为 phi。
        assertTrue(chirFunction.blocks.any { it.name == "logical_rhs" })
        val mergeBlock = chirFunction.blocks.single { it.name == "logical_cont" }
        val phi = mergeBlock.expressions
            .filterIsInstance<ChirOtherExpression>()
            .single { it.operation == "phi" }
        val shortCircuitIncoming = phi.operands.single { it is ChirConstantValue && it.literal == "false" }
        assertTrue(
            shortCircuitIncoming.attributes.any { it is ChirStringAttribute && it.key == "pred" },
        )
    }

    /**
     * 验证 `||` 短路二元操作会降级为"条件分支 + 显式布尔常量 + phi"控制流。
     */
    @Test
    fun `lowers short-circuit OR into explicit control flow with phi`() {
        val function = functionReturningExpression(
            name = "orShortCircuit",
            returnType = ConePrimitiveType.BOOLEAN,
            expression = buildBinaryOp {
                coneTypeOrNull = ConePrimitiveType.BOOLEAN
                kind = CfirBinaryOpKind.OR
                left = boolLiteral(true)
                right = boolLiteral(false)
            },
        )

        val chirFunction = convertSingleFunction(function)

        assertTrue(Cfir2ChirOperation.CFIR_BINARY_OP.canonicalName !in chirFunction.otherOperations())
        assertTrue(chirFunction.blocks.any { it.name == "logical_rhs" })
        val mergeBlock = chirFunction.blocks.single { it.name == "logical_cont" }
        val phi = mergeBlock.expressions
            .filterIsInstance<ChirOtherExpression>()
            .single { it.operation == "phi" }
        val shortCircuitIncoming = phi.operands.single { it is ChirConstantValue && it.literal == "true" }
        assertTrue(
            shortCircuitIncoming.attributes.any { it is ChirStringAttribute && it.key == "pred" },
        )
    }

    /**
     * 验证非 bool 结果的短路二元操作会被拒绝而不是静默生成错误 CHIR。
     */
    @Test
    fun `rejects short-circuit binary op with non-bool result type`() {
        val function = functionReturningExpression(
            name = "badShortCircuit",
            returnType = ConePrimitiveType.BOOLEAN,
            expression = buildBinaryOp {
                coneTypeOrNull = ConePrimitiveType.INT32
                kind = CfirBinaryOpKind.AND
                left = boolLiteral(true)
                right = boolLiteral(false)
            },
        )

        assertThrows(Cfir2ChirConversionException::class.java) {
            convertSingleFunction(function)
        }
    }

    /**
     * 验证成员函数调用（隐式 this dispatch receiver）会降级为带 receiver 首实参的
     * `ChirCallExpression`，而不是保留为 `cfir.call_with_receiver` 占位操作。
     */
    @Test
    fun `lowers member function call with dispatch receiver into receiver-first call`() {
        val boxType = ConeClassLikeType(ConeClassLikeLookupTagImpl(classId("Box")))
        val answer = memberFunctionReturningInt(
            name = "answer",
            receiverType = boxType,
            value = 42,
        )
        val caller = memberFunctionCallingMember(
            callerName = "callAnswer",
            callee = answer,
            receiverType = boxType,
        )

        val module = convertModule(answer, caller)
        val chirCaller = module.declarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .single { it.name == "callAnswer" }

        // 不再保留为 cfir.call_with_receiver 占位操作。
        assertTrue(Cfir2ChirOperation.CFIR_CALL_WITH_RECEIVER.canonicalName !in chirCaller.otherOperations())

        val call = chirCaller.blocks
            .flatMap { it.expressions }
            .filterIsInstance<ChirCallExpression>()
            .single()
        val calleeType = (call.callee.type as ChirResolvedTypeRef).type as ChirFunctionType
        assertTrue(calleeType.receiverType != null)
        // receiver 是第一实参，其余实参为空（无参成员函数）。
        assertEquals(1, call.arguments.size)
        assertEquals(calleeType.receiverType, call.arguments.single().type)
    }

    /**
     * 验证成员函数体内的 this 表达式会降级为名为 `this` 的局部值引用，
     * 与成员函数 header 的 receiverType 一致；而不是保留为 cfir.this_receiver 占位操作。
     */
    @Test
    fun `lowers this receiver in member function into named this local value`() {
        val boxType = ConeClassLikeType(ConeClassLikeLookupTagImpl(classId("Box")))
        val answer = memberFunctionReturningInt(
            name = "answer",
            receiverType = boxType,
            value = 42,
        )
        val caller = memberFunctionCallingMember(
            callerName = "callAnswer",
            callee = answer,
            receiverType = boxType,
        )

        val module = convertModule(answer, caller)
        val chirCaller = module.declarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .single { it.name == "callAnswer" }

        // this 已不再是占位操作。
        assertTrue(Cfir2ChirOperation.CFIR_THIS_RECEIVER.canonicalName !in chirCaller.otherOperations())
        // 成员调用的第一实参就是名为 this 的局部值。
        val call = chirCaller.blocks
            .flatMap { it.expressions }
            .filterIsInstance<ChirCallExpression>()
            .single()
        val receiver = call.arguments.single() as ChirLocalValue
        assertEquals("this", receiver.name)
        assertEquals(ChirResolvedTypeRef(ChirClassType("sample.Box")), receiver.type)
    }

    /**
     * 验证无 receiverType 的函数体内出现 this 时保留为占位操作，由后端诊断。
     */
    @Test
    fun `keeps this receiver as placeholder outside member function`() {
        val function = functionReturningExpression(
            name = "staticThis",
            returnType = ConePrimitiveType.INT32,
            expression = buildThisReceiverExpression {
                coneTypeOrNull = ConeStructType(ConeClassLikeLookupTagImpl(classId("Self")))
                calleeReference = buildThisReference {
                    boundSymbol = CfirStructSymbol(classId("Self"))
                    isImplicit = false
                }
            },
        )

        val chirFunction = convertSingleFunction(function)

        assertTrue(Cfir2ChirOperation.CFIR_THIS_RECEIVER.canonicalName in chirFunction.otherOperations())
    }

    /**
     * 验证成员字段读取会降级为带 jvm.owner/jvm.name ABI 的 `jvm.getfield`，
     * 而不是保留为 cfir.member_access 占位操作。
     */
    @Test
    fun `lowers member field read into jvm getfield with abi attributes`() {
        val boxType = ConeClassLikeType(ConeClassLikeLookupTagImpl(classId("Box")))
        val valueField = field("value", ConePrimitiveType.INT32, mutable = true)
        val reader = memberFunction(
            name = "readValue",
            receiverType = boxType,
            body = buildNamedAccessExpression {
                coneTypeOrNull = ConePrimitiveType.INT32
                calleeReference = buildResolvedNamedReference {
                    name = valueField.name
                    resolvedSymbol = valueField.symbol
                }
                dispatchReceiver = buildThisReceiverExpression {
                    coneTypeOrNull = boxType
                    calleeReference = buildThisReference {
                        boundSymbol = CfirClassSymbol(classId("Box"))
                        isImplicit = true
                    }
                }
            },
        )
        val box = buildClass {
            commonClassLikeDeclaration("Box", CfirClassSymbol(classId("Box")))
            declarations += valueField
            declarations += reader
        }

        val module = convertModule(box)
        val boxDeclaration = module.declarations.filterIsInstance<ChirClassDeclaration>().single()
        val chirReader = boxDeclaration.memberDeclarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .single { it.name == "readValue" }

        assertTrue(Cfir2ChirOperation.CFIR_MEMBER_ACCESS.canonicalName !in chirReader.otherOperations())
        val getField = chirReader.blocks
            .flatMap { it.expressions }
            .filterIsInstance<ChirOtherExpression>()
            .single { it.operation == "jvm.getfield" }
        assertTrue(
            getField.attributes.any { it is ChirStringAttribute && it.key == "jvm.owner" && it.value == "sample/Box" },
        )
        assertTrue(
            getField.attributes.any { it is ChirStringAttribute && it.key == "jvm.name" && it.value == "value" },
        )
    }

    /**
     * 验证成员字段赋值会降级为 `jvm.putfield`，而不是保留为 cfir.member_assign 占位操作。
     */
    @Test
    fun `lowers member field write into jvm putfield`() {
        val boxType = ConeClassLikeType(ConeClassLikeLookupTagImpl(classId("Box")))
        val valueField = field("value", ConePrimitiveType.INT32, mutable = true)
        val writer = memberFunction(
            name = "writeValue",
            receiverType = boxType,
            body = buildAssignment {
                coneTypeOrNull = ConePrimitiveType.UNIT
                lValue = buildNamedAccessExpression {
                    coneTypeOrNull = ConePrimitiveType.INT32
                    calleeReference = buildResolvedNamedReference {
                        name = valueField.name
                        resolvedSymbol = valueField.symbol
                    }
                    dispatchReceiver = buildThisReceiverExpression {
                        coneTypeOrNull = boxType
                        calleeReference = buildThisReference {
                            boundSymbol = CfirClassSymbol(classId("Box"))
                            isImplicit = true
                        }
                    }
                }
                rValue = intLiteral(7)
            },
        )
        val box = buildClass {
            commonClassLikeDeclaration("Box", CfirClassSymbol(classId("Box")))
            declarations += valueField
            declarations += writer
        }

        val module = convertModule(box)
        val boxDeclaration = module.declarations.filterIsInstance<ChirClassDeclaration>().single()
        val chirWriter = boxDeclaration.memberDeclarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .single { it.name == "writeValue" }

        assertTrue(Cfir2ChirOperation.CFIR_MEMBER_ASSIGN.canonicalName !in chirWriter.otherOperations())
        val putField = chirWriter.blocks
            .flatMap { it.expressions }
            .filterIsInstance<ChirOtherExpression>()
            .single { it.operation == "jvm.putfield" }
        assertEquals(2, putField.operands.size)
        assertTrue(
            putField.attributes.any { it is ChirStringAttribute && it.key == "jvm.owner" && it.value == "sample/Box" },
        )
    }

    /**
     * 验证单下标数组读取会降级为 `jvm.arrayLoad`，
     * 而不是保留为 cfir.subscript 占位操作。
     */
    @Test
    fun `lowers single index array read into jvm arrayLoad`() {
        val intArrayType = ConeVArrayType(ConePrimitiveType.INT32, size = 3)
        val arraySymbol = CfirFieldVariableSymbol(CallableId(PackageName, Name.identifier("arr")))
        val arrayVariable = buildFieldVariable {
            commonFieldVariableDeclaration(arraySymbol)
            name = Name.identifier("arr")
            returnTypeRef = typeRef(intArrayType)
            initializer = null
            isVar = true
        }
        val function = functionWithBody(
            name = "readIndex",
            returnType = ConePrimitiveType.INT32,
            bodyType = ConePrimitiveType.INT32,
            statements = listOf(
                arrayVariable,
                buildSubscriptExpression {
                    coneTypeOrNull = ConePrimitiveType.INT32
                    receiver = buildNamedAccessExpression {
                        coneTypeOrNull = intArrayType
                        calleeReference = buildResolvedNamedReference {
                            name = arrayVariable.name
                            resolvedSymbol = arraySymbol
                        }
                    }
                    indices += intLiteral(1)
                },
            ),
        )

        val chirFunction = convertSingleFunction(function)
        val otherExpressions = chirFunction.blocks
            .flatMap { it.expressions }
            .filterIsInstance<ChirOtherExpression>()

        assertTrue(Cfir2ChirOperation.CFIR_SUBSCRIPT.canonicalName !in otherExpressions.map { it.operation })
        val arrayLoad = otherExpressions.single { it.operation == "jvm.arrayLoad" }
        assertEquals(2, arrayLoad.operands.size)
        assertEquals(ChirResolvedTypeRef(ChirPrimitiveType.INT32), arrayLoad.resultType)
    }

    /**
     * 验证数组下标赋值会降级为 `jvm.arrayStore`，
     * 而不是保留为 cfir.member_assign 占位操作。
     */
    @Test
    fun `lowers array index assignment into jvm arrayStore`() {
        val intArrayType = ConeVArrayType(ConePrimitiveType.INT32, size = 3)
        val arraySymbol = CfirFieldVariableSymbol(CallableId(PackageName, Name.identifier("arr")))
        val arrayVariable = buildFieldVariable {
            commonFieldVariableDeclaration(arraySymbol)
            name = Name.identifier("arr")
            returnTypeRef = typeRef(intArrayType)
            initializer = null
            isVar = true
        }
        val function = functionWithBody(
            name = "writeIndex",
            returnType = ConePrimitiveType.INT32,
            bodyType = ConePrimitiveType.INT32,
            statements = listOf(
                arrayVariable,
                buildAssignment {
                    coneTypeOrNull = ConePrimitiveType.UNIT
                    lValue = buildSubscriptExpression {
                        coneTypeOrNull = ConePrimitiveType.INT32
                        receiver = buildNamedAccessExpression {
                            coneTypeOrNull = intArrayType
                            calleeReference = buildResolvedNamedReference {
                                name = arrayVariable.name
                                resolvedSymbol = arraySymbol
                            }
                        }
                        indices += intLiteral(0)
                    }
                    rValue = intLiteral(7)
                },
                intLiteral(0),
            ),
        )

        val chirFunction = convertSingleFunction(function)
        val otherExpressions = chirFunction.blocks
            .flatMap { it.expressions }
            .filterIsInstance<ChirOtherExpression>()

        assertTrue(Cfir2ChirOperation.CFIR_MEMBER_ASSIGN.canonicalName !in otherExpressions.map { it.operation })
        assertTrue(Cfir2ChirOperation.CFIR_SUBSCRIPT.canonicalName !in otherExpressions.map { it.operation })
        val arrayStore = otherExpressions.single { it.operation == "jvm.arrayStore" }
        assertEquals(3, arrayStore.operands.size)
        assertEquals(null, arrayStore.resultType)
    }

    /**
     * 验证成员函数体内的 super 接收者会降级为名为 `this` 的局部值引用
     * （与 this 降级共用 JVM slot 0 绑定），而不是保留为占位操作。
     */
    @Test
    fun `lowers super receiver in member function into this local value`() {
        val boxType = ConeClassLikeType(ConeClassLikeLookupTagImpl(classId("Box")))
        val baseType = ConeClassLikeType(ConeClassLikeLookupTagImpl(classId("Base")))
        val superUser = memberFunction(
            name = "useSuper",
            receiverType = boxType,
            body = buildSuperReceiverExpression {
                coneTypeOrNull = baseType
                calleeReference = buildSuperReference {
                    superTypeRef = typeRef(baseType)
                }
            },
        )

        val module = convertModule(superUser)
        val chirUser = module.declarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .single { it.name == "useSuper" }

        // super 已不再是占位操作，而是名为 this 的局部值引用。
        assertTrue(Cfir2ChirOperation.CFIR_SUPER_RECEIVER.canonicalName !in chirUser.otherOperations())
    }

    /**
     * 验证引用类型 smart cast 会降级为带 jvm.type ABI 的 `jvm.checkcast`，
     * 数值类型 smart cast 保留为占位操作。
     */
    @Test
    fun `lowers reference smart cast into jvm checkcast`() {
        val anyType = ConeClassLikeType(ConeClassLikeLookupTagImpl(classId("Any")))
        val boxType = ConeClassLikeType(ConeClassLikeLookupTagImpl(classId("Box")))
        val referenceCast = memberFunctionReturningExpression(
            name = "referenceCast",
            receiverType = boxType,
            expression = buildSmartCastExpression {
                coneTypeOrNull = boxType
                originalExpression = buildThisReceiverExpression {
                    coneTypeOrNull = anyType
                    calleeReference = buildThisReference {
                        boundSymbol = CfirClassSymbol(classId("Any"))
                        isImplicit = false
                    }
                }
                smartcastType = typeRef(boxType)
                upperTypesFromSmartCast = listOf(boxType)
                lowerTypesFromSmartCast = emptyList()
                smartcastStability = CfirSmartcastStability.STABLE_VALUE
            },
        )
        val primitiveCast = functionReturningExpression(
            name = "primitiveCast",
            returnType = ConePrimitiveType.INT64,
            expression = buildSmartCastExpression {
                coneTypeOrNull = ConePrimitiveType.INT64
                originalExpression = intLiteral(1)
                smartcastType = typeRef(ConePrimitiveType.INT64)
                upperTypesFromSmartCast = listOf(ConePrimitiveType.INT64)
                lowerTypesFromSmartCast = listOf(ConePrimitiveType.INT32)
                smartcastStability = CfirSmartcastStability.STABLE_VALUE
            },
        )

        val module = convertModule(referenceCast, primitiveCast)
        val functionsByName = module.declarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .associateBy { it.name }

        val referenceCastFunction = requireNotNull(functionsByName["referenceCast"])
        val referenceOther = referenceCastFunction.blocks
            .flatMap { it.expressions }
            .filterIsInstance<ChirOtherExpression>()
        assertTrue(Cfir2ChirOperation.CFIR_SMART_CAST.canonicalName !in referenceCastFunction.otherOperations())
        val checkCast = referenceOther.single { it.operation == "jvm.checkcast" }
        assertTrue(
            checkCast.attributes.any { it is ChirStringAttribute && it.key == "jvm.type" && it.value == "sample/Box" },
        )

        val primitiveCastFunction = requireNotNull(functionsByName["primitiveCast"])
        assertTrue(Cfir2ChirOperation.CFIR_SMART_CAST.canonicalName in primitiveCastFunction.otherOperations())
    }

    /**
     * 构造带 dispatch receiver、返回指定表达式值的成员函数。
     */
    private fun memberFunctionReturningExpression(
        name: String,
        receiverType: ConeSimpleCangJieType,
        expression: CfirExpression,
    ): CfirNamedFunction {
        val symbol = CfirNamedFunctionSymbol(CallableId(PackageName, Name.identifier(name)))
        return buildNamedFunction {
            commonNamedFunctionDeclaration(symbol)
            this.name = Name.identifier(name)
            dispatchReceiverType = receiverType
            returnTypeRef = typeRef(ConePrimitiveType.INT32)
            body = blockOf(ConePrimitiveType.INT32, expression, intLiteral(0))
        }
    }

    /**
     * 验证 VArray 字面量会降级为 `jvm.newarray` + 逐元素 `jvm.arrayStore` 序列，
     * 而不是保留为 cfir.array_literal 占位操作。
     */
    @Test
    fun `lowers varray literal into newarray and array store sequence`() {
        val intArrayType = ConeVArrayType(ConePrimitiveType.INT32, size = 3)
        val function = functionReturningExpression(
            name = "intArray",
            returnType = ConePrimitiveType.INT32,
            expression = buildArrayLiteral {
                coneTypeOrNull = intArrayType
                elements += intLiteral(10)
                elements += intLiteral(20)
                elements += intLiteral(30)
            },
        )

        val chirFunction = convertSingleFunction(function)
        val otherExpressions = chirFunction.blocks
            .flatMap { it.expressions }
            .filterIsInstance<ChirOtherExpression>()

        assertTrue(Cfir2ChirOperation.CFIR_ARRAY_LITERAL.canonicalName !in otherExpressions.map { it.operation })
        val newArray = otherExpressions.single { it.operation == "jvm.newarray" }
        assertEquals(1, newArray.operands.size)
        assertEquals("3", (newArray.operands.single() as ChirConstantValue).literal)
        val stores = otherExpressions.filter { it.operation == "jvm.arrayStore" }
        assertEquals(3, stores.size)
        stores.forEachIndexed { index, store ->
            assertEquals(3, store.operands.size)
            assertEquals(index.toString(), (store.operands[1] as ChirConstantValue).literal)
        }
    }

    /**
     * 验证 try-catch 的异常绑定降级：try 内 throw 携带 unwind 目标块与绑定名，
     * catch 块为 pattern binding variable 注册局部 slot。
     */
    @Test
    fun `lowers try catch with exception binding on unwind edges`() {
        val exceptionType = ConeClassLikeType(ConeClassLikeLookupTagImpl(classId("MyError")))
        val bindingSymbol = CfirPatternBindingSymbol(
            CallableId(PackageName, Name.identifier("err")),
        )
        val errBinding = buildPatternBindingVariable {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            status = CfirDeclarationStatusImpl()
            returnTypeRef = typeRef(exceptionType)
            isVar = false
            symbol = bindingSymbol
            name = Name.identifier("err")
        }
        val tryExpression = buildTryExpression {
            coneTypeOrNull = exceptionType
            tryBlock = buildBlock {
                coneTypeOrNull = ConePrimitiveType.UNIT
                statements += buildThrowExpression {
                    coneTypeOrNull = ConePrimitiveType.NOTHING
                    exception = buildNamedAccessExpression {
                        coneTypeOrNull = exceptionType
                        calleeReference = buildResolvedNamedReference {
                            name = Name.identifier("failure")
                            resolvedSymbol = CfirNamedFunctionSymbol(CallableId(PackageName, Name.identifier("failure")))
                        }
                    }
                }
            }
            catches += buildCatch {
                coneTypeOrNull = exceptionType
                pattern = buildCatchPattern {
                    bindingName = Name.identifier("err")
                    isWildcard = false
                    resolvedTypeRef = org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef {
                        coneType = exceptionType
                    }
                    this.bindingVariable = errBinding
                }
                body = buildBlock {
                    coneTypeOrNull = exceptionType
                    statements += buildNamedAccessExpression {
                        coneTypeOrNull = exceptionType
                        calleeReference = buildResolvedNamedReference {
                            name = errBinding.name
                            resolvedSymbol = bindingSymbol
                        }
                    }
                }
            }
        }
        val function = functionReturningExpression(
            name = "guarded",
            returnType = ConePrimitiveType.INT32,
            expression = tryExpression,
        )

        val chirFunction = convertSingleFunction(function)

        // catch 块存在；try 内 throw 携带 unwind 目标与绑定名。
        val catchBlock = chirFunction.blocks.single { it.name == "catch_0" }
        val tryBlock = chirFunction.blocks.single { it.name == "try" }
        val throwTerminator = tryBlock.terminator as ChirThrowTerminator
        assertEquals(catchBlock.semanticId, throwTerminator.unwindTargetBlockId)
        assertEquals("err", throwTerminator.unwindExceptionBinding)
        // catch 体读取绑定变量：load 的地址名即绑定名。
        val loadOperations = catchBlock.expressions
            .filterIsInstance<ChirMemoryExpression>()
            .filter { it.operation == "load" }
        assertTrue(loadOperations.isNotEmpty())
        assertTrue(loadOperations.all { (it.address as? ChirLocalValue)?.name == "err" })
    }

    /**
     * 验证多 catch 场景的 throw 携带完整类型分派表：
     * 每个分派条目对应对应 catch 块、绑定与异常类型名。
     */
    @Test
    fun `lowers multi catch throws with typed dispatch table`() {
        val errorType = ConeClassLikeType(ConeClassLikeLookupTagImpl(classId("MyError")))
        val warnType = ConeClassLikeType(ConeClassLikeLookupTagImpl(classId("MyWarn")))
        fun catchOf(type: org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType, binding: String): CfirCatch {
            val symbol = CfirPatternBindingSymbol(CallableId(PackageName, Name.identifier(binding)))
            val bindingVariable = buildPatternBindingVariable {
                moduleData = TestModuleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Synthetic.Default
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = true
                status = CfirDeclarationStatusImpl()
                returnTypeRef = typeRef(type)
                isVar = false
                this.symbol = symbol
                name = Name.identifier(binding)
            }
            return buildCatch {
                coneTypeOrNull = type
                pattern = buildCatchPattern {
                    bindingName = Name.identifier(binding)
                    isWildcard = false
                    resolvedTypeRef = org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef {
                        coneType = type
                    }
                    this.bindingVariable = bindingVariable
                }
                body = buildBlock {
                    coneTypeOrNull = ConePrimitiveType.INT32
                    statements += intLiteral(1)
                }
            }
        }

        val tryExpression = buildTryExpression {
            coneTypeOrNull = ConePrimitiveType.INT32
            tryBlock = buildBlock {
                coneTypeOrNull = ConePrimitiveType.NOTHING
                statements += buildThrowExpression {
                    coneTypeOrNull = ConePrimitiveType.NOTHING
                    exception = buildThisReceiverExpression {
                        coneTypeOrNull = errorType
                        calleeReference = buildThisReference {
                            boundSymbol = CfirClassSymbol(classId("MyError"))
                            isImplicit = false
                        }
                    }
                }
            }
            catches += catchOf(errorType, "fatalErr")
            catches += catchOf(warnType, "warnErr")
        }
        val function = functionReturningExpression(
            name = "multiGuarded",
            returnType = ConePrimitiveType.INT32,
            expression = tryExpression,
        )

        val chirFunction = convertSingleFunction(function)

        val tryBlock = chirFunction.blocks.single { it.name == "try" }
        val throwTerminator = tryBlock.terminator as ChirThrowTerminator
        assertEquals(2, throwTerminator.catchDispatches.size)

        val fatalDispatch = throwTerminator.catchDispatches.single { it.exceptionTypeName == "sample.MyError" }
        assertEquals("fatalErr", fatalDispatch.exceptionBinding)
        val catchFatalBlock = chirFunction.blocks.single { it.semanticId == fatalDispatch.catchBlockId }
        assertEquals("catch_0", catchFatalBlock.name)

        val warnDispatch = throwTerminator.catchDispatches.single { it.exceptionTypeName == "sample.MyWarn" }
        assertEquals("warnErr", warnDispatch.exceptionBinding)
        val catchWarnBlock = chirFunction.blocks.single { it.semanticId == warnDispatch.catchBlockId }
        assertEquals("catch_1", catchWarnBlock.name)
    }

    /**
     * 验证元素类型与数组元素类型不一致的字面量保留为占位操作。
     */
    @Test
    fun `keeps mismatched element array literal as placeholder`() {
        val boolArrayType = ConeVArrayType(ConePrimitiveType.BOOLEAN, size = 2)
        val function = functionReturningExpression(
            name = "mixedArray",
            returnType = ConePrimitiveType.INT32,
            expression = buildArrayLiteral {
                coneTypeOrNull = boolArrayType
                elements += boolLiteral(true)
                elements += intLiteral(2)
            },
        )

        val chirFunction = convertSingleFunction(function)

        assertTrue(Cfir2ChirOperation.CFIR_ARRAY_LITERAL.canonicalName in chirFunction.otherOperations())
    }

    /**
     * 验证字符串插值会降级为 build → append* → result 的 intrinsic 序列，
     * 而不是保留为 cfir.string_interpolation 占位操作。
     */
    @Test
    fun `lowers string interpolation into build append result intrinsic sequence`() {
        val stringType = ConeClassLikeType(ConeClassLikeLookupTagImpl(classId("String")))
        val function = functionReturningExpression(
            name = "greet",
            returnType = ConePrimitiveType.INT32,
            expression = buildStringInterpolation {
                coneTypeOrNull = stringType
                parts += stringLiteral("value = ")
                parts += intLiteral(42)
            },
        )

        val chirFunction = convertSingleFunction(function)
        val otherExpressions = chirFunction.blocks
            .flatMap { it.expressions }
            .filterIsInstance<ChirOtherExpression>()

        assertTrue(Cfir2ChirOperation.CFIR_STRING_INTERPOLATION.canonicalName !in otherExpressions.map { it.operation })
        val build = otherExpressions.single { it.operation == "chir.string_build" }
        assertEquals("chir.StringBuilder", build.resultType?.renderName)
        val appends = otherExpressions.filter { it.operation == "chir.string_append" }
        assertEquals(2, appends.size)
        appends.forEach { append ->
            assertEquals(2, append.operands.size)
            assertEquals("chir.StringBuilder", append.operands.first().type.renderName)
        }
        val result = otherExpressions.single { it.operation == "chir.string_result" }
        assertEquals("sample.String", result.resultType?.renderName)
    }

    /**
     * 验证元素类型逐位匹配的 tuple 字面量会降级为 `chir.tuple_pack`，
     * 而不是保留为 cfir.tuple_literal 占位操作。
     */
    @Test
    fun `lowers matching tuple literal into chir tuple_pack`() {
        val intBoolTuple = ConeTupleType(listOf(ConePrimitiveType.INT32, ConePrimitiveType.BOOLEAN))
        val function = functionReturningExpression(
            name = "makePair",
            returnType = ConePrimitiveType.INT32,
            expression = buildTupleLiteral {
                coneTypeOrNull = intBoolTuple
                elements += intLiteral(7)
                elements += boolLiteral(true)
            },
        )

        val chirFunction = convertSingleFunction(function)
        val otherExpressions = chirFunction.blocks
            .flatMap { it.expressions }
            .filterIsInstance<ChirOtherExpression>()

        assertTrue(Cfir2ChirOperation.CFIR_TUPLE_LITERAL.canonicalName !in otherExpressions.map { it.operation })
        val pack = otherExpressions.single { it.operation == "chir.tuple_pack" }
        assertEquals(2, pack.operands.size)
        assertEquals("(int32, bool)", pack.resultType?.renderName)
    }

    /**
     * 验证 finally 块会内联到 try 与 catch 的每个 fallthrough 出口：
     * finally 内的局部 store 在两个出口各出现一次。
     */
    @Test
    fun `inlines finally into try and catch fallthrough exits`() {
        val intType = ConePrimitiveType.INT32
        val cleanupSymbol = CfirFieldVariableSymbol(CallableId(PackageName, Name.identifier("cleanupCount")))
        val tryExpression = buildTryExpression {
            coneTypeOrNull = intType
            tryBlock = buildBlock {
                coneTypeOrNull = intType
                statements += intLiteral(1)
            }
            catches += buildCatch {
                coneTypeOrNull = intType
                pattern = buildCatchPattern {
                    isWildcard = true
                }
                body = buildBlock {
                    coneTypeOrNull = intType
                    statements += intLiteral(2)
                }
            }
            finallyBlock = buildBlock {
                coneTypeOrNull = ConePrimitiveType.UNIT
                statements += buildFieldVariable {
                    commonFieldVariableDeclaration(cleanupSymbol)
                    isLocal = true
                    name = Name.identifier("cleanupCount")
                    returnTypeRef = typeRef(intType)
                    initializer = intLiteral(1)
                    isVar = true
                }
            }
        }
        val function = functionReturningExpression(
            name = "withFinally",
            returnType = intType,
            expression = tryExpression,
        )

        val chirFunction = convertSingleFunction(function)

        // finally 内联后不再存在独立的 finally 块。
        assertTrue(chirFunction.blocks.none { it.name.startsWith("finally") })
        // try 与 catch 两个出口块各包含一次 finally 内局部变量的初始化 store。
        val tryBlock = chirFunction.blocks.single { it.name == "try" }
        val catchBlock = chirFunction.blocks.single { it.name == "catch_0" }
        val tryStores = tryBlock.expressions
            .filterIsInstance<ChirMemoryExpression>()
            .filter { it.operation == "store" }
        val catchStores = catchBlock.expressions
            .filterIsInstance<ChirMemoryExpression>()
            .filter { it.operation == "store" }
        assertEquals(1, tryStores.size)
        assertEquals(1, catchStores.size)
    }

    /**
     * 构造字符串字面量片段。
     */
    private fun stringLiteral(value: String) = buildLiteralExpression {
        kind = CfirLiteralKind.STRING
        this.value = value
        coneTypeOrNull = ConeClassLikeType(ConeClassLikeLookupTagImpl(classId("String")))
    }

    /**
     * 构造带 dispatch receiver 与指定 body 语句的成员函数。
     *
     * 赋值类语句不产出值，body 末尾追加 Int32 字面量 0 兜底返回。
     */
    private fun memberFunction(
        name: String,
        receiverType: ConeSimpleCangJieType,
        body: CfirStatement,
    ): CfirNamedFunction {
        val symbol = CfirNamedFunctionSymbol(CallableId(PackageName, Name.identifier(name)))
        return buildNamedFunction {
            commonNamedFunctionDeclaration(symbol)
            this.name = Name.identifier(name)
            dispatchReceiverType = receiverType
            returnTypeRef = typeRef(ConePrimitiveType.INT32)
            this.body = buildBlock {
                coneTypeOrNull = ConePrimitiveType.INT32
                statements += body
                statements += intLiteral(0)
            }
        }
    }

    /**
     * 构造带 dispatch receiver 的成员函数，返回指定 Int32 常量。
     */
    private fun memberFunctionReturningInt(
        name: String,
        receiverType: ConeSimpleCangJieType,
        value: Int,
    ): CfirNamedFunction {
        val symbol = CfirNamedFunctionSymbol(CallableId(PackageName, Name.identifier(name)))
        return buildNamedFunction {
            commonNamedFunctionDeclaration(symbol)
            this.name = Name.identifier(name)
            dispatchReceiverType = receiverType
            returnTypeRef = intTypeRef()
            body = blockOf(ConePrimitiveType.INT32, intLiteral(value))
        }
    }

    /**
     * 构造通过隐式 this 调用指定成员函数的调用方成员函数。
     */
    private fun memberFunctionCallingMember(
        callerName: String,
        callee: CfirNamedFunction,
        receiverType: ConeSimpleCangJieType,
    ): CfirNamedFunction {
        val symbol = CfirNamedFunctionSymbol(CallableId(PackageName, Name.identifier(callerName)))
        return buildNamedFunction {
            commonNamedFunctionDeclaration(symbol)
            this.name = Name.identifier(callerName)
            dispatchReceiverType = receiverType
            returnTypeRef = intTypeRef()
            body = blockOf(
                ConePrimitiveType.INT32,
                buildFunctionCall {
                    coneTypeOrNull = ConePrimitiveType.INT32
                    calleeReference = buildResolvedNamedReference {
                        this.name = callee.name
                        resolvedSymbol = callee.symbol
                    }
                    dispatchReceiver = buildThisReceiverExpression {
                        coneTypeOrNull = receiverType
                        calleeReference = buildThisReference {
                            boundSymbol = CfirClassSymbol(classId("Box"))
                            isImplicit = true
                        }
                    }
                    argumentList = buildArgumentList()
                    origin = CfirFunctionCallOrigin.Regular
                },
            )
        }
    }

    /**
     * 验证匿名函数重复出现时复用已转换声明，并保留两次表达式操作。
     */
    @Test
    fun `reuses already converted anonymous function without swallowing storage errors`() {
        val anonymous = buildAnonymousFunction {
            commonDeclaration()
            status = CfirDeclarationStatusImpl()
            returnTypeRef = typeRef(ConePrimitiveType.UNIT)
            body = blockOf(ConePrimitiveType.UNIT)
            symbol = CfirAnonymousFunctionSymbol()
            hasExplicitParameterList = false
            isLambda = true
            typeRef = typeRef(ConeFunctionType(emptyList(), ConePrimitiveType.UNIT, isClosureType = true))
        }
        val anonymousExpression = buildAnonymousFunctionExpression {
            anonymousFunction = anonymous
            isTrailingLambda = false
        }
        val function = functionWithBody(
            name = "useLambdaTwice",
            returnType = ConePrimitiveType.UNIT,
            bodyType = ConePrimitiveType.UNIT,
            statements = listOf(anonymousExpression, anonymousExpression),
        )

        val operations = convertSingleFunction(function).otherOperations()

        assertEquals(2, operations.count { it == Cfir2ChirOperation.CFIR_ANONYMOUS_FUNCTION.canonicalName })
    }

    /**
     * 验证 CFIR 专有表达式形态会以 cfir2chir operation 保留到 CHIR。
     */
    @Test
    fun `preserves CFIR-only expression forms as cfir2chir operations`() {
        val function = functionWithBody(
            name = "sourceForms",
            returnType = ConePrimitiveType.UNIT,
            bodyType = ConePrimitiveType.UNIT,
            statements = listOf(
                // 数组字面量已降级为 jvm.newarray/jvm.arrayStore 序列；
                // 这里用元素类型不匹配的形态保留 cfir.array_literal 占位路径。
                buildArrayLiteral {
                    coneTypeOrNull = ConeVArrayType(ConePrimitiveType.BOOLEAN, size = 2)
                    elements += intLiteral(1)
                    elements += intLiteral(2)
                },
                // tuple 字面量已降级为 chir.tuple_pack；这里用元素类型
                // 不匹配的形态保留 cfir.tuple_literal 占位路径。
                buildTupleLiteral {
                    coneTypeOrNull = ConeTupleType(listOf(ConePrimitiveType.BOOLEAN, ConePrimitiveType.INT32))
                    elements += intLiteral(1)
                    elements += boolLiteral(true)
                },
                buildRangeExpression {
                    coneTypeOrNull = ConeStructType(ConeClassLikeLookupTagImpl(classId("Range")))
                    start = intLiteral(1)
                    end = intLiteral(3)
                    isInclusive = true
                },
                buildTypeConversion {
                    coneTypeOrNull = ConePrimitiveType.INT64
                    argument = intLiteral(1)
                    targetTypeRef = typeRef(ConePrimitiveType.INT64)
                },
                buildTypeOperator {
                    coneTypeOrNull = ConePrimitiveType.BOOLEAN
                    operation = CfirTypeOperationKind.IS
                    argument = intLiteral(1)
                    typeRef = intTypeRef()
                },
                buildBinaryOp {
                    coneTypeOrNull = ConePrimitiveType.INT64
                    kind = CfirBinaryOpKind.COALESCING
                    left = intLiteral(1)
                    right = intLiteral(2)
                },
                buildThisReceiverExpression {
                    coneTypeOrNull = ConeStructType(ConeClassLikeLookupTagImpl(classId("Self")))
                    calleeReference = buildThisReference {
                        boundSymbol = CfirStructSymbol(classId("Self"))
                        isImplicit = false
                    }
                },
                buildInaccessibleReceiverExpression {
                    coneTypeOrNull = ConeStructType(ConeClassLikeLookupTagImpl(classId("Self")))
                    calleeReference = buildThisReference {
                        boundSymbol = CfirStructSymbol(classId("Self"))
                        isImplicit = true
                    }
                    kind = InaccessibleReceiverKind.CLASS_HEADER
                },
                buildSmartCastExpression {
                    coneTypeOrNull = ConePrimitiveType.INT64
                    originalExpression = intLiteral(1)
                    smartcastType = typeRef(ConePrimitiveType.INT64)
                    upperTypesFromSmartCast = listOf(ConePrimitiveType.INT64)
                    lowerTypesFromSmartCast = listOf(ConePrimitiveType.INT32)
                    smartcastStability = CfirSmartcastStability.STABLE_VALUE
                },
            ),
        )

        val operations = convertSingleFunction(function).otherOperations().toSet()

        assertTrue(Cfir2ChirOperation.CFIR_ARRAY_LITERAL.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_TUPLE_LITERAL.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_RANGE.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_TYPE_CONVERSION.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_TYPE_OPERATOR.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_BINARY_OP.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_THIS_RECEIVER.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_INACCESSIBLE_RECEIVER.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_SMART_CAST.canonicalName in operations)
    }

    /**
     * 验证复合 Cone 类型映射只依赖 cfir2chir 的类型映射层，不反向依赖 CHIR tree 细节。
     */
    @Test
    fun `maps composite Cone types without depending on CFIR tree module from chir tree`() {
        val mapper = Cfir2ChirTypeMapper()

        // CFIR VArray 的 size 是编译期长度；CHIR rank 表达维度，VArray 恒为一维。
        assertEquals(
            ChirVArrayType(ChirResolvedTypeRef(ChirPrimitiveType.INT32), rank = 1),
            mapper.mapConeType(ConeVArrayType(ConePrimitiveType.INT32, size = 4)),
        )
        assertEquals(
            ChirTupleType(listOf(ChirResolvedTypeRef(ChirPrimitiveType.INT32), ChirResolvedTypeRef(ChirPrimitiveType.BOOL))),
            mapper.mapConeType(ConeTupleType(listOf(ConePrimitiveType.INT32, ConePrimitiveType.BOOLEAN))),
        )
        assertEquals(
            ChirPrimitiveType.INT64,
            mapper.mapConeType(ConeTypeAliasType(classId("AliasInt64"), expandedType = ConePrimitiveType.INT64)),
        )
        assertEquals(
            ChirNamedType("Any"),
            mapper.mapConeType(ConeAnyType),
        )
        assertThrows(Cfir2ChirConversionException::class.java) {
            mapper.mapConeType(ConePrimitiveType.IDEAL_INT)
        }
    }

    /**
     * 验证 struct、interface、extend、typealias、primitive 和 code fragment 声明转换。
     */
    @Test
    fun `converts class-like declarations in cfir2chir module`() {
        val module = convertModule(
            buildStruct {
                commonClassLikeDeclaration("Point", CfirStructSymbol(classId("Point")))
                declarations += field("x", ConePrimitiveType.INT32, mutable = false)
            },
            buildInterface {
                commonClassLikeDeclaration("Drawable", CfirInterfaceSymbol(classId("Drawable")))
            },
            buildExtend {
                commonDeclaration()
                symbol = CfirExtendSymbol()
                status = CfirDeclarationStatusImpl()
                extendedTypeRef = typeRef(ConeStructType(ConeClassLikeLookupTagImpl(classId("Point"))))
                superTypeRefs += typeRef(ConeClassLikeType(ConeClassLikeLookupTagImpl(classId("Drawable")), isInterface = true))
            },
            buildTypeAlias {
                commonDeclaration()
                scopeProvider = UnusedScopeProvider
                symbol = CfirTypeAliasSymbol(classId("Meters"))
                status = CfirDeclarationStatusImpl()
                name = Name.identifier("Meters")
                expandedTypeRef = intTypeRef()
            },
            CfirPrimitiveTypeDeclaration(
                moduleData = TestModuleData,
                symbol = CfirPrimitiveTypeSymbol(classId("BuiltinInt32"), PrimitiveTypeKind.INT32),
                name = Name.identifier("BuiltinInt32"),
                kind = PrimitiveTypeKind.INT32,
                scopeProvider = UnusedScopeProvider,
            ),
            buildCodeFragment {
                commonDeclaration()
                symbol = CfirCodeFragmentSymbol()
                block = blockOf(ConePrimitiveType.INT32, intLiteral(42))
            },
        )

        val struct = module.declarations.filterIsInstance<ChirStructDeclaration>().single()
        val iface = module.declarations.filterIsInstance<ChirInterfaceDeclaration>().single { it.name == "Drawable" }
        val extend = module.declarations.filterIsInstance<ChirExtendDeclaration>().single()
        val typeAlias = module.declarations.filterIsInstance<ChirTypeDeclaration>().single { it.name == "Meters" }
        val primitive = module.declarations.filterIsInstance<ChirTypeDeclaration>().single { it.name == "BuiltinInt32" }
        val codeFragment = module.declarations.filterIsInstance<ChirFunctionDeclaration>().single { it.name == "<code-fragment>" }

        assertEquals("Point", struct.name)
        assertEquals(listOf("x"), struct.fieldDeclarations.map { it.name })
        assertTrue(ChirStringAttribute("cfir.kind", "interface") in iface.attributes)
        assertTrue(iface.memberDeclarations.isEmpty())
        assertTrue(iface.definitionKind == ChirTypeDefinitionKind.INTERFACE)
        assertEquals("sample.Point", extend.targetType.renderName)
        assertEquals(listOf("sample.Drawable"), extend.extendedTypes.map { it.renderName })
        assertEquals("Meters", typeAlias.name)
        assertTrue(typeAlias.attributes.any { it is ChirStringAttribute && it.key == "cfir.expandedType" && it.value == "int32" })
        assertTrue(primitive.attributes.any { it is ChirStringAttribute && it.key == "cfir.primitive.kind" && it.value == "INT32" })
        assertEquals("int32", codeFragment.returnType.renderName)
        assertTrue(ChirStringAttribute("cfir.kind", "codeFragment") in codeFragment.attributes)
    }

    /**
     * 验证 named argument 透传内层值、if-available 与 augmented assignment
     * 以独立操作名 lowering 并保留语义属性。
     */
    @Test
    fun `lowers named argument if-available and augmented assignment expressions`() {
        val function = functionWithBody(
            name = "newExpressionForms",
            returnType = ConePrimitiveType.INT32,
            bodyType = ConePrimitiveType.INT32,
            statements = listOf(
                buildNamedArgumentExpression {
                    coneTypeOrNull = ConePrimitiveType.INT32
                    expression = intLiteral(7)
                    argumentName = Name.identifier("value")
                },
                buildIfAvailableExpression {
                    coneTypeOrNull = ConePrimitiveType.INT32
                    conditionName = Name.identifier("foo")
                    condition = boolLiteral(true)
                    thenBranch = intLiteral(1)
                    elseBranch = intLiteral(2)
                },
                buildAugmentedAssignment {
                    coneTypeOrNull = ConePrimitiveType.INT32
                    operation = Name.identifier("+=")
                    leftArgument = intLiteral(3)
                    rightArgument = intLiteral(4)
                },
            ),
        )

        val chirFunction = convertSingleFunction(function)
        val operations = chirFunction.otherOperations()
        val otherExpressions = chirFunction.blocks
            .flatMap { it.expressions }
            .filterIsInstance<ChirOtherExpression>()

        // 具名实参是纯透传包装：不发射占位操作，内层字面量直接保留。
        assertTrue(Cfir2ChirOperation.CFIR_NAMED_ARGUMENT.canonicalName !in operations)
        assertTrue(Cfir2ChirOperation.CFIR_IF_AVAILABLE.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_AUGMENTED_ASSIGNMENT.canonicalName in operations)

        val ifAvailable = otherExpressions.single { it.operation == Cfir2ChirOperation.CFIR_IF_AVAILABLE.canonicalName }
        assertEquals(3, ifAvailable.operands.size)
        assertTrue(
            ifAvailable.attributes.any {
                it is ChirStringAttribute && it.key == "cfir.conditionName" && it.value == "foo"
            },
        )
        val augmented = otherExpressions.single { it.operation == Cfir2ChirOperation.CFIR_AUGMENTED_ASSIGNMENT.canonicalName }
        assertEquals(2, augmented.operands.size)
        assertTrue(
            augmented.attributes.any {
                it is ChirStringAttribute && it.key == "cfir.binary.kind" && it.value == "+="
            },
        )
    }

    /**
     * 验证枚举 case 引用会降级为带 jvm.owner/jvm.name ABI 的 `jvm.getstatic`，
     * 而不是物化为函数值（case 构造函数体是占位操作，JVM 后端无法执行）。
     */
    @Test
    fun `lowers enum case reference into jvm getstatic`() {
        val colorType = ConeEnumType(ConeClassLikeLookupTagImpl(classId("Color")))
        val redCaseSymbol = CfirEnumConstructorSymbol(CallableId(PackageName, Name.identifier("RED")))
        val function = functionReturningExpression(
            name = "pickRed",
            returnType = ConePrimitiveType.INT32,
            expression = buildNamedAccessExpression {
                coneTypeOrNull = colorType
                calleeReference = buildResolvedNamedReference {
                    name = Name.identifier("RED")
                    resolvedSymbol = redCaseSymbol
                }
            },
        )

        val chirFunction = convertSingleFunction(function)
        val otherExpressions = chirFunction.blocks
            .flatMap { it.expressions }
            .filterIsInstance<ChirOtherExpression>()

        val getStatic = otherExpressions.single { it.operation == "jvm.getstatic" }
        assertTrue(
            getStatic.attributes.any { it is ChirStringAttribute && it.key == "jvm.owner" && it.value == "sample/Color" },
        )
        assertTrue(
            getStatic.attributes.any { it is ChirStringAttribute && it.key == "jvm.name" && it.value == "RED" },
        )
        assertEquals("sample.Color", getStatic.resultType?.renderName)
    }

    /**
     * 验证 body 内的匿名函数声明会作为辅助函数追加到 module，
     * 函数值引用的目标在 CHIR 输出中真实存在。
     */
    @Test
    fun `collects anonymous function declarations as module auxiliary functions`() {
        val anonymous = buildAnonymousFunction {
            commonDeclaration()
            status = CfirDeclarationStatusImpl()
            returnTypeRef = typeRef(ConePrimitiveType.INT32)
            body = blockOf(ConePrimitiveType.INT32, intLiteral(5))
            symbol = CfirAnonymousFunctionSymbol()
            hasExplicitParameterList = false
            isLambda = true
            typeRef = typeRef(ConeFunctionType(emptyList(), ConePrimitiveType.INT32, isClosureType = true))
        }
        val host = functionWithBody(
            name = "useLambda",
            returnType = ConePrimitiveType.UNIT,
            bodyType = ConePrimitiveType.UNIT,
            statements = listOf(buildAnonymousFunctionExpression { anonymousFunction = anonymous; isTrailingLambda = false }),
        )

        val module = convertModule(host)

        val functionDeclarations = module.declarations.filterIsInstance<ChirFunctionDeclaration>()
        // 宿主函数 + 匿名函数体都进入 module。
        assertEquals(2, functionDeclarations.size)
        assertTrue(functionDeclarations.any { it.name == org.cangnova.cangjie.name.SpecialNames.ANONYMOUS_STRING })
    }

    /**
     * 转换单个 CFIR 函数并返回同名 CHIR 函数声明。
     *
     * body 内的匿名/局部函数会作为辅助声明追加到 module，
     * 因此按函数名定位宿主函数而不是假设 module 内只有一个函数。
     */
    private fun convertSingleFunction(function: CfirNamedFunction): ChirFunctionDeclaration =
        convertModule(function).declarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .single { it.name == function.name.asString() }

    /**
     * 将一组测试声明包进 CFIR 文件并转换为唯一 CHIR module。
     */
    private fun convertModule(vararg declarations: CfirDeclaration): ChirModule =
        DefaultCfir2ChirConverter().convert(listOf(fileWith(*declarations))).modules.single()

    /**
     * 构造返回指定表达式值的命名函数。
     */
    private fun functionReturningExpression(
        name: String,
        returnType: ConePrimitiveType,
        expression: CfirExpression,
    ): CfirNamedFunction =
        functionWithBody(name, returnType, returnType, listOf(expression))

    /**
     * 构造带指定 body 语句列表的命名函数。
     */
    private fun functionWithBody(
        name: String,
        returnType: ConePrimitiveType,
        bodyType: ConePrimitiveType,
        statements: List<CfirStatement>,
    ): CfirNamedFunction {
        val symbol = CfirNamedFunctionSymbol(CallableId(PackageName, Name.identifier(name)))
        return buildNamedFunction {
            commonNamedFunctionDeclaration(symbol)
            this.name = Name.identifier(name)
            returnTypeRef = typeRef(returnType)
            body = blockOf(bodyType, *statements.toTypedArray())
        }
    }

    /**
     * 构造带解析类型的 CFIR block。
     */
    private fun blockOf(type: ConePrimitiveType, vararg statements: CfirStatement) = buildBlock {
        coneTypeOrNull = type
        this.statements += statements
    }

    /**
     * 构造测试用字段变量。
     */
    private fun field(name: String, type: ConePrimitiveType, mutable: Boolean): CfirFieldVariable {
        val variableName = Name.identifier(name)
        return buildFieldVariable {
            commonFieldVariableDeclaration(CfirFieldVariableSymbol(CallableId(PackageName, variableName)))
            this.name = variableName
            returnTypeRef = typeRef(type)
            initializer = null
            isVar = mutable
        }
    }

    /**
     * 构造包含指定声明的测试 CFIR 文件。
     */
    private fun fileWith(vararg declarations: CfirDeclaration): CfirFile =
        buildFile {
            commonDeclaration()
            symbol = CfirFileSymbol()
            name = "coverage.cj"
            packageDirective = buildPackageDirective {
                packageFqName = PackageName
                isMacroPackage = false
            }
            this.declarations += declarations
        }

    /**
     * 填充命名函数声明在转换测试中必需的公共字段。
     */
    private fun CfirNamedFunctionBuilder.commonNamedFunctionDeclaration(symbol: CfirNamedFunctionSymbol) {
        commonDeclaration()
        isLocal = false
        dispatchReceiverType = null
        status = CfirDeclarationStatusImpl()
        this.symbol = symbol
        isMut = false
    }

    /**
     * 填充字段变量声明在转换测试中必需的公共字段。
     */
    private fun CfirFieldVariableBuilder.commonFieldVariableDeclaration(symbol: CfirFieldVariableSymbol) {
        commonDeclaration()
        isLocal = false
        dispatchReceiverType = null
        status = CfirDeclarationStatusImpl()
        this.symbol = symbol
    }

    /**
     * 填充命名函数 builder 的基础声明元数据。
     */
    private fun CfirNamedFunctionBuilder.commonDeclaration() {
        moduleData = TestModuleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.Default
        attributes = CfirDeclarationAttributes.EMPTY
    }

    /**
     * 填充字段变量 builder 的基础声明元数据。
     */
    private fun CfirFieldVariableBuilder.commonDeclaration() {
        moduleData = TestModuleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.Default
        attributes = CfirDeclarationAttributes.EMPTY
    }

    /**
     * 填充文件 builder 的基础声明元数据。
     */
    private fun CfirFileBuilder.commonDeclaration() {
        moduleData = TestModuleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.Default
        attributes = CfirDeclarationAttributes.EMPTY
    }

    /**
     * 填充 extend builder 的基础声明元数据。
     */
    private fun CfirExtendBuilder.commonDeclaration() {
        moduleData = TestModuleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.Default
        attributes = CfirDeclarationAttributes.EMPTY
    }

    /**
     * 填充 typealias builder 的基础声明元数据。
     */
    private fun CfirTypeAliasBuilder.commonDeclaration() {
        moduleData = TestModuleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.Default
        attributes = CfirDeclarationAttributes.EMPTY
    }

    /**
     * 填充 code fragment builder 的基础声明元数据。
     */
    private fun CfirCodeFragmentBuilder.commonDeclaration() {
        moduleData = TestModuleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.Default
        attributes = CfirDeclarationAttributes.EMPTY
    }

    /**
     * 填充匿名函数 builder 的基础声明元数据。
     */
    private fun CfirAnonymousFunctionBuilder.commonDeclaration() {
        moduleData = TestModuleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.Default
        attributes = CfirDeclarationAttributes.EMPTY
        isLocal = true
        dispatchReceiverType = null
    }

    /**
     * 填充 struct builder 的 class-like 公共字段。
     */
    private fun CfirStructBuilder.commonClassLikeDeclaration(
        name: String,
        symbol: CfirStructSymbol,
    ) {
        commonDeclaration()
        scopeProvider = UnusedScopeProvider
        status = CfirDeclarationStatusImpl()
        this.name = Name.identifier(name)
        this.symbol = symbol
    }

    /**
     * 填充 interface builder 的 class-like 公共字段。
     */
    private fun CfirInterfaceBuilder.commonClassLikeDeclaration(
        name: String,
        symbol: CfirInterfaceSymbol,
    ) {
        commonDeclaration()
        scopeProvider = UnusedScopeProvider
        status = CfirDeclarationStatusImpl()
        this.name = Name.identifier(name)
        this.symbol = symbol
    }

    /**
     * 填充 class builder 的 class-like 公共字段。
     */
    private fun CfirClassBuilder.commonClassLikeDeclaration(
        name: String,
        symbol: CfirClassSymbol,
    ) {
        moduleData = TestModuleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.Default
        attributes = CfirDeclarationAttributes.EMPTY
        scopeProvider = UnusedScopeProvider
        status = CfirDeclarationStatusImpl()
        this.name = Name.identifier(name)
        this.symbol = symbol
    }

    /**
     * 填充 struct builder 的基础声明元数据。
     */
    private fun CfirStructBuilder.commonDeclaration() {
        moduleData = TestModuleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.Default
        attributes = CfirDeclarationAttributes.EMPTY
    }

    /**
     * 填充 interface builder 的基础声明元数据。
     */
    private fun CfirInterfaceBuilder.commonDeclaration() {
        moduleData = TestModuleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.Default
        attributes = CfirDeclarationAttributes.EMPTY
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
     * 构造 Bool 字面量表达式。
     */
    private fun boolLiteral(value: Boolean) = buildLiteralExpression {
        kind = CfirLiteralKind.BOOLEAN
        this.value = value
        coneTypeOrNull = ConePrimitiveType.BOOLEAN
    }

    /**
     * 构造 Int32 resolved type ref。
     */
    private fun intTypeRef(): CfirTypeRef = typeRef(ConePrimitiveType.INT32)

    /**
     * 构造指定 Cone 类型的 resolved type ref。
     */
    private fun typeRef(type: org.cangnova.cangjie.cfir.types.ConeCangJieType): CfirTypeRef =
        buildResolvedTypeRef {
            coneType = type
        }

    /**
     * 构造 sample 包下的测试 class id。
     */
    private fun classId(name: String): ClassId =
        ClassId(PackageName, Name.identifier(name))

    /**
     * 提取 CHIR 函数中所有 other expression 的 operation 名称。
     */
    private fun ChirFunctionDeclaration.otherOperations(): List<String> =
        blocks.flatMap { block ->
            block.expressions.mapNotNull { expression ->
                (expression as? ChirOtherExpression)?.operation
            }
        }

    /**
     * 转换测试不应触发真实 scope 查询的占位 scope provider。
     */
    private object UnusedScopeProvider : CfirScopeProvider() {
        /**
         * 禁止在测试 fixture 中查询 use-site 成员作用域。
         */
        override fun getUseSiteMemberScope(
            klass: CfirClass,
            useSiteSession: CfirSession,
            scopeSession: ScopeSession,
        ): CfirTypeScope = error("scope provider must not be used by cfir2chir coverage tests")

        /**
         * 禁止在测试 fixture 中查询 typealias constructor 作用域。
         */
        override fun getTypealiasConstructorScope(
            typeAlias: CfirTypeAlias,
            useSiteSession: CfirSession,
            scopeSession: ScopeSession,
        ): CfirScope = error("scope provider must not be used by cfir2chir coverage tests")

        /**
         * 禁止在测试 fixture 中查询 declaration-site 成员作用域。
         */
        override fun getDeclarationSiteMemberScope(
            klass: CfirClass,
            useSiteSession: CfirSession,
            scopeSession: ScopeSession,
        ): CfirTypeScope = error("scope provider must not be used by cfir2chir coverage tests")
    }

    /**
     * cfir2chir 覆盖测试使用的最小 source session。
     */
    private object TestSession : CfirSession(Kind.Source)

    /**
     * cfir2chir 覆盖测试使用的最小 module data。
     */
    private object TestModuleData : CfirModuleData() {
        /**
         * 测试模块名称。
         */
        override val name: Name = Name.identifier("cfir2chir-coverage")
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
        override val stableModuleName: String = "cfir2chir-coverage"
        /**
         * 测试模块绑定的 CFIR session。
         */
        override val session: CfirSession
            get() = TestSession

        init {
            bindSession(TestSession)
        }
    }

    private companion object {
        val PackageName = FqName("sample")
    }
}
