package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.CfirLoopTarget
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.deserialization.SingleModuleDataProvider
import org.cangnova.cangjie.cfir.entrypoint.configuration.createForCfirFrontend
import org.cangnova.cangjie.cfir.entrypoint.session.CfirDefaultSessionFactory
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.patterns.builder.buildBindingPattern
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticCollector
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.body.CfirBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzer
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzerContext
import org.cangnova.cangjie.cfir.resolve.dfa.*
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.*
import org.cangnova.cangjie.cfir.resolve.transformers.CfirImportResolveTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.source.CfirSourceCallKind
import org.cangnova.cangjie.cfir.source.CfirSourceForInKind
import org.cangnova.cangjie.cfir.source.CfirSourceOperator
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.source.CjBinarySourceElement
import kotlin.test.*

/**
 * 固定 K 3c7a5041 的 incoming/transfer/freeze、子图恢复与赋值身份合同。
 * 所有节点的 Flow 均由公开 analyzer 或正常 BODY 产生；测试不初始化节点 Flow。
 * native RANGE/STRING 形状沿用正常 BODY 测试及 N 14276740 的实际保存槽。
 * 本类不证明尚缺模型的 Match/enum/literal/Option 值关系，也不声明完整 G3 ready。
 */
class CfirDataFlowLifecycleTest : SessionHolder {
    private val library = CfirBinaryDependenciesModuleData(Name.special("<dfa-lifecycle-dependencies>"))
    private val module = CfirSourceModuleData(
        Name.identifier("dfa-lifecycle"), listOf(library), emptyList(),
        CangJiePlatforms.defaultCangJiePlatform, CfirPlatform.DEFAULT,
    )
    private val intType = ConePrimitiveType(PrimitiveTypeKind.INT64)
    private val boolType = ConePrimitiveType(PrimitiveTypeKind.BOOLEAN)
    private val unitType = ConePrimitiveType(PrimitiveTypeKind.UNIT)
    private val stringType = ConeClassLikeType(ConeClassLikeLookupTagImpl(StdlibClassIds.String))
    override val session: CfirSession = createSession()
    private val logic = object : LogicSystem(session.typeContext) {}

    @Test
    fun fileFunctionBlockAndLiteralFreezeWithRealBranchHistory() {
        val symbol = functionSymbol("branches")
        val flag = parameter("flag", boolType, symbol)
        val value = variable("value", intType, integer(0), true)
        val before = assignment(value, integer(1))
        val left = assignment(value, integer(2))
        val right = assignment(value, integer(3))
        val condition = access(flag)
        val branch = buildIfExpression {
            source = source("if")
            coneTypeOrNull = unitType
            this.condition = condition
            thenBranch = block(left)
            elseBranch = block(right)
        }
        val tail = integer(4)
        val function = function(symbol, block(value, before, branch, tail).also { it.replaceConeTypeOrNull(intType) },
            listOf(flag), resultType = intType)
        val driver = Driver(function)
        val graph = driver.run()
        val root = graph.nodes.filterIsInstance<FunctionEnterNode>().single().flow
        val conditionFlow = graph.nodes.filterIsInstance<QualifiedAccessNode>().single { it.fir === condition }.flow
        val leftFlow = graph.nodes.filterIsInstance<VariableAssignmentNode>().single { it.fir === left }.flow
        val rightFlow = graph.nodes.filterIsInstance<VariableAssignmentNode>().single { it.fir === right }.flow
        val join = graph.nodes.filterIsInstance<IfExitNode>().single().flow
        val real = known(conditionFlow, value)
        assertNotSame(leftFlow, rightFlow)
        assertSame(conditionFlow, leftFlow.lowestCommonAncestor(rightFlow))
        assertSame(conditionFlow, conditionFlow.lowestCommonAncestor(join))
        assertSame(root, root.lowestCommonAncestor(graph.exitNode.flow))
        assertFalse(logic.isSameValueIn(conditionFlow, leftFlow, real))
        assertFalse(logic.isSameValueIn(leftFlow, rightFlow, real))
        assertFalse(logic.isSameValueIn(leftFlow, join, real))
        assertEquals(setOf(intType), join.getTypeStatement(real)?.upperTypes)
        assertSame(join, join.lowestCommonAncestor(graph.nodes.single { it.fir === tail }.flow))
        assertSame(driver.file, driver.fileGraph.declaration)
        assertNull(driver.analyzer.currentSmartCastPosition)
        assertComplete(graph)
        assertComplete(driver.fileGraph)
    }

    @Test
    fun inferredImmutableCopyKeepsOldRepresentativeAfterActualRhsAssignment() {
        val value = variable("value", ConeAnyType, integer(0), true)
        val seed = assignment(value, integer(1))
        val copy = variable("copy", ConeAnyType, access(value), false, implicit = true)
        val typedCopy = variable("typedCopy", ConeAnyType, access(value), false)
        val write = assignment(value, boolean(true))
        val function = function(functionSymbol("aliases"), block(value, seed, copy, typedCopy, write))
        val driver = Driver(function)
        val graph = driver.run()
        val before = graph.nodes.filterIsInstance<VariableDeclarationExitNode>().single { it.fir === typedCopy }.flow
        val after = graph.nodes.filterIsInstance<VariableAssignmentNode>().single { it.fir === write }.flow
        val real = known(before, value)
        val saved = known(before, copy)
        val typed = known(before, typedCopy)
        assertSame(real, before.unwrapVariable(saved))
        assertSame(typed, before.unwrapVariable(typed))
        assertSame(saved, after.unwrapVariable(saved))
        assertSame(real, after.unwrapVariable(real))
        assertFalse(logic.isSameValueIn(before, after, real))
        assertTrue(logic.isSameValueIn(before, after, saved))
        assertEquals(setOf(intType), before.getTypeStatement(saved)?.upperTypes)
        assertEquals(setOf(intType), after.getTypeStatement(saved)?.upperTypes)
        assertEquals(setOf(boolType), after.getTypeStatement(real)?.upperTypes)
        assertSame(boolType, write.rValue.coneTypeOrNull)
        assertSame(before, before.lowestCommonAncestor(after))
        assertComplete(graph)
    }

    @Test
    fun declaredLocalAndAnonymousFunctionsRestoreOuterPositionAndValue() {
        val value = variable("captured", ConeAnyType, integer(0), true)
        val seed = assignment(value, integer(1))
        val innerWrite = assignment(value, boolean(true))
        val deepest = function(functionSymbol("deepest", true), block(innerWrite), local = true)
        val local = function(functionSymbol("local", true), block(deepest, unitLiteral()), local = true)
        val anonymousWrite = assignment(value, boolean(false))
        val anonymous = buildAnonymousFunction {
            source = source("anonymous")
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            status = CfirDeclarationStatusImpl(Visibilities.Local)
            symbol = CfirAnonymousFunctionSymbol()
            returnTypeRef = typeRef(unitType)
            typeRef = typeRef(ConeFunctionType(emptyList(), unitType))
            body = block(anonymousWrite)
            hasExplicitParameterList = true
            isLambda = true
        }
        val expression = buildAnonymousFunctionExpression {
            source = source("lambda-expression")
            anonymousFunction = anonymous
            isTrailingLambda = false
        }
        val tail = access(value)
        val outer = function(functionSymbol("closures"), block(value, seed, local, expression, tail, unitLiteral()))
        val driver = Driver(outer)
        val graph = driver.run()
        val seeded = graph.nodes.filterIsInstance<VariableAssignmentNode>().single { it.fir === seed }.flow
        val finalFlow = graph.nodes.filterIsInstance<QualifiedAccessNode>().single { it.fir === tail }.flow
        val real = known(seeded, value)
        assertTrue(logic.isSameValueIn(seeded, finalFlow, real))
        assertEquals(setOf(intType), finalFlow.getTypeStatement(real)?.upperTypes)
        for (child in listOf(deepest, anonymous)) {
            val childGraph = assertIs<CfirControlFlowGraphReferenceImpl>(child.controlFlowGraphReference).controlFlowGraph
            assertFalse(logic.isSameValueIn(seeded, childGraph.exitNode.flow, real))
            assertEquals(setOf(boolType), childGraph.exitNode.flow.getTypeStatement(real)?.upperTypes)
            assertComplete(childGraph)
        }
        assertComplete(assertIs<CfirControlFlowGraphReferenceImpl>(local.controlFlowGraphReference).controlFlowGraph)
        assertEquals(3, driver.restoredFunctions)
        assertComplete(graph)
    }

    @Test
    fun typedDefaultGraphHasFourBoundariesAndCompletedDefaultIsNeverReplayed() {
        val symbol = functionSymbol("defaults")
        val default = integer(7)
        val parameter = parameter("arg", intType, symbol, default)
        val noDefault = parameter("required", intType, symbol)
        val function = function(symbol, block(unitLiteral()), listOf(parameter, noDefault))
        val first = Driver(function)
        val graph = first.run()
        val reference = assertIs<CfirControlFlowGraphReferenceImpl>(parameter.controlFlowGraphReference)
        val defaultGraph = reference.controlFlowGraph
        val enter = graph.nodes.filterIsInstance<EnterValueParameterNode>().single()
        val exit = graph.nodes.filterIsInstance<ExitValueParameterNode>().single()
        assertSame(parameter, enter.fir)
        assertSame(parameter, exit.fir)
        assertSame(parameter, defaultGraph.declaration)
        assertIs<EnterDefaultArgumentsNode>(defaultGraph.enterNode)
        assertIs<ExitDefaultArgumentsNode>(defaultGraph.exitNode)
        assertSame(default, defaultGraph.nodes.filterIsInstance<LiteralExpressionNode>().single().fir)
        assertSame(enter.flow, enter.flow.lowestCommonAncestor(exit.flow))
        assertSame(enter.flow, enter.flow.lowestCommonAncestor(defaultGraph.exitNode.flow))
        assertSame(defaultGraph.enterNode.flow,
            defaultGraph.enterNode.flow.lowestCommonAncestor(defaultGraph.exitNode.flow))
        assertComplete(defaultGraph)
        assertComplete(graph)
        parameter.replaceResolvePhase(CfirResolvePhase.BODY_RESOLVE)
        val second = Driver(function)
        val secondGraph = second.run()
        assertSame(default, parameter.defaultValue)
        assertSame(reference, parameter.controlFlowGraphReference)
        assertEquals(1, first.defaultsEvaluated)
        assertEquals(0, second.defaultsEvaluated)
        assertFalse(secondGraph.nodes.any { it.fir === default })
        assertEquals(1, secondGraph.nodes.count { it is EnterValueParameterNode })
        assertEquals(1, secondGraph.nodes.count { it is ExitValueParameterNode })
        assertComplete(secondGraph)
    }

    @Test
    fun repeatInvalidatesMutableValueWhileFalseRestoresInputAndDoWhileKeepsActualWrite() {
        for (doWhile in listOf(false, true)) {
            val value = variable("repeatValue", ConeAnyType, integer(0), true)
            val seed = assignment(value, integer(1))
            val saved = variable("saved", ConeAnyType, access(value), false, implicit = true)
            val write = assignment(value, boolean(true))
            val loop = buildLoopExpression {
                source = source("repeat-$doWhile")
                condition = boolean(false)
                body = block(write)
                isDoWhile = doWhile
                coneTypeOrNull = unitType
            }
            val function = function(functionSymbol("repeat$doWhile"), block(value, seed, saved, loop))
            val driver = Driver(function)
            val graph = driver.run()
            val before = graph.nodes.filterIsInstance<VariableDeclarationExitNode>().single { it.fir === saved }.flow
            val repeat = if (doWhile) graph.nodes.filterIsInstance<LoopEnterNode>().single().flow
                else graph.nodes.filterIsInstance<LoopConditionEnterNode>().single().flow
            val exit = graph.nodes.filterIsInstance<LoopExitNode>().single().flow
            val real = known(before, value)
            val copy = known(before, saved)
            assertFalse(logic.isSameValueIn(before, repeat, real))
            assertNull(repeat.getTypeStatement(real))
            assertEquals(setOf(intType), repeat.getTypeStatement(copy)?.upperTypes,
                "before=${before.getTypeStatement(copy)}, alias=${before.unwrapVariable(copy)}, repeat=${repeat.getTypeStatement(copy)}")
            assertEquals(setOf(if (doWhile) boolType else intType), exit.getTypeStatement(real)?.upperTypes,
                "doWhile=$doWhile before=${before.getTypeStatement(real)} repeat=${repeat.getTypeStatement(real)} inputs=" +
                    graph.nodes.filterIsInstance<LoopExitNode>().single().previousNodes.map { "${it::class.simpleName}:dead=${it.isDead}:${it.flow.getTypeStatement(real)}" })
            assertEquals(setOf(intType), exit.getTypeStatement(copy)?.upperTypes)
            assertComplete(graph)
        }
    }

    @Test
    fun breakAndContinueFinallyPathsCarryActualAssignmentAndRestoreLoopExit() {
        for (breaking in listOf(false, true)) {
            val symbol = functionSymbol("finally$breaking")
            val flag = parameter("again", boolType, symbol)
            val value = variable("finallyValue", ConeAnyType, integer(0), true)
            val seed = assignment(value, integer(1))
            val target = CfirLoopTarget()
            val jump: CfirJump<*> = if (breaking) buildBreakExpression {
                source = source("break"); this.target = target; coneTypeOrNull = ConePrimitiveType.NOTHING
            } else buildContinueExpression {
                source = source("continue"); this.target = target; coneTypeOrNull = ConePrimitiveType.NOTHING
            }
            val finalWrite = assignment(value, integer(9))
            val tryExpression = buildTryExpression {
                source = source("try-finally")
                coneTypeOrNull = ConePrimitiveType.NOTHING
                tryBlock = block(assignment(value, boolean(true)), jump).also {
                    it.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
                }
                finallyBlock = block(finalWrite)
            }
            val loop = buildLoopExpression {
                source = source("finally-loop")
                condition = access(flag)
                body = block(tryExpression).also { it.replaceConeTypeOrNull(ConePrimitiveType.NOTHING) }
                isDoWhile = false
                coneTypeOrNull = unitType
            }
            target.bind(loop)
            val function = function(symbol, block(value, seed, loop), listOf(flag))
            val driver = Driver(function)
            val graph = driver.run()
            val enter = graph.nodes.filterIsInstance<FinallyBlockEnterNode>().single()
            val exit = graph.nodes.filterIsInstance<FinallyBlockExitNode>().single()
            val destination: CFGNode<*> = if (breaking) graph.nodes.filterIsInstance<LoopExitNode>().single()
                else graph.nodes.filterIsInstance<LoopConditionEnterNode>().single()
            val path = FlowPath.CfgEdge(destination as EdgeLabel, tryExpression)
            val incoming = assertNotNull(enter.getAlternateFlow(path))
            val outgoing = assertNotNull(exit.getAlternateFlow(path))
            val real = known(outgoing, value)
            assertEquals(setOf(boolType), incoming.getTypeStatement(real)?.upperTypes)
            assertEquals(setOf(intType), outgoing.getTypeStatement(real)?.upperTypes)
            assertFalse(logic.isSameValueIn(incoming, outgoing, real))
            val writeNode = graph.nodes.filterIsInstance<VariableAssignmentNode>().single { it.fir === finalWrite }
            val writeFlow = assertNotNull(writeNode.getAlternateFlow(path))
            assertTrue(logic.isSameValueIn(writeFlow, outgoing, real))
            assertEquals(setOf(intType), graph.nodes.filterIsInstance<LoopExitNode>().single().flow.getTypeStatement(real)?.upperTypes)
            assertTrue(exit.followingNodes.any { it === destination })
            assertSame(destination, exit.edgeTo(destination).label)
            assertComplete(graph)
        }
    }

    @Test
    fun normalBodyRangeAndStringPublishFlowsAtStoredNativeStages() {
        for (kind in listOf(CfirSourceForInKind.RANGE, CfirSourceForInKind.STRING)) {
            val outerValue = variable("outerValue", intType, integer(0), true)
            val seed = assignment(outerValue, integer(1))
            val index = variable("index", intType, integer(0), true)
            val text = variable("text", stringType, buildLiteralExpression {
                source = source("text"); this.kind = CfirLiteralKind.STRING; value = "ab"; coneTypeOrNull = stringType
            }, false)
            val bound = variable("bound", intType, if (kind == CfirSourceForInKind.RANGE) integer(3)
                else call(member("size", intType), access(text)), false)
            val update = assignment(index, integer(1), CfirSourceOperator.ADD_ASSIGN)
            val comparison = buildBinaryExpression {
                source = source("range-check"); left = access(index); right = access(bound)
                operation = CfirSourceOperator.LESS; coneTypeOrNull = boolType
            }
            val binding = variable("element", if (kind == CfirSourceForInKind.RANGE) intType
                else ConePrimitiveType(PrimitiveTypeKind.UINT8), if (kind == CfirSourceForInKind.RANGE) access(index)
                else call(member("[]", ConePrimitiveType(PrimitiveTypeKind.UINT8), listOf(intType)), access(text), listOf(access(index))), false)
            val storage = if (kind == CfirSourceForInKind.RANGE) block(index, bound, update, comparison).also {
                it.replaceConeTypeOrNull(boolType)
            } else block(index, text, bound)
            val loop = buildForInExpression {
                source = source("native-$kind"); iterationKind = kind; iterable = storage
                pattern = buildBindingPattern {
                    name = binding.name; bindingVariable = binding
                    semanticTypeRef = binding.returnTypeRef; typeRef = binding.returnTypeRef
                }
                body = block(assignment(outerValue, integer(2)), unitLiteral()); coneTypeOrNull = unitType
            }
            val function = function(functionSymbol("native$kind"), block(outerValue, seed, loop), phase = CfirResolvePhase.IMPLICIT_TYPES)
            val file = file(function)
            CfirImportResolveTransformer(session, CfirDiagnosticCollector()).recordImportBindingsForFile(file)
            CfirBodyResolveTransformer(session, ScopeSession(), ReturnTypeCalculatorForFullBodyResolve.Default)
                .transformFile(file, ResolutionMode.ContextIndependent)
            val graph = assertIs<CfirControlFlowGraphReferenceImpl>(function.controlFlowGraphReference).controlFlowGraph
            val declared = graph.nodes.filterIsInstance<VariableDeclarationExitNode>().single { it.fir === index }
            val condition = graph.nodes.filterIsInstance<ForInLoopConditionEnterNode>().single()
            val updateNode: CFGNode<*> = if (kind == CfirSourceForInKind.RANGE)
                graph.nodes.filterIsInstance<VariableAssignmentNode>().single { it.fir === update }
                else graph.nodes.filterIsInstance<ForInLoopStringUpdateNode>().single()
            val real = known(declared.flow, index)
            assertTrue(logic.isSameValueIn(declared.flow, condition.flow, real))
            val seeded = graph.nodes.filterIsInstance<VariableAssignmentNode>().single { it.fir === seed }.flow
            val outerReal = known(seeded, outerValue)
            assertFalse(logic.isSameValueIn(seeded, condition.flow, outerReal))
            assertNull(condition.flow.getTypeStatement(outerReal))
            assertFalse(logic.isSameValueIn(condition.flow, updateNode.flow, real))
            assertEquals(setOf(intType), updateNode.flow.getTypeStatement(real)?.upperTypes)
            assertSame(bound.symbol, known(updateNode.flow, bound).symbol)
            val bindingExit = graph.nodes.filterIsInstance<VariableDeclarationExitNode>().single { it.fir === binding }
            assertSame(binding.symbol, known(bindingExit.flow, binding).symbol)
            val positions = listOf(declared, graph.nodes.filterIsInstance<ForInLoopInitialCheckExitNode>().single(),
                condition, graph.nodes.filterIsInstance<ForInLoopIterationExitNode>().single(), bindingExit,
                graph.nodes.filterIsInstance<LoopBlockExitNode>().single(), updateNode).map(graph.nodes::indexOf)
            positions.zipWithNext().forEach { (a, b) -> assertTrue(a < b, "Native stage order: $positions") }
            assertTrue(updateNode.followingNodes.any { it === condition && updateNode.edgeTo(it).kind.isBack })
            assertSame(storage, loop.iterable)
            assertComplete(graph)
        }
    }

    /** 只按这些真实 typed 载荷发公开事件；未列出的表达式使测试驱动立即失败。 */
    private inner class Driver(val function: CfirNamedFunction) {
        val file = file(function)
        private val dfa = CfirDataFlowAnalyzerContext()
        private val context = BodyResolveContext(ReturnTypeCalculatorForFullBodyResolve.Default, dfa)
        val analyzer = CfirDataFlowAnalyzer(this@CfirDataFlowLifecycleTest, context)
        lateinit var fileGraph: ControlFlowGraph
        var restoredFunctions = 0
        var defaultsEvaluated = 0

        fun run(): ControlFlowGraph = context.withFile(file) {
            analyzer.enterFile(file, buildGraph = true)
            val outer = assertIs<PersistentFlow>(analyzer.currentSmartCastPosition)
            emitFunction(function)
            assertSame(outer, analyzer.currentSmartCastPosition)
            fileGraph = assertNotNull(analyzer.exitFile())
            assertIs<CfirControlFlowGraphReferenceImpl>(function.controlFlowGraphReference).controlFlowGraph
        }

        private fun emitFunction(owner: CfirFunction) = context.withContainer(owner) {
            analyzer.enterFunction(owner)
            owner.valueParameters.forEach { parameter ->
                assertSame(owner.symbol, parameter.containingDeclarationSymbol)
                analyzer.enterValueParameter(parameter)
                if (parameter.defaultValue != null && parameter.resolvePhase < CfirResolvePhase.BODY_RESOLVE) {
                    defaultsEvaluated++
                    emit(checkNotNull(parameter.defaultValue))
                }
                val graph = analyzer.exitValueParameter(parameter)
                if (parameter.defaultValue != null) {
                    val reference = parameter.controlFlowGraphReference
                    if (parameter.resolvePhase < CfirResolvePhase.BODY_RESOLVE) {
                        parameter.replaceControlFlowGraphReference(CfirControlFlowGraphReferenceImpl(assertNotNull(graph)))
                    } else assertSame(assertIs<CfirControlFlowGraphReferenceImpl>(reference).controlFlowGraph, graph)
                    assertSame(dfa.graphBuilder.lastNode.flow, analyzer.currentSmartCastPosition)
                }
            }
            emit(checkNotNull(owner.body))
            owner.replaceControlFlowGraphReference(assertNotNull(analyzer.exitFunction(owner)))
        }

        private fun emit(statement: CfirStatement) {
            when (statement) {
                is CfirBlock -> {
                    analyzer.enterBlock(statement)
                    statement.statements.forEach(::emit)
                    analyzer.exitBlock(statement)
                }
                is CfirVariable -> {
                    analyzer.enterVariableDeclaration(statement)
                    statement.initializer?.let(::emit)
                    analyzer.exitVariableDeclaration(statement)
                }
                is CfirNamedFunction -> {
                    emitFunction(statement)
                    assertSame(dfa.graphBuilder.lastNode.flow, analyzer.currentSmartCastPosition)
                    restoredFunctions++
                }
                is CfirAnonymousFunctionExpression -> {
                    analyzer.enterAnonymousFunctionExpression(statement)
                    val outer = analyzer.currentSmartCastPosition
                    emitFunction(statement.anonymousFunction)
                    assertSame(outer, analyzer.currentSmartCastPosition)
                    restoredFunctions++
                }
                is CfirLiteralExpression -> analyzer.exitLiteralExpression(statement)
                is CfirNamedAccessExpression -> analyzer.exitQualifiedAccessExpression(statement)
                is CfirAssignment -> {
                    emit(statement.lValue); emit(statement.rValue)
                    analyzer.recordAssignment(statement)
                    analyzer.exitVariableAssignment(statement)
                }
                is CfirIfExpression -> {
                    analyzer.enterIfExpression(statement)
                    emit(statement.condition); analyzer.exitIfCondition(statement)
                    emit(statement.thenBranch); analyzer.exitIfThenBranch(statement)
                    statement.elseBranch?.let(::emit)
                    analyzer.exitIfExpression(statement, callCompleted = true)
                }
                is CfirLoopExpression -> {
                    if (statement.isDoWhile) {
                        analyzer.enterDoWhileLoop(statement); emit(statement.body)
                        analyzer.enterDoWhileLoopCondition(statement); emit(statement.condition)
                        analyzer.exitDoWhileLoop(statement)
                    } else {
                        analyzer.enterWhileLoop(statement); emit(statement.condition)
                        analyzer.exitWhileLoopCondition(statement); emit(statement.body)
                        analyzer.exitWhileLoop(statement)
                    }
                    assertSame(dfa.graphBuilder.lastNode.flow, analyzer.currentSmartCastPosition)
                }
                is CfirJump<*> -> { analyzer.enterJump(statement); analyzer.exitJump(statement) }
                is CfirTryExpression -> {
                    analyzer.enterTryExpression(statement); emit(statement.tryBlock); analyzer.exitTryMainBlock()
                    analyzer.enterFinallyBlock(); emit(checkNotNull(statement.finallyBlock)); analyzer.exitFinallyBlock()
                    analyzer.exitTryExpression(callCompleted = true)
                }
                else -> error("No actual lifecycle event driver for ${statement::class}")
            }
        }
    }

    private fun known(flow: Flow, variable: CfirVariable): RealVariable =
        assertIs<RealVariable>(flow.getVariableIfKnown(access(variable)))

    private fun assertComplete(graph: ControlFlowGraph) {
        assertTrue(graph.nodes.isNotEmpty())
        graph.nodes.forEach { node ->
            assertTrue(node.flowInitialized, "Missing actual flow: $node")
            assertSame(node.flow, node.flow.lowestCommonAncestor(node.flow))
            node.alternateFlowPaths.forEach { assertNotNull(node.getAlternateFlow(it)) }
        }
    }

    private fun createSession(): CfirSession {
        val configuration = CompilerConfiguration.createForCfirFrontend()
        val factory = CfirDefaultSessionFactory()
        val shared = factory.createSharedLibrarySession(module.name, emptyList(), configuration.languageVersionSettings)
        factory.createLibrarySession(shared, SingleModuleDataProvider(library), emptyList(), configuration.languageVersionSettings)
        return factory.createSourceSession(module, emptyList(), configuration)
    }

    private fun file(function: CfirNamedFunction): CfirFile = buildFile {
        source = source("lifecycle.cj"); moduleData = module; resolvePhase = CfirResolvePhase.IMPLICIT_TYPES
        origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
        name = "lifecycle.cj"; symbol = CfirFileSymbol()
        packageDirective = buildPackageDirective { packageFqName = FqName("lifecycle"); isMacroPackage = false }
        declarations += function
    }

    private fun functionSymbol(name: String, local: Boolean = false) = CfirNamedFunctionSymbol(
        if (local) CallableId(Name.identifier(name)) else CallableId(FqName("lifecycle"), Name.identifier(name)),
    )

    private fun function(symbol: CfirNamedFunctionSymbol, body: CfirBlock?, parameters: List<CfirValueParameter> = emptyList(),
        local: Boolean = false, phase: CfirResolvePhase = CfirResolvePhase.BODY_RESOLVE,
        resultType: ConeCangJieType = unitType): CfirNamedFunction = buildNamedFunction {
        source = source(symbol.name.asString()); moduleData = module; resolvePhase = phase
        origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
        isLocal = local; status = CfirDeclarationStatusImpl(if (local) Visibilities.Local else Visibilities.Public)
        name = symbol.name; this.symbol = symbol; returnTypeRef = typeRef(resultType)
        this.body = body; isMut = false; valueParameters += parameters
    }

    private fun variable(name: String, type: ConeCangJieType, initializer: CfirExpression?, mutable: Boolean,
        implicit: Boolean = false): CfirPatternBindingVariable = buildPatternBindingVariable {
        source = source(name); moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
        isLocal = true; status = CfirDeclarationStatusImpl(Visibilities.Local)
        this.name = Name.identifier(name); symbol = CfirPatternBindingSymbol(CallableId(this.name))
        returnTypeRef = typeRef(type); this.ownedInitializer = initializer; isVar = mutable; isTypeImplicit = implicit
    }

    private fun parameter(name: String, type: ConeCangJieType, owner: CfirNamedFunctionSymbol,
        default: CfirExpression? = null): CfirValueParameter = buildValueParameter {
        source = source(name); moduleData = module
        resolvePhase = if (default == null) CfirResolvePhase.BODY_RESOLVE else CfirResolvePhase.IMPLICIT_TYPES
        origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
        isLocal = true; isNamed = false; this.name = Name.identifier(name)
        symbol = CfirValueParameterSymbol(CallableId(this.name)); containingDeclarationSymbol = owner
        returnTypeRef = typeRef(type); defaultValue = default
    }

    private fun access(variable: CfirVariable): CfirNamedAccessExpression = buildNamedAccessExpression {
        source = source("read-${variable.symbol.name}"); coneTypeOrNull = assertIs<CfirResolvedTypeRef>(variable.returnTypeRef).coneType
        calleeReference = buildResolvedNamedReference { name = variable.symbol.name; resolvedSymbol = variable.symbol }
    }

    private fun assignment(variable: CfirVariable, value: CfirExpression,
        operation: CfirSourceOperator = CfirSourceOperator.ASSIGN): CfirAssignment = buildAssignment {
        source = source("write-${variable.symbol.name}"); coneTypeOrNull = unitType
        lValue = access(variable); rValue = value; this.operation = operation; isCompound = operation != CfirSourceOperator.ASSIGN
    }

    private fun member(name: String, type: ConeCangJieType, parameters: List<ConeCangJieType> = emptyList()): CfirNamedFunction {
        val symbol = CfirNamedFunctionSymbol(CallableId(StdlibClassIds.String, Name.identifier(name)))
        return function(symbol, null, parameters.mapIndexed { index, value -> parameter("arg$index", value, symbol) }, resultType = type)
    }

    private fun call(function: CfirNamedFunction, receiver: CfirExpression,
        values: List<CfirExpression> = emptyList()): CfirCallExpression = buildCallExpression {
        source = source("call-${function.name}"); coneTypeOrNull = assertIs<CfirResolvedTypeRef>(function.returnTypeRef).coneType
        base = buildNamedAccessExpression {
            source = source("callee-${function.name}"); explicitReceiver = receiver
            calleeReference = buildResolvedNamedReference { name = function.name; resolvedSymbol = function.symbol }
            coneTypeOrNull = ConeFunctionType(function.valueParameters.map { assertIs<CfirResolvedTypeRef>(it.returnTypeRef).coneType },
                assertIs<CfirResolvedTypeRef>(function.returnTypeRef).coneType)
        }
        arguments += values.map { expression -> buildFunctionArgument {
            source = expression.source; value = expression; coneTypeOrNull = expression.coneTypeOrNull
            isDefaultValue = false; withInout = false
        } }
        callKind = CfirSourceCallKind.DECLARED; hasSideEffect = true
    }

    private fun block(vararg statements: CfirStatement): CfirBlock = buildBlock {
        source = source("block"); this.statements += statements; coneTypeOrNull = unitType
    }
    private fun integer(value: Int): CfirLiteralExpression = buildLiteralExpression {
        source = source("int-$value"); kind = CfirLiteralKind.INT; this.value = value.toLong(); coneTypeOrNull = intType
    }
    private fun boolean(value: Boolean): CfirLiteralExpression = buildLiteralExpression {
        source = source("bool-$value"); kind = CfirLiteralKind.BOOLEAN; this.value = value; coneTypeOrNull = boolType
    }
    private fun unitLiteral(): CfirLiteralExpression = buildLiteralExpression {
        source = source("unit"); kind = CfirLiteralKind.UNIT; value = Unit; coneTypeOrNull = unitType
    }
    private fun typeRef(type: ConeCangJieType): CfirResolvedTypeRef = buildResolvedTypeRef { coneType = type }
    private fun source(name: String) = object : CjBinarySourceElement(name, null, name) {}
}
