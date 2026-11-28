package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.CfirLoopTarget
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirLoopJump
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildBreakExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildContinueExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildForInExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedAccessExpression
import org.cangnova.cangjie.cfir.patterns.builder.buildWildcardPattern
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraphBuilder
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ForInLoopIterationEnterNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ForInLoopIterationExitNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ForInLoopGuardExitNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.JumpNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.LoopConditionEnterNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.LoopConditionExitNode
import org.cangnova.cangjie.cfir.source.CfirSourceForInKind
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.source.CjBinarySourceElement
import kotlin.test.*

/**
 * 统一 For 的真实控制转移合同。
 *
 * Native 14276740ffec1262b81b6b5236b1d840af270b37 的 Node.h:2020–2033、
 * ExprWriter.cpp:598–617 确认 pattern/iterable/guard/body/kind；固定文档
 * 70281ea0ecb4adb86260ac233cf87fb4dbb23bc9 的 expression.md:475、498
 * 确认 where 假值与 continue 进入下一轮，break 退出循环。
 * Kotlin 3c7a5041f238102285764d370665f56f587f9dc4 的 ControlFlowGraphBuilder
 * 原 loop/Jump 目标接边职责由统一 For 消费；不借用 Kotlin 的 for 降低语义。
 *
 * 手工构造合法源语言结构对应的 CFIR，直接检查正式 CFG builder 的控制边；
 * 本测试不安装 DFA flow，也不将 CFG 完成冒充 BODY 或分析重建完成。
 */
class CfirUnifiedForInControlFlowTest {
    private val module = CfirSourceModuleData(
        name = Name.identifier("unified-for-cfg"),
        dependencies = emptyList(),
        refinementDependencies = emptyList(),
        targetPlatform = CangJiePlatforms.defaultCangJiePlatform,
        platform = CfirPlatform.DEFAULT,
    )

    @Test
    fun forGuardSkipsToNextIterationAndJumpsUseTheirActualLoopExit() {
        val elementType = ConePrimitiveType(PrimitiveTypeKind.INT64)
        val unitType = ConePrimitiveType(PrimitiveTypeKind.UNIT)
        val boolType = ConePrimitiveType(PrimitiveTypeKind.BOOLEAN)
        val iterableType = ConeVArrayType(elementType, 2)
        for (guardValue in listOf<Boolean?>(null, false, true)) {
            for (isBreak in listOf(false, true)) {
                val functionSymbol = CfirNamedFunctionSymbol(CallableId(Name.identifier("iterate")))
                val parameter = buildValueParameter {
                    source = source("items"); moduleData = module; resolvePhase = CfirResolvePhase.RAW_CFIR
                    origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = true; name = Name.identifier("items"); isNamed = false; isTypeImplicit = false
                    symbol = CfirValueParameterSymbol(CallableId(Name.identifier("items")))
                    containingDeclarationSymbol = functionSymbol
                    returnTypeRef = buildResolvedTypeRef { coneType = iterableType }
                }
                val iterable = buildNamedAccessExpression {
                    source = source("items-access"); coneTypeOrNull = iterableType
                    calleeReference = buildResolvedNamedReference { name = parameter.name; resolvedSymbol = parameter.symbol }
                }
                val guard = guardValue?.let { value -> buildLiteralExpression {
                    source = source("where"); kind = CfirLiteralKind.BOOLEAN; this.value = value; coneTypeOrNull = boolType
                } }
                val target = CfirLoopTarget()
                val jump: CfirLoopJump = if (isBreak) buildBreakExpression {
                    source = source("break"); coneTypeOrNull = ConePrimitiveType.NOTHING; this.target = target
                } else buildContinueExpression {
                    source = source("continue"); coneTypeOrNull = ConePrimitiveType.NOTHING; this.target = target
                }
                val pattern = buildWildcardPattern {
                    source = source("pattern"); semanticTypeRef = buildResolvedTypeRef { coneType = elementType }
                }
                val loopBody = buildBlock {
                    source = source("for-body"); coneTypeOrNull = ConePrimitiveType.NOTHING; statements += jump
                }
                val loop = buildForInExpression {
                    source = source("for"); coneTypeOrNull = unitType; this.pattern = pattern
                    this.iterable = iterable; patternGuard = guard; body = loopBody
                    iterationKind = CfirSourceForInKind.DIRECT
                }
                target.bind(loop)
                val body = buildBlock { coneTypeOrNull = unitType; statements += loop }
                val function = buildNamedFunction {
                    source = source("iterate"); moduleData = module; resolvePhase = CfirResolvePhase.RAW_CFIR
                    origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = false; status = CfirDeclarationStatusImpl(); name = Name.identifier("iterate")
                    symbol = functionSymbol; returnTypeRef = buildResolvedTypeRef { coneType = unitType }
                    valueParameters += parameter; this.body = body; isMut = false
                }

                val builder = ControlFlowGraphBuilder()
                val (_, functionEnter) = builder.enterFunction(function)
                builder.enterValueParameter(parameter)
                builder.exitValueParameter(parameter, defaultGraph = null)
                builder.enterBlock(body)
                val iterableNode = builder.exitQualifiedAccessExpression(iterable)
                val (loopEnter, iteration) = builder.enterForInLoop(loop)
                val iterationExit = builder.exitForInLoopIteration(loop)
                guard?.let { builder.exitLiteralExpression(it) }
                val bodyEnter = builder.exitForInLoopGuard(loop).second
                builder.enterBlock(loopBody)
                builder.enterJump(jump)
                val jumpNode = builder.exitJump(jump)
                builder.exitBlock(loopBody)
                builder.exitForInLoopBody(loop)
                val (repeatEntry, bodyExit, loopExit) = builder.exitForInLoop(loop)
                builder.exitBlock(body)
                val (functionExit, graph) = builder.exitFunction(function)

                assertSame(function, graph.declaration)
                assertSame(functionEnter, graph.enterNode)
                assertSame(functionExit, graph.exitNode)
                assertSame(iterableNode, loopEnter.previousNodes.single())
                assertEquals(1, graph.nodes.count { it.fir === iterable })
                assertFalse(iterableNode.previousNodes.any { it.edgeTo(iterableNode).kind.isBack })
                assertSame(loop, iteration.fir)
                assertSame(iteration, repeatEntry)
                assertSame(loop, iterationExit.fir)
                assertTrue(loopExit in iterationExit.followingNodes)
                assertFalse(iterationExit.edgeTo(loopExit).kind.isDead)
                assertFalse(loopExit.isDead, "零次迭代始终保留真实循环出口")
                assertEquals(guardValue == false, bodyEnter.isDead)
                assertTrue(graph.nodes.filterIsInstance<LoopConditionEnterNode>().isEmpty())
                assertEquals(1, graph.nodes.filterIsInstance<ForInLoopIterationEnterNode>().size)
                assertEquals(1, graph.nodes.filterIsInstance<ForInLoopIterationExitNode>().size)
                assertTrue(graph.nodes.filterIsInstance<LoopConditionExitNode>().isEmpty())
                val guardExits = graph.nodes.filterIsInstance<ForInLoopGuardExitNode>()
                assertEquals(if (guard == null) 0 else 1, guardExits.size)
                if (guard != null) {
                    val guardExit = guardExits.single { it.fir === guard }
                    assertSame(loop, guardExit.loop)
                    assertTrue(iteration in guardExit.followingNodes)
                    assertTrue(bodyEnter in guardExit.followingNodes)
                    assertFalse(loopExit in guardExit.followingNodes)
                    assertTrue(guardExit.edgeTo(iteration).kind.isBack)
                    assertEquals(guardValue == true, guardExit.edgeTo(iteration).kind.isDead)
                    assertEquals(guardValue == false, guardExit.edgeTo(bodyEnter).kind.isDead)
                }
                assertSame(jumpNode, graph.nodes.filterIsInstance<JumpNode>().single())
                assertSame(jump, jumpNode.fir)
                val destination = if (isBreak) loopExit else iteration
                assertTrue(destination in jumpNode.followingNodes)
                assertTrue(jumpNode in destination.previousNodes)
                assertEquals(!isBreak, jumpNode.edgeTo(destination).kind.isBack)
                assertEquals(guardValue == false, jumpNode.edgeTo(destination).kind.isDead)
                assertFalse((if (isBreak) iteration else loopExit) in jumpNode.followingNodes)
                assertTrue(iteration in bodyExit.followingNodes)
                assertTrue(bodyExit.edgeTo(iteration).kind.isBack)
                assertSame(target, jump.target)
                assertSame(loop, target.labeledElement)
                assertSame(pattern, loop.pattern)
                assertSame(iterable, loop.iterable)
                assertSame(guard, loop.patternGuard)
                assertSame(loopBody, loop.body)
                assertSame(jump, loopBody.statements.single())
                assertSame(function, functionSymbol.cfir)
                assertSame(functionSymbol, parameter.containingDeclarationSymbol)
                assertNull(builder.currentGraphOrNull)
                assertNull(builder.lastNodeOrNull)
                assertNull(function.controlFlowGraphReference)
            }
        }
    }

    private fun source(name: String) = object : CjBinarySourceElement(name, null, name) {}
}
