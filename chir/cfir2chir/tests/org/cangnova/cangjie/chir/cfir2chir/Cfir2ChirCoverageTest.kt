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
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
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
import org.cangnova.cangjie.cfir.declarations.builder.buildCodeFragment
import org.cangnova.cangjie.cfir.declarations.builder.buildClass
import org.cangnova.cangjie.cfir.declarations.builder.buildExtend
import org.cangnova.cangjie.cfir.declarations.builder.buildFieldVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildInterface
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.builder.buildStruct
import org.cangnova.cangjie.cfir.declarations.builder.buildTypeAlias
import org.cangnova.cangjie.cfir.declarations.builder.buildTypeParameter
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOpKind
import org.cangnova.cangjie.cfir.expressions.CfirComparisonOp
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirSmartcastStability
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperationKind
import org.cangnova.cangjie.cfir.expressions.InaccessibleReceiverKind
import org.cangnova.cangjie.cfir.expressions.builder.buildAssignment
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildArrayLiteral
import org.cangnova.cangjie.cfir.expressions.builder.buildAugmentedAssignment
import org.cangnova.cangjie.cfir.expressions.builder.buildAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildBinaryOp
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildComparisonExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.buildIfExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildIfAvailableExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedArgumentExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildRangeExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildSuperReceiverExpression
import org.cangnova.cangjie.cfir.expressions.buildInaccessibleReceiverExpression
import org.cangnova.cangjie.cfir.expressions.buildSmartCastExpression
import org.cangnova.cangjie.cfir.expressions.buildThisReceiverExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildTupleLiteral
import org.cangnova.cangjie.cfir.expressions.builder.buildTypeConversion
import org.cangnova.cangjie.cfir.expressions.builder.buildTypeOperator
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildThisReference
import org.cangnova.cangjie.cfir.references.builder.buildSuperReference
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCodeFragmentSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPrimitiveTypeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirStructSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeAnyType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.declaration.ChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirExtendDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.ChirStructDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirTypeDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherOperation
import org.cangnova.cangjie.chir.core.expression.ChirTupleExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.type.ChirNamedType
import org.cangnova.cangjie.chir.core.type.ChirClassType
import org.cangnova.cangjie.chir.core.type.ChirGenericType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirRefType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirTupleType
import org.cangnova.cangjie.chir.core.type.ChirVArrayType
import org.cangnova.cangjie.chir.core.value.ChirImportedFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirParameterValue
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
     * 验证 common 声明和 specific 包中的导入调用使用 family actualization 提供的同一稳定身份。
     */
    @Test
    fun `uses shared semantic identity for common declaration and specific imported call`() {
        val commonFunction = functionWithBody(
            name = "shared",
            returnType = ConePrimitiveType.UNIT,
            bodyType = ConePrimitiveType.UNIT,
            statements = emptyList(),
        )
        val specificFunction = functionWithBody(
            name = "shared",
            returnType = ConePrimitiveType.UNIT,
            bodyType = ConePrimitiveType.UNIT,
            statements = emptyList(),
        )
        val actualizedId = ChirSemanticId("cangjie:@sharedPostSemaMangledName")
        val identityProvider = Cfir2ChirSemanticIdentityProvider.fromCjmpCounterparts(
            specificToCommon = listOf(specificFunction to commonFunction),
            identityOfStableBinaryDeclaration = { symbol -> actualizedId.takeIf { symbol === commonFunction.symbol } },
        )
        val commonPackage = DefaultCfir2ChirConverter(identityProvider).convert(listOf(fileWith(commonFunction)))

        val commonDeclaration = commonPackage.modules.single().declarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .single()
        assertEquals(actualizedId, commonDeclaration.semanticId)

        val call = buildFunctionCall {
            coneTypeOrNull = ConePrimitiveType.UNIT
            calleeReference = buildResolvedNamedReference {
                name = Name.identifier("shared")
                resolvedSymbol = commonFunction.symbol
            }
        }
        val specificCaller = functionWithBody(
            name = "specificCaller",
            returnType = ConePrimitiveType.UNIT,
            bodyType = ConePrimitiveType.UNIT,
            statements = listOf(call),
        )
        val specificPackage = DefaultCfir2ChirConverter(identityProvider)
            .convert(listOf(fileWith(specificFunction, specificCaller)))
        val specificDeclarations = specificPackage.modules.single().declarations
            .filterIsInstance<ChirFunctionDeclaration>()
        assertEquals(actualizedId, specificDeclarations.single { it.name == "shared" }.semanticId)
        val callExpression = specificDeclarations.single { it.name == "specificCaller" }
            .blocks
            .flatMap { it.expressions }
            .filterIsInstance<ChirCallExpression>()
            .single()

        assertTrue(callExpression.callee is ChirImportedFunctionValue)
        assertEquals(actualizedId, callExpression.callee.semanticId)
    }

    /**
     * 验证身份映射拒绝缺少官方身份或将一个 specific 声明映射到多个不同 common 身份的 matcher 结果。
     */
    @Test
    fun `rejects missing or conflicting common identities for matched declarations`() {
        val specificFunction = functionWithBody(
            name = "shared",
            returnType = ConePrimitiveType.UNIT,
            bodyType = ConePrimitiveType.UNIT,
            statements = emptyList(),
        )
        val firstCommonFunction = functionWithBody(
            name = "shared",
            returnType = ConePrimitiveType.UNIT,
            bodyType = ConePrimitiveType.UNIT,
            statements = emptyList(),
        )
        val secondCommonFunction = functionWithBody(
            name = "shared",
            returnType = ConePrimitiveType.UNIT,
            bodyType = ConePrimitiveType.UNIT,
            statements = emptyList(),
        )

        assertThrows(Cfir2ChirConversionException::class.java) {
            Cfir2ChirSemanticIdentityProvider.fromCjmpCounterparts(
                specificToCommon = listOf(specificFunction to firstCommonFunction),
                identityOfStableBinaryDeclaration = { null },
            )
        }

        val firstId = ChirSemanticId("cangjie:@firstMangledName")
        val secondId = ChirSemanticId("cangjie:@secondMangledName")
        assertThrows(Cfir2ChirConversionException::class.java) {
            Cfir2ChirSemanticIdentityProvider.fromCjmpCounterparts(
                specificToCommon = listOf(
                    specificFunction to firstCommonFunction,
                    specificFunction to secondCommonFunction,
                ),
                identityOfStableBinaryDeclaration = { symbol ->
                    when {
                        symbol === firstCommonFunction.symbol -> firstId
                        symbol === secondCommonFunction.symbol -> secondId
                        else -> null
                    }
                },
            )
        }
    }

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
                buildArrayLiteral {
                    coneTypeOrNull = ConeVArrayType(ConePrimitiveType.INT32, size = 2)
                    elements += intLiteral(1)
                    elements += intLiteral(2)
                },
                buildTupleLiteral {
                    coneTypeOrNull = ConeTupleType(listOf(ConePrimitiveType.INT32, ConePrimitiveType.BOOLEAN))
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
                    coneTypeOrNull = ConePrimitiveType.BOOLEAN
                    kind = CfirBinaryOpKind.AND
                    left = boolLiteral(true)
                    right = boolLiteral(false)
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

        val chirFunction = convertSingleFunction(function)
        val operations = chirFunction.otherOperations().toSet()

        assertTrue(Cfir2ChirOperation.CFIR_ARRAY_LITERAL.canonicalName in operations)
        val tuple = chirFunction.blocks.flatMap { it.expressions }.filterIsInstance<ChirTupleExpression>().single()
        assertEquals(2, tuple.elements.size)
        assertEquals(
            listOf(ChirResolvedTypeRef(ChirPrimitiveType.INT32), ChirResolvedTypeRef(ChirPrimitiveType.BOOL)),
            (tuple.resultType as ChirResolvedTypeRef).type.let { (it as ChirTupleType).elementTypes },
        )
        assertTrue(Cfir2ChirOperation.CFIR_RANGE.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_TYPE_CONVERSION.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_TYPE_OPERATOR.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_BINARY_OP.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_INACCESSIBLE_RECEIVER.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_SMART_CAST.canonicalName in operations)
    }

    /** 实例函数的 this 与所有普通形参共用官方 CHIR 参数序列，receiver 固定排在首位。 */
    @Test
    fun `lowers this receiver to the first ABI parameter`() {
        val selfClassId = classId("Self")
        val selfSymbol = CfirClassSymbol(selfClassId)
        val selfType = ConeClassLikeType(ConeClassLikeLookupTagImpl(selfClassId))
        val memberSymbol = CfirNamedFunctionSymbol(CallableId(PackageName, Name.identifier("current")))
        val thisExpression = buildThisReceiverExpression {
            coneTypeOrNull = selfType
            calleeReference = buildThisReference {
                boundSymbol = selfSymbol
                isImplicit = false
            }
        }
        val member = buildNamedFunction {
            commonNamedFunctionDeclaration(memberSymbol)
            name = Name.identifier("current")
            dispatchReceiverType = selfType
            returnTypeRef = typeRef(selfType)
            body = buildBlock {
                coneTypeOrNull = selfType
                statements += thisExpression
            }
        }
        val owner = buildClass {
            commonClassDeclaration("Self", selfSymbol)
            declarations += member
        }

        val chirPackage = DefaultCfir2ChirConverter().convert(listOf(fileWith(owner)))
        val chirOwner = chirPackage.modules.single().declarations.filterIsInstance<ChirClassDeclaration>().single()
        val chirMember = chirOwner.memberDeclarations.filterIsInstance<ChirFunctionDeclaration>().single()
        val receiver = chirMember.parameters.single()
        val returnedReceiver = (chirMember.blocks.single().terminator as ChirReturnTerminator).returnValue

        assertEquals(Cfir2ChirIds.dispatchReceiverId(chirMember.semanticId), receiver.semanticId)
        assertEquals(receiver.semanticId, (returnedReceiver as ChirParameterValue).semanticId)
        assertEquals(chirMember.semanticId, returnedReceiver.ownerFunctionId)
        assertTrue(chirMember.otherOperations().none { it == Cfir2ChirOperation.CFIR_THIS_RECEIVER.canonicalName })
    }

    /** 接收者与实参都带调用副作用时，必须按源码求值顺序降低且各执行一次。 */
    @Test
    fun `lowers dispatch receiver before call arguments`() {
        val ownerId = classId("CallReceiver")
        val ownerSymbol = CfirClassSymbol(ownerId)
        val ownerType = ConeClassLikeType(ConeClassLikeLookupTagImpl(ownerId))
        val receiverName = Name.identifier("receiverEffect")
        val receiverSymbol = CfirNamedFunctionSymbol(CallableId(PackageName, receiverName))
        val receiverFunction = buildNamedFunction {
            commonNamedFunctionDeclaration(receiverSymbol)
            name = receiverName
            returnTypeRef = typeRef(ownerType)
        }
        val argumentName = Name.identifier("argumentEffect")
        val argumentSymbol = CfirNamedFunctionSymbol(CallableId(PackageName, argumentName))
        val argumentFunction = buildNamedFunction {
            commonNamedFunctionDeclaration(argumentSymbol)
            name = argumentName
            returnTypeRef = intTypeRef()
        }
        val targetName = Name.identifier("target")
        val targetSymbol = CfirNamedFunctionSymbol(CallableId(PackageName, targetName))
        val target = buildNamedFunction {
            commonNamedFunctionDeclaration(targetSymbol)
            name = targetName
            dispatchReceiverType = ownerType
            returnTypeRef = intTypeRef()
            valueParameters += buildValueParameter {
                moduleData = TestModuleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Synthetic.Default
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                symbol = CfirValueParameterSymbol(CallableId(PackageName, Name.identifier("value")))
                containingDeclarationSymbol = targetSymbol
                isNamed = true
                name = Name.identifier("value")
                returnTypeRef = intTypeRef()
            }
            body = blockOf(ConePrimitiveType.INT32, intLiteral(1))
        }
        val receiverCall = buildFunctionCall {
            coneTypeOrNull = ownerType
            calleeReference = buildResolvedNamedReference {
                name = receiverName
                resolvedSymbol = receiverSymbol
            }
        }
        val argumentCall = buildFunctionCall {
            coneTypeOrNull = ConePrimitiveType.INT32
            calleeReference = buildResolvedNamedReference {
                name = argumentName
                resolvedSymbol = argumentSymbol
            }
        }
        val memberCall = buildFunctionCall {
            coneTypeOrNull = ConePrimitiveType.INT32
            calleeReference = buildResolvedNamedReference {
                name = targetName
                resolvedSymbol = targetSymbol
            }
            dispatchReceiver = receiverCall
            explicitReceiver = receiverCall
            argumentList = buildArgumentList {
                arguments += argumentCall
            }
        }
        val owner = buildClass {
            commonClassDeclaration("CallReceiver", ownerSymbol)
            declarations += target
        }
        val caller = functionWithBody(
            name = "caller",
            returnType = ConePrimitiveType.INT32,
            bodyType = ConePrimitiveType.INT32,
            statements = listOf(memberCall),
        )

        val chirModule = DefaultCfir2ChirConverter()
            .convert(listOf(fileWith(receiverFunction, argumentFunction, owner, caller)))
            .modules
            .single()
        val topLevelFunctions = chirModule.declarations.filterIsInstance<ChirFunctionDeclaration>()
        val chirOwner = chirModule.declarations.filterIsInstance<ChirClassDeclaration>().single()
        val chirTarget = chirOwner.memberDeclarations.filterIsInstance<ChirFunctionDeclaration>().single()
        val chirCaller = topLevelFunctions.single { it.name == "caller" }
        val calls = chirCaller.blocks.flatMap { it.expressions }.filterIsInstance<ChirCallExpression>()

        assertEquals(
            listOf(
                topLevelFunctions.single { it.name == "receiverEffect" }.semanticId,
                topLevelFunctions.single { it.name == "argumentEffect" }.semanticId,
                chirTarget.semanticId,
            ),
            calls.map { it.callee.semanticId },
        )
    }

    /** explicit receiver 形态的 super 调用复用当前 ABI receiver，并保留 super 调用标记。 */
    @Test
    fun `lowers explicit super receiver through current ABI receiver`() {
        val ownerId = classId("SuperReceiver")
        val ownerSymbol = CfirClassSymbol(ownerId)
        val ownerType = ConeClassLikeType(ConeClassLikeLookupTagImpl(ownerId))
        val targetName = Name.identifier("target")
        val targetSymbol = CfirNamedFunctionSymbol(CallableId(PackageName, targetName))
        val target = buildNamedFunction {
            commonNamedFunctionDeclaration(targetSymbol)
            name = targetName
            dispatchReceiverType = ownerType
            returnTypeRef = intTypeRef()
            body = blockOf(ConePrimitiveType.INT32, intLiteral(1))
        }
        val superReceiver = buildSuperReceiverExpression {
            coneTypeOrNull = ownerType
            calleeReference = buildSuperReference {
                superTypeRef = typeRef(ownerType)
            }
        }
        val call = buildFunctionCall {
            coneTypeOrNull = ConePrimitiveType.INT32
            calleeReference = buildResolvedNamedReference {
                name = targetName
                resolvedSymbol = targetSymbol
            }
            explicitReceiver = superReceiver
        }
        val callerName = Name.identifier("caller")
        val callerSymbol = CfirNamedFunctionSymbol(CallableId(PackageName, callerName))
        val caller = buildNamedFunction {
            commonNamedFunctionDeclaration(callerSymbol)
            name = callerName
            dispatchReceiverType = ownerType
            returnTypeRef = intTypeRef()
            body = blockOf(ConePrimitiveType.INT32, call)
        }
        val owner = buildClass {
            commonClassDeclaration("SuperReceiver", ownerSymbol)
            declarations += target
            declarations += caller
        }

        val chirModule = DefaultCfir2ChirConverter().convert(listOf(fileWith(owner))).modules.single()
        val chirOwner = chirModule.declarations.filterIsInstance<ChirClassDeclaration>().single()
        val chirCaller = chirOwner.memberDeclarations.filterIsInstance<ChirFunctionDeclaration>()
            .single { it.name == "caller" }
        val callExpression = chirCaller.blocks.flatMap { it.expressions }
            .filterIsInstance<ChirCallExpression>()
            .single()

        assertTrue(callExpression.isSuperCall)
        assertEquals(
            Cfir2ChirIds.dispatchReceiverId(chirCaller.semanticId),
            callExpression.arguments.first().semanticId,
        )
    }

    /** super 调用与派生类上的继承成员调用都按目标声明 owner 转换 receiver。 */
    @Test
    fun `adapts inherited and super call receivers to the declaring owner`() {
        val baseId = classId("BaseOwner")
        val baseSymbol = CfirClassSymbol(baseId)
        val baseType = ConeClassLikeType(ConeClassLikeLookupTagImpl(baseId))
        val derivedId = classId("DerivedOwner")
        val derivedSymbol = CfirClassSymbol(derivedId)
        val derivedType = ConeClassLikeType(ConeClassLikeLookupTagImpl(derivedId))
        val targetName = Name.identifier("target")
        val targetSymbol = CfirNamedFunctionSymbol(CallableId(PackageName, targetName))
        val target = buildNamedFunction {
            commonNamedFunctionDeclaration(targetSymbol)
            name = targetName
            status = CfirDeclarationStatusImpl().apply { isOpen = true }
            dispatchReceiverType = baseType
            returnTypeRef = intTypeRef()
            body = blockOf(ConePrimitiveType.INT32, intLiteral(1))
        }

        fun derivedThisReceiver() = buildThisReceiverExpression {
            coneTypeOrNull = derivedType
            calleeReference = buildThisReference {
                boundSymbol = derivedSymbol
                isImplicit = false
            }
        }

        val superCall = buildFunctionCall {
            coneTypeOrNull = ConePrimitiveType.INT32
            calleeReference = buildResolvedNamedReference {
                name = targetName
                resolvedSymbol = targetSymbol
            }
            explicitReceiver = buildSuperReceiverExpression {
                coneTypeOrNull = baseType
                calleeReference = buildSuperReference {
                    superTypeRef = typeRef(baseType)
                }
            }
        }
        val inheritedCall = buildFunctionCall {
            coneTypeOrNull = ConePrimitiveType.INT32
            calleeReference = buildResolvedNamedReference {
                name = targetName
                resolvedSymbol = targetSymbol
            }
            dispatchReceiver = derivedThisReceiver()
        }
        val callerName = Name.identifier("caller")
        val callerSymbol = CfirNamedFunctionSymbol(CallableId(PackageName, callerName))
        val caller = buildNamedFunction {
            commonNamedFunctionDeclaration(callerSymbol)
            name = callerName
            dispatchReceiverType = derivedType
            returnTypeRef = intTypeRef()
            body = blockOf(ConePrimitiveType.INT32, superCall, inheritedCall)
        }
        val base = buildClass {
            commonClassDeclaration("BaseOwner", baseSymbol)
            declarations += target
        }
        val derived = buildClass {
            commonClassDeclaration("DerivedOwner", derivedSymbol)
            superTypeRefs += typeRef(baseType)
            declarations += caller
        }

        val chirPackage = DefaultCfir2ChirConverter().convert(listOf(fileWith(base, derived)))
        val chirModule = chirPackage.modules.single()
        val chirCaller = chirModule.declarations.filterIsInstance<ChirClassDeclaration>()
            .single { it.name == "DerivedOwner" }
            .memberDeclarations.filterIsInstance<ChirFunctionDeclaration>()
            .single { it.name == "caller" }
        val expressions = chirCaller.blocks.flatMap { it.expressions }
        val calls = expressions.filterIsInstance<ChirCallExpression>()
        val receiverCasts = expressions.filterIsInstance<ChirOtherExpression>()
            .filter { it.operation == ChirOtherOperation.CANGJIE_TYPE_CAST_NA.canonicalName }
        val baseReceiverType = ChirResolvedTypeRef(
            ChirRefType(ChirResolvedTypeRef(ChirClassType(baseId.toChirTypeDeclarationId())), mutable = true),
        )
        val derivedReceiverType = ChirResolvedTypeRef(
            ChirRefType(ChirResolvedTypeRef(ChirClassType(derivedId.toChirTypeDeclarationId())), mutable = true),
        )

        assertEquals(2, receiverCasts.size)
        receiverCasts.forEach { cast ->
            assertEquals(derivedReceiverType, cast.operands.single().type)
            assertEquals(baseReceiverType, cast.resultType)
        }
        val superCallExpression = calls.single { it.isSuperCall }
        assertEquals(baseReceiverType, superCallExpression.arguments.first().type)
        val virtualCall = calls.single { !it.isSuperCall }
        val virtualContext = checkNotNull(virtualCall.virtualMethodContext)
        assertEquals(baseReceiverType, virtualCall.callee.type)
        assertEquals(baseReceiverType, virtualContext.originalFunctionType.parameterTypes.first())
    }

    /** 静态成员 header 保留 owner，让同 owner 无接收者调用仍携带该类型的 TypeInfo。 */
    @Test
    fun `preserves owner type for same-owner static calls`() {
        val ownerId = classId("StaticOwner")
        val ownerSymbol = CfirClassSymbol(ownerId)
        val targetName = Name.identifier("target")
        val targetSymbol = CfirNamedFunctionSymbol(CallableId(PackageName, targetName))
        val target = buildNamedFunction {
            commonNamedFunctionDeclaration(targetSymbol)
            name = targetName
            status = CfirDeclarationStatusImpl().apply { isStatic = true }
            returnTypeRef = intTypeRef()
            body = blockOf(ConePrimitiveType.INT32, intLiteral(1))
        }
        val callerName = Name.identifier("caller")
        val callerSymbol = CfirNamedFunctionSymbol(CallableId(PackageName, callerName))
        val call = buildFunctionCall {
            coneTypeOrNull = ConePrimitiveType.INT32
            calleeReference = buildResolvedNamedReference {
                name = targetName
                resolvedSymbol = targetSymbol
            }
        }
        val caller = buildNamedFunction {
            commonNamedFunctionDeclaration(callerSymbol)
            name = callerName
            status = CfirDeclarationStatusImpl().apply { isStatic = true }
            returnTypeRef = intTypeRef()
            body = blockOf(ConePrimitiveType.INT32, call)
        }
        val owner = buildClass {
            commonClassDeclaration("StaticOwner", ownerSymbol)
            declarations += target
            declarations += caller
        }

        val chirModule = DefaultCfir2ChirConverter().convert(listOf(fileWith(owner))).modules.single()
        val chirOwner = chirModule.declarations.filterIsInstance<ChirClassDeclaration>().single()
        val chirCaller = chirOwner.memberDeclarations.filterIsInstance<ChirFunctionDeclaration>()
            .single { it.name == "caller" }
        val callExpression = chirCaller.blocks.flatMap { it.expressions }
            .filterIsInstance<ChirCallExpression>()
            .single()

        assertEquals(
            ChirResolvedTypeRef(
                ChirRefType(
                    ChirResolvedTypeRef(ChirClassType(ownerId.toChirTypeDeclarationId())),
                    mutable = true,
                ),
            ),
            callExpression.thisType,
        )
    }

    /** 普通 open 成员调用使用 receiver 驱动的虚分派，并携带原始方法签名。 */
    @Test
    fun `lowers open member call with virtual method context`() {
        val ownerId = classId("VirtualDispatch")
        val ownerSymbol = CfirClassSymbol(ownerId)
        val ownerType = ConeClassLikeType(ConeClassLikeLookupTagImpl(ownerId))
        val targetName = Name.identifier("value")
        val targetSymbol = CfirNamedFunctionSymbol(CallableId(PackageName, targetName))
        val target = buildNamedFunction {
            commonNamedFunctionDeclaration(targetSymbol)
            name = targetName
            status = CfirDeclarationStatusImpl().apply { isOpen = true }
            dispatchReceiverType = ownerType
            returnTypeRef = intTypeRef()
            body = blockOf(ConePrimitiveType.INT32, intLiteral(1))
        }
        val call = buildFunctionCall {
            coneTypeOrNull = ConePrimitiveType.INT32
            calleeReference = buildResolvedNamedReference {
                name = targetName
                resolvedSymbol = targetSymbol
            }
        }
        val callerName = Name.identifier("caller")
        val callerSymbol = CfirNamedFunctionSymbol(CallableId(PackageName, callerName))
        val caller = buildNamedFunction {
            commonNamedFunctionDeclaration(callerSymbol)
            name = callerName
            dispatchReceiverType = ownerType
            returnTypeRef = intTypeRef()
            body = blockOf(ConePrimitiveType.INT32, call)
        }
        val owner = buildClass {
            commonClassDeclaration("VirtualDispatch", ownerSymbol)
            declarations += target
            declarations += caller
        }

        val chirModule = DefaultCfir2ChirConverter().convert(listOf(fileWith(owner))).modules.single()
        val chirOwner = chirModule.declarations.filterIsInstance<ChirClassDeclaration>().single()
        val chirCaller = chirOwner.memberDeclarations.filterIsInstance<ChirFunctionDeclaration>()
            .single { it.name == "caller" }
        val callExpression = chirCaller.blocks.flatMap { it.expressions }
            .filterIsInstance<ChirCallExpression>()
            .single()
        val virtualContext = checkNotNull(callExpression.virtualMethodContext)

        assertEquals(Cfir2ChirIds.dispatchReceiverId(chirCaller.semanticId), callExpression.callee.semanticId)
        assertTrue(callExpression.arguments.isEmpty())
        assertEquals(false, callExpression.isSuperCall)
        assertEquals("value", virtualContext.sourceCodeIdentifier)
        assertEquals(callExpression.callee.type, virtualContext.originalFunctionType.parameterTypes.first())
        assertEquals(ChirResolvedTypeRef(ChirPrimitiveType.INT32), virtualContext.originalFunctionType.returnType)
    }

    /**
     * 验证复合 Cone 类型映射只依赖 cfir2chir 的类型映射层，不反向依赖 CHIR tree 细节。
     */
    @Test
    fun `maps composite Cone types without depending on CFIR tree module from chir tree`() {
        val mapper = Cfir2ChirTypeMapper()

        assertEquals(
            ChirVArrayType(ChirResolvedTypeRef(ChirPrimitiveType.INT32), size = 4),
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
        val referenceClassId = classId("ReferenceClass")
        assertEquals(
            ChirRefType(
                ChirResolvedTypeRef(
                    ChirClassType(referenceClassId.toChirTypeDeclarationId()),
                ),
                mutable = true,
            ),
            mapper.mapConeType(ConeClassLikeType(ConeClassLikeLookupTagImpl(referenceClassId))),
        )
        assertThrows(Cfir2ChirConversionException::class.java) {
            mapper.mapConeType(ConePrimitiveType.IDEAL_INT)
        }
    }

    /** 同名类型参数必须按所属声明身份区分，并在重复映射时保持稳定。 */
    @Test
    fun `maps generic parameters with declaration identities`() {
        fun genericClass(name: String): CfirTypeParameter {
            val ownerSymbol = CfirClassSymbol(classId(name))
            val parameterSymbol = CfirTypeParameterSymbol()
            val parameter = buildTypeParameter {
                moduleData = TestModuleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Synthetic.Default
                attributes = CfirDeclarationAttributes.EMPTY
                containingDeclarationSymbol = ownerSymbol
                symbol = parameterSymbol
                this.name = Name.identifier("T")
            }
            buildClass {
                commonClassDeclaration(name, ownerSymbol)
                typeParameters += parameter
            }
            return parameter
        }

        val firstParameter = genericClass("GenericOwnerA")
        val secondParameter = genericClass("GenericOwnerB")
        val mapper = Cfir2ChirTypeMapper()
        val first = mapper.mapConeType(ConeTypeParameterTypeImpl(firstParameter.symbol.toLookupTag())) as ChirGenericType
        val second = mapper.mapConeType(ConeTypeParameterTypeImpl(secondParameter.symbol.toLookupTag())) as ChirGenericType

        assertEquals("T", first.identifier)
        assertEquals(0, first.declarationIndex)
        assertTrue(first.semanticIdentity != second.semanticIdentity)
        assertEquals(first, mapper.mapConeType(ConeTypeParameterTypeImpl(firstParameter.symbol.toLookupTag())))
    }

    /** 泛型调用将完成后的类型实参保留为 CHIR TypeInfo ABI 输入。 */
    @Test
    fun `preserves instantiated generic function call arguments`() {
        fun newTypeParameter(owner: CfirNamedFunctionSymbol, name: String): CfirTypeParameter {
            val symbol = CfirTypeParameterSymbol()
            return buildTypeParameter {
                moduleData = TestModuleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Synthetic.Default
                attributes = CfirDeclarationAttributes.EMPTY
                containingDeclarationSymbol = owner
                this.symbol = symbol
                this.name = Name.identifier(name)
            }
        }

        fun newValueParameter(
            owner: CfirNamedFunctionSymbol,
            name: String,
            type: CfirTypeRef,
        ): org.cangnova.cangjie.cfir.declarations.CfirValueParameter {
            val parameterName = Name.identifier(name)
            return buildValueParameter {
                moduleData = TestModuleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Synthetic.Default
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                symbol = CfirValueParameterSymbol(CallableId(PackageName, parameterName))
                containingDeclarationSymbol = owner
                isNamed = true
                this.name = parameterName
                returnTypeRef = type
            }
        }

        fun parameterAccess(
            parameter: org.cangnova.cangjie.cfir.declarations.CfirValueParameter,
            type: org.cangnova.cangjie.cfir.types.ConeCangJieType,
        ) = buildNamedAccessExpression {
            coneTypeOrNull = type
            calleeReference = buildResolvedNamedReference {
                name = parameter.name
                resolvedSymbol = parameter.symbol
            }
        }

        val targetName = Name.identifier("identity")
        val targetSymbol = CfirNamedFunctionSymbol(CallableId(PackageName, targetName))
        val targetTypeParameter = newTypeParameter(targetSymbol, "T")
        val targetType = ConeTypeParameterTypeImpl(targetTypeParameter.symbol.toLookupTag())
        val targetValueParameter = newValueParameter(targetSymbol, "value", typeRef(targetType))
        val targetBodyValue = parameterAccess(targetValueParameter, targetType)
        val target = buildNamedFunction {
            commonNamedFunctionDeclaration(targetSymbol)
            name = targetName
            typeParameters += targetTypeParameter
            valueParameters += targetValueParameter
            returnTypeRef = typeRef(targetType)
            body = buildBlock {
                coneTypeOrNull = targetType
                statements += targetBodyValue
            }
        }

        val callerName = Name.identifier("useIdentity")
        val callerSymbol = CfirNamedFunctionSymbol(CallableId(PackageName, callerName))
        val callerTypeParameter = newTypeParameter(callerSymbol, "U")
        val callerType = ConeTypeParameterTypeImpl(callerTypeParameter.symbol.toLookupTag())
        val callerValueParameter = newValueParameter(callerSymbol, "value", typeRef(callerType))
        val callArgument = parameterAccess(callerValueParameter, callerType)
        val call = buildFunctionCall {
            coneTypeOrNull = callerType
            calleeReference = buildResolvedNamedReference {
                name = targetName
                resolvedSymbol = targetSymbol
            }
            typeArguments += typeRef(callerType)
            argumentList = buildArgumentList {
                arguments += callArgument
            }
        }
        val caller = buildNamedFunction {
            commonNamedFunctionDeclaration(callerSymbol)
            name = callerName
            typeParameters += callerTypeParameter
            valueParameters += callerValueParameter
            returnTypeRef = typeRef(callerType)
            body = buildBlock {
                coneTypeOrNull = callerType
                statements += call
            }
        }

        val chirModule = DefaultCfir2ChirConverter().convert(listOf(fileWith(target, caller))).modules.single()
        val chirTarget = chirModule.declarations.filterIsInstance<ChirFunctionDeclaration>()
            .single { it.name == "identity" }
        val chirCaller = chirModule.declarations.filterIsInstance<ChirFunctionDeclaration>()
            .single { it.name == "useIdentity" }
        val callExpression = chirCaller.blocks.flatMap { it.expressions }
            .filterIsInstance<ChirCallExpression>()
            .single()

        assertEquals(1, chirTarget.genericTypeParameters.size)
        assertEquals(listOf(chirCaller.genericTypeParameters.single()), callExpression.instantiatedTypeArguments)
        assertEquals(callExpression.instantiatedTypeArguments.single(), callExpression.arguments.single().type)
        assertEquals(callExpression.instantiatedTypeArguments.single(), callExpression.resultType)
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
            buildInterface {
                commonClassLikeDeclaration("DrawableChild", CfirInterfaceSymbol(classId("DrawableChild")))
                superTypeRefs += typeRef(
                    ConeClassLikeType(ConeClassLikeLookupTagImpl(classId("Drawable")), isInterface = true),
                )
            },
            buildClass {
                commonClassDeclaration("Base", CfirClassSymbol(classId("Base")))
            },
            buildClass {
                commonClassDeclaration("DrawableHolder", CfirClassSymbol(classId("DrawableHolder")))
                superTypeRefs += typeRef(ConeClassLikeType(ConeClassLikeLookupTagImpl(classId("Base"))))
                superTypeRefs += typeRef(
                    ConeClassLikeType(ConeClassLikeLookupTagImpl(classId("Drawable")), isInterface = true),
                )
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
        val iface = module.declarations.filterIsInstance<ChirClassDeclaration>().single { it.name == "Drawable" }
        val childInterface = module.declarations.filterIsInstance<ChirClassDeclaration>().single { it.name == "DrawableChild" }
        val drawableHolder = module.declarations.filterIsInstance<ChirClassDeclaration>().single { it.name == "DrawableHolder" }
        val base = module.declarations.filterIsInstance<ChirClassDeclaration>().single { it.name == "Base" }
        val extend = module.declarations.filterIsInstance<ChirExtendDeclaration>().single()
        val typeAlias = module.declarations.filterIsInstance<ChirTypeDeclaration>().single { it.name == "Meters" }
        val primitive = module.declarations.filterIsInstance<ChirTypeDeclaration>().single { it.name == "BuiltinInt32" }
        val codeFragment = module.declarations.filterIsInstance<ChirFunctionDeclaration>().single { it.name == "<code-fragment>" }

        assertEquals("Point", struct.name)
        assertEquals(listOf("x"), struct.fieldDeclarations.map { it.name })
        assertTrue(ChirStringAttribute("cfir.kind", "interface") in iface.attributes)
        val drawableType = ChirResolvedTypeRef(ChirClassType(classId("Drawable").toChirTypeDeclarationId()))
        assertEquals(listOf(drawableType), childInterface.implementedTypes)
        assertEquals(listOf(ChirResolvedTypeRef(ChirClassType(base.typeDeclarationId))), drawableHolder.superTypes)
        assertEquals(listOf(drawableType), drawableHolder.implementedTypes)
        assertEquals("sample/Point", extend.targetType.renderName)
        assertEquals(
            listOf(drawableType),
            extend.extendedTypes,
        )
        assertEquals("Meters", typeAlias.name)
        assertTrue(typeAlias.attributes.any { it is ChirStringAttribute && it.key == "cfir.expandedType" && it.value == "int32" })
        assertTrue(primitive.attributes.any { it is ChirStringAttribute && it.key == "cfir.primitive.kind" && it.value == "INT32" })
        assertEquals("int32", codeFragment.returnType.renderName)
        assertTrue(ChirStringAttribute("cfir.kind", "codeFragment") in codeFragment.attributes)
    }

    /**
     * 验证 named argument、if-available 与 augmented assignment 会以独立操作名 lowering，并保留语义属性。
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

        assertTrue(Cfir2ChirOperation.CFIR_NAMED_ARGUMENT.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_IF_AVAILABLE.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_AUGMENTED_ASSIGNMENT.canonicalName in operations)

        val namedArgument = otherExpressions.single { it.operation == Cfir2ChirOperation.CFIR_NAMED_ARGUMENT.canonicalName }
        assertTrue(
            namedArgument.attributes.any {
                it is ChirStringAttribute && it.key == "cfir.argumentName" && it.value == "value"
            },
        )
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
     * 验证 CFIR 局部变量的分配、初始化、赋值和读取共享同一 CHIR 地址身份。
     */
    @Test
    fun `binds local variable storage to its alloca result identity`() {
        val variableName = Name.identifier("mutableLocal")
        val symbol = CfirFieldVariableSymbol(CallableId(PackageName, variableName))
        val variable = buildFieldVariable {
            commonFieldVariableDeclaration(symbol)
            isLocal = true
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            name = variableName
            returnTypeRef = typeRef(ConePrimitiveType.INT32)
            isVar = true
            initializer = intLiteral(1)
        }
        val assignment = buildAssignment {
            coneTypeOrNull = ConePrimitiveType.UNIT
            lValue = localAccess(symbol, variableName)
            rValue = intLiteral(2)
        }
        val function = functionWithBody(
            name = "mutableLocalStorage",
            returnType = ConePrimitiveType.INT32,
            bodyType = ConePrimitiveType.INT32,
            statements = listOf(variable, assignment, localAccess(symbol, variableName)),
        )

        val chirFunction = convertSingleFunction(function)
        val memories = chirFunction.blocks.flatMap { it.expressions }.filterIsInstance<ChirMemoryExpression>()
        val alloca = memories.single { it.operation == "alloca" }
        val localAddress = (alloca.resultType as ChirResolvedTypeRef).type as ChirRefType
        val localStores = memories.filter { it.operation == "store" }
        val localLoad = memories.single { it.operation == "load" }

        assertEquals(ChirResolvedTypeRef(ChirPrimitiveType.INT32), localAddress.referencedType)
        assertEquals(alloca.semanticId, localStores.first().address.semanticId)
        assertEquals(alloca.semanticId, localLoad.address.semanticId)
        assertEquals("1", (alloca.address as ChirConstantValue).literal)
        assertEquals(2, localStores.size)
    }

    /**
     * 转换单个 CFIR 函数并返回唯一 CHIR 函数声明。
     */
    private fun convertSingleFunction(function: CfirNamedFunction): ChirFunctionDeclaration =
        convertModule(function).declarations.filterIsInstance<ChirFunctionDeclaration>().single()

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
     * 构造绑定到给定变量符号的无接收者局部读取。
     */
    private fun localAccess(symbol: CfirFieldVariableSymbol, name: Name) = buildNamedAccessExpression {
        coneTypeOrNull = ConePrimitiveType.INT32
        calleeReference = buildResolvedNamedReference {
            this.name = name
            resolvedSymbol = symbol
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

    /** 填充 class builder 的 class-like 公共字段。 */
    private fun CfirClassBuilder.commonClassDeclaration(
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
