@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
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
import org.cangnova.cangjie.cfir.expressions.CfirThrowExpression
import org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral
import org.cangnova.cangjie.cfir.expressions.builder.buildAssignment
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.buildIfExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildThrowExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildTupleLiteral
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.resolve.dfa.DataFlowVariable
import org.cangnova.cangjie.cfir.resolve.dfa.DfaSourceVisitor
import org.cangnova.cangjie.cfir.resolve.dfa.Flow
import org.cangnova.cangjie.cfir.resolve.dfa.LogicSystem
import org.cangnova.cangjie.cfir.resolve.dfa.RealVariable
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.BlockExitNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.CFGNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraphVisitorVoid
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.FunctionCallExitNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.QualifiedAccessNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ThrowExceptionNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.TupleLiteralElementExitNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.TupleLiteralEnterNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.TupleLiteralExitNode
import org.cangnova.cangjie.cfir.resolve.dfa.createDfaSourceVisitor
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCodeFragmentSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Collections
import java.util.IdentityHashMap

/**
 * typed CFIR 的 tuple 求值程序点合同，依据固定 N14276740 TranslateTupleLit:15–22
 * 与 K3c7a5041 的冻结 Flow / 赋值身份。所有状态由公共 analyzer 生命周期驱动。
 * 不证明 cjc/source roundtrip、tuple 值 generation/projection 或完整 semantics readiness。
 */
class CfirTupleEvaluationLifecycleTest {
    @Test
    fun `later if element writes current x without changing the first element frozen version`() {
        val fixture = Fixture()
        val first = fixture.access("first x")
        val write = fixture.assignment(3, "then write")
        val thenRead = fixture.access("then x")
        val elseRead = fixture.access("else x")
        val second = buildIfExpression {
            source = binarySource("second if")
            condition = fixture.boolean(true)
            thenBranch = fixture.block("then", write, thenRead)
            elseBranch = fixture.block("else", elseRead)
            coneTypeOrNull = ConePrimitiveType.INT64
        }
        val tuple = fixture.tuple("outer", first, second)
        fixture.analyze(tuple) { state ->
            val initialized = state.node(fixture.x)
            val beforeTuple = state.node(fixture.writeTwo)
            val realX = state.realX(first)
            assertFalse(fixture.logic.isSameValueIn(initialized.flow, beforeTuple.flow, realX))
            val exit = state.evaluate(tuple) as TupleLiteralExitNode
            val firstExit = exit.elementExitNodes[0]
            val frozen = firstExit.flow
            assertCapture(firstExit, tuple, 0, first, state.node(first))
            assertCapture(exit.elementExitNodes[1], tuple, 1, second, state.node(second))
            assertTrue(fixture.logic.isSameValueIn(beforeTuple.flow, firstExit.flow, realX))
            assertFalse(fixture.logic.isSameValueIn(firstExit.flow, exit.flow, realX))
            assertTrue(fixture.logic.isSameValueIn(state.node(write).flow, exit.flow, realX))
            assertTrue(fixture.logic.isSameValueIn(state.node(thenRead).flow, exit.flow, realX))
            assertFalse(state.node(thenRead).isDead)
            assertTrue(state.node(elseRead).isDead)
            assertSame(frozen, firstExit.flow)
            assertTrue(fixture.logic.isSameValueIn(beforeTuple.flow, frozen, realX))
        }
    }

    @Test
    fun `nested tuple captures actual leaf owners and program points without flattening paths`() {
        val fixture = Fixture()
        val first = fixture.access("outer first")
        val innerFirst = fixture.access("inner first")
        val write = fixture.assignment(3, "inner write")
        val innerLast = fixture.access("inner last")
        val innerBlock = fixture.block("inner second block", write, innerLast)
        val inner = fixture.tuple("inner", innerFirst, innerBlock)
        val last = fixture.access("outer last")
        val outer = fixture.tuple("outer", first, inner, last)
        fixture.analyze(outer) { state ->
            val realX = state.realX(first)
            val outerExit = state.evaluate(outer) as TupleLiteralExitNode
            val innerExit = state.node(inner) as TupleLiteralExitNode
            assertCapture(outerExit.elementExitNodes[0], outer, 0, first, state.node(first))
            assertCapture(outerExit.elementExitNodes[1], outer, 1, inner, innerExit)
            assertCapture(outerExit.elementExitNodes[2], outer, 2, last, state.node(last))
            assertCapture(innerExit.elementExitNodes[0], inner, 0, innerFirst, state.node(innerFirst))
            assertCapture(innerExit.elementExitNodes[1], inner, 1, innerBlock, state.node(innerBlock))
            val early = outerExit.elementExitNodes[0].flow
            assertTrue(fixture.logic.isSameValueIn(early, innerExit.elementExitNodes[0].flow, realX))
            assertFalse(fixture.logic.isSameValueIn(early, innerExit.elementExitNodes[1].flow, realX))
            assertTrue(fixture.logic.isSameValueIn(state.node(innerLast).flow, innerExit.flow, realX))
            assertTrue(fixture.logic.isSameValueIn(innerExit.flow, outerExit.elementExitNodes[1].flow, realX))
            assertTrue(fixture.logic.isSameValueIn(innerExit.flow, outerExit.elementExitNodes[2].flow, realX))
            assertFalse(fixture.logic.isSameValueIn(early, outerExit.flow, realX))

            // 只走保留状态的 Source 协议；不递归 AST 来伪造来源覆盖。
            val sources = SourceFacts().also { state.context.acceptSourceChildren(it) }
            val expectedNodes = listOf(state.enters.getValue(outer), state.enters.getValue(inner), outerExit, innerExit) +
                outerExit.elementExitNodes + innerExit.elementExitNodes +
                listOf(first, innerFirst, innerLast, last, write).map(state::node)
            expectedNodes.forEach { node ->
                assertTrue(sources.nodes.contains(node), "Source must visit the actual ${node::class.simpleName}")
                assertTrue(sources.elements.any { it === node.fir })
                assertSame(node.fir.source, sources.elements.first { it === node.fir }.source)
            }
            listOf(outer, inner, innerBlock, first, innerFirst, innerLast, last).forEach { element ->
                assertTrue(sources.elements.any { it === element })
            }
        }
    }

    @Test
    fun `snapshot after first element completes both graphs with their own actual capture nodes`() {
        val fixture = Fixture()
        val first = fixture.access("first")
        val write = fixture.assignment(3, "second write")
        val last = fixture.access("second read")
        val second = fixture.block("second", write, last)
        val tuple = fixture.tuple("snapshot tuple", first, second)
        fixture.analyze(tuple) { original ->
            val realX = original.realX(first)
            original.enterTuple(tuple)
            original.evaluate(first)
            val originalFirst = original.capture(tuple, 0)
            val frozen = originalFirst.flow
            val originalGraph = original.context.graphBuilder.currentGraph
            val snapshot = original.analyzer.captureDataFlowAnalyzerContext(IdentityMapper)
            val copy = fixture.resume(snapshot, original)
            val copiedFirst = copy.context.graphBuilder.lastNode as TupleLiteralElementExitNode
            val copiedGraph = snapshot.graphMapping.getValue(originalGraph)
            assertNotSame(originalFirst, copiedFirst)
            assertSame(copiedGraph, copiedFirst.owner)
            assertSame(frozen, copiedFirst.flow)
            assertSame(frozen, snapshot.smartCastPosition)

            lateinit var copiedExit: TupleLiteralExitNode
            lateinit var copiedSecond: TupleLiteralElementExitNode
            val completedCopy = copy.inContext {
                copy.evaluate(second)
                copiedSecond = copy.capture(tuple, 1)
                copiedExit = copy.exitTuple(tuple)
                assertFalse(fixture.logic.isSameValueIn(frozen, copiedExit.flow, realX))
                assertTrue(fixture.logic.isSameValueIn(originalFirst.flow, checkNotNull(original.analyzer.currentSmartCastPosition), realX))
                copy.finish()
            }
            assertSame(copiedGraph, completedCopy)
            assertSame(frozen, originalFirst.flow)
            assertSame(frozen, copiedFirst.flow)

            original.evaluate(second)
            val originalSecond = original.capture(tuple, 1)
            val originalExit = original.exitTuple(tuple)
            assertFalse(fixture.logic.isSameValueIn(frozen, originalExit.flow, realX))
            assertSame(originalFirst, originalExit.elementExitNodes[0])
            assertSame(originalSecond, originalExit.elementExitNodes[1])
            assertSame(copiedFirst, copiedExit.elementExitNodes[0])
            assertSame(copiedSecond, copiedExit.elementExitNodes[1])
            assertNotSame(originalSecond, copiedSecond)
            assertNotSame(originalExit, copiedExit)
            val completedOriginal = original.finish()
            for ((graph, exit) in listOf(completedOriginal to originalExit, completedCopy to copiedExit)) {
                assertSame(original.fragment, graph.declaration)
                assertCapture(
                    exit.elementExitNodes[0], tuple, 0, first,
                    graph.nodes.single { it is QualifiedAccessNode && it.fir === first },
                )
                assertCapture(
                    exit.elementExitNodes[1], tuple, 1, second,
                    graph.nodes.single { it is BlockExitNode && it.fir === second },
                )
                exit.elementExitNodes.forEach { capture ->
                    assertSame(graph, capture.owner)
                    assertTrue(graph.nodes.any { it === capture })
                    assertFalse((if (graph === completedOriginal) completedCopy else completedOriginal).nodes.any { it === capture })
                }
                assertTrue(graph.nodes.any { it === exit })
                val sources = SourceFacts().also { graph.acceptSourceChildren(it) }
                exit.elementExitNodes.forEach { assertTrue(sources.nodes.contains(it)) }
                assertTrue(sources.nodes.all { it.owner === graph })
            }
            assertSame(frozen, originalFirst.flow)
            assertSame(frozen, copiedFirst.flow)
        }
    }

    @Test
    fun `throw element leaves actual suffix write read and tuple exit dead`() {
        val fixture = Fixture()
        val exception = fixture.call(fixture.makeException)
        val terminal = buildThrowExpression {
            source = binarySource("throw")
            this.exception = exception
            coneTypeOrNull = ConePrimitiveType.NOTHING
        }
        assertDeadSuffix(fixture, terminal)
    }

    @Test
    fun `Nothing returning call element leaves actual suffix write read and tuple exit dead`() {
        val fixture = Fixture()
        assertDeadSuffix(fixture, fixture.call(fixture.fail))
    }

    @Test
    fun `completed nested tuple graph dispatches ordered one parameter Void callbacks`() {
        val fixture = Fixture()
        val first = fixture.access("consumer outer first")
        val innerFirst = fixture.access("consumer inner first")
        val write = fixture.assignment(3, "consumer inner write")
        val innerLast = fixture.access("consumer inner last")
        val innerBlock = fixture.block("consumer inner second", write, innerLast)
        val inner = fixture.tuple("consumer inner tuple", innerFirst, innerBlock)
        val last = fixture.access("consumer outer last")
        val outer = fixture.tuple("consumer outer tuple", first, inner, last)
        fixture.analyze(outer) { state ->
            val outerExit = state.evaluate(outer) as TupleLiteralExitNode
            val innerExit = state.node(inner) as TupleLiteralExitNode
            val outerEnter = state.enters.getValue(outer)
            val innerEnter = state.enters.getValue(inner)
            val graph = state.finish()
            val callbacks = mutableListOf<Pair<String, CFGNode<*>>>()

            // 真正的公共 consumer：accept 的 final 双参桥必须到达这三个一参覆盖。
            graph.traverse(object : ControlFlowGraphVisitorVoid() {
                override fun visitNode(node: CFGNode<*>) = Unit

                override fun visitTupleLiteralEnterNode(node: TupleLiteralEnterNode) {
                    callbacks += "enter" to node
                }

                override fun visitTupleLiteralElementExitNode(node: TupleLiteralElementExitNode) {
                    callbacks += "element" to node
                }

                override fun visitTupleLiteralExitNode(node: TupleLiteralExitNode) {
                    callbacks += "exit" to node
                }
            })

            // inner 完成后才捕获 outer 的第二槽，不能把 nested 叶展平或挪到 outer 出口。
            val expected = listOf(
                Triple("enter", outer, outerEnter),
                Triple("element", first, outerExit.elementExitNodes[0]),
                Triple("enter", inner, innerEnter),
                Triple("element", innerFirst, innerExit.elementExitNodes[0]),
                Triple("element", innerBlock, innerExit.elementExitNodes[1]),
                Triple("exit", inner, innerExit),
                Triple("element", inner, outerExit.elementExitNodes[1]),
                Triple("element", last, outerExit.elementExitNodes[2]),
                Triple("exit", outer, outerExit),
            )
            assertEquals(expected.size, callbacks.size)
            expected.forEachIndexed { index, (kind, element, actualNode) ->
                val (callbackKind, callbackNode) = callbacks[index]
                assertEquals(kind, callbackKind, "dedicated callback at tuple program point $index")
                assertSame(actualNode, callbackNode, "actual graph node at tuple program point $index")
                assertSame(graph, callbackNode.owner)
                assertSame(element, callbackNode.fir)
                assertSame(element.source, callbackNode.fir.source)
                assertTrue(graph.nodes.any { it === callbackNode })
            }
            assertCapture(outerExit.elementExitNodes[0], outer, 0, first, state.node(first))
            assertCapture(innerExit.elementExitNodes[0], inner, 0, innerFirst, state.node(innerFirst))
            assertCapture(innerExit.elementExitNodes[1], inner, 1, innerBlock, state.node(innerBlock))
            assertCapture(outerExit.elementExitNodes[1], outer, 1, inner, innerExit)
            assertCapture(outerExit.elementExitNodes[2], outer, 2, last, state.node(last))
        }
    }

    /** 死亡断言针对实际语句与捕获节点，不用人工 normal 节点冒充未执行的后缀。 */
    private fun assertDeadSuffix(fixture: Fixture, terminal: CfirExpression) {
        val first = fixture.access("live first")
        val write = fixture.assignment(4, "dead write")
        val read = fixture.access("dead read")
        val suffix = fixture.block("dead suffix", write, read)
        val tuple = fixture.tuple("terminating tuple", first, terminal, suffix)
        fixture.analyze(tuple) { state ->
            val exit = state.evaluate(tuple) as TupleLiteralExitNode
            val graph = state.finish()
            val terminalNode = graph.nodes.single {
                it.fir === terminal && (it is ThrowExceptionNode || it is FunctionCallExitNode)
            }
            assertFalse(terminalNode.isDead)
            assertFalse(state.node(first).isDead)
            assertFalse(exit.elementExitNodes[0].isDead)
            assertTrue(exit.elementExitNodes[1].isDead)
            assertTrue(state.node(write).isDead)
            assertTrue(state.node(read).isDead)
            assertTrue(state.node(suffix).isDead)
            assertTrue(exit.elementExitNodes[2].isDead)
            assertTrue(exit.isDead)
            assertCapture(exit.elementExitNodes[2], tuple, 2, suffix, state.node(suffix))
            assertTrue(graph.nodes.any { it === state.node(write) })
            assertTrue(graph.nodes.any { it === state.node(read) })
            assertTrue(graph.nodes.any { it === exit })
        }
    }

    /** 捕获保留原元素/源身份与真实完成前驱；index 是当前 tuple 的局部槽位。 */
    private fun assertCapture(
        capture: TupleLiteralElementExitNode,
        tuple: CfirTupleLiteral,
        index: Int,
        element: CfirExpression,
        completed: CFGNode<*>,
    ) {
        assertSame(tuple, capture.tuple)
        assertEquals(index, capture.index)
        assertSame(tuple.elements[index], capture.fir)
        assertSame(element, capture.fir)
        assertSame(element.source, capture.fir.source)
        assertSame(completed.owner, capture.owner)
        assertTrue(capture.previousNodes.any { it === completed })
    }

    /** 与既有 Context/AssignmentOccurrence 测试共用正式 test session 和内部 typed 模型。 */
    private class Fixture {
        val session = CallResolutionTestFixtures.newTestSession().also {
            it.register(CfirBuiltinTypes::class, CfirBuiltinTypes())
        }
        val logic = object : LogicSystem(session.typeContext) {}
        val x = buildPatternBindingVariable {
            source = binarySource("x declaration")
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            status = CfirDeclarationStatusImpl()
            isLocal = true
            name = Name.identifier("x")
            symbol = CfirPatternBindingSymbol(CallableId(name))
            returnTypeRef = buildResolvedTypeRef { coneType = ConePrimitiveType.INT64 }
            isVar = true
            initializer = integer(1)
        }
        val writeTwo = assignment(2, "x equals two")
        // 已解析的签名是内部 CFIR 输入；此测试不解析或执行签名正文。
        val makeException = signature("makeException", ConeClassLikeType(ConeClassLikeLookupTagImpl(StdlibClassIds.Exception)))
        val fail = signature("fail", ConePrimitiveType.NOTHING)

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

        fun access(identity: String) = buildNamedAccessExpression {
            source = binarySource(identity)
            calleeReference = buildResolvedNamedReference { name = x.name; resolvedSymbol = x.symbol }
            coneTypeOrNull = ConePrimitiveType.INT64
        }

        fun assignment(value: Long, identity: String) = buildAssignment {
            source = binarySource(identity)
            lValue = access("$identity target")
            rValue = integer(value)
            coneTypeOrNull = ConePrimitiveType.UNIT
        }

        fun block(identity: String, vararg children: CfirExpression) = buildBlock {
            source = binarySource(identity)
            statements += children
            coneTypeOrNull = checkNotNull(children.last().coneTypeOrNull)
        }

        fun tuple(identity: String, vararg children: CfirExpression) = buildTupleLiteral {
            source = binarySource(identity)
            elements += children
            coneTypeOrNull = ConeTupleType(children.map { checkNotNull(it.coneTypeOrNull) })
        }

        private fun signature(identity: String, type: ConeCangJieType) = buildNamedFunction {
            source = binarySource(identity)
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            status = CfirDeclarationStatusImpl()
            isLocal = false
            name = Name.identifier(identity)
            symbol = CfirNamedFunctionSymbol(CallableId(name))
            isMut = false
            returnTypeRef = buildResolvedTypeRef { coneType = type }
        }

        fun call(function: CfirNamedFunction) = buildFunctionCall {
            source = binarySource("call ${function.name}")
            calleeReference = buildResolvedNamedReference { name = function.name; resolvedSymbol = function.symbol }
            coneTypeOrNull = (function.returnTypeRef as org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef).coneType
        }

        fun resume(snapshot: CfirDataFlowAnalyzerContextSnapshot, original: State): State =
            State(this, original.fragment, original.file).also {
                it.analyzer.restoreDataFlowAnalyzerContext(snapshot)
            }

        fun analyze(tuple: CfirTupleLiteral, test: (State) -> Unit) {
            val state = createState(tuple)
            state.inContext {
                state.analyzer.enterCodeFragment(state.fragment)
                state.analyzer.enterBlock(state.fragment.block)
                state.analyzer.enterVariableDeclaration(x)
                state.evaluate(checkNotNull(x.initializer))
                state.analyzer.exitVariableDeclaration(x)
                state.completed[x] = state.context.graphBuilder.lastNode
                state.evaluate(writeTwo)
                test(state)
                if (!state.finished) state.finish()
            }
        }

        /** 输入只构造一次；snapshot 与原图必须退出同一个 fragment/block 身份。 */
        private fun createState(tuple: CfirTupleLiteral): State {
            val fragment = buildCodeFragment {
                source = binarySource("tuple fragment")
                moduleData = session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                symbol = CfirCodeFragmentSymbol()
                block = buildBlock {
                    source = binarySource("fragment block")
                    statements += listOf(x, writeTwo, tuple)
                    coneTypeOrNull = tuple.coneTypeOrNull
                }
            }
            val file = buildFile {
                source = binarySource("tupleEvaluation.cj")
                moduleData = session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                symbol = CfirFileSymbol()
                name = "tupleEvaluation.cj"
                packageDirective = buildPackageDirective { packageFqName = FqName.ROOT; isMacroPackage = false }
                declarations += listOf(makeException, fail, fragment)
            }
            return State(this, fragment, file)
        }
    }

    /**
     * 测试驱动按公开生命周期遍历原输入。只读取 lastNode 记录实际结果；不设置 Flow、
     * assignment index、节点 readiness 或 mini 私表，也不制造替代表达式。
     */
    private class State(val fixture: Fixture, val fragment: CfirCodeFragment, val file: CfirFile) {
        val context = CfirDataFlowAnalyzerContext()
        private val bodyContext = BodyResolveContext(ReturnTypeCalculatorForFullBodyResolve.Default, context)
        val analyzer = CfirDataFlowAnalyzer(object : SessionHolder { override val session = fixture.session }, bodyContext)
        val completed = IdentityHashMap<CfirElement, CFGNode<*>>()
        val enters = IdentityHashMap<CfirTupleLiteral, TupleLiteralEnterNode>()
        var finished = false
            private set

        fun <T> inContext(action: () -> T): T = bodyContext.withFile(file) {
            bodyContext.withContainer(fragment) { action() }
        }

        fun node(element: CfirElement): CFGNode<*> = completed.getValue(element)

        fun realX(access: CfirExpression): RealVariable {
            val variable = analyzer.getOrCreateVariable(access) as RealVariable
            assertSame(fixture.x.symbol, variable.symbol)
            assertEquals(ConePrimitiveType.INT64, variable.originalType)
            assertSame(variable, checkNotNull(analyzer.currentSmartCastPosition).getVariableIfKnown(variable))
            return variable
        }

        fun enterTuple(tuple: CfirTupleLiteral) {
            analyzer.enterTupleLiteral(tuple)
            enters[tuple] = context.graphBuilder.lastNode as TupleLiteralEnterNode
        }

        fun capture(tuple: CfirTupleLiteral, index: Int): TupleLiteralElementExitNode {
            analyzer.exitTupleLiteralElement(tuple, index, tuple.elements[index])
            return context.graphBuilder.lastNode as TupleLiteralElementExitNode
        }

        fun exitTuple(tuple: CfirTupleLiteral): TupleLiteralExitNode {
            analyzer.exitTupleLiteral(tuple)
            val exit = context.graphBuilder.lastNode as TupleLiteralExitNode
            completed[tuple] = exit
            return exit
        }

        fun evaluate(expression: CfirExpression): CFGNode<*> {
            when (expression) {
                is CfirTupleLiteral -> {
                    enterTuple(expression)
                    expression.elements.forEachIndexed { index, element ->
                        evaluate(element)
                        capture(expression, index)
                    }
                    exitTuple(expression)
                }
                is CfirBlock -> {
                    analyzer.enterBlock(expression)
                    expression.statements.forEach { evaluate(it as CfirExpression) }
                    analyzer.exitBlock(expression)
                }
                is CfirIfExpression -> {
                    analyzer.enterIfExpression(expression)
                    evaluate(expression.condition)
                    analyzer.exitIfCondition(expression)
                    evaluate(expression.thenBranch)
                    analyzer.exitIfThenBranch(expression)
                    expression.elseBranch?.let(::evaluate)
                    analyzer.exitIfExpression(expression, callCompleted = true)
                }
                is CfirAssignment -> {
                    // 普通 scalar 写按固定 N 的 RHS 完成后再求 lhs；实际写只能由 analyzer 执行。
                    evaluate(expression.rValue)
                    evaluate(expression.lValue)
                    analyzer.recordAssignment(expression)
                    analyzer.exitVariableAssignment(expression)
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
                is CfirQualifiedAccessExpression -> analyzer.exitQualifiedAccessExpression(expression)
                is CfirThrowExpression -> {
                    evaluate(expression.exception)
                    analyzer.exitThrowException(expression)
                }
                else -> error("Unsupported typed test expression: ${expression::class.simpleName}")
            }
            return context.graphBuilder.lastNode.also { completed[expression] = it }
        }

        fun finish(): ControlFlowGraph {
            check(!finished)
            analyzer.exitBlock(fragment.block)
            return analyzer.exitCodeFragment(fragment).also { finished = true }
        }
    }

    /** 记录 Source 主动交出的节点身份；Flow/变量载荷委托现有只读协议，不走 AST visitor。 */
    private class SourceFacts : DfaSourceVisitor {
        val elements = mutableListOf<CfirElement>()
        val nodes: MutableSet<CFGNode<*>> = Collections.newSetFromMap(IdentityHashMap())
        private val graphs: MutableSet<ControlFlowGraph> = Collections.newSetFromMap(IdentityHashMap())
        private val payload = createDfaSourceVisitor({ elements += it }, {}, { false })

        override fun visitElement(element: CfirElement) = payload.visitElement(element)
        override fun visitType(type: ConeCangJieType) = payload.visitType(type)
        override fun visitVariable(variable: DataFlowVariable) = payload.visitVariable(variable)
        override fun visitFlow(flow: Flow) = payload.visitFlow(flow)
        override fun visitGraph(graph: ControlFlowGraph) {
            if (graphs.add(graph)) graph.acceptSourceChildren(this)
        }
        override fun visitNode(node: CFGNode<*>) {
            if (nodes.add(node)) node.acceptSourceChildren(this)
        }
    }

    /** 复制图保持同一 typed 输入身份；只复制 CFG owner/节点，不替换 source 或 symbol。 */
    private object IdentityMapper : SnapshotCfirMapper {
        override fun <T : CfirBasedSymbol<*>> mapSymbol(symbol: T): T = symbol
        override fun <T : CfirElement> mapElement(element: T): T = element
    }

    companion object {
        private fun binarySource(identity: String) = object : CjBinarySourceElement(identity, null, identity) {}
    }
}
