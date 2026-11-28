package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
import org.cangnova.cangjie.cfir.declarations.builder.buildAnonymousFunction
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirCatch
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.expressions.builder.buildAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildArrayLiteral
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildCatch
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.buildLoopExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedArgumentExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildOptionalExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildOptionalChainExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildReturnExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildTryExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildTupleLiteral
import org.cangnova.cangjie.cfir.expressions.builder.buildTypeConversion
import org.cangnova.cangjie.cfir.patterns.builder.buildCatchPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildWildcardPattern
import org.cangnova.cangjie.cfir.references.CfirResolvedBuiltinOperatorReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedBuiltinOperatorReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.resolve.inference.ExpectedArgumentType
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildFunctionSymbol
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.newResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.newTestSession
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.registerHierarchyProviders
import org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperatorSignature
import org.cangnova.cangjie.cfir.types.ConeAttribute
import org.cangnova.cangjie.cfir.types.ConeAttributes
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeIdealFloatConstantType
import org.cangnova.cangjie.cfir.types.ConeIdealFloatOperatorType
import org.cangnova.cangjie.cfir.types.ConeIdealIntConstantType
import org.cangnova.cangjie.cfir.types.ConeIdealIntOperatorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 用真实 typed CFIR 和会话组件验证角色化近似边界：结果目标、控制流输入及转换输入各有归属。
 * 只断言类型、属性、来源和节点身份，不调用 normalizer 的内部规则生成期望或匹配错误文案。
 */
@OptIn(CfirImplementationDetail::class)
class IntegerLiteralAndOperatorApproximationTransformerTest {
    /** tuple 的两个位置分别消费 Int8/Float32，整体来源属性不会被目标类型的属性替换。 */
    @Test
    fun mixedTupleUsesElementTargetsAndRetainsAttributesAndNodeIdentities() {
        val fixture = Fixture()
        val integer = fixture.integer(7L)
        val floating = fixture.floating(1.25)
        val integerSource = integer.source
        val floatingSource = floating.source
        val marker = MarkerAttribute("tuple-source")
        val tuple = buildTupleLiteral {
            source = TestSource("(7, 1.25)")
            coneTypeOrNull = ConeTupleType(
                listOf(checkNotNull(integer.coneTypeOrNull), checkNotNull(floating.coneTypeOrNull)),
                ConeAttributes.create(listOf(marker)),
            )
            elements += listOf(integer, floating)
        }
        val target = ConeTupleType(
            listOf(ConePrimitiveType.INT8, ConePrimitiveType.FLOAT32),
            ConeAttributes.create(listOf(MarkerAttribute("tuple-context"))),
        )

        assertSame(tuple, fixture.normalize(tuple, target))

        assertEquals(2, tuple.elements.size)
        assertSame(integer, tuple.elements[0])
        assertSame(floating, tuple.elements[1])
        assertEquals(ConePrimitiveType.INT8, integer.coneTypeOrNull)
        assertEquals(ConePrimitiveType.FLOAT32, floating.coneTypeOrNull)
        assertEquals(7L, integer.value)
        assertEquals(1.25, floating.value)
        assertSame(integerSource, integer.source)
        assertSame(floatingSource, floating.source)
        val type = tuple.coneTypeOrNull
        assertTrue(type is ConeTupleType)
        assertEquals(listOf(ConePrimitiveType.INT8, ConePrimitiveType.FLOAT32), (type as ConeTupleType).elementTypes)
        assertEquals(marker, type.attributes[MarkerAttribute::class])
    }

    /** VArray 元素目标是 UInt16，数组的固定尺寸与来源属性仍由原 VArray 类型拥有。 */
    @Test
    fun varrayRetainsSizeAttributesAndElementIdentitiesWhileUsingUInt16() {
        val fixture = Fixture()
        val literals = listOf(fixture.integer(1L), fixture.integer(2L), fixture.integer(3L))
        val marker = MarkerAttribute("varray-source")
        val array = buildArrayLiteral {
            source = TestSource("[1, 2, 3]")
            coneTypeOrNull = ConeVArrayType(ConeIdealIntOperatorType(), 3L, ConeAttributes.create(listOf(marker)))
            elements += literals
        }
        val target = ConeVArrayType(
            ConePrimitiveType.UINT16, 3L,
            ConeAttributes.create(listOf(MarkerAttribute("varray-context"))),
        )

        assertSame(array, fixture.normalize(array, target))

        assertEquals(3, array.elements.size)
        literals.forEachIndexed { index, literal ->
            assertSame(literal, array.elements[index])
            assertEquals(ConePrimitiveType.UINT16, literal.coneTypeOrNull)
            assertEquals((index + 1).toLong(), literal.value)
        }
        val type = array.coneTypeOrNull
        assertTrue(type is ConeVArrayType)
        val varray = type as ConeVArrayType
        assertEquals(ConePrimitiveType.UINT16, varray.elementType)
        assertEquals(3L, varray.size)
        assertEquals(marker, varray.attributes[MarkerAttribute::class])
    }

    /** return 显式绑定 Int32 函数；外围 block 的 Float64 目标不能越过该绑定边界。 */
    @Test
    fun returnResultUsesBoundFunctionTypeInsteadOfOuterBlockTarget() {
        val fixture = Fixture()
        val function = buildFunctionSymbol(fixture.session, "returnOwner", returnType = ConePrimitiveType.INT32).cfir
        val functionTypeRef = function.returnTypeRef
        val target = CfirFunctionTarget(isLambda = false).also { it.bind(function) }
        val literal = fixture.integer(7L)
        val returnExpression = buildReturnExpression {
            source = TestSource("return 7")
            coneTypeOrNull = ConePrimitiveType.NOTHING
            this.target = target
            result = literal
        }
        val outer = buildBlock {
            source = TestSource("{ return 7 }")
            coneTypeOrNull = ConePrimitiveType.NOTHING
            statements += returnExpression
        }

        assertSame(outer, fixture.normalize(outer, ConePrimitiveType.FLOAT64))

        assertSame(returnExpression, outer.statements.single())
        assertSame(literal, returnExpression.result)
        assertEquals(ConePrimitiveType.INT32, literal.coneTypeOrNull)
        assertSame(target, returnExpression.target)
        assertSame(function, target.labeledElement)
        assertSame(functionTypeRef, function.returnTypeRef)
        assertEquals(ConePrimitiveType.NOTHING, returnExpression.coneTypeOrNull)
        assertEquals(ConePrimitiveType.NOTHING, outer.coneTypeOrNull)
    }

    /** try/catch 的值分支消费 Float32，finally 的丢弃值各自默认化为 Int64/Float64。 */
    @Test
    fun tryAndCatchUseResultTargetWhileFinallyDefaultsIndependently() {
        val fixture = Fixture()
        val tryValue = fixture.floating(1.25)
        val catchValue = fixture.floating(2.5)
        val finallyInteger = fixture.integer(9L)
        val finallyFloating = fixture.floating(3.75)
        val tryBlock = fixture.valueBlock(tryValue)
        val catchBody = fixture.valueBlock(catchValue)
        val catchClause = fixture.catchClause(catchBody)
        val pattern = catchClause.pattern
        val patternTypeRef = pattern.semanticTypeRef
        val finallyBlock = buildBlock {
            source = TestSource("finally")
            coneTypeOrNull = ConePrimitiveType.UNIT
            statements += listOf(finallyInteger, finallyFloating)
        }
        val expression = buildTryExpression {
            source = TestSource("try/catch/finally")
            coneTypeOrNull = ConeIdealFloatOperatorType()
            this.tryBlock = tryBlock
            catches += catchClause
            this.finallyBlock = finallyBlock
        }

        assertSame(expression, fixture.normalize(expression, ConePrimitiveType.FLOAT32))

        assertSame(tryBlock, expression.tryBlock)
        assertSame(catchClause, expression.catches.single())
        assertSame(catchBody, catchClause.body)
        assertSame(finallyBlock, expression.finallyBlock)
        assertSame(tryValue, tryBlock.statements.single())
        assertSame(catchValue, catchBody.statements.single())
        assertEquals(ConePrimitiveType.FLOAT32, tryValue.coneTypeOrNull)
        assertEquals(ConePrimitiveType.FLOAT32, catchValue.coneTypeOrNull)
        assertEquals(ConePrimitiveType.FLOAT32, tryBlock.coneTypeOrNull)
        assertEquals(ConePrimitiveType.FLOAT32, catchBody.coneTypeOrNull)
        assertEquals(ConePrimitiveType.FLOAT32, catchClause.coneTypeOrNull)
        assertEquals(ConePrimitiveType.FLOAT32, expression.coneTypeOrNull)
        assertSame(pattern, catchClause.pattern)
        assertSame(patternTypeRef, pattern.semanticTypeRef)
        assertSame(finallyInteger, finallyBlock.statements[0])
        assertSame(finallyFloating, finallyBlock.statements[1])
        assertEquals(ConePrimitiveType.INT64, finallyInteger.coneTypeOrNull)
        assertEquals(ConePrimitiveType.FLOAT64, finallyFloating.coneTypeOrNull)
        assertEquals(ConePrimitiveType.UNIT, finallyBlock.coneTypeOrNull)
    }

    /** while/do-while 均丢弃正文结果，外部 Float32 目标不能改变正文的默认数值类型。 */
    @Test
    fun whileAndDoWhileBodiesDoNotConsumeOuterResultTarget() {
        val fixture = Fixture()
        for (isDoWhile in listOf(false, true)) {
            val integer = fixture.integer(1L)
            val floating = fixture.floating(2.5)
            val condition = buildLiteralExpression {
                source = TestSource("true")
                coneTypeOrNull = ConePrimitiveType.BOOLEAN
                kind = CfirLiteralKind.BOOLEAN
                value = true
            }
            val body = buildBlock {
                source = TestSource("loop body")
                coneTypeOrNull = ConePrimitiveType.UNIT
                statements += listOf(integer, floating)
            }
            val loop = buildLoopExpression {
                source = TestSource(if (isDoWhile) "do/while" else "while")
                coneTypeOrNull = ConePrimitiveType.UNIT
                this.condition = condition
                this.body = body
                this.isDoWhile = isDoWhile
            }

            assertSame(loop, fixture.normalize(loop, ConePrimitiveType.FLOAT32))

            assertSame(body, loop.body)
            assertSame(condition, loop.condition)
            assertSame(integer, body.statements[0])
            assertSame(floating, body.statements[1])
            assertEquals(ConePrimitiveType.INT64, integer.coneTypeOrNull)
            assertEquals(ConePrimitiveType.FLOAT64, floating.coneTypeOrNull)
            assertEquals(ConePrimitiveType.BOOLEAN, condition.coneTypeOrNull)
            assertEquals(ConePrimitiveType.UNIT, body.coneTypeOrNull)
            assertEquals(ConePrimitiveType.UNIT, loop.coneTypeOrNull)
        }
    }

    /** UInt8(300) 的输入先作为 Int64 求值，不能预先窄化成转换目标 UInt8。 */
    @Test
    fun explicitNumericConversionDefaultsInputBeforeConvertingResult() {
        val fixture = Fixture()
        val literal = fixture.integer(300L)
        val targetTypeRef = buildResolvedTypeRef { coneType = ConePrimitiveType.UINT8 }
        val conversion = buildTypeConversion {
            source = TestSource("UInt8(300)")
            coneTypeOrNull = ConePrimitiveType.UINT8
            argument = literal
            this.targetTypeRef = targetTypeRef
        }

        assertSame(conversion, fixture.normalize(conversion, ConePrimitiveType.FLOAT64))

        assertSame(literal, conversion.argument)
        assertEquals(ConePrimitiveType.INT64, literal.coneTypeOrNull)
        assertEquals(300L, literal.value)
        assertEquals(CfirLiteralKind.INT, literal.kind)
        assertSame(targetTypeRef, conversion.targetTypeRef)
        assertEquals(ConePrimitiveType.UINT8, conversion.coneTypeOrNull)
    }

    /** 内层成功定型不能擦掉 wrapper 已经承载的错误及其关联类型和属性。 */
    @Test
    fun successfulInnerApproximationDoesNotRemoveWrappedErrorType() {
        val fixture = Fixture()
        val literal = fixture.integer(7L)
        val diagnostic = ConeSimpleDiagnostic("retained source error")
        val marker = MarkerAttribute("error-source")
        val delegatedType = ConePrimitiveType.FLOAT64
        val error = ConeErrorType(
            diagnostic, delegatedType = delegatedType,
            attributes = ConeAttributes.create(listOf(marker)),
        )
        val wrapper = buildNamedArgumentExpression {
            source = TestSource("value: 7")
            argumentName = Name.identifier("value")
            expression = literal
            coneTypeOrNull = error
        }

        assertSame(wrapper, fixture.normalize(wrapper, ConePrimitiveType.INT8))

        assertSame(literal, wrapper.expression)
        assertEquals(ConePrimitiveType.INT8, literal.coneTypeOrNull)
        assertSame(error, wrapper.coneTypeOrNull)
        assertSame(diagnostic, error.diagnostic)
        assertSame(delegatedType, error.delegatedType)
        assertEquals(marker, error.attributes[MarkerAttribute::class])
    }

    /** ? 的输出已是 payload，输入仍是 Option；不能把它当透明 wrapper 同步成输入类型。 */
    @Test
    fun optionalExpressionRetainsPayloadWhileItsInputRemainsOption() {
        val fixture = Fixture()
        val payload = ConePrimitiveType.INT8
        val option = fixture.optionType(payload)
        val input = fixture.parameterAccess("optionalInput", option)
        val reference = input.calleeReference
        val expression = buildOptionalExpression {
            source = TestSource("input?")
            coneTypeOrNull = payload
            this.expression = input
        }

        assertSame(expression, fixture.normalize(expression, payload))

        assertSame(input, expression.expression)
        assertSame(reference, input.calleeReference)
        assertEquals(option, input.coneTypeOrNull)
        assertEquals(payload, expression.coneTypeOrNull)
    }

    /** selector 的 payload 与整条链的 lifted Option 分别拥有类型，外部目标不能覆盖二者。 */
    @Test
    fun optionalChainRetainsLiftedOptionAndIndependentSelectorType() {
        val fixture = Fixture()
        val payload = ConePrimitiveType.INT8
        val option = fixture.optionType(payload)
        val input = fixture.parameterAccess("chainInput", option)
        val selector = buildOptionalExpression {
            source = TestSource("input?")
            coneTypeOrNull = payload
            expression = input
        }
        val chain = buildOptionalChainExpression {
            source = TestSource("optional chain")
            coneTypeOrNull = option
            expression = selector
        }

        assertSame(chain, fixture.normalize(chain, fixture.optionType(ConePrimitiveType.FLOAT32)))

        assertSame(selector, chain.expression)
        assertSame(input, selector.expression)
        assertEquals(payload, selector.coneTypeOrNull)
        assertEquals(option, input.coneTypeOrNull)
        assertEquals(option, chain.coneTypeOrNull)
    }

    /** 已检查的 tuple 保存 interface/Option 装箱目标，元素自身的裸 primitive 类型不能取代它。 */
    @Test
    fun existingTupleInterfaceAndOptionBoxingTypesAreRetained() {
        val fixture = Fixture()
        val any = ExtendTestFixtures.newInterface(fixture.session.moduleData, "Any", StdlibClassIds.Any)
        fixture.session.registerHierarchyProviders(listOf(any))
        val interfaceType = ConeClassLikeType(any.symbol.toLookupTag(), isInterface = true)
        val optionType = fixture.optionType(ConePrimitiveType.INT8)
        val marker = MarkerAttribute("boxed-tuple-source")
        val tupleType = ConeTupleType(listOf(interfaceType, optionType), ConeAttributes.create(listOf(marker)))
        val first = fixture.concreteInteger(1L, ConePrimitiveType.INT64)
        val second = fixture.concreteInteger(2L, ConePrimitiveType.INT8)
        val tuple = buildTupleLiteral {
            source = TestSource("boxed tuple")
            coneTypeOrNull = tupleType
            elements += listOf(first, second)
        }

        assertSame(tuple, fixture.normalize(tuple, tupleType))

        assertSame(first, tuple.elements[0])
        assertSame(second, tuple.elements[1])
        assertEquals(ConePrimitiveType.INT64, first.coneTypeOrNull)
        assertEquals(ConePrimitiveType.INT8, second.coneTypeOrNull)
        val resultType = tuple.coneTypeOrNull
        assertTrue(resultType is ConeTupleType)
        val result = resultType as ConeTupleType
        assertEquals(interfaceType, result.elementTypes[0])
        assertEquals(optionType, result.elementTypes[1])
        assertTrue((result.elementTypes[0] as ConeClassLikeType).isInterface)
        assertEquals(marker, result.attributes[MarkerAttribute::class])
    }

    /** 旧 lambda header 是 IdealInt；完成后的 header、函数类型与绑定 explicit return 必须一致。 */
    @Test
    fun anonymousFunctionHeaderAndExplicitReturnAgreeOnInt8Target() {
        val fixture = Fixture()
        val literal = fixture.integer(7L)
        val target = CfirFunctionTarget(isLambda = true)
        val returnExpression = buildReturnExpression {
            source = TestSource("return 7")
            coneTypeOrNull = ConePrimitiveType.NOTHING
            this.target = target
            result = literal
        }
        val body = buildBlock {
            source = TestSource("lambda body")
            coneTypeOrNull = ConePrimitiveType.NOTHING
            statements += returnExpression
        }
        val oldFunctionType = ConeFunctionType(emptyList(), ConePrimitiveType.IDEAL_INT, isClosureType = true)
        val function = buildAnonymousFunction {
            source = TestSource("anonymous function")
            moduleData = fixture.session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            isLocal = true
            status = DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
            returnTypeRef = buildResolvedTypeRef { coneType = ConePrimitiveType.IDEAL_INT }
            typeRef = buildResolvedTypeRef { coneType = oldFunctionType }
            symbol = CfirAnonymousFunctionSymbol()
            hasExplicitParameterList = false
            isLambda = true
            this.body = body
        }
        target.bind(function)
        val expression = buildAnonymousFunctionExpression {
            source = TestSource("lambda expression")
            anonymousFunction = function
            isTrailingLambda = false
        }
        val functionTarget = ConeFunctionType(emptyList(), ConePrimitiveType.INT8, isClosureType = true)

        assertSame(expression, fixture.completeLiteralReturnLambda(expression, functionTarget))

        assertSame(function, expression.anonymousFunction)
        assertSame(body, function.body)
        assertSame(returnExpression, body.statements.single())
        assertSame(target, returnExpression.target)
        assertSame(function, target.labeledElement)
        assertSame(literal, returnExpression.result)
        assertEquals(ConePrimitiveType.INT8, function.returnTypeRef.coneTypeOrNull)
        assertEquals(function.returnTypeRef.coneTypeOrNull, literal.coneTypeOrNull)
        assertEquals(functionTarget, function.typeRef.coneTypeOrNull)
        assertEquals(functionTarget, expression.coneTypeOrNull)
        assertEquals(ConePrimitiveType.NOTHING, returnExpression.coneTypeOrNull)
    }

    /** 两个合法 Int64 操作数和成功 builtin binding 不能清除根节点原有的错误。 */
    @Test
    fun validBuiltinOperandsDoNotClearRootErrorType() {
        val fixture = Fixture()
        val left = fixture.concreteInteger(1L, ConePrimitiveType.INT64)
        val right = fixture.concreteInteger(2L, ConePrimitiveType.INT64)
        val signature = BuiltinPrimitiveOperatorSignature(
            OperatorNameConventions.PLUS, PrimitiveTypeKind.INT64,
            listOf(PrimitiveTypeKind.INT64), PrimitiveTypeKind.INT64,
        )
        val reference = buildResolvedBuiltinOperatorReference {
            source = TestSource("+")
            name = OperatorNameConventions.PLUS
            this.signature = signature
        }
        val diagnostic = ConeSimpleDiagnostic("retained builtin root error")
        val marker = MarkerAttribute("builtin-error-source")
        val error = ConeErrorType(
            diagnostic, delegatedType = ConePrimitiveType.INT64,
            attributes = ConeAttributes.create(listOf(marker)),
        )
        val call = buildFunctionCall {
            source = TestSource("left + right")
            origin = CfirFunctionCallOrigin.Operator
            explicitReceiver = left
            argumentList = buildArgumentList { arguments += right }
            calleeReference = reference
            coneTypeOrNull = error
        }

        assertSame(call, fixture.normalize(call, ConePrimitiveType.INT64))

        assertSame(left, call.explicitReceiver)
        assertSame(right, call.argumentList.arguments.single())
        assertEquals(ConePrimitiveType.INT64, left.coneTypeOrNull)
        assertEquals(ConePrimitiveType.INT64, right.coneTypeOrNull)
        assertTrue(call.calleeReference is CfirResolvedBuiltinOperatorReference)
        assertEquals(signature, (call.calleeReference as CfirResolvedBuiltinOperatorReference).signature)
        assertSame(error, call.coneTypeOrNull)
        assertSame(diagnostic, error.diagnostic)
        assertEquals(ConePrimitiveType.INT64, error.delegatedType)
        assertEquals(marker, error.attributes[MarkerAttribute::class])
    }

    /** 正式会话组件提供 normalizer；表达式、类型引用与 catch pattern 均使用真实 builder。 */
    private class Fixture {
        val session = newTestSession()
        private val resolutionContext = newResolutionContext(session)
        private val transformer = resolutionContext.bodyResolveComponents.integerLiteralAndOperatorApproximationTransformer

        fun normalize(expression: CfirExpression, target: ConeCangJieType?): CfirExpression =
            expression.transformSingle(transformer, target)

        /** lambda 签名与返回值由正式 completion owner 完成；CFG 查询使用原输入的真实 return。 */
        fun completeLiteralReturnLambda(
            expression: CfirAnonymousFunctionExpression,
            target: ConeFunctionType,
        ): CfirAnonymousFunctionExpression {
            val function = expression.anonymousFunction
            val body = checkNotNull(function.body)
            val returned = body.statements.single() as CfirReturnExpression
            val literal = returned.result as CfirLiteralExpression
            val analyzer = resolutionContext.bodyResolveComponents.dataFlowAnalyzer
            analyzer.enterFunction(function)
            analyzer.enterBlock(body)
            analyzer.enterJump(returned)
            analyzer.exitLiteralExpression(literal)
            analyzer.exitJump(returned)
            analyzer.exitBlock(body)
            function.replaceControlFlowGraphReference(checkNotNull(analyzer.exitFunction(function)))

            // 输入没有泛型变量，空 substitutor 是该约束完成域的实际替换结果。
            val writer = resolutionContext.bodyResolveComponents.callCompleter.createCompletionResultsWriter(ConeSubstitutor.Empty)
            return expression.transformSingle(writer, ExpectedArgumentType.ExpectedType(target, argumentReplacements = null))
        }

        fun integer(value: Long): CfirLiteralExpression = buildLiteralExpression {
            source = TestSource(value.toString())
            coneTypeOrNull = ConeIdealIntConstantType(value)
            kind = CfirLiteralKind.INT
            this.value = value
        }

        fun floating(value: Double): CfirLiteralExpression = buildLiteralExpression {
            source = TestSource(value.toString())
            coneTypeOrNull = ConeIdealFloatConstantType(value)
            kind = CfirLiteralKind.FLOAT
            this.value = value
        }

        fun concreteInteger(value: Long, type: ConePrimitiveType): CfirLiteralExpression = buildLiteralExpression {
            source = TestSource(value.toString())
            coneTypeOrNull = type
            kind = CfirLiteralKind.INT
            this.value = value
        }

        fun optionType(payload: ConeCangJieType): ConeClassLikeType =
            ConeClassLikeType(ConeClassLikeLookupTagImpl(StdlibClassIds.Option), listOf(payload))

        /** Option 输入来自实际绑定的函数参数读取，不给整数 literal 伪造 Option 类型。 */
        fun parameterAccess(ownerName: String, type: ConeCangJieType): CfirNamedAccessExpression {
            val owner = buildFunctionSymbol(
                session, ownerName, returnType = type,
                parameterTypes = listOf(type), parameterNames = listOf("input"),
            ).cfir
            val parameter = owner.valueParameters.single()
            return buildNamedAccessExpression {
                source = TestSource("input")
                coneTypeOrNull = type
                calleeReference = buildResolvedNamedReference {
                    name = parameter.name
                    resolvedSymbol = parameter.symbol
                }
            }
        }

        fun valueBlock(expression: CfirExpression): CfirBlock = buildBlock {
            source = TestSource("value block")
            coneTypeOrNull = expression.coneTypeOrNull
            statements += expression
        }

        fun catchClause(body: CfirBlock): CfirCatch {
            val exception = ExtendTestFixtures.newClass(session.moduleData, "Exception", StdlibClassIds.Exception)
            session.registerHierarchyProviders(listOf(exception))
            val exceptionType = buildResolvedTypeRef {
                source = TestSource("Exception")
                coneType = ConeClassLikeType(exception.symbol.toLookupTag())
            }
            val pattern = buildCatchPattern {
                source = TestSource("_: Exception")
                nestedPattern = buildWildcardPattern {}
                typeRefs += exceptionType
                semanticTypeRef = exceptionType
            }
            return buildCatch {
                source = TestSource("catch")
                coneTypeOrNull = body.coneTypeOrNull
                this.pattern = pattern
                this.body = body
            }
        }
    }

    /** 合法的 ConeAttribute 扩展点，只标记属性归属，避免给固定 Empty attributes 的 Ideal 常量虚构属性。 */
    private data class MarkerAttribute(val owner: String) : ConeAttribute<MarkerAttribute>() {
        override val key = MarkerAttribute::class
        override val keepInInferredDeclarationType: Boolean = true
        override val implementsEquality: Boolean = true

        override fun union(other: MarkerAttribute?): MarkerAttribute? = if (other == null || other == this) this else null
        override fun intersect(other: MarkerAttribute?): MarkerAttribute? = if (other == this) this else null
        override fun add(other: MarkerAttribute?): MarkerAttribute? = union(other)
        override fun isSubtypeOf(other: MarkerAttribute?): Boolean = other == null || other == this
        override fun toString(): String = owner
    }

    /** 稳定的非空来源，用于验证正常化保持原节点的来源身份。 */
    private class TestSource(identity: String) : CjBinarySourceElement(identity, null, identity)
}
