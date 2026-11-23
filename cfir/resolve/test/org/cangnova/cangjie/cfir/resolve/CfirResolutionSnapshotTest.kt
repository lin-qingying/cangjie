package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostic.ConeInvalidCatchTypeError
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvedArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.buildResolvedArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildErrorExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildLazyBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildLazyExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildHandleClause
import org.cangnova.cangjie.cfir.expressions.builder.buildLetPatternExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.buildMatchBranch
import org.cangnova.cangjie.cfir.expressions.builder.buildMatchExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildOptionalExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedArgumentExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildTupleLiteral
import org.cangnova.cangjie.cfir.expressions.builder.buildTypeConversion
import org.cangnova.cangjie.cfir.expressions.builder.buildSubscriptExpression
import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.builder.buildPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildPatternVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildResourceVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirResourceVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.cfir.patterns.builder.buildCatchPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildCommandTypePattern
import org.cangnova.cangjie.cfir.patterns.builder.buildBindingPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildWildcardPattern
import org.cangnova.cangjie.cfir.patterns.restorePatternTreeRoots
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** 快照恢复真实可变类型状态，不向错误表达式的派生类型 getter 写回。 */
class CfirResolutionSnapshotTest {
    /** PCLA 的类型槽恢复不能使暂停中的实参和正文迭代器失效。 */
    @Test
    fun `type restoration preserves active body and argument iterators`() {
        val first = createIntLiteral(1)
        val second = createIntLiteral(2)
        val arguments = buildArgumentList { this.arguments.addAll(listOf(first, second)) }
        val call = buildFunctionCall {
            calleeReference = buildNamedReference { name = Name.identifier("call") }
            argumentList = arguments
        }
        val body = buildBlock { statements.addAll(listOf(call, second)) }
        val argumentIterator = arguments.arguments.iterator()
        val bodyIterator = body.statements.iterator()
        assertSame(first, argumentIterator.next())
        assertSame(call, bodyIterator.next())
        val snapshot = CfirResolutionSnapshot.capture(body)
        first.replaceConeTypeOrNull(ConePrimitiveType.INT64)
        snapshot.restoreTypeResolutionState()
        assertSame(second, argumentIterator.next())
        assertSame(second, bodyIterator.next())
        assertEquals(ConePrimitiveType.INT32, first.coneTypeOrNull)
    }

    /** 数值/字符默认化可以替换 tuple 元素；后一次解析必须重新看到原始 literal 节点和值。 */
    @Test
    fun `restore restores tuple literal element slots`() {
        val first = createIntLiteral(1)
        val second = createIntLiteral(2)
        val tuple = buildTupleLiteral { elements.addAll(listOf(first, second)) }
        val snapshot = CfirResolutionSnapshot.capture(tuple)

        repeat(2) {
            tuple.transformElements(replacingLiterals(mapOf(first to createIntLiteral(11), second to createIntLiteral(22))), Unit)
            first.replaceConeTypeOrNull(ConePrimitiveType.INT64)
            snapshot.restore()
            assertArgumentIdentities(listOf(first, second), tuple.elements)
            assertEquals(1, first.value)
            assertEquals(2, second.value)
            assertEquals(ConePrimitiveType.INT32, first.coneTypeOrNull)
        }
    }

    /** named wrapper 和转换节点都可以原地保留对象而替换 child，目标类型槽也须恢复。 */
    @Test
    fun `restore restores wrapped expression and conversion type slots`() {
        val literal = createIntLiteral(1)
        val originalTypeRef = ConePrimitiveType.INT32.toCfirResolvedTypeRef()
        val conversion = buildTypeConversion {
            argument = literal
            targetTypeRef = originalTypeRef
        }
        val wrapper = buildNamedArgumentExpression {
            argumentName = Name.identifier("value")
            expression = conversion
        }
        val snapshot = CfirResolutionSnapshot.capture(wrapper)

        repeat(2) {
            conversion.transformArgument(replacingLiterals(mapOf(literal to createIntLiteral(2))), Unit)
            conversion.transformTargetTypeRef(object : CfirTransformer<Unit>() {
                @Suppress("UNCHECKED_CAST")
                override fun <E : CfirElement> transformElement(element: E, data: Unit): E =
                    ConePrimitiveType.INT64.toCfirResolvedTypeRef() as E
            }, Unit)
            wrapper.transformExpression(object : CfirTransformer<Unit>() {
                @Suppress("UNCHECKED_CAST")
                override fun <E : CfirElement> transformElement(element: E, data: Unit): E = createIntLiteral(3) as E
            }, Unit)
            snapshot.restore()
            assertSame(conversion, wrapper.expression)
            assertSame(literal, conversion.argument)
            assertSame(originalTypeRef, conversion.targetTypeRef)
        }
    }

    /** 惰性正文/表达式的类型 getter 尚不可读；快照只恢复拥有者保留的原占位节点。 */
    @Test
    fun `restore preserves opaque lazy nodes before materialization`() {
        val lazyBlock = buildLazyBlock()
        val lazyExpression = buildLazyExpression()
        val literal = createIntLiteral(1)
        val root = buildBlock { statements.addAll(listOf(lazyBlock, lazyExpression, literal)) }
        val snapshot = CfirResolutionSnapshot.capture(root)

        repeat(2) {
            root.replaceStatements(listOf(createIntLiteral(2)))
            literal.replaceConeTypeOrNull(ConePrimitiveType.INT64)
            snapshot.restore()
            assertEquals(3, root.statements.size)
            assertSame(lazyBlock, root.statements[0])
            assertSame(lazyExpression, root.statements[1])
            assertSame(literal, root.statements[2])
            assertEquals(ConePrimitiveType.INT32, literal.coneTypeOrNull)
        }
    }

    /** 已检查的 optional payload 不能跨失败候选保留，原接收者的类型也必须恢复。 */
    @Test
    fun `restore discards a checked optional subject type`() {
        val receiver = buildLiteralExpression {
            kind = CfirLiteralKind.INT
            value = 1
            coneTypeOrNull = ConePrimitiveType.INT32
        }
        val subject = buildOptionalExpression { expression = receiver }
        val snapshot = CfirResolutionSnapshot.capture(subject)
        receiver.replaceConeTypeOrNull(ConePrimitiveType.INT64)
        subject.replaceConeTypeOrNull(ConePrimitiveType.INT64)
        snapshot.restore()
        assertNull(subject.coneTypeOrNull)
        assertEquals(ConePrimitiveType.INT32, receiver.coneTypeOrNull)
    }

    /** 复合赋值的语义调用可回指同一个下标；快照不能递归失控或遗留失败候选的绑定。 */
    @Test
    fun `restore preserves operator bindings in a shared subscript graph`() {
        val subscript = buildSubscriptExpression {
            receiver = buildErrorExpression { diagnostic = ConeSimpleDiagnostic("receiver") }
        }
        val originalReference = buildNamedReference { name = Name.identifier("set") }
        val setCall = buildFunctionCall {
            calleeReference = originalReference
            argumentList = buildArgumentList { arguments += subscript }
        }
        subscript.replaceResolvedSetCall(setCall)
        val snapshot = CfirResolutionSnapshot.capture(subscript)
        repeat(2) {
            subscript.replaceResolvedSetCall(null)
            subscript.replaceResolvedGetCall(setCall)
            setCall.replaceCalleeReference(buildNamedReference { name = Name.identifier("failed") })
            snapshot.restore()
            assertNull(subscript.resolvedGetCall)
            assertSame(setCall, subscript.resolvedSetCall)
            assertSame(originalReference, setCall.calleeReference)
        }
    }

    /** initializer 只由资源和模式声明持有；叶子的派生 initializer 必须随真实 owner 恢复。 */
    @Test
    fun `restore reinstates owning variable initializers without writing a derived binding initializer`() {
        val originalResourceInitializer = createIntLiteral(1)
        val resource = buildResourceVariable {
            moduleData = CfirBinaryDependenciesModuleData(Name.identifier("resource-snapshot-test"))
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            status = CfirDeclarationStatusImpl()
            isVar = false
            symbol = CfirResourceVariableSymbol(CallableId(Name.identifier("resource")))
            returnTypeRef = ConePrimitiveType.INT32.toCfirResolvedTypeRef()
            name = Name.identifier("resource")
            initializer = originalResourceInitializer
        }
        val binding = createPatternBindingVariable("value")
        val originalPattern = buildBindingPattern {
            name = binding.name
            bindingVariable = binding
        }
        val originalPatternInitializer = createIntLiteral(2)
        val patternOwner = buildPatternVariable {
            moduleData = binding.moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            status = CfirDeclarationStatusImpl()
            isVar = false
            symbol = CfirPatternVariableSymbol(CallableId(Name.identifier("pattern")))
            returnTypeRef = ConePrimitiveType.INT32.toCfirResolvedTypeRef()
            pattern = originalPattern
            initializer = originalPatternInitializer
        }
        val block = buildBlock { statements.addAll(listOf(resource, patternOwner)) }
        assertSame(patternOwner, binding.patternTreeOwner)
        assertSame(originalPatternInitializer, binding.initializer)
        val snapshot = CfirResolutionSnapshot.capture(block)
        val candidateResourceInitializer = createIntLiteral(3)
        val candidatePatternInitializer = createIntLiteral(4)

        repeat(2) {
            resource.replaceInitializer(candidateResourceInitializer)
            patternOwner.replaceInitializer(candidatePatternInitializer)
            assertSame(candidateResourceInitializer, resource.initializer)
            assertSame(candidatePatternInitializer, binding.initializer)
            originalResourceInitializer.replaceConeTypeOrNull(ConePrimitiveType.INT64)
            originalPatternInitializer.replaceConeTypeOrNull(ConePrimitiveType.INT64)

            // 若快照向 binding 的派生属性调用 replaceInitializer，这里会由真实节点的非法 setter 抛错。
            snapshot.restore()

            assertSame(originalResourceInitializer, resource.initializer)
            assertSame(originalPatternInitializer, patternOwner.initializer)
            assertSame(originalPatternInitializer, binding.initializer)
            assertSame(patternOwner, binding.patternTreeOwner)
            assertSame(binding, originalPattern.bindingVariable)
            assertEquals(ConePrimitiveType.INT32, originalResourceInitializer.coneTypeOrNull)
            assertEquals(ConePrimitiveType.INT32, originalPatternInitializer.coneTypeOrNull)
        }
    }

    /** 显式 receiver 与 dispatch receiver 是独立字段，替换前者后不能仅恢复后者。 */
    @Test
    fun `restore reinstates an explicit receiver replaced by a transformer`() {
        val originalReceiver = createIntLiteral(1)
        val dispatch = createIntLiteral(2)
        val call = buildFunctionCall {
            calleeReference = buildNamedReference { name = Name.identifier("target") }
            explicitReceiver = originalReceiver
            dispatchReceiver = dispatch
        }
        val snapshot = CfirResolutionSnapshot.capture(call)
        val candidateReceiver = createIntLiteral(3)
        val transformer = replacingLiterals(mapOf(originalReceiver to candidateReceiver))

        repeat(2) {
            assertSame(call, call.transformExplicitReceiver(transformer, Unit))
            assertSame(candidateReceiver, call.explicitReceiver)
            assertSame(dispatch, call.dispatchReceiver)
            originalReceiver.replaceConeTypeOrNull(ConePrimitiveType.INT64)

            snapshot.restore()

            assertSame(originalReceiver, call.explicitReceiver)
            assertSame(dispatch, call.dispatchReceiver)
            assertEquals(ConePrimitiveType.INT32, originalReceiver.coneTypeOrNull)
        }
    }

    /** 列表对象未替换也可能已改写实参；已解析列表还必须恢复实参到原形参的映射。 */
    @Test
    fun `restore reinstates arguments and parameter mapping after in place transformation`() {
        for (isResolved in listOf(false, true)) {
            val originalArguments = listOf(createIntLiteral(1), createIntLiteral(2), createIntLiteral(3))
            val originalArgumentList = buildArgumentList { arguments.addAll(originalArguments) }
            val functionSymbol = CfirNamedFunctionSymbol(CallableId(Name.identifier("target")))
            val parameters = listOf("first", "second", "third").map { parameterName ->
                buildValueParameter {
                    moduleData = CfirBinaryDependenciesModuleData(Name.identifier("arguments-snapshot-test"))
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    origin = CfirDeclarationOrigin.Synthetic.Default
                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = true
                    symbol = CfirValueParameterSymbol(CallableId(Name.identifier(parameterName)))
                    containingDeclarationSymbol = functionSymbol
                    isNamed = false
                    returnTypeRef = ConePrimitiveType.INT32.toCfirResolvedTypeRef()
                    name = Name.identifier(parameterName)
                }
            }
            val originalMapping = linkedMapOf<CfirExpression, CfirValueParameter>()
            originalArguments.forEachIndexed { index, argument -> originalMapping[argument] = parameters[index] }
            val call = buildFunctionCall {
                calleeReference = buildNamedReference { name = Name.identifier("target") }
                argumentList = if (isResolved) {
                    buildResolvedArgumentList(originalArgumentList, originalMapping)
                } else {
                    originalArgumentList
                }
            }
            val snapshot = CfirResolutionSnapshot.capture(call)
            val candidateArguments = listOf(createIntLiteral(4), createIntLiteral(5), createIntLiteral(6))
            val transformer = replacingLiterals(originalArguments.zip(candidateArguments).toMap())

            repeat(2) {
                assertSame(originalArgumentList, originalArgumentList.transformArguments(transformer, Unit))
                assertArgumentIdentities(candidateArguments, originalArgumentList.arguments)
                if (isResolved) {
                    val resolved = call.argumentList as CfirResolvedArgumentList
                    assertSame(resolved, resolved.transformArguments(transformer, Unit))
                    assertArgumentIdentities(candidateArguments, resolved.arguments)
                    candidateArguments.forEachIndexed { index, argument ->
                        assertSame(parameters[index], resolved.mapping[argument])
                    }
                } else {
                    assertSame(originalArgumentList, call.argumentList)
                }

                snapshot.restore()

                assertArgumentIdentities(originalArguments, originalArgumentList.arguments)
                assertArgumentIdentities(originalArguments, call.argumentList.arguments)
                if (isResolved) {
                    val restored = call.argumentList as CfirResolvedArgumentList
                    assertSame(originalArgumentList, restored.originalArgumentList)
                    assertArgumentIdentities(originalArguments, restored.mapping.keys.toList())
                    assertArgumentIdentities(originalArguments, restored.mappingIncludingContextArguments.keys.toList())
                    originalArguments.forEachIndexed { index, argument ->
                        assertSame(parameters[index], restored.mapping[argument])
                        assertSame(parameters[index], restored.mappingIncludingContextArguments[argument])
                    }
                } else {
                    assertSame(originalArgumentList, call.argumentList)
                }
            }
        }
    }

    /** 候选试跑检查了 catch 后，回滚必须恢复原类型引用和“尚未检查”的整体状态。 */
    @Test
    fun `restore discards catch pattern typing from a failed candidate`() {
        val originalType = ConePrimitiveType.INT64.toCfirResolvedTypeRef()
        val pattern = buildCatchPattern {
            isWildcard = true
            typeRefs += originalType
        }
        val snapshot = CfirResolutionSnapshot.capture(pattern)
        repeat(2) {
            val errorType = ConeErrorType(ConeInvalidCatchTypeError).toCfirResolvedTypeRef()
            pattern.replaceTypeRefs(listOf(errorType))
            pattern.replaceResolvedTypeRef(errorType)
            snapshot.restore()
            assertNull(pattern.resolvedTypeRef)
            assertEquals(1, pattern.typeRefs.size)
            assertSame(originalType, pattern.typeRefs.single())
        }
    }

    /** 延迟解析替换的 let pattern 根必须在失败候选回滚时恢复，并重新绑定原树。 */
    @Test
    fun `restore reinstates the let pattern root and owner`() {
        val originalPattern = buildWildcardPattern()
        val letExpression = buildLetPatternExpression {
            initializer = buildLiteralExpression {
                kind = CfirLiteralKind.INT
                value = 1
            }
            pattern = originalPattern
        }
        val snapshot = CfirResolutionSnapshot.capture(letExpression)
        val candidatePattern = buildWildcardPattern()

        letExpression.replacePattern(candidatePattern)
        snapshot.restore()

        assertSame(originalPattern, letExpression.pattern)
        assertSame(letExpression, originalPattern.patternTreeOwner)
        assertNull(candidatePattern.patternTreeOwner)
    }

    /** Handle 的命令类型引用和 owner 根都属于候选 body 的可变 CFIR 状态。 */
    @Test
    fun `restore reinstates handle pattern root and command type references`() {
        val originalTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef()
        val originalPattern = buildCommandTypePattern {
            bindingName = Name.identifier("value")
            isWildcard = false
            typeRefs += originalTypeRef
        }
        val handleClause = buildHandleClause {
            commandPattern = originalPattern
            body = buildBlock()
        }
        val snapshot = CfirResolutionSnapshot.capture(handleClause)
        val candidateTypeRef = ConePrimitiveType.INT32.toCfirResolvedTypeRef()
        val candidatePattern = buildCommandTypePattern {
            isWildcard = true
        }

        originalPattern.replaceTypeRefs(listOf(candidateTypeRef))
        handleClause.replaceCommandPattern(candidatePattern)
        snapshot.restore()

        assertSame(originalPattern, handleClause.commandPattern)
        assertSame(handleClause, originalPattern.patternTreeOwner)
        assertNull(candidatePattern.patternTreeOwner)
        assertSame(originalTypeRef, originalPattern.typeRefs.single())
    }

    /** 从外层表达式捕获多个 owner；跨 owner 接管后的根与反向链接必须整体恢复。 */
    @Test
    fun `restore reinstates multiple nested roots after a pattern tree transfer`() {
        val firstPattern = buildBindingPattern { name = Name.identifier("first") }
        val secondPattern = buildBindingPattern { name = Name.identifier("second") }
        val firstBinding = createPatternBindingVariable("first")
        val secondBinding = createPatternBindingVariable("second")
        firstPattern.replaceBindingVariable(firstBinding)
        secondPattern.replaceBindingVariable(secondBinding)
        val firstBranch = buildMatchBranch {
            pattern = firstPattern
            body = buildBlock()
        }
        val secondBranch = buildMatchBranch {
            pattern = secondPattern
            body = buildBlock()
        }
        val match = buildMatchExpression {
            branches += firstBranch
            branches += secondBranch
        }
        val snapshot = CfirResolutionSnapshot.capture(match)
        val candidatePattern = buildBindingPattern { name = Name.identifier("candidate") }

        firstBranch.replacePattern(candidatePattern)
        secondBranch.replacePattern(firstPattern)
        assertSame(secondBranch, firstBinding.patternTreeOwner)
        assertNull(secondBinding.patternTreeOwner)
        assertSame(firstBranch, candidatePattern.patternTreeOwner)
        snapshot.restore()

        assertSame(firstPattern, firstBranch.pattern)
        assertSame(firstBranch, firstPattern.patternTreeOwner)
        assertSame(firstBranch, firstBinding.patternTreeOwner)
        assertSame(secondPattern, secondBranch.pattern)
        assertSame(secondBranch, secondPattern.patternTreeOwner)
        assertSame(secondBranch, secondBinding.patternTreeOwner)
        assertNull(candidatePattern.patternTreeOwner)
    }

    /** 所有根类型必须在事务开始前验证，避免较早的 owner 已被部分恢复。 */
    @Test
    fun `restore validates every owner root type before changing any owner`() {
        val originalBranchPattern = buildWildcardPattern()
        val branch = buildMatchBranch {
            pattern = originalBranchPattern
            body = buildBlock()
        }
        val originalHandlePattern = buildCommandTypePattern { isWildcard = true }
        val handle = buildHandleClause {
            commandPattern = originalHandlePattern
            body = buildBlock()
        }
        val candidateBranchPattern = buildWildcardPattern()
        val invalidHandlePattern = buildWildcardPattern()

        assertThrows(IllegalArgumentException::class.java) {
            restorePatternTreeRoots(
                listOf(
                    branch to candidateBranchPattern,
                    handle to invalidHandlePattern,
                ),
            )
        }

        assertSame(originalBranchPattern, branch.pattern)
        assertSame(originalHandlePattern, handle.commandPattern)
        assertNull(candidateBranchPattern.patternTreeOwner)
        assertNull(invalidHandlePattern.patternTreeOwner)
    }

    /** 恢复输入不能重复声明同一 owner，否则根字段顺序会产生歧义。 */
    @Test
    fun `restore rejects duplicate owners before changing roots`() {
        val originalPattern = buildWildcardPattern()
        val branch = buildMatchBranch {
            pattern = originalPattern
            body = buildBlock()
        }
        val firstCandidate = buildWildcardPattern()
        val secondCandidate = buildWildcardPattern()

        assertThrows(IllegalArgumentException::class.java) {
            restorePatternTreeRoots(
                listOf(
                    branch to firstCandidate,
                    branch to secondCandidate,
                ),
            )
        }

        assertSame(originalPattern, branch.pattern)
        assertNull(firstCandidate.patternTreeOwner)
        assertNull(secondCandidate.patternTreeOwner)
    }

    /** 不同 owner 的目标根不能共享同一模式节点，验证失败必须保持当前图不变。 */
    @Test
    fun `restore rejects a pattern node shared by different target owners`() {
        val firstOriginal = buildWildcardPattern()
        val secondOriginal = buildWildcardPattern()
        val firstBranch = buildMatchBranch {
            pattern = firstOriginal
            body = buildBlock()
        }
        val secondBranch = buildMatchBranch {
            pattern = secondOriginal
            body = buildBlock()
        }
        val sharedTarget = buildWildcardPattern()

        assertThrows(IllegalArgumentException::class.java) {
            restorePatternTreeRoots(
                listOf(
                    firstBranch to sharedTarget,
                    secondBranch to sharedTarget,
                ),
            )
        }

        assertSame(firstOriginal, firstBranch.pattern)
        assertSame(secondOriginal, secondBranch.pattern)
        assertNull(sharedTarget.patternTreeOwner)
    }

    private fun createIntLiteral(value: Int): CfirLiteralExpression = buildLiteralExpression {
        kind = CfirLiteralKind.INT
        this.value = value
        coneTypeOrNull = ConePrimitiveType.INT32
    }

    /** 通过正式 transformer 回调替换实参或 receiver，保留外围列表和调用对象。 */
    private fun replacingLiterals(
        replacements: Map<CfirLiteralExpression, CfirLiteralExpression>,
    ): CfirTransformer<Unit> = object : CfirTransformer<Unit>() {
        override fun <E : CfirElement> transformElement(element: E, data: Unit): E = element

        override fun transformLiteralExpression(literalExpression: CfirLiteralExpression, data: Unit): CfirStatement =
            replacements.getValue(literalExpression)
    }

    private fun assertArgumentIdentities(expected: List<CfirExpression>, actual: List<CfirExpression>) {
        assertEquals(expected.size, actual.size)
        expected.forEachIndexed { index, argument -> assertSame(argument, actual[index], "实参顺序或节点身份发生变化。") }
    }

    private fun createPatternBindingVariable(name: String): CfirPatternBindingVariable =
        buildPatternBindingVariable {
            moduleData = CfirBinaryDependenciesModuleData(Name.identifier("pattern-snapshot-test"))
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            status = CfirDeclarationStatusImpl()
            isVar = false
            symbol = CfirPatternBindingSymbol(CallableId(Name.identifier(name)))
            returnTypeRef = ConePrimitiveType.INT32.toCfirResolvedTypeRef()
            this.name = Name.identifier(name)
        }

    @Test
    fun `restore keeps a fixed error diagnostic and restores sibling types`() {
        val diagnostic = ConeSimpleDiagnostic("unexpanded macro")
        val error = buildErrorExpression { this.diagnostic = diagnostic }
        val literal = buildLiteralExpression {
            kind = CfirLiteralKind.INT
            value = 1
            coneTypeOrNull = ConePrimitiveType.INT32
        }
        val block = buildBlock { statements.addAll(listOf(error, literal)) }
        val snapshot = CfirResolutionSnapshot.capture(block)

        repeat(2) {
            literal.replaceConeTypeOrNull(ConePrimitiveType.INT64)
            snapshot.restore()
            assertEquals(ConePrimitiveType.INT32, literal.coneTypeOrNull)
            val restoredDiagnostic = (error.coneTypeOrNull as ConeErrorType).diagnostic as ConeUnreportedDuplicateDiagnostic
            assertSame(diagnostic, restoredDiagnostic.original)
        }
    }

    @Test
    fun `restore visits the expression supplying an error wrapper type`() {
        val literal = buildLiteralExpression {
            kind = CfirLiteralKind.INT
            value = 1
            coneTypeOrNull = ConePrimitiveType.INT32
        }
        val diagnostic = ConeSimpleDiagnostic("erroneous wrapper")
        val error = buildErrorExpression {
            this.diagnostic = diagnostic
            expression = literal
        }
        val snapshot = CfirResolutionSnapshot.capture(error)
        literal.replaceConeTypeOrNull(ConePrimitiveType.INT64)

        snapshot.restore()

        assertEquals(ConePrimitiveType.INT32, error.coneTypeOrNull)
        assertSame(literal, error.expression)
        assertSame(diagnostic, error.diagnostic)
    }
}
