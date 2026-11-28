@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.calls.ImplicitDispatchReceiverValue
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationExportFacts
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.exportFacts
import org.cangnova.cangjie.cfir.declarations.builder.buildClass
import org.cangnova.cangjie.cfir.declarations.builder.buildConstructor
import org.cangnova.cangjie.cfir.declarations.builder.buildFieldVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildStruct
import org.cangnova.cangjie.cfir.expressions.buildThisReceiverExpression
import org.cangnova.cangjie.cfir.references.builder.buildThisReference
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.registerHierarchyProviders
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirStructSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.ClassId
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildCodeFragment
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.builder.buildPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperationKind
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperator
import org.cangnova.cangjie.cfir.expressions.builder.buildAssignment
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.buildIfExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildTupleLiteral
import org.cangnova.cangjie.cfir.expressions.builder.buildTypeOperator
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.resolve.dfa.BindingVersion
import org.cangnova.cangjie.cfir.resolve.dfa.CompletedValueVariable
import org.cangnova.cangjie.cfir.resolve.dfa.ExpressionCompletion
import org.cangnova.cangjie.cfir.resolve.dfa.Flow
import org.cangnova.cangjie.cfir.resolve.dfa.LogicSystem
import org.cangnova.cangjie.cfir.resolve.dfa.toPersistent
import org.cangnova.cangjie.cfir.resolve.dfa.typeEq
import org.cangnova.cangjie.cfir.resolve.dfa.RealVariable
import org.cangnova.cangjie.cfir.resolve.dfa.ValueBinding
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.CFGNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.TupleLiteralExitNode
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCodeFragmentSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeAnyType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.IdentityHashMap

/**
 * 固定 K 的初始化/赋值 SSA 合同与固定 N 的完成值保存合同。
 * 只经公共 analyzer 产生节点、事实和 binding，再查询不可变 Flow；不操作 core 私表。
 * 同一 typed expression 的重复生命周期是事件身份压力输入，不宣称源码 roundtrip。
 */
class CfirCompletedValueLifecycleTest {
    @Test
    fun `recomputing the same literal starts a new value and event without old approved facts`() {
        val fixture = Fixture()
        val literal = fixture.integer(1)
        val branch = fixture.branch(
            fixture.isType(literal, ConePrimitiveType.BOOLEAN),
            fixture.block("impossible then", fixture.unit()),
            fixture.block("literal recomputed", literal),
        )
        fixture.analyze(listOf(branch)) { state ->
            lateinit var old: ExpressionCompletion
            lateinit var oldFlow: Flow
            lateinit var firstNode: CFGNode<*>
            state.evaluateIf(branch, beforeElse = { flow ->
                old = flow.getExpressionCompletion(literal)
                oldFlow = flow
                firstNode = state.node(literal)
                // 旧知识必须来自真实 IS-false 分支，不能用空表让隔离断言虚假通过。
                assertTrue(checkNotNull(flow.getTypeStatement(old.value)).lowerTypes.isNotEmpty())
            })
            val secondNode = state.node(literal)
            val second = secondNode.flow.getExpressionCompletion(literal)
            assertNotSame(firstNode, secondNode)
            assertNotSame(old.event, second.event)
            assertNotSame(old.value, second.value)
            assertSame(literal, old.event.expression)
            assertSame(literal, second.event.expression)
            assertEquals(ConePrimitiveType.INT64, second.type)
            assertTrue(checkNotNull(secondNode.flow.getTypeStatement(second.value)).lowerTypes.isEmpty())
            assertTrue(checkNotNull(secondNode.flow.getTypeStatement(old.value)).lowerTypes.isNotEmpty())
            assertSame(old, oldFlow.getExpressionCompletion(literal))
        }
    }

    @Test
    fun `recomputing the same Any call does not inherit the earlier Int proof`() {
        val fixture = Fixture()
        val call = fixture.call()
        val branch = fixture.branch(
            fixture.isType(call, ConePrimitiveType.INT64),
            fixture.block("call recomputed", call),
            fixture.block("other result", fixture.unit()),
        )
        fixture.analyze(listOf(branch)) { state ->
            lateinit var old: ExpressionCompletion
            lateinit var oldFlow: Flow
            lateinit var firstNode: CFGNode<*>
            state.evaluateIf(branch, beforeThen = { flow ->
                old = flow.getExpressionCompletion(call)
                oldFlow = flow
                firstNode = state.node(call)
                assertTrue(checkNotNull(flow.getTypeStatement(old.value)).upperTypes.contains(ConePrimitiveType.INT64))
            })
            val secondNode = state.node(call)
            val second = secondNode.flow.getExpressionCompletion(call)
            assertNotSame(firstNode, secondNode)
            assertNotSame(old.event, second.event)
            assertNotSame(old.value, second.value)
            assertSame(call, second.event.expression)
            assertEquals(ConeAnyType, old.type)
            assertEquals(ConeAnyType, second.type)
            assertFalse(checkNotNull(secondNode.flow.getTypeStatement(second.value)).upperTypes.contains(ConePrimitiveType.INT64))
            assertTrue(checkNotNull(secondNode.flow.getTypeStatement(old.value)).upperTypes.contains(ConePrimitiveType.INT64))
            assertSame(old, oldFlow.getExpressionCompletion(call))
        }
    }

    @Test
    fun `stable repeated reads share the initialized value but have distinct completion events`() {
        val fixture = Fixture()
        val initializer = fixture.integer(1)
        val x = fixture.variable("x", ConePrimitiveType.INT64, initializer)
        val read = fixture.access(x, "same read")
        fixture.analyze(listOf(x, read, read)) { state ->
            val declaration = state.declare(x)
            val realX = state.real(read)
            val initialized = state.binding(declaration.flow, realX)
            val initializerCompletion = declaration.flow.getExpressionCompletion(initializer)
            assertSame(initializerCompletion.value, initialized.value)
            val firstNode = state.evaluate(read)
            val first = firstNode.flow.getExpressionCompletion(read)
            val secondNode = state.evaluate(read)
            val second = secondNode.flow.getExpressionCompletion(read)
            assertSame(initialized.value, first.value)
            assertSame(first.value, second.value)
            assertNotSame(first.event, second.event)
            assertNotSame(initializerCompletion.event, first.event)
            assertNotSame(firstNode, secondNode)
            assertTrue(fixture.logic.isSameValueIn(firstNode.flow, secondNode.flow, realX))
            assertSame(initialized, state.binding(secondNode.flow, realX))
            assertEquals(0, state.context.assignmentCounter)
            assertSame(first, firstNode.flow.getExpressionCompletion(read))
        }
    }

    @Test
    fun `a real write changes the repeated read value and event without copying old value facts`() {
        val fixture = Fixture()
        val x = fixture.variable("x", ConePrimitiveType.INT64, fixture.integer(1))
        val read = fixture.access(x, "same storage read")
        val write = fixture.assignment(x, fixture.integer(2), "real second value")
        val branch = fixture.branch(
            fixture.isType(read, ConePrimitiveType.BOOLEAN),
            fixture.block("impossible then", fixture.unit()),
            fixture.block("write then read", write, read),
        )
        fixture.analyze(listOf(x, branch)) { state ->
            state.declare(x)
            val realX = state.real(read)
            lateinit var old: ExpressionCompletion
            lateinit var oldFlow: Flow
            state.evaluateIf(branch, beforeElse = { flow ->
                old = flow.getExpressionCompletion(read)
                oldFlow = flow
                assertSame(state.binding(flow, realX).value, old.value)
                assertTrue(checkNotNull(flow.getTypeStatement(old.value)).lowerTypes.isNotEmpty())
            })
            val currentFlow = state.node(read).flow
            val current = currentFlow.getExpressionCompletion(read)
            val rhs = state.node(write.rValue).flow.getExpressionCompletion(write.rValue)
            assertNotSame(old.value, current.value)
            assertNotSame(old.event, current.event)
            assertSame(rhs.value, current.value)
            assertSame(current.value, state.binding(currentFlow, realX).value)
            assertFalse(fixture.logic.isSameValueIn(oldFlow, currentFlow, realX))
            assertTrue(checkNotNull(currentFlow.getTypeStatement(current.value)).lowerTypes.isEmpty())
            assertTrue(checkNotNull(currentFlow.getTypeStatement(old.value)).lowerTypes.isNotEmpty())
            assertSame(old, oldFlow.getExpressionCompletion(read))
            assertEquals(1, state.context.assignmentCounter)
        }
    }

    @Test
    fun `initializers preserve captured Int uses without consuming assignment indices and writes consume one each`() {
        val fixture = Fixture()
        val first = fixture.integer(1)
        val other = fixture.integer(4)
        val x = fixture.variable("x", ConeAnyType, first)
        val y = fixture.variable("y", ConePrimitiveType.INT64, other)
        val read = fixture.access(x, "Any storage")
        val second = fixture.integer(2)
        val third = fixture.integer(3)
        val writeTwo = fixture.assignment(x, second, "x equals two")
        val writeThree = fixture.assignment(x, third, "x equals three")
        fixture.analyze(listOf(x, y, writeTwo, writeThree)) { state ->
            assertEquals(0, state.context.assignmentCounter)
            val declaration = state.declare(x)
            state.declare(y)
            assertEquals(0, state.context.assignmentCounter)
            val realX = state.real(read)
            assertEquals(ConeAnyType, realX.originalType)
            val initial = state.binding(declaration.flow, realX)
            val captured = declaration.flow.getExpressionCompletion(first)
            assertSame(captured.value, initial.value)
            assertEquals(captured.type, initial.type)
            assertEquals(ConePrimitiveType.INT64, initial.type)
            assertTrue(initial.version is BindingVersion.Entry)
            assertSame(x, (initial.version as BindingVersion.Entry).event.origin)
            val afterTwo = state.evaluate(writeTwo)
            val bindingTwo = state.binding(afterTwo.flow, realX)
            val capturedTwo = afterTwo.flow.getExpressionCompletion(second)
            assertEquals(1, state.context.assignmentCounter)
            assertEquals(BindingVersion.Assignment(0), bindingTwo.version)
            assertSame(capturedTwo.value, bindingTwo.value)
            assertEquals(capturedTwo.type, bindingTwo.type)
            assertEquals(ConePrimitiveType.INT64, bindingTwo.type)
            val afterThree = state.evaluate(writeThree)
            val bindingThree = state.binding(afterThree.flow, realX)
            assertEquals(2, state.context.assignmentCounter)
            assertEquals(BindingVersion.Assignment(1), bindingThree.version)
            assertSame(afterThree.flow.getExpressionCompletion(third).value, bindingThree.value)
            assertEquals(ConePrimitiveType.INT64, bindingThree.type)
            assertFalse(fixture.logic.isSameValueIn(declaration.flow, afterTwo.flow, realX))
            assertFalse(fixture.logic.isSameValueIn(afterTwo.flow, afterThree.flow, realX))
            assertSame(initial, state.binding(declaration.flow, realX))
            assertEquals(ConePrimitiveType.INT64, initial.type)
        }
    }

    @Test
    fun `tuple aggregate keeps the first captured x while the if element returns the new stored x`() {
        val fixture = Fixture()
        val x = fixture.variable("x", ConePrimitiveType.INT64, fixture.integer(1))
        val writeTwo = fixture.assignment(x, fixture.integer(2), "before tuple")
        val first = fixture.access(x, "first tuple read")
        val writeThree = fixture.assignment(x, fixture.integer(3), "then write")
        val thenRead = fixture.access(x, "then read")
        val elseRead = fixture.access(x, "else read")
        val second = fixture.branch(
            fixture.boolean(true),
            fixture.block("then", writeThree, thenRead),
            fixture.block("else", elseRead),
            ConePrimitiveType.INT64,
        )
        val tuple = fixture.tuple(first, second)
        fixture.analyze(listOf(x, writeTwo, tuple)) { state ->
            state.declare(x)
            val before = state.evaluate(writeTwo)
            val realX = state.real(first)
            val oldBinding = state.binding(before.flow, realX)
            val exit = state.evaluate(tuple) as TupleLiteralExitNode
            val firstPoint = exit.elementExitNodes[0]
            val secondPoint = exit.elementExitNodes[1]
            val capturedFirst = firstPoint.flow.getExpressionCompletion(first)
            val capturedSecond = secondPoint.flow.getExpressionCompletion(second)
            val completion = exit.flow.getExpressionCompletion(tuple)
            val aggregate = completion.value as CompletedValueVariable
            val thenValue = state.node(thenRead).flow.getExpressionCompletion(thenRead)
            val currentBinding = state.binding(exit.flow, realX)
            assertSame(oldBinding.value, capturedFirst.value)
            assertSame(state.node(first).flow.getExpressionCompletion(first).event, capturedFirst.event)
            assertSame(oldBinding, state.binding(firstPoint.flow, realX))
            assertSame(thenValue.value, capturedSecond.value)
            assertSame(currentBinding.value, capturedSecond.value)
            assertNotSame(capturedFirst.value, capturedSecond.value)
            assertTrue(fixture.logic.isSameValueIn(before.flow, firstPoint.flow, realX))
            assertFalse(fixture.logic.isSameValueIn(firstPoint.flow, exit.flow, realX))
            assertSame(tuple, aggregate.origin)
            assertEquals(2, aggregate.inputs.size)
            assertSame(capturedFirst.value, aggregate.inputs[0])
            assertSame(capturedSecond.value, aggregate.inputs[1])
            assertEquals(ConePrimitiveType.INT64, capturedFirst.type)
            assertEquals(ConePrimitiveType.INT64, capturedSecond.type)
            assertEquals(ConeTupleType(listOf(ConePrimitiveType.INT64, ConePrimitiveType.INT64)), completion.type)
            assertTrue(state.node(elseRead).isDead)
            assertNull(state.node(elseRead).flow.getExpressionCompletionOrNull(elseRead))
            assertSame(capturedFirst, firstPoint.flow.getExpressionCompletion(first))
            assertSame(oldBinding.value, aggregate.inputs[0])
            assertEquals(2, state.context.assignmentCounter)
        }
    }

    @Test
    fun `uninitialized local read and IS knowledge cannot install a binding or completion`() {
        val fixture = Fixture()
        val x = fixture.variable("uninitialized", ConePrimitiveType.INT64, null)
        val read = fixture.access(x, "uninitialized local read")
        val branch = fixture.branch(
            fixture.isType(read, ConePrimitiveType.INT64),
            fixture.block("assumed type", read),
            fixture.block("other path", fixture.unit()),
        )
        fixture.analyze(listOf(x, read, branch)) { state ->
            state.declare(x)
            val realX = state.real(read)
            assertNull(state.currentFlow.getValueBinding(realX))
            val firstRead = state.evaluate(read)
            assertNull(firstRead.flow.getValueBinding(realX))
            assertNull(firstRead.flow.getExpressionCompletionOrNull(read))
            // IS 的知识批准可以作用于已登记 storage，却不能执行初始化。
            state.evaluateIf(branch, beforeThen = { flow ->
                assertTrue(checkNotNull(flow.getTypeStatement(realX)).upperTypes.contains(ConePrimitiveType.INT64))
                assertNull(flow.getValueBinding(realX))
                assertNull(flow.getExpressionCompletionOrNull(read))
            })
            assertNull(state.node(read).flow.getValueBinding(realX))
            assertNull(state.node(read).flow.getExpressionCompletionOrNull(read))
            assertNull(state.currentFlow.getValueBinding(realX))
            assertEquals(0, state.context.assignmentCounter)
        }
    }

    @Test
    fun `sourceReadUninitialized cannot masquerade as a successful external stable read`() {
        val fixture = Fixture()
        val declaration = fixture.variable("sourceValue", ConePrimitiveType.INT64, null, mutable = false, local = false)
        val read = fixture.access(declaration, "source read without initialization")
        fixture.analyze(listOf(declaration, read, read)) { state ->
            state.declare(declaration)
            val real = state.real(read)
            assertEquals(CfirDeclarationOrigin.Source, declaration.origin)
            assertNull(state.currentFlow.getValueBinding(real))
            val first = state.evaluate(read)
            assertNull(first.flow.getValueBinding(real))
            assertNull(first.flow.getExpressionCompletionOrNull(read))
            val second = state.evaluate(read)
            assertNull(second.flow.getValueBinding(real))
            assertNull(second.flow.getExpressionCompletionOrNull(read))
            assertNotSame(first, second)
            assertEquals(0, state.context.assignmentCounter)
        }
    }

    @Test
    fun `snapshot forks keep the old initialized value while real writes and recomputations stay independent`() {
        val fixture = Fixture()
        val initializer = fixture.integer(1)
        val x = fixture.variable("x", ConePrimitiveType.INT64, initializer)
        val read = fixture.access(x, "repeated fork read")
        val rhs = fixture.integer(2)
        val write = fixture.assignment(x, rhs, "fork write")
        fixture.analyze(listOf(x, read, write, read)) { original ->
            original.declare(x)
            val oldNode = original.evaluate(read)
            val realX = original.real(read)
            val old = oldNode.flow.getExpressionCompletion(read)
            val oldBinding = original.binding(oldNode.flow, realX)
            val snapshot = original.analyzer.captureDataFlowAnalyzerContext(IdentityMapper)
            val peerSnapshot = original.analyzer.captureDataFlowAnalyzerContext(IdentityMapper)
            val copy = fixture.resume(original, snapshot)
            val peer = fixture.resume(original, peerSnapshot)
            assertNotSame(original.context.graphBuilder.lastNode, copy.context.graphBuilder.lastNode)
            assertSame(oldNode.flow, copy.context.graphBuilder.lastNode.flow)
            assertSame(oldNode.flow, peer.context.graphBuilder.lastNode.flow)
            assertSame(old, copy.currentFlow.getExpressionCompletion(read))
            assertSame(oldBinding, copy.binding(copy.currentFlow, realX))
            lateinit var copyWritten: ExpressionCompletion
            lateinit var copyGraph: ControlFlowGraph
            copy.inContext {
                val reread = copy.evaluate(read).flow.getExpressionCompletion(read)
                assertSame(old.value, reread.value)
                assertNotSame(old.event, reread.event)
                copy.evaluate(write)
                val newNode = copy.evaluate(read)
                copyWritten = newNode.flow.getExpressionCompletion(read)
                assertSame(newNode.flow.getExpressionCompletion(rhs).value, copyWritten.value)
                assertNotSame(old.value, copyWritten.value)
                assertNotSame(old.event, copyWritten.event)
                assertFalse(fixture.logic.isSameValueIn(oldNode.flow, newNode.flow, realX))
                assertEquals(1, copy.context.assignmentCounter)
                assertSame(oldBinding, original.binding(original.currentFlow, realX))
                assertSame(oldBinding, peer.binding(peer.currentFlow, realX))
                assertEquals(0, original.context.assignmentCounter)
                assertEquals(0, peer.context.assignmentCounter)
                copyGraph = copy.finish()
            }
            peer.inContext {
                val peerRead = peer.evaluate(read)
                val completion = peerRead.flow.getExpressionCompletion(read)
                assertSame(old.value, completion.value)
                assertNotSame(old.event, completion.event)
                assertSame(oldBinding, peer.binding(peerRead.flow, realX))
                assertTrue(fixture.logic.isSameValueIn(oldNode.flow, peerRead.flow, realX))
                assertEquals(0, peer.context.assignmentCounter)
                peer.finish()
            }
            original.evaluate(write)
            val originalNewNode = original.evaluate(read)
            val originalWritten = originalNewNode.flow.getExpressionCompletion(read)
            assertNotSame(old.value, originalWritten.value)
            assertNotSame(copyWritten.value, originalWritten.value)
            assertNotSame(copyWritten.event, originalWritten.event)
            assertSame(originalNewNode.flow.getExpressionCompletion(rhs).value, originalWritten.value)
            assertEquals(1, original.context.assignmentCounter)
            assertSame(oldBinding, original.binding(oldNode.flow, realX))
            assertSame(old, oldNode.flow.getExpressionCompletion(read))
            val originalGraph = original.finish()
            assertNotSame(originalGraph, copyGraph)
            assertSame(original.fragment, originalGraph.declaration)
            assertSame(original.fragment, copyGraph.declaration)
        }
    }

    @Test
    fun `published initialized source imported and foreign globals admit Read without a local store`() {
        for (case in listOf("source", "imported", "foreign")) {
            val fixture = Fixture()
            val global = fixture.externalGlobal(case, initialized = true)
            val read = fixture.externalAccess(global, "$case first read")
            fixture.analyze(listOf(read, read), externalDeclarations = listOf(global)) { state ->
                val prototype = state.analyzer.getOrCreateVariable(read) as RealVariable
                assertNull(state.currentFlow.getValueBinding(prototype), case)
                val firstNode = state.evaluate(read)
                val real = state.storage(read)
                val first = firstNode.flow.getExpressionCompletion(read)
                val binding = state.binding(firstNode.flow, real)
                assertTrue(binding.version is BindingVersion.Read, case)
                assertSame(read, (binding.version as BindingVersion.Read).event.origin)
                assertSame(binding.value, first.value)
                assertEquals(ConePrimitiveType.INT64, first.type)
                assertTrue(checkNotNull(firstNode.flow.getTypeStatement(first.value)).upperTypes.contains(first.type))
                val secondNode = state.evaluate(read)
                val second = secondNode.flow.getExpressionCompletion(read)
                assertSame(binding, state.binding(secondNode.flow, real))
                assertSame(first.value, second.value)
                assertNotSame(first.event, second.event)
                assertSame(first, firstNode.flow.getExpressionCompletion(read))
                global.initializer?.let { assertNull(firstNode.flow.getExpressionCompletionOrNull(it)) }
                assertEquals(0, state.context.assignmentCounter)
            }
        }
    }

    @Test
    fun `imported and foreign initialization exceptions require their published evidence`() {
        for (case in listOf("imported", "foreign")) {
            val fixture = Fixture()
            val global = fixture.externalGlobal(case, initialized = false)
            assertNull(global.initializer)
            assertFalse(global.exportFacts.isInitialized)
            assertTrue(global.exportFacts.isImported || global.status.isForeign)
            val read = fixture.externalAccess(global, "$case platform read")
            fixture.analyze(listOf(read), externalDeclarations = listOf(global)) { state ->
                val node = state.evaluate(read)
                val binding = state.binding(node.flow, state.storage(read))
                assertTrue(binding.version is BindingVersion.Read)
                assertSame(binding.value, node.flow.getExpressionCompletion(read).value)
                assertEquals(0, state.context.assignmentCounter)
            }
        }
    }

    @Test
    fun `source initializer syntax and Library origin do not replace initialized or imported facts`() {
        for (case in listOf("source", "sourceWithoutInitializer", "libraryWithoutImport")) {
            val fixture = Fixture()
            val global = fixture.externalGlobal(case, initialized = false)
            assertFalse(global.exportFacts.isInitialized)
            assertFalse(global.exportFacts.isImported)
            assertFalse(global.status.isForeign)
            val read = fixture.externalAccess(global, "$case unproved external read")
            fixture.analyze(listOf(read, read), externalDeclarations = listOf(global)) { state ->
                val firstNode = state.evaluate(read)
                val real = state.storage(read)
                assertSame(global.symbol, real.symbol)
                assertEquals(ConePrimitiveType.INT64, real.originalType)
                assertNull(firstNode.flow.getValueBinding(real))
                assertNull(firstNode.flow.getExpressionCompletionOrNull(read))
                assertTrue(firstNode.owner.hasPendingSemanticTransfers)
                val secondNode = state.evaluate(read)
                assertNull(secondNode.flow.getValueBinding(real))
                assertNull(secondNode.flow.getExpressionCompletionOrNull(read))
                assertEquals(0, state.context.assignmentCounter)
            }
        }
    }

    @Test
    fun `final Class parameter entry and this occurrence receiver completion admit an instance field`() {
        val fixture = Fixture()
        val owner = fixture.nominalOwner("CompletedObject", struct = false)
        val field = fixture.instanceField(owner)
        val function = fixture.reader(owner, "readParameter")
        val parameter = function.valueParameters.single()
        val receiver = fixture.externalAccess(parameter, "completed Class parameter")
        val read = fixture.externalAccess(field, "parameter member", receiver)
        fixture.analyzeFunction(function, declarations = listOf(owner)) { state ->
            val parameterReal = state.analyzer.getOrCreateVariable(receiver) as RealVariable
            val entry = state.binding(state.currentFlow, parameterReal)
            assertTrue(entry.version is BindingVersion.Entry)
            assertSame(parameter, (entry.version as BindingVersion.Entry).event.origin)
            assertSame(owner.symbol, entry.value.originalType.toSymbol(fixture.session))
            assertFalse(field.exportFacts.isInitialized)
            val before = state.currentFlow
            val node = state.evaluate(read)
            val real = state.storage(read)
            assertSame(field.symbol, real.symbol)
            assertSame(parameter.symbol, checkNotNull(real.dispatchReceiver).symbol)
            assertNull(before.getValueBinding(real))
            val completedReceiver = node.flow.getExpressionCompletion(receiver)
            assertSame(entry.value, completedReceiver.value)
            val binding = state.binding(node.flow, real)
            assertTrue(binding.version is BindingVersion.Read)
            assertSame(binding.value, node.flow.getExpressionCompletion(read).value)
            assertEquals(0, state.context.assignmentCounter)
        }
    }

    @Test
    fun `parameter entry alone does not admit a member before its receiver occurrence completes`() {
        val fixture = Fixture()
        val owner = fixture.nominalOwner("UnevaluatedObject", struct = false)
        val field = fixture.instanceField(owner)
        val function = fixture.reader(owner, "unevaluatedReceiver")
        val receiver = fixture.externalAccess(function.valueParameters.single(), "not evaluated parameter read")
        val read = fixture.externalAccess(field, "missing receiver occurrence", receiver)
        fixture.analyzeFunction(function, declarations = listOf(owner)) { state ->
            val parameterReal = state.analyzer.getOrCreateVariable(receiver) as RealVariable
            assertTrue(state.binding(state.currentFlow, parameterReal).version is BindingVersion.Entry)
            val node = state.readWithoutEvaluatingReceiver(read)
            val real = state.storage(read)
            assertNull(node.flow.getExpressionCompletionOrNull(receiver))
            assertNull(node.flow.getValueBinding(real))
            assertNull(node.flow.getExpressionCompletionOrNull(read))
            assertTrue(node.owner.hasPendingSemanticTransfers)
            assertEquals(0, state.context.assignmentCounter)
        }
    }

    @Test
    fun `uninitialized local Class receiver cannot supply member initialization evidence`() {
        val fixture = Fixture()
        val owner = fixture.nominalOwner("UninitializedObject", struct = false)
        val field = fixture.instanceField(owner)
        val local = fixture.variable("missingObject", owner.symbol.constructType(), null, mutable = false)
        val receiver = fixture.access(local, "uninitialized local receiver")
        val read = fixture.externalAccess(field, "uninitialized receiver member", receiver)
        fixture.analyze(listOf(local, read), externalDeclarations = listOf(owner)) { state ->
            state.declare(local)
            val receiverReal = state.storage(receiver)
            val node = state.evaluate(read)
            assertNull(node.flow.getValueBinding(receiverReal))
            assertNull(node.flow.getExpressionCompletionOrNull(receiver))
            assertNull(node.flow.getValueBinding(state.storage(read)))
            assertNull(node.flow.getExpressionCompletionOrNull(read))
            assertEquals(0, state.context.assignmentCounter)
        }
    }

    @Test
    fun `constructor own field without a binding rejects even a final declaration initialized fact`() {
        for (initialized in listOf(false, true)) {
            val fixture = Fixture()
            val owner = fixture.nominalOwner("ConstructingObject$initialized", struct = false) as CfirClass
            val field = fixture.instanceField(owner)
            field.exportFacts = field.exportFacts.copy(isInitialized = initialized)
            val receiver = buildThisReceiverExpression {
                source = binarySource("own this")
                coneTypeOrNull = owner.symbol.constructType()
                calleeReference = buildThisReference { boundSymbol = owner.symbol; isImplicit = true }
            }
            val read = fixture.externalAccess(field, "own field before store", receiver)
            val constructor = fixture.constructor(owner, read)
            fixture.analyzeFunction(constructor, declarations = listOf(owner), constructingClass = owner) { state ->
                val ownReceiver = state.analyzer.getOrCreateVariable(receiver) as RealVariable
                assertTrue(ownReceiver.isImplicit)
                assertTrue(state.binding(state.currentFlow, ownReceiver).version is BindingVersion.Entry)
                val before = state.currentFlow
                // this 已有函数 entry；当前 analyzer 没有独立 this 表达式出口，直接推进 own field 读。
                val node = state.readWithoutEvaluatingReceiver(read)
                val real = state.storage(read)
                assertTrue(checkNotNull(real.dispatchReceiver).isImplicit)
                assertSame(owner.symbol, real.dispatchReceiver!!.symbol)
                assertNull(before.getValueBinding(real))
                assertNull(node.flow.getValueBinding(real))
                assertNull(node.flow.getExpressionCompletionOrNull(read))
                assertTrue(node.owner.hasPendingSemanticTransfers)
                assertEquals(0, state.context.assignmentCounter)
            }
        }
    }

    @Test
    fun `Class aliases share member addresses while Struct copies retain distinct receiver storage`() {
        for (struct in listOf(false, true)) {
            val fixture = Fixture()
            val owner = fixture.nominalOwner(if (struct) "ValueObject" else "ReferenceObject", struct)
            val field = fixture.instanceField(owner)
            val function = fixture.reader(owner, "copyReceiver")
            val original = fixture.externalAccess(function.valueParameters.single(), "original receiver")
            val copy = fixture.variable("copiedReceiver", owner.symbol.constructType(), original, mutable = false)
            val copiedReceiver = fixture.access(copy, "copied receiver")
            val originalMember = fixture.externalAccess(field, "original member", original)
            val copiedMember = fixture.externalAccess(field, "copied member", copiedReceiver)
            fixture.analyzeFunction(function, declarations = listOf(owner)) { state ->
                state.declare(copy)
                val originalStorage = state.storage(original)
                val copyStorage = state.storage(copiedReceiver)
                assertNotEquals(originalStorage, copyStorage)
                assertSame(state.binding(state.currentFlow, originalStorage).value, state.binding(state.currentFlow, copyStorage).value)
                val firstNode = state.evaluate(originalMember)
                val firstAddress = state.storage(originalMember)
                val firstCompletion = firstNode.flow.getExpressionCompletion(originalMember)
                val secondNode = state.evaluate(copiedMember)
                val secondAddress = state.storage(copiedMember)
                val secondCompletion = secondNode.flow.getExpressionCompletion(copiedMember)
                assertEquals(ConePrimitiveType.INT64, firstCompletion.type)
                assertEquals(ConePrimitiveType.INT64, secondCompletion.type)
                if (struct) {
                    assertNotEquals(firstAddress, secondAddress)
                    assertSame(originalStorage, firstAddress.dispatchReceiver)
                    assertSame(copyStorage, secondAddress.dispatchReceiver)
                } else {
                    assertSame(firstAddress, secondAddress)
                    val firstBinding = state.binding(firstNode.flow, firstAddress)
                    val secondBinding = state.binding(secondNode.flow, secondAddress)
                    assertSame(firstBinding, secondBinding)
                    assertSame(firstCompletion.value, secondCompletion.value)
                    assertTrue(secondBinding.version is BindingVersion.Read)
                }
                assertSame(firstCompletion, firstNode.flow.getExpressionCompletion(originalMember))
                assertEquals(0, state.context.assignmentCounter)
            }
        }
    }

    @Test
    fun `OR preserves independent all path explicit Any beside intrinsic Int64`() {
        val fixture = Fixture()
        val literal = fixture.integer(1)
        fixture.analyze(listOf(literal)) { state ->
            val node = state.evaluate(literal)
            val completion = node.flow.getExpressionCompletion(literal)
            val intrinsic = checkNotNull(node.flow.getTypeStatement(completion.value)).toPersistent()
            assertEquals(ConePrimitiveType.INT64, completion.type)
            assertEquals(ConePrimitiveType.INT64, completion.value.originalType)
            assertEquals(setOf(ConePrimitiveType.INT64), intrinsic.upperTypes)
            assertEquals(setOf(ConePrimitiveType.INT64), intrinsic.intrinsicUpperTypes)

            // Any 是公开 typeEq 给出的独立显式输入；不向 flow 注入陈述，也不伪造 intrinsic。
            val explicitAny = (completion.value typeEq ConeAnyType).toPersistent()
            assertEquals(setOf(ConeAnyType), explicitAny.upperTypes)
            assertTrue(explicitAny.intrinsicUpperTypes.isEmpty())
            val left = fixture.logic.and(intrinsic, explicitAny).toPersistent()
            val right = checkNotNull(fixture.logic.and(listOf(intrinsic, explicitAny))).toPersistent()
            assertNotSame(left, right)
            assertEquals(setOf(ConePrimitiveType.INT64, ConeAnyType), left.upperTypes)
            assertEquals(setOf(ConePrimitiveType.INT64), left.intrinsicUpperTypes)
            assertEquals(left, right)

            val joined = checkNotNull(fixture.logic.or(listOf(left, right))).toPersistent()
            val reversed = checkNotNull(fixture.logic.or(listOf(right, left))).toPersistent()
            assertSame(completion.value, joined.variable)
            assertEquals(setOf(ConePrimitiveType.INT64, ConeAnyType), joined.upperTypes)
            assertEquals(setOf(ConePrimitiveType.INT64), joined.intrinsicUpperTypes)
            assertEquals(setOf(ConeAnyType), joined.upperTypes - joined.intrinsicUpperTypes)
            assertEquals(joined, reversed)
            assertTrue(joined.lowerTypes.isEmpty())
            assertEquals(setOf(ConePrimitiveType.INT64, ConeAnyType), left.upperTypes)
            assertEquals(setOf(ConePrimitiveType.INT64), right.intrinsicUpperTypes)
            assertEquals(intrinsic, node.flow.getTypeStatement(completion.value)?.toPersistent())
            assertEquals(0, state.context.assignmentCounter)
        }
    }

    @Test
    fun `OR does not promote a type explicit on only one required path`() {
        val fixture = Fixture()
        val literal = fixture.integer(2)
        fixture.analyze(listOf(literal)) { state ->
            val node = state.evaluate(literal)
            val completion = node.flow.getExpressionCompletion(literal)
            val intrinsic = checkNotNull(node.flow.getTypeStatement(completion.value)).toPersistent()
            assertEquals(setOf(ConePrimitiveType.INT64), intrinsic.intrinsicUpperTypes)
            val explicitInt = (completion.value typeEq ConePrimitiveType.INT64).toPersistent()
            val explicitAny = (completion.value typeEq ConeAnyType).toPersistent()
            val upgradedInt = fixture.logic.and(intrinsic, explicitInt).toPersistent()
            val intAndAny = fixture.logic.and(intrinsic, explicitAny).toPersistent()
            assertTrue(upgradedInt.intrinsicUpperTypes.isEmpty())
            assertEquals(setOf(ConeAnyType), intAndAny.upperTypes - intAndAny.intrinsicUpperTypes)

            // 相同 T 的显式/固有混合，以及较宽 B 只有一支显式，均不能补齐另一支的来源。
            for (inputs in listOf(
                listOf(explicitInt, intrinsic),
                listOf(upgradedInt, intrinsic),
                listOf(intAndAny, intrinsic),
            )) {
                for (ordered in listOf(inputs, inputs.asReversed())) {
                    val joined = checkNotNull(fixture.logic.or(ordered)).toPersistent()
                    assertSame(completion.value, joined.variable)
                    assertEquals(setOf(ConePrimitiveType.INT64), joined.upperTypes)
                    assertEquals(setOf(ConePrimitiveType.INT64), joined.intrinsicUpperTypes)
                    assertTrue((joined.upperTypes - joined.intrinsicUpperTypes).isEmpty())
                }
            }
            assertEquals(setOf(ConePrimitiveType.INT64), intrinsic.intrinsicUpperTypes)
            assertTrue(explicitInt.intrinsicUpperTypes.isEmpty())
            assertEquals(intrinsic, node.flow.getTypeStatement(completion.value)?.toPersistent())
            assertEquals(0, state.context.assignmentCounter)
        }
    }

    @Test
    fun `AND upgrades same type explicit proof without changing frozen intrinsic input`() {
        val fixture = Fixture()
        val literal = fixture.integer(3)
        fixture.analyze(listOf(literal)) { state ->
            val node = state.evaluate(literal)
            val completion = node.flow.getExpressionCompletion(literal)
            val intrinsic = checkNotNull(node.flow.getTypeStatement(completion.value)).toPersistent()
            assertEquals(setOf(ConePrimitiveType.INT64), intrinsic.intrinsicUpperTypes)
            val explicitInt = (completion.value typeEq ConePrimitiveType.INT64).toPersistent()
            for (inputs in listOf(listOf(intrinsic, explicitInt), listOf(explicitInt, intrinsic))) {
                val pair = fixture.logic.and(inputs[0], inputs[1]).toPersistent()
                val collection = checkNotNull(fixture.logic.and(inputs)).toPersistent()
                assertEquals(pair, collection)
                assertSame(completion.value, pair.variable)
                assertEquals(setOf(ConePrimitiveType.INT64), pair.upperTypes)
                assertTrue(pair.intrinsicUpperTypes.isEmpty())
                assertEquals(setOf(ConePrimitiveType.INT64), pair.upperTypes - pair.intrinsicUpperTypes)
                assertTrue(pair.lowerTypes.isEmpty())
            }
            assertEquals(setOf(ConePrimitiveType.INT64), intrinsic.upperTypes)
            assertEquals(setOf(ConePrimitiveType.INT64), intrinsic.intrinsicUpperTypes)
            assertEquals(setOf(ConePrimitiveType.INT64), explicitInt.upperTypes)
            assertTrue(explicitInt.intrinsicUpperTypes.isEmpty())
            assertEquals(intrinsic, node.flow.getTypeStatement(completion.value)?.toPersistent())
            assertEquals(0, state.context.assignmentCounter)
        }
    }

    @Test
    fun `provenance algebra and fork recomputation leave frozen Value and Real queries isolated`() {
        val fixture = Fixture()
        val initializer = fixture.integer(4)
        val variable = fixture.variable("provenanceStorage", ConePrimitiveType.INT64, initializer, mutable = false)
        val read = fixture.access(variable, "frozen provenance storage read")
        val recomputed = fixture.integer(5)
        fixture.analyze(listOf(variable, read, recomputed)) { original ->
            original.declare(variable)
            val node = original.evaluate(read)
            val real = original.real(read)
            val completion = node.flow.getExpressionCompletion(read)
            val intrinsic = checkNotNull(node.flow.getTypeStatement(completion.value)).toPersistent()
            assertEquals(setOf(ConePrimitiveType.INT64), intrinsic.intrinsicUpperTypes)
            assertNull(node.flow.getTypeStatement(real))
            val binding = original.binding(node.flow, real)
            val snapshot = original.analyzer.captureDataFlowAnalyzerContext(IdentityMapper)
            val fork = fixture.resume(original, snapshot)
            assertSame(node.flow, fork.currentFlow)
            fork.inContext {
                val explicitAny = (completion.value typeEq ConeAnyType).toPersistent()
                val path = fixture.logic.and(intrinsic, explicitAny).toPersistent()
                val otherPath = checkNotNull(fixture.logic.and(listOf(intrinsic, explicitAny))).toPersistent()
                val joined = checkNotNull(fixture.logic.or(listOf(path, otherPath))).toPersistent()
                assertEquals(setOf(ConeAnyType), joined.upperTypes - joined.intrinsicUpperTypes)
                assertEquals(intrinsic, fork.currentFlow.getTypeStatement(completion.value)?.toPersistent())
                assertNull(fork.currentFlow.getTypeStatement(real))
                val explicitInt = (completion.value typeEq ConePrimitiveType.INT64).toPersistent()
                val upgraded = fixture.logic.and(intrinsic, explicitInt).toPersistent()
                assertTrue(upgraded.intrinsicUpperTypes.isEmpty())
                assertEquals(setOf(ConePrimitiveType.INT64), intrinsic.intrinsicUpperTypes)

                val newNode = fork.evaluate(recomputed)
                val newCompletion = newNode.flow.getExpressionCompletion(recomputed)
                val newFact = checkNotNull(newNode.flow.getTypeStatement(newCompletion.value)).toPersistent()
                assertNotSame(completion.value, newCompletion.value)
                assertNotSame(completion.event, newCompletion.event)
                assertEquals(setOf(ConePrimitiveType.INT64), newFact.upperTypes)
                assertEquals(setOf(ConePrimitiveType.INT64), newFact.intrinsicUpperTypes)
                assertEquals(intrinsic, newNode.flow.getTypeStatement(completion.value)?.toPersistent())
                assertNull(newNode.flow.getTypeStatement(real))
                assertSame(binding, fork.binding(newNode.flow, real))
                assertEquals(0, fork.context.assignmentCounter)
                fork.finish()
            }
            assertSame(node.flow, original.currentFlow)
            assertSame(completion, original.currentFlow.getExpressionCompletion(read))
            assertSame(binding, original.binding(original.currentFlow, real))
            assertEquals(intrinsic, original.currentFlow.getTypeStatement(completion.value)?.toPersistent())
            assertNull(original.currentFlow.getTypeStatement(real))
            assertNull(original.currentFlow.getExpressionCompletionOrNull(recomputed))
            assertEquals(0, original.context.assignmentCounter)
        }
    }

    /** 正式 resolve.test session；声明/表达式类型是内部 typed 输入，不借源码解析授证。 */

    /** 显式类型不建立 K 的值别名；Class copy 仍保留实际对象的地址关系。 */
    @Test
    fun `explicit Class copy retains nonempty old member facts after original stores another Class value`() {
        val case = ReceiverAddressCase()
        case.run { state ->
            case.initialize(state)
            val oldRead = case.member(case.a, "old member")
            val keptRead = case.member(case.b, "kept member")
            val aRead = case.fixture.access(case.a, "new original")
            val proof = case.proveNotBool(oldRead, case.fixture.block("replace original", case.writeA, keptRead, aRead))
            lateinit var old: ExpressionCompletion
            lateinit var frozen: Flow
            lateinit var oldAddress: RealVariable
            lateinit var oldBinding: ValueBinding
            state.evaluateIf(proof, beforeElse = { flow ->
                frozen = flow
                old = flow.getExpressionCompletion(oldRead)
                oldAddress = state.storage(oldRead)
                oldBinding = state.binding(flow, oldAddress)
                assertSame(case.a.symbol, checkNotNull(oldAddress.dispatchReceiver).symbol)
                assertTrue(checkNotNull(flow.getTypeStatement(old.value)).lowerTypes.isNotEmpty())
            })
            val keptFlow = state.node(keptRead).flow
            val keptAddress = state.storage(keptRead, keptFlow)
            assertNotEquals(oldAddress, keptAddress)
            assertSame(case.b.symbol, checkNotNull(keptAddress.dispatchReceiver).symbol)
            assertSame(old.value, keptFlow.getExpressionCompletion(keptRead).value)
            assertSame(oldBinding.value, state.binding(keptFlow, keptAddress).value)
            assertEquals(oldBinding.version, state.binding(keptFlow, keptAddress).version)
            assertTrue(checkNotNull(keptFlow.getTypeStatement(old.value)).lowerTypes.isNotEmpty())
            val aFlow = state.node(aRead).flow
            val current = aFlow.getExpressionCompletion(aRead)
            assertNotSame(old.value, current.value)
            assertSame(aFlow.getExpressionCompletion(case.replacement).value, current.value)
            assertSame(oldBinding, state.binding(frozen, oldAddress))
            assertSame(old, frozen.getExpressionCompletion(oldRead))
            assertEquals(1, state.context.assignmentCounter)
        }
    }

    @Test
    fun `two explicit Class copies preserve old member value when original and first copy are replaced`() {
        val case = ReceiverAddressCase()
        case.run { state ->
            case.initialize(state, twoCopies = true)
            val read = case.member(case.a, "old two copy member")
            val old = state.evaluate(read).flow.getExpressionCompletion(read)
            val oldAddress = state.storage(read)
            val frozen = state.currentFlow
            state.evaluate(case.writeA)
            val bRead = case.member(case.b, "first copy after store")
            val cRead = case.member(case.c, "second copy after store")
            assertSame(old.value, state.evaluate(bRead).flow.getExpressionCompletion(bRead).value)
            val bAddress = state.storage(bRead)
            val bBinding = state.binding(state.currentFlow, bAddress)
            assertSame(old.value, state.evaluate(cRead).flow.getExpressionCompletion(cRead).value)
            assertSame(bAddress, state.storage(cRead))
            assertSame(bBinding, state.binding(state.currentFlow, state.storage(cRead)))
            state.evaluate(case.fixture.assignment(case.b, case.replacement, "replace first copy"))
            val retained = case.member(case.c, "last copy after representative store")
            assertSame(old.value, state.evaluate(retained).flow.getExpressionCompletion(retained).value)
            val moved = state.storage(retained)
            assertSame(case.c.symbol, checkNotNull(moved.dispatchReceiver).symbol)
            assertSame(bBinding.value, state.binding(state.currentFlow, moved).value)
            assertEquals(bBinding.version, state.binding(state.currentFlow, moved).version)
            assertSame(old.value, state.binding(frozen, oldAddress).value)
            assertEquals(2, state.context.assignmentCounter)
        }
    }

    @Test
    fun `branch join preserves copied Class member proof when only one path replaces original`() {
        val case = ReceiverAddressCase()
        case.run { state ->
            case.initialize(state)
            val read = case.member(case.a, "before join")
            val thenRead = case.member(case.b, "then member")
            val elseRead = case.member(case.b, "else member")
            val choice = case.fixture.branch(case.flag,
                case.fixture.block("then replace", case.writeA, thenRead),
                case.fixture.block("else retain", elseRead), ConePrimitiveType.INT64)
            val copied = case.member(case.b, "member after join")
            val aRead = case.fixture.access(case.a, "joined original")
            val proof = case.proveNotBool(read, case.fixture.block("join with proof", choice, copied, aRead))
            lateinit var old: ExpressionCompletion
            state.evaluateIf(proof, beforeElse = { flow ->
                old = flow.getExpressionCompletion(read)
                assertTrue(checkNotNull(flow.getTypeStatement(old.value)).lowerTypes.isNotEmpty())
            })
            assertFalse(state.node(thenRead).isDead)
            assertFalse(state.node(elseRead).isDead)
            assertSame(old.value, state.node(thenRead).flow.getExpressionCompletion(thenRead).value)
            assertSame(old.value, state.node(elseRead).flow.getExpressionCompletion(elseRead).value)
            assertSame(old.value, state.node(choice).flow.getExpressionCompletion(choice).value)
            val copiedFlow = state.node(copied).flow
            assertSame(old.value, copiedFlow.getExpressionCompletion(copied).value)
            assertTrue(checkNotNull(copiedFlow.getTypeStatement(old.value)).lowerTypes.isNotEmpty())
            val aFlow = state.node(aRead).flow
            val bStorage = state.storage(case.fixture.access(case.b, "copied storage"), aFlow)
            assertNotSame(state.binding(aFlow, bStorage).value, aFlow.getExpressionCompletion(aRead).value)
            assertEquals(1, state.context.assignmentCounter)
        }
    }

    @Test
    fun `Class receiver snapshot forks retain old member facts and Source children after representative moves`() {
        val case = ReceiverAddressCase()
        case.run { original ->
            case.initialize(original, twoCopies = true)
            val read = case.member(case.a, "snapshot member")
            val otherwise = case.fixture.block("retain proof", read)
            val proof = case.proveNotBool(read, otherwise)
            // 快照必须在 IS-false 的实际分支位置捕获；不能用合流后的失效事实。
            original.evaluateIf(proof, beforeElse = { frozen ->
                val old = frozen.getExpressionCompletion(read)
                val oldAddress = original.storage(read, frozen)
                assertTrue(checkNotNull(frozen.getTypeStatement(old.value)).lowerTypes.isNotEmpty())
                val fork = case.fixture.resume(original, original.analyzer.captureDataFlowAnalyzerContext(IdentityMapper))
                val peer = case.fixture.resume(original, original.analyzer.captureDataFlowAnalyzerContext(IdentityMapper))
                assertSame(frozen, fork.currentFlow)
                assertSame(frozen, peer.currentFlow)
                fork.inFunctionContext(case.function, null) {
                    assertSame(old.value, fork.evaluate(read).flow.getExpressionCompletion(read).value)
                    fork.evaluate(case.writeA)
                    val retained = case.member(case.c, "fork retained")
                    assertSame(old.value, fork.evaluate(retained).flow.getExpressionCompletion(retained).value)
                    val moved = fork.storage(retained)
                    assertNotEquals(oldAddress, moved)
                    assertTrue(checkNotNull(fork.currentFlow.getTypeStatement(old.value)).lowerTypes.isNotEmpty())
                    assertReceiverSourceClosure(fork.currentFlow, moved, old, case, retained)
                    assertEquals(1, fork.context.assignmentCounter)
                    // snapshot 仍在外层 If 的 else block；按原 owner 关闭该 block/If，再关闭函数。
                    fork.analyzer.exitBlock(otherwise)
                    fork.analyzer.exitIfExpression(proof, callCompleted = true)
                    fork.analyzer.exitBlock(checkNotNull(case.function.body))
                    fork.analyzer.exitFunction(case.function)
                }
                peer.inFunctionContext(case.function, null) {
                    val reread = peer.evaluate(read).flow.getExpressionCompletion(read)
                    assertSame(old.value, reread.value)
                    assertNotSame(old.event, reread.event)
                    assertSame(oldAddress, peer.storage(read))
                    assertEquals(0, peer.context.assignmentCounter)
                    peer.analyzer.exitBlock(otherwise)
                    peer.analyzer.exitIfExpression(proof, callCompleted = true)
                    peer.analyzer.exitBlock(checkNotNull(case.function.body))
                    peer.analyzer.exitFunction(case.function)
                }
                assertSame(old, frozen.getExpressionCompletion(read))
                assertSame(old.value, original.binding(original.currentFlow, oldAddress).value)
                assertEquals(0, original.context.assignmentCounter)
            })
        }
    }

    @Test
    fun `Struct copies keep independent member facts while original stores another Struct value`() {
        val case = ReceiverAddressCase(struct = true)
        case.run { state ->
            case.initialize(state, twoCopies = true)
            val aRead = case.member(case.a, "Struct a member")
            val bRead = case.member(case.b, "Struct b member")
            val cRead = case.member(case.c, "Struct c member")
            val aValue = state.evaluate(aRead).flow.getExpressionCompletion(aRead).value
            val aAddress = state.storage(aRead)
            val bValue = state.evaluate(bRead).flow.getExpressionCompletion(bRead).value
            val bAddress = state.storage(bRead)
            val cValue = state.evaluate(cRead).flow.getExpressionCompletion(cRead).value
            val cAddress = state.storage(cRead)
            assertNotEquals(aAddress, bAddress)
            assertNotEquals(bAddress, cAddress)
            assertNotSame(aValue, bValue)
            assertNotSame(bValue, cValue)
            assertSame(case.a.symbol, checkNotNull(aAddress.dispatchReceiver).symbol)
            assertSame(case.b.symbol, checkNotNull(bAddress.dispatchReceiver).symbol)
            val frozen = state.currentFlow
            val proof = case.proveNotBool(bRead,
                case.fixture.block("Struct store original", case.writeA, bRead, cRead))
            state.evaluateIf(proof, beforeElse = { flow ->
                assertTrue(checkNotNull(flow.getTypeStatement(bValue)).lowerTypes.isNotEmpty())
                assertTrue(checkNotNull(flow.getTypeStatement(aValue)).lowerTypes.isEmpty())
                assertTrue(checkNotNull(flow.getTypeStatement(cValue)).lowerTypes.isEmpty())
            })
            val bFlow = state.node(bRead).flow
            val cFlow = state.node(cRead).flow
            assertSame(bValue, bFlow.getExpressionCompletion(bRead).value)
            assertSame(cValue, cFlow.getExpressionCompletion(cRead).value)
            assertSame(bAddress, state.storage(bRead, bFlow))
            assertSame(cAddress, state.storage(cRead, cFlow))
            assertSame(bValue, state.binding(frozen, bAddress).value)
            assertTrue(checkNotNull(cFlow.getTypeStatement(bValue)).lowerTypes.isNotEmpty())
            assertEquals(1, state.context.assignmentCounter)
        }
    }

    @Test
    fun `K direct alias gate remains independent of Class addresses and Struct copy addresses`() {
        for (struct in listOf(false, true)) for (inferred in listOf(false, true)) {
            val fixture = Fixture()
            val owner = fixture.nominalOwner("AliasGate", struct)
            val field = fixture.instanceField(owner)
            val function = fixture.reader(owner, "aliasGate")
            val parameter = fixture.externalAccess(function.valueParameters.single(), "immutable input")
            val source = fixture.variable("stableSource", owner.symbol.constructType(), parameter, mutable = false)
            val sourceRead = fixture.access(source, "source read")
            val copy = fixture.variable("gatedCopy", owner.symbol.constructType(), sourceRead,
                mutable = false, typeImplicit = inferred)
            val copyRead = fixture.access(copy, "copy read")
            fixture.analyzeFunction(function, declarations = listOf(owner)) { state ->
                state.declare(source)
                state.declare(copy)
                val firstStorage = state.storage(sourceRead)
                val secondStorage = state.storage(copyRead)
                assertNotEquals(firstStorage, secondStorage)
                assertSame(state.binding(state.currentFlow, firstStorage).value,
                    state.binding(state.currentFlow, secondStorage).value)
                if (inferred) assertSame(firstStorage, state.currentFlow.unwrapVariable(secondStorage))
                else assertSame(secondStorage, state.currentFlow.unwrapVariable(secondStorage))
                val first = fixture.externalAccess(field, "source member", sourceRead)
                val second = fixture.externalAccess(field, "copy member", copyRead)
                state.evaluate(first)
                state.evaluate(second)
                if (struct) assertNotEquals(state.storage(first), state.storage(second))
                else assertSame(state.storage(first), state.storage(second))
                assertEquals(0, state.context.assignmentCounter)
            }
        }
    }

    /** 仅收集公开 Source 访问的真实载荷；去重记录属于测试，不读取 Flow 私表。 */
    private fun assertReceiverSourceClosure(flow: Flow, moved: RealVariable, old: ExpressionCompletion,
        case: ReceiverAddressCase, read: CfirExpression) {
        val elements = java.util.Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
        val types = mutableSetOf<ConeCangJieType>()
        val variables = java.util.Collections.newSetFromMap(
            IdentityHashMap<org.cangnova.cangjie.cfir.resolve.dfa.DataFlowVariable, Boolean>())
        val flows = java.util.Collections.newSetFromMap(IdentityHashMap<Flow, Boolean>())
        val visitor = object : org.cangnova.cangjie.cfir.resolve.dfa.DfaSourceVisitor {
            override fun visitElement(element: CfirElement) { elements.add(element) }
            override fun visitType(type: ConeCangJieType) { types.add(type) }
            override fun visitGraph(graph: ControlFlowGraph) {}
            override fun visitNode(node: CFGNode<*>) {}
            override fun visitVariable(variable: org.cangnova.cangjie.cfir.resolve.dfa.DataFlowVariable) {
                if (variables.add(variable)) variable.acceptSourceChildren(this)
            }
            override fun visitFlow(flow: Flow) {
                if (flows.add(flow)) flow.acceptSourceChildren(this)
            }
        }
        // 从 Flow 进入 Source，包含历史 flow；没有普通 AST 遍历补齐缓存来源。
        visitor.visitFlow(flow)
        assertTrue(variables.any { it === moved })
        assertTrue(variables.any { it === checkNotNull(moved.dispatchReceiver) })
        assertTrue(variables.any { it === old.value })
        listOf(case.a, case.b, case.c, case.field, read).forEach { assertTrue(elements.contains(it)) }
        assertTrue(types.contains(case.owner.symbol.constructType()))
        assertTrue(types.contains(ConePrimitiveType.INT64))
    }

    /** 初始值由真实 nominal 构造产生，避免参数共享代表掩盖原变量代表迁移。 */
    private class ReceiverAddressCase(struct: Boolean = false) {
        val fixture = Fixture()
        val owner = fixture.nominalOwner(if (struct) "ReceiverStruct" else "ReceiverClass", struct)
        val field = fixture.instanceField(owner)
        val function = fixture.reader(owner, "receiverAddress", alternatives = true)
        private val initializer = fixture.constructedValue(owner)
        val a = fixture.variable("originalReceiver", owner.symbol.constructType(), initializer)
        val b = fixture.variable("firstCopy", owner.symbol.constructType(), fixture.access(a, "first copy"))
        val c = fixture.variable("secondCopy", owner.symbol.constructType(), fixture.access(a, "second copy"), mutable = false)
        val replacement = fixture.externalAccess(function.valueParameters[1], "other initialized object")
        val flag = fixture.externalAccess(function.valueParameters[2], "unknown Bool")
        val writeA = fixture.assignment(a, replacement, "replace original")

        fun member(variable: CfirPatternBindingVariable, identity: String): CfirExpression =
            fixture.externalAccess(field, identity, fixture.access(variable, identity + " receiver"))

        fun proveNotBool(read: CfirExpression, otherwise: CfirBlock): CfirIfExpression = fixture.branch(
            fixture.isType(read, ConePrimitiveType.BOOLEAN),
            fixture.block("impossible Bool member", fixture.unit()), otherwise,
            // typed fixture 保留两条 IS 路径；Unit 与实际 else 值的公共结果类型为 Any。
            ConeAnyType,
        )

        fun initialize(state: State, twoCopies: Boolean = false) {
            state.declare(a)
            state.declare(b)
            if (twoCopies) state.declare(c)
            val aStorage = state.storage(fixture.access(a, "a storage"))
            val bStorage = state.storage(fixture.access(b, "b storage"))
            assertNotEquals(aStorage, bStorage)
            assertSame(aStorage, state.currentFlow.unwrapVariable(aStorage))
            assertSame(bStorage, state.currentFlow.unwrapVariable(bStorage))
            val binding = state.binding(state.currentFlow, aStorage)
            assertTrue(binding.version is BindingVersion.Entry)
            assertSame(binding.value, state.binding(state.currentFlow, bStorage).value)
            assertSame(a, (binding.version as BindingVersion.Entry).event.origin)
            assertSame(owner.symbol, binding.value.originalType.toSymbol(fixture.session))
            assertEquals(0, state.context.assignmentCounter)
        }

        fun run(test: (State) -> Unit) = fixture.analyzeFunction(function, declarations = listOf(owner), test = test)
    }

    private class Fixture {
        val session = CallResolutionTestFixtures.newTestSession().also {
            it.register(CfirBuiltinTypes::class, CfirBuiltinTypes())
        }
        val logic = object : LogicSystem(session.typeContext) {}
        private val producer: CfirNamedFunction = buildNamedFunction {
            source = binarySource("makeValue signature")
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            status = CfirDeclarationStatusImpl()
            isLocal = false
            name = Name.identifier("makeValue")
            symbol = CfirNamedFunctionSymbol(CallableId(name))
            isMut = false
            returnTypeRef = buildResolvedTypeRef { coneType = ConeAnyType }
        }

        fun integer(value: Long) = buildLiteralExpression {
            source = binarySource("integer $value")
            kind = CfirLiteralKind.INT
            this.value = value
            coneTypeOrNull = ConePrimitiveType.INT64
        }

        fun boolean(value: Boolean) = buildLiteralExpression {
            source = binarySource("boolean $value")
            kind = CfirLiteralKind.BOOLEAN
            this.value = value
            coneTypeOrNull = ConePrimitiveType.BOOLEAN
        }

        fun unit() = buildLiteralExpression {
            source = binarySource("unit")
            kind = CfirLiteralKind.UNIT
            value = Unit
            coneTypeOrNull = ConePrimitiveType.UNIT
        }

        fun variable(
            identity: String,
            type: ConeCangJieType,
            initializer: CfirExpression?,
            mutable: Boolean = true,
            local: Boolean = true,
            typeImplicit: Boolean = false,
        ) = buildPatternBindingVariable {
            source = binarySource(identity)
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            status = CfirDeclarationStatusImpl()
            isLocal = local
            name = Name.identifier(identity)
            symbol = CfirPatternBindingSymbol(CallableId(name))
            returnTypeRef = buildResolvedTypeRef { coneType = type }
            isVar = mutable
            isTypeImplicit = typeImplicit
            this.ownedInitializer = initializer
        }

        fun access(variable: CfirPatternBindingVariable, identity: String) = buildNamedAccessExpression {
            source = binarySource(identity)
            calleeReference = buildResolvedNamedReference { name = variable.name; resolvedSymbol = variable.symbol }
            coneTypeOrNull = (variable.returnTypeRef as CfirResolvedTypeRef).coneType
        }

        fun assignment(variable: CfirPatternBindingVariable, value: CfirExpression, identity: String) = buildAssignment {
            source = binarySource(identity)
            lValue = access(variable, "$identity target")
            rValue = value
            coneTypeOrNull = ConePrimitiveType.UNIT
        }

        fun call() = buildFunctionCall {
            source = binarySource("makeValue call")
            calleeReference = buildResolvedNamedReference { name = producer.name; resolvedSymbol = producer.symbol }
            coneTypeOrNull = ConeAnyType
            sourceCallHasSideEffect = true
        }

        fun isType(expression: CfirExpression, type: ConeCangJieType) = buildTypeOperator {
            source = binarySource("IS proof")
            operation = CfirTypeOperationKind.IS
            argument = expression
            typeRef = buildResolvedTypeRef { coneType = type }
            coneTypeOrNull = ConePrimitiveType.BOOLEAN
        }

        fun block(identity: String, vararg expressions: CfirExpression) = buildBlock {
            source = binarySource(identity)
            statements += expressions
            coneTypeOrNull = checkNotNull(expressions.last().coneTypeOrNull)
        }

        fun branch(
            condition: CfirExpression,
            then: CfirBlock,
            otherwise: CfirBlock,
            type: ConeCangJieType = ConeAnyType,
        ) = buildIfExpression {
            source = binarySource("if")
            this.condition = condition
            thenBranch = then
            elseBranch = otherwise
            coneTypeOrNull = type
        }

        fun tuple(vararg expressions: CfirExpression) = buildTupleLiteral {
            source = binarySource("tuple")
            elements += expressions
            coneTypeOrNull = ConeTupleType(expressions.map { checkNotNull(it.coneTypeOrNull) })
        }

        /**
         * 这里输入的是初始化分析/CJO/FFI 已发布的声明事实，测试的是 consumer 准入合同。
         * 不执行上游 producer，也不以 Source/Library origin 或 initializer 语法替代 INITIALIZED。
         */
        fun externalGlobal(case: String, initialized: Boolean): CfirFieldVariable = buildFieldVariable {
            source = binarySource("$case global")
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = if (case == "imported" || case == "libraryWithoutImport") CfirDeclarationOrigin.Library else CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            status = CfirDeclarationStatusImpl().apply { isForeign = case == "foreign" }
            isLocal = false
            name = Name.identifier("externalValue")
            symbol = CfirFieldVariableSymbol(CallableId(FqName.ROOT, name))
            returnTypeRef = buildResolvedTypeRef { coneType = ConePrimitiveType.INT64 }
            isVar = false
            initializer = if (case == "source") integer(1) else null
        }.also {
            it.exportFacts = CfirDeclarationExportFacts(
                isGlobal = true,
                sourceFactsPublished = case == "source" || case == "foreign",
                isInitialized = initialized,
                isImported = case == "imported",
            )
        }

        // 保存 builder 自己的声明列表，供 fixture 在发布前添加 field/constructor；不访问 analyzer 私表。
        private val nominalMembers = IdentityHashMap<CfirClassLikeDeclaration, MutableList<CfirDeclaration>>()

        /** 注册真实 nominal owner，final status 只服务稳定性判断，不授初始化资格。 */
        fun nominalOwner(identity: String, struct: Boolean): CfirClassLikeDeclaration {
            val id = ClassId(FqName.ROOT, Name.identifier(identity))
            lateinit var members: MutableList<CfirDeclaration>
            val declaration = if (struct) buildStruct {
                members = declarations
                source = binarySource(identity)
                moduleData = session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                scopeProvider = CfirCangJieScopeProvider()
                status = CfirDeclarationStatusImpl().resolved(Visibilities.Public, Modality.FINAL)
                symbol = CfirStructSymbol(id)
                name = id.shortClassName
            } else buildClass {
                members = declarations
                source = binarySource(identity)
                moduleData = session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                scopeProvider = CfirCangJieScopeProvider()
                status = CfirDeclarationStatusImpl().resolved(Visibilities.Public, Modality.FINAL)
                symbol = CfirClassSymbol(id)
                name = id.shortClassName
            }
            nominalMembers[declaration] = members
            session.registerHierarchyProviders(listOf(declaration))
            return declaration
        }

        fun instanceField(owner: CfirClassLikeDeclaration): CfirFieldVariable = buildFieldVariable {
            source = binarySource("instance field")
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            status = CfirDeclarationStatusImpl().resolved(Visibilities.Public, Modality.FINAL)
            isLocal = false
            dispatchReceiverType = owner.symbol.constructType() as ConeSimpleCangJieType
            name = Name.identifier("number")
            symbol = CfirFieldVariableSymbol(CallableId(owner.symbol.classId, name))
            returnTypeRef = buildResolvedTypeRef { coneType = ConePrimitiveType.INT64 }
            isVar = false
        }.also { nominalMembers.getValue(owner).add(it) }

        fun externalAccess(declaration: CfirCallableDeclaration, identity: String, receiver: CfirExpression? = null) =
            buildNamedAccessExpression {
                source = binarySource(identity)
                calleeReference = buildResolvedNamedReference { name = declaration.symbol.name; resolvedSymbol = declaration.symbol }
                dispatchReceiver = receiver
                coneTypeOrNull = (declaration.returnTypeRef as CfirResolvedTypeRef).coneType
            }

        fun reader(owner: CfirClassLikeDeclaration, identity: String, alternatives: Boolean = false): CfirNamedFunction = buildNamedFunction {
            source = binarySource(identity)
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            status = CfirDeclarationStatusImpl()
            isLocal = false
            name = Name.identifier(identity)
            symbol = CfirNamedFunctionSymbol(CallableId(FqName.ROOT, name))
            isMut = false
            returnTypeRef = buildResolvedTypeRef { coneType = ConePrimitiveType.UNIT }
            valueParameters += CallResolutionTestFixtures.buildValueParameter(
                session, "objectParameter", owner.symbol.constructType(), containingDeclarationSymbol = symbol,
            )
            if (alternatives) {
                valueParameters += CallResolutionTestFixtures.buildValueParameter(
                    session, "otherObject", owner.symbol.constructType(), containingDeclarationSymbol = symbol,
                )
                valueParameters += CallResolutionTestFixtures.buildValueParameter(
                    session, "condition", ConePrimitiveType.BOOLEAN, containingDeclarationSymbol = symbol,
                )
            }
            body = buildBlock { source = binarySource("$identity body"); coneTypeOrNull = ConePrimitiveType.UNIT }
        }

        /** 真实 Class/Struct constructor call 走 State 已有的公开 call 生命周期。 */
        fun constructedValue(owner: CfirClassLikeDeclaration): CfirFunctionCall {
            val constructor = constructor(owner, unit())
            return buildFunctionCall {
                source = binarySource("nominal constructor call")
                calleeReference = buildResolvedNamedReference {
                    name = constructor.symbol.name
                    resolvedSymbol = constructor.symbol
                }
                coneTypeOrNull = owner.symbol.constructType()
                sourceCallHasSideEffect = true
            }
        }

        fun constructor(owner: CfirClassLikeDeclaration, read: CfirExpression): CfirConstructor = buildConstructor {
            source = binarySource("constructing owner")
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            status = CfirDeclarationStatusImpl()
            isLocal = false
            symbol = CfirConstructorSymbol(CallableId(owner.symbol.classId, Name.special("<init>")))
            returnTypeRef = buildResolvedTypeRef { coneType = owner.symbol.constructType() }
            body = buildBlock {
                source = binarySource("constructor body")
                statements += read
                coneTypeOrNull = ConePrimitiveType.UNIT
            }
        }.also { nominalMembers.getValue(owner).add(it) }

        /** 与既有 State 共用公开 analyzer；函数/构造器参数和 this 由 enterFunction 建 entry。 */
        fun analyzeFunction(
            function: org.cangnova.cangjie.cfir.declarations.CfirFunction,
            declarations: List<CfirDeclaration>,
            constructingClass: CfirClass? = null,
            test: (State) -> Unit,
        ) {
            val fragment = buildCodeFragment {
                source = binarySource("unused fragment holder")
                moduleData = session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                symbol = CfirCodeFragmentSymbol()
                block = buildBlock { coneTypeOrNull = ConePrimitiveType.UNIT }
            }
            val file = buildFile {
                source = binarySource("externalRead.cj")
                moduleData = session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                symbol = CfirFileSymbol()
                name = "externalRead.cj"
                packageDirective = buildPackageDirective { packageFqName = FqName.ROOT; isMacroPackage = false }
                this.declarations += declarations
                this.declarations += function
            }
            val state = State(this, fragment, file)
            state.inFunctionContext(function, constructingClass) {
                state.analyzer.enterFunction(function)
                val body = checkNotNull(function.body)
                state.analyzer.enterBlock(body)
                test(state)
                state.analyzer.exitBlock(body)
                state.analyzer.exitFunction(function)
            }
        }

        fun resume(original: State, snapshot: CfirDataFlowAnalyzerContextSnapshot): State =
            State(this, original.fragment, original.file).also { it.analyzer.restoreDataFlowAnalyzerContext(snapshot) }

        fun analyze(
            statements: List<CfirStatement>,
            externalDeclarations: List<CfirDeclaration> = emptyList(),
            test: (State) -> Unit,
        ) {
            val fragment = buildCodeFragment {
                source = binarySource("completed values fragment")
                moduleData = session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                symbol = CfirCodeFragmentSymbol()
                block = buildBlock {
                    source = binarySource("fragment block")
                    this.statements += statements
                    coneTypeOrNull = ConePrimitiveType.UNIT
                }
            }
            val file = buildFile {
                source = binarySource("completedValue.cj")
                moduleData = session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                symbol = CfirFileSymbol()
                name = "completedValue.cj"
                packageDirective = buildPackageDirective { packageFqName = FqName.ROOT; isMacroPackage = false }
                declarations += externalDeclarations
                declarations += listOf(producer, fragment)
            }
            val state = State(this, fragment, file)
            state.inContext {
                state.analyzer.enterCodeFragment(fragment)
                state.analyzer.enterBlock(fragment.block)
                test(state)
                if (!state.finished) state.finish()
            }
        }
    }

    /**
     * 只推进原 typed 输入的公共生命周期。节点索引是测试自己的事件观察记录；
     * 不注入 Flow、Binding、type/implication 表或 assignment counter。
     */
    private class State(val fixture: Fixture, val fragment: CfirCodeFragment, val file: CfirFile) {
        val context = CfirDataFlowAnalyzerContext()
        private val bodyContext = BodyResolveContext(ReturnTypeCalculatorForFullBodyResolve.Default, context)
        val analyzer = CfirDataFlowAnalyzer(object : SessionHolder { override val session = fixture.session }, bodyContext)
        private val completed = IdentityHashMap<CfirElement, CFGNode<*>>()
        val currentFlow: Flow get() = checkNotNull(analyzer.currentSmartCastPosition)
        var finished = false
            private set

        fun <T> inContext(action: () -> T): T = bodyContext.withFile(file) {
            bodyContext.withContainer(fragment) { action() }
        }

        /** 观察 storage 自身，避免查询稳定值 alias 时把 Struct 的副本变量折叠掉。 */
        fun storage(read: CfirExpression, flow: Flow = currentFlow): RealVariable = analyzer.run {
            val variable = flow.getKnownVariableWithoutUnwrappingAlias(read) as RealVariable
            assertSame(variable, flow.getVariableIfKnown(variable))
            variable
        }

        fun <T> inFunctionContext(
            function: org.cangnova.cangjie.cfir.declarations.CfirFunction,
            constructingClass: CfirClass?,
            action: () -> T,
        ): T = bodyContext.withFile(file) {
            if (constructingClass == null) bodyContext.withContainer(function, action)
            else bodyContext.withContainingClass(constructingClass) {
                bodyContext.withContainer(constructingClass) {
                    bodyContext.addReceiver(
                        constructingClass.name,
                        ImplicitDispatchReceiverValue(
                            constructingClass.symbol, useSiteSession = fixture.session, scopeSession = ScopeSession(),
                        ),
                    )
                    bodyContext.forConstructor(function as CfirConstructor) {
                        bodyContext.forConstructorBody(function, fixture.session, action)
                    }
                }
            }
        }

        /** 故意省去 receiver 出口，验证 entry 与本次完成证据不能互相替代；不注入 Flow。 */
        fun readWithoutEvaluatingReceiver(read: CfirQualifiedAccessExpression): CFGNode<*> {
            analyzer.exitQualifiedAccessExpression(read)
            return context.graphBuilder.lastNode.also { completed[read] = it }
        }

        fun node(element: CfirElement): CFGNode<*> = completed.getValue(element)

        fun real(read: CfirExpression): RealVariable {
            val variable = analyzer.getOrCreateVariable(read) as RealVariable
            assertSame(variable, currentFlow.getVariableIfKnown(variable))
            return variable
        }

        fun binding(flow: Flow, real: RealVariable): ValueBinding = checkNotNull(flow.getValueBinding(real))

        fun declare(variable: CfirPatternBindingVariable): CFGNode<*> {
            analyzer.enterVariableDeclaration(variable)
            variable.initializer?.let(::evaluate)
            analyzer.exitVariableDeclaration(variable)
            return context.graphBuilder.lastNode.also { completed[variable] = it }
        }

        /** 回调只读真实分支入口，旧事实必须由 condition 和 analyzer 的批准产生。 */
        fun evaluateIf(
            expression: CfirIfExpression,
            beforeThen: (Flow) -> Unit = {},
            beforeElse: (Flow) -> Unit = {},
        ): CFGNode<*> {
            analyzer.enterIfExpression(expression)
            evaluate(expression.condition)
            analyzer.exitIfCondition(expression)
            analyzer.enterBlock(expression.thenBranch)
            beforeThen(currentFlow)
            expression.thenBranch.statements.forEach { evaluate(it as CfirExpression) }
            analyzer.exitBlock(expression.thenBranch)
            completed[expression.thenBranch] = context.graphBuilder.lastNode
            analyzer.exitIfThenBranch(expression)
            val otherwise = checkNotNull(expression.elseBranch) as CfirBlock
            analyzer.enterBlock(otherwise)
            beforeElse(currentFlow)
            otherwise.statements.forEach { evaluate(it as CfirExpression) }
            analyzer.exitBlock(otherwise)
            completed[otherwise] = context.graphBuilder.lastNode
            analyzer.exitIfExpression(expression, callCompleted = true)
            return context.graphBuilder.lastNode.also { completed[expression] = it }
        }

        fun evaluate(expression: CfirExpression): CFGNode<*> {
            when (expression) {
                is CfirIfExpression -> return evaluateIf(expression)
                is CfirTupleLiteral -> {
                    analyzer.enterTupleLiteral(expression)
                    expression.elements.forEachIndexed { index, element ->
                        evaluate(element)
                        analyzer.exitTupleLiteralElement(expression, index, element)
                    }
                    analyzer.exitTupleLiteral(expression)
                }
                is CfirBlock -> {
                    analyzer.enterBlock(expression)
                    expression.statements.forEach { evaluate(it as CfirExpression) }
                    analyzer.exitBlock(expression)
                }
                is CfirAssignment -> {
                    evaluate(expression.rValue)
                    evaluate(expression.lValue)
                    analyzer.recordAssignment(expression)
                    analyzer.exitVariableAssignment(expression)
                }
                is CfirTypeOperator -> {
                    evaluate(expression.argument)
                    analyzer.exitTypeOperatorCall(expression)
                }
                is CfirLiteralExpression -> analyzer.exitLiteralExpression(expression)
                is CfirFunctionCall -> {
                    analyzer.enterCallArguments(expression, expression.argumentList.arguments)
                    analyzer.exitCallExplicitReceiver()
                    expression.argumentList.arguments.forEach(::evaluate)
                    analyzer.exitCallArguments()
                    analyzer.enterFunctionCall(expression)
                    analyzer.exitFunctionCall(expression, callCompleted = true)
                }
                is CfirQualifiedAccessExpression -> {
                    expression.dispatchReceiver?.let(::evaluate)
                    analyzer.exitQualifiedAccessExpression(expression)
                }
                else -> error("Unsupported typed value test expression: ${expression::class.simpleName}")
            }
            return context.graphBuilder.lastNode.also { completed[expression] = it }
        }

        fun finish(): ControlFlowGraph {
            check(!finished)
            analyzer.exitBlock(fragment.block)
            return analyzer.exitCodeFragment(fragment).also { finished = true }
        }
    }

    /** 保留同一真实输入身份，只复制 CFG/context；恢复后不得重建 fragment 或 source。 */
    private object IdentityMapper : SnapshotCfirMapper {
        override fun <T : CfirBasedSymbol<*>> mapSymbol(symbol: T): T = symbol
        override fun <T : CfirElement> mapElement(element: T): T = element
    }

    companion object {
        private fun binarySource(identity: String) = object : CjBinarySourceElement(identity, null, identity) {}
    }
}
