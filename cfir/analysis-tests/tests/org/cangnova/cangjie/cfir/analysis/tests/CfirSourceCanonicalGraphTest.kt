package org.cangnova.cangjie.cfir.analysis.tests


import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpMode
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.addClasspathRoot
import org.cangnova.cangjie.config.classpathRoots
import org.cangnova.cangjie.config.diagnosticsCollector
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.config.useLightTree
import org.cangnova.cangjie.frontend.pipeline.CfirFrontendPipelinePhase
import org.cangnova.cangjie.frontend.pipeline.ConfigurationPipelineArtifact
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.test.services.compilerConfigurationProvider
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCfirDiagnosticTestBase
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirBreakExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirForInExpression
import org.cangnova.cangjie.cfir.expressions.CfirJump
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirLoopJump
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysis
import org.cangnova.cangjie.cfir.resolve.source.CfirSourceGraphProducer
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.source.CfirSourceCallKind
import org.cangnova.cangjie.cfir.source.CfirSourceExpressionIdentity
import org.cangnova.cangjie.cfir.source.CfirSourceExpressionPayload as SourcePayload
import org.cangnova.cangjie.cfir.source.CfirSourceForInKind
import org.cangnova.cangjie.cfir.source.CfirSourceGraph
import org.cangnova.cangjie.cfir.source.CfirSourceOperator
import org.cangnova.cangjie.cfir.source.CfirSourcePattern
import org.cangnova.cangjie.cfir.source.CfirSourcePatternKind
import org.cangnova.cangjie.cfir.source.CfirSourceReferenceTarget
import org.cangnova.cangjie.cfir.source.CfirSourceSlot
import org.cangnova.cangjie.cfir.source.expressionChildren
import org.cangnova.cangjie.cfir.source.patterns
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.isBoolean
import org.cangnova.cangjie.cfir.types.isInt64
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.test.CfirParser
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder
import org.cangnova.cangjie.test.config.TestPhase
import org.cangnova.cangjie.test.config.TestPhaseDirectives.RUN_PIPELINE_TILL
import org.cangnova.cangjie.test.config.firHandlersStep
import org.cangnova.cangjie.test.directives.CangjieTestDirectives.WITH_STDLIB
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives.CJMP_MODE
import org.cangnova.cangjie.test.directives.LanguageSettingsDirectives.LANGUAGE_VERSION
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.frontend.CfirAnalysisHandler
import org.cangnova.cangjie.test.frontend.CfirOutputArtifact
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.globalMetadataInfoHandler
import org.cangnova.cangjie.test.services.sourceFileProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Collections
import java.util.HexFormat
import java.util.IdentityHashMap

/** PSI：真实 normal BODY 后的独立 SOURCE 产品验收。 */
class CfirSourceCanonicalGraphTest : AbstractCfirSourceCanonicalGraphTest(CfirParser.Psi)

/** LightTree 与 PSI 使用同一批官方合法源码和同一组语义断言。 */
class CfirSourceCanonicalGraphLightTreeTest : AbstractCfirSourceCanonicalGraphTest(CfirParser.LightTree)

/**
 * 语义仅来自官方 v113 Git 对象 14276740ffec1262b81b6b5236b1d840af270b37：
 * src/Sema/Desugar/AfterTypeCheck/{Package,ForInExpr,IsExpr,AsExpr,TryExpr,Create}.cpp，
 * src/Sema/Desugar/DesugarBeforeTypeCheck.cpp 和 src/Modules/ASTSerialization/ExprWriter.cpp。
 * 具体见 build/source-expression-codec-audit-v113/source-canonical-codec-native-continuation-20261117.md
 * 及原 source-expression-export/FIXTURE.md；四份输入的 SDK 1.1.3 编译均成功。
 *
 * 框架对应 Kotlin 3c7a5041f238102285764d370665f56f587f9dc4 的
 * compiler/jvm.tests/testFixtures/.../runners/AbstractFirDiagnosticTest.kt 与
 * compiler/tests-common-new/testFixtures/.../frontend/fir/handlers/FirAnalysisHandler.kt。
 * 保留正常诊断 handler；此 handler 只消费同一正常 frontend 的完成文件，不建立孤立 session。
 */
abstract class AbstractCfirSourceCanonicalGraphTest(parser: CfirParser) : AbstractCfirDiagnosticTestBase(parser) {
    private lateinit var fixture: CanonicalFixture

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            LANGUAGE_VERSION with "1.1.3"
            CJMP_MODE with CfirCjmpMode.NONE
            RUN_PIPELINE_TILL with TestPhase.FRONTEND
            +WITH_STDLIB
        }
        builder.firHandlersStep {
            useHandlers({ services: TestServices -> SourceCanonicalGraphHandler(services, parser, fixture) })
        }
    }

    @Test
    fun declarationsCalls() = runCase(CanonicalFixture.DECLARATIONS_CALLS)

    @Test
    fun controlFlow() = runCase(CanonicalFixture.CONTROL_FLOW)

    @Test
    fun operatorsConversions() = runCase(CanonicalFixture.OPERATORS_CONVERSIONS)

    @Test
    fun exceptionsResources() = runCase(CanonicalFixture.EXCEPTIONS_RESOURCES)

    private fun runCase(case: CanonicalFixture) {
        fixture = case
        val path = Path.of("cfir/analysis-tests/testData/diagnostics2/common-specific/source-expression-export/${case.fileName}.cj")
        val digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)))
        assertEquals(case.sha256, digest, "必须使用 FIXTURE.md 中官方成功编译的原输入")
        runTest(path.toString())
    }
}

private enum class CanonicalFixture(val fileName: String, val owner: String, val sha256: String) {
    DECLARATIONS_CALLS("declarationsCalls", "DeclarationsCalls", "bf8f88e2398ba1f562f848ab0814f68cef90c7803b96c3b1bc6394d145ad9002"),
    CONTROL_FLOW("controlFlow", "ControlFlow", "1d83b1b49f11f98adcb5c5a9e025dc1993ae396436633983624b5960c811930d"),
    OPERATORS_CONVERSIONS("operatorsConversions", "OperatorsConversions", "5b237eb7082ec4d061f9cff7c341dfc8c39ee0af81018db489f0d5be0d2acb98"),
    EXCEPTIONS_RESOURCES("exceptionsResources", "ExceptionsResources", "4a017a550e5cd25d828c2750872bf2c1053746bd5e064a0252f7de4cc49d4858"),
}

/** 生产调用位于 normal BODY 之后；没有 CJO 输出配置、writer 或产品注入。 */
@OptIn(CompilerConfiguration.Internals::class)
private class SourceCanonicalGraphHandler(testServices: TestServices, private val parser: CfirParser, private val fixture: CanonicalFixture) : CfirAnalysisHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer> = emptyList()
    private var checkedModules = 0

    override fun processModule(module: TestModule, info: CfirOutputArtifact) {
        assertEquals(0, checkedModules)
        val part = info.partsForDependsOnModules.single { it.module === module }
        val input = part.firFilesByTestFile.keys.single { !it.isAdditional }
        assertTrue(testServices.globalMetadataInfoHandler.getExistingMetaInfosForFile(input).isEmpty(), "官方正例没有错误或警告")
        assertEquals(Files.readString(input.originalFile.toPath(), Charsets.UTF_8), testServices.sourceFileProvider.getContentOfSourceFile(input))
        // 与正式SOURCE→CJO验收共用公开frontend工厂；测试phase-checking resolver在handler
        // 结束后禁止新的scope请求，不能伪造currentPhase或放宽它的解析锁/阶段合同。
        val disposable = Disposer.newDisposable("source-canonical-capture")
        try {
        val moduleConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val diagnostics = DiagnosticsCollectorImpl()
        val configuration = CompilerConfiguration().apply {
            moduleName = module.name
            languageVersionSettings = moduleConfiguration.languageVersionSettings
            useLightTree = parser == CfirParser.LightTree
            cjmpMode = module.directives[CJMP_MODE].single()
            diagnosticsCollector = diagnostics
            messageCollector = MessageCollector.NONE
            moduleConfiguration.classpathRoots.forEach { addClasspathRoot(it.path) }
            addCangJieSourceRoot(input.originalFile.toPath().toAbsolutePath().normalize().toString())
        }
        val artifact = checkNotNull(CfirFrontendPipelinePhase.executePhase(ConfigurationPipelineArtifact(configuration, disposable)))
        assertTrue(diagnostics.diagnostics.isEmpty(), diagnostics.diagnostics.joinToString("\n") { "${it.factoryName}: ${it.renderMessage()}" })
        val output = artifact.frontendOutput.outputs.single()
        assertNotSame(part.session, output.session)
        val file = output.fir.single()
        val functions = normalElements(file).filterIsInstance<CfirFunction>().filter { it.body != null }
        assertTrue(functions.isNotEmpty())
        functions.forEach { function ->
            assertEquals(CfirResolvePhase.BODY_RESOLVE, function.resolvePhase, "producer 只能消费 normal BODY 完成的声明")
            val cfg = function.controlFlowGraphReference as CfirControlFlowGraphReferenceImpl
            assertSame(function, cfg.controlFlowGraph.declaration)
        }
        val unchanged = NormalSourceSnapshot(file)
        val graph = CfirSourceGraphProducer.produce(listOf(file))
        unchanged.assertUnchanged()
        val export = CfirExportAnalysis.analyze(listOf(file), canonicalSources = listOf(graph))
        assertTrue(export.hasCanonicalSource)
        assertSame(graph, export.canonicalSourceGraphs().single())
        graph.generatedDeclarations.forEach { (declaration, context) ->
            assertTrue(export.isSourceDeclaration(declaration), "SOURCE生成声明必须进入同代来源域")
            assertSame(context.owner, export.ownerOf(declaration))
            assertSame(context.file, export.sourceFileOf(declaration))
            assertEquals((declaration as CfirCallableDeclaration).returnTypeRef.coneType, export.declarationTypeOf(declaration))
            assertNull(export.nativeOuterOf(declaration), "SOURCE局部变量没有native outerDecl")
        }
        graph.expressions.values.forEach { record -> export.typeElement(record.type, record.context) }
        export.validateSourceUnchanged()
        CanonicalGraphAssertions(file, graph, output.session).assertFixture(fixture)
        unchanged.assertUnchanged()
        checkedModules++
        } finally {
            Disposer.dispose(disposable)
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        assertEquals(1, checkedModules, "每个官方输入必须实际进入公开 producer 并完成语义断言")
    }
}

/** 只读 SOURCE 拥有闭包；回边单独验身份，不把 ref target 的普通方法正文当成导出选择。 */
private class CanonicalGraphAssertions(private val file: CfirFile, private val graph: CfirSourceGraph, private val session: CfirSession) {
    private val normal = normalElements(file)

    fun assertFixture(fixture: CanonicalFixture) {
        val owner = file.declarations.filterIsInstance<CfirInterface>().single { it.name.asString() == fixture.owner }
        assertTrue(owner.declarations.filterIsInstance<CfirNamedFunction>().all { it.status.isDefault })
        assertTrue(graph.expressions.isNotEmpty())
        graph.expressions.forEach { (identity, record) ->
            assertSame(record, graph.expression(identity), "同一身份只能查询到同一完成记录")
            assertFalse(record.type is ConeErrorType)
            record.payload.expressionChildren().forEach { graph.expression(it) }
            record.mapExpression?.let { graph.expression(it) }
            when (val payload = record.payload) {
                is SourcePayload.Return -> {
                    assertTrue(record.type.isNothing)
                    assertTrue(graph.functionBodies.containsKey(payload.function))
                }
                is SourcePayload.Jump -> {
                    assertTrue(record.type.isNothing)
                    assertTrue(graph.expression(payload.loop).payload.let { it is SourcePayload.Loop || it is SourcePayload.ForIn })
                }
                is SourcePayload.Call -> assertAppliedCall(identity)
                is SourcePayload.Try -> assertEquals(payload.patterns.size, payload.catches.size)
                else -> Unit
            }
        }
        graph.generatedDeclarations.forEach { (declaration, context) ->
            assertTrue(declaration.symbol.isBound)
            assertSame(declaration, declaration.symbol.cfir)
            assertTrue(normal.none { it === declaration }, "合成声明不能冒用 normal 声明身份")
            assertSame(file, context.file)
        }
        when (fixture) {
            CanonicalFixture.DECLARATIONS_CALLS -> declarationsCalls(owner)
            CanonicalFixture.CONTROL_FLOW -> controlFlow(owner)
            CanonicalFixture.OPERATORS_CONVERSIONS -> operatorsConversions(owner)
            CanonicalFixture.EXCEPTIONS_RESOURCES -> exceptionsResources(owner)
        }
    }

    /** v113 Package.cpp:25–60、75–134；ExprWriter.cpp:113–122、510–548。 */
    private fun declarationsCalls(owner: CfirInterface) {
        val evaluate = method(owner, "evaluate")
        val slots = block(body(evaluate)).statements
        assertEquals(6, slots.size)
        val step = variable(evaluate, "step")
        val local = variable(evaluate, "local")
        assertTrue(local.isVar)
        assertFalse(step.isVar)
        val nested = normalElements(evaluate).filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "nested" }
        val transform = variable(evaluate, "transform")
        assertEquals(listOf(step, local, nested, transform), slots.take(4).map { (it as CfirSourceSlot.Declaration).declaration })
        slots.take(4).zip(listOf(step, local, nested, transform)).forEach { (slot, declaration) ->
            assertSame(declaration, (slot as CfirSourceSlot.Declaration).declaration)
        }
        val privateStep = file.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "privateStep" }
        reference(initializer(step), privateStep)
        reference(initializer(local), evaluate.valueParameters[1])
        assertSame(graph.expressionOf(step.initializer!!), initializer(step))
        assertSame(graph.expressionOf(local.initializer!!), initializer(local))

        val lambda = payload<SourcePayload.Lambda>(initializer(transform))
        val originalLambda = (transform.initializer as CfirAnonymousFunctionExpression).anonymousFunction
        assertSame(originalLambda, lambda.function.owner)
        assertEquals(originalLambda.valueParameters.size, lambda.function.parameters.size)
        originalLambda.valueParameters.zip(lambda.function.parameters).forEach { (original, saved) -> assertSame(original, saved) }
        assertSame(body(originalLambda), lambda.function.body)
        assertEquals(originalLambda.returnTypeRef.coneType,
            assertInstanceOf(org.cangnova.cangjie.cfir.source.CfirSourceCallableSignature.Resolved::class.java,
                lambda.function.signature).returnType)
        val lambdaReturn = returned(checkNotNull(lambda.function.body), originalLambda)
        val nestedCall = payload<SourcePayload.Call>(lambdaReturn.value)
        assertEquals(CfirSourceCallKind.DECLARED, nestedCall.kind)
        reference(nestedCall.base, nested)
        reference(argument(nestedCall.arguments.single()), originalLambda.valueParameters.single())
        val nestedReturn = returned(body(nested), nested)
        val nestedResult = payload<SourcePayload.Binary>(nestedReturn.value)
        reference(nestedResult.left, nested.valueParameters.single())
        reference(nestedResult.right, evaluate.valueParameters[1])

        val assignment = payload<SourcePayload.Assignment>(expressionSlot(slots[4]))
        assertEquals(CfirSourceOperator.ASSIGN, assignment.operator)
        assertFalse(assignment.isCompound)
        reference(assignment.left, local)
        val stepCall = payload<SourcePayload.Call>(assignment.right)
        assertEquals(CfirSourceCallKind.STRUCT_CREATION, stepCall.kind, "v113 函数值调用的 CallStructCreation")
        reference(stepCall.base, step)
        reference(argument(stepCall.arguments.single()), local)

        val returned = returned(body(evaluate), evaluate)
        val originalReturn = evaluate.body!!.statements.last() as CfirReturnExpression
        assertSame(graph.expressionOf(originalReturn), expressionSlot(slots.last()), "直接 Return 不重复包装")
        assertSame(graph.expressionOf(originalReturn.result), returned.value)
        val memberCall = payload<SourcePayload.Call>(returned.value)
        val member = payload<SourcePayload.Member>(memberCall.base)
        val receiverClass = file.declarations.filterIsInstance<CfirClass>().single { it.name.asString() == "SourceReceiver" }
        val add = receiverClass.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "add" }
        assertSame(add, assertInstanceOf(CfirSourceReferenceTarget.Declaration::class.java, member.reference.target).declaration)
        reference(member.receiver, evaluate.valueParameters[0])
        val transformCall = payload<SourcePayload.Call>(argument(memberCall.arguments.single()))
        assertEquals(CfirSourceCallKind.STRUCT_CREATION, transformCall.kind)
        reference(transformCall.base, transform)
        reference(argument(transformCall.arguments.single()), local)

        val implicit = method(owner, "implicitValue")
        val copy = variable(implicit, "copy")
        val implicitSlots = block(body(implicit)).statements
        assertSame(copy, (implicitSlots.first() as CfirSourceSlot.Declaration).declaration)
        val implicitReturn = returned(body(implicit), implicit)
        assertTrue(graph.expression(body(implicit)).type.isNothing)
        assertSame(graph.expressionOf(implicit.body!!.statements.last() as CfirExpression), implicitReturn.value, "值尾保留原表达式身份")
        reference(implicitReturn.value, copy)
        assertEquals(implicit.returnTypeRef.coneType, graph.expression(implicitReturn.value).type)
        val explicit = method(owner, "explicitUnit")
        assertEquals(1, block(body(explicit)).statements.size)
        val unitReturn = returned(body(explicit), explicit)
        assertTrue(graph.expression(unitReturn.value).type.isUnit)
        assertEquals(CfirLiteralKind.UNIT, payload<SourcePayload.Literal>(unitReturn.value).kind)
        assertSame(graph.expressionOf(explicit.body!!.statements.single() as CfirReturnExpression), expressionSlot(block(body(explicit)).statements.single()))
    }

    /** v113 ForInExpr.cpp:23–94、133–150、462–507；DesugarBeforeTypeCheck.cpp:284–310。 */
    private fun controlFlow(owner: CfirInterface) {
        val walk = method(owner, "walk")
        val slots = block(body(walk)).statements
        assertEquals(6, slots.size)
        val total = variable(walk, "total")
        val index = variable(walk, "index")
        assertSame(total, (slots[0] as CfirSourceSlot.Declaration).declaration)
        assertSame(index, (slots[1] as CfirSourceSlot.Declaration).declaration)
        val whileId = expressionSlot(slots[2])
        val whileLoop = payload<SourcePayload.Loop>(whileId)
        assertFalse(whileLoop.isDoWhile)
        assertTrue(graph.expression(whileLoop.condition).type.isBoolean)
        val whileSlots = block(whileLoop.body).statements
        nativeAssign(expressionSlot(whileSlots.first()), index, CfirSourceOperator.ADD_ASSIGN, "1")
        val compounds = owned(whileLoop.body).filter { graph.expression(it).payload is SourcePayload.Assignment }
        val totalAdd = compounds.single { payload<SourcePayload.Assignment>(it).operator == CfirSourceOperator.ADD_ASSIGN && target(payload<SourcePayload.Assignment>(it).left) === total }
        reference(payload<SourcePayload.Assignment>(totalAdd).right, index)
        assertTrue(payload<SourcePayload.Assignment>(totalAdd).isCompound)
        assertTrue(graph.expression(totalAdd).type.isUnit)
        val doLoop = payload<SourcePayload.Loop>(expressionSlot(slots[3]))
        assertTrue(doLoop.isDoWhile)
        assertTrue(graph.expression(doLoop.condition).type.isBoolean)
        nativeAssign(expressionSlot(block(doLoop.body).statements.single()), total, CfirSourceOperator.SUBTRACT_ASSIGN, "1")

        val forId = expressionSlot(slots[4])
        val forLoop = payload<SourcePayload.ForIn>(forId)
        val original = normalElements(walk).filterIsInstance<CfirForInExpression>().single()
        assertSame(graph.expressionOf(original), forId)
        assertEquals(CfirSourceForInKind.RANGE, forLoop.kind)
        assertNull(forLoop.guard)
        assertSame(graph.expressionOf(original.body), forLoop.body)
        val mixed = block(forLoop.iterable)
        assertFalse(mixed.isUnsafe)
        assertTrue(graph.expression(forLoop.iterable).type.isUnit)
        assertEquals(4, mixed.statements.size, "原生顺序 iter d / stop d / update e / condition e")
        val iter = (mixed.statements[0] as CfirSourceSlot.Declaration).declaration as CfirVariable
        val stop = (mixed.statements[1] as CfirSourceSlot.Declaration).declaration as CfirVariable
        assertNotSame(iter, stop)
        generated(iter, walk)
        generated(stop, walk)
        assertTrue(iter.isVar)
        assertFalse(stop.isVar)
        assertTrue(iter.returnTypeRef.coneType.isInt64)
        assertEquals(iter.returnTypeRef.coneType, stop.returnTypeRef.coneType)
        assertEquals("0", payload<SourcePayload.Literal>(initializer(iter)).text)
        reference(initializer(stop), walk.valueParameters.single())
        nativeAssign(expressionSlot(mixed.statements[2]), iter, CfirSourceOperator.ADD_ASSIGN, "1")
        val condition = payload<SourcePayload.Binary>(expressionSlot(mixed.statements[3]))
        assertEquals(CfirSourceOperator.LESS, condition.operator, "0..limit 是半开递增区间")
        assertTrue(graph.expression(expressionSlot(mixed.statements[3])).type.isBoolean)
        reference(condition.left, iter)
        reference(condition.right, stop)
        val element = variable(walk, "element")
        assertSame(element, binding(forLoop.pattern))
        reference(initializer(element), iter, "用户模式声明保持原身份，SOURCE initializer 指同一 iter")
        assertNotSame(element, iter)
        val whileJumps = owned(whileLoop.body).filter { graph.expression(it).payload is SourcePayload.Jump }
        val forJumps = owned(forLoop.body).filter { graph.expression(it).payload is SourcePayload.Jump }
        for ((loopId, jumps) in listOf(whileId to whileJumps, forId to forJumps)) {
            assertEquals(2, jumps.size)
            assertEquals(setOf(false, true), jumps.map { payload<SourcePayload.Jump>(it).isBreak }.toSet())
            jumps.forEach { assertSame(loopId, payload<SourcePayload.Jump>(it).loop) }
        }
        normalElements(walk).filterIsInstance<CfirLoopJump>().forEach { jump ->
            val sourceJump = payload<SourcePayload.Jump>(graph.expressionOf(jump))
            assertSame(graph.expressionOf(jump.target.labeledElement), sourceJump.loop)
            assertEquals(jump is CfirBreakExpression, sourceJump.isBreak)
        }
        val choose = method(owner, "choose")
        val chosen = returned(body(choose), choose)
        assertSame(graph.expressionOf(choose.body!!.statements.last() as CfirExpression), chosen.value)
        val branches = payload<SourcePayload.Conditional>(chosen.value)
        assertTrue(graph.expression(body(choose)).type.isNothing)
        assertTrue(graph.expression(chosen.value).type.isInt64)
        assertEquals("1", payload<SourcePayload.Literal>(onlyExpression(branches.thenBranch)).text)
        assertEquals("2", payload<SourcePayload.Literal>(onlyExpression(checkNotNull(branches.elseBranch))).text)
        assertTrue(owned(chosen.value).none { graph.expression(it).payload is SourcePayload.Return }, "普通 if Block 不套函数尾规则")
    }

    /** v113 IsExpr.cpp:28–54、AsExpr.cpp:19–97、Create.cpp:16–23；固定 inspect flags=false/true。 */
    private fun operatorsConversions(owner: CfirInterface) {
        val calculate = method(owner, "calculate")
        val calculation = owned(body(calculate))
        val accumulator = variable(calculate, "accumulator")
        val assignments = calculation.filter { graph.expression(it).payload is SourcePayload.Assignment }
        val ordinary = assignments.single { payload<SourcePayload.Assignment>(it).operator == CfirSourceOperator.ASSIGN }
        assertFalse(payload<SourcePayload.Assignment>(ordinary).isCompound)
        reference(payload<SourcePayload.Assignment>(ordinary).left, accumulator)
        val compound = assignments.single { payload<SourcePayload.Assignment>(it).operator == CfirSourceOperator.ADD_ASSIGN }
        nativeAssign(compound, accumulator, CfirSourceOperator.ADD_ASSIGN, "2")
        assertTrue(calculation.any { graph.expression(it).payload is SourcePayload.Subscript })
        calculation.map { graph.expression(it).payload }.filterIsInstance<SourcePayload.Subscript>().forEach { assertTrue(it.isTupleAccess) }
        assertEquals(2, calculation.count { graph.expression(it).payload is SourcePayload.TypeConversion })

        val inspect = method(owner, "inspect")
        val inspectReturn = returned(body(inspect), inspect)
        val tuple = payload<SourcePayload.Tuple>(inspectReturn.value)
        assertEquals(2, tuple.elements.size)
        assertNotSame(tuple.elements[0], tuple.elements[1], "is/as 是两个独立拥有节点")
        assertNotSame(payload<SourcePayload.Match>(tuple.elements[0]).selector, payload<SourcePayload.Match>(tuple.elements[1]).selector)
        val leaf = file.declarations.filterIsInstance<CfirClass>().single { it.name.asString() == "ConversionLeaf" }
        for ((position, matchId) in tuple.elements.withIndex()) {
            val match = payload<SourcePayload.Match>(matchId)
            val selector = checkNotNull(match.selector)
            reference(selector, inspect.valueParameters.single())
            assertEquals(2, match.cases.size)
            val success = payload<SourcePayload.MatchCase>(match.cases[0])
            val failure = payload<SourcePayload.MatchCase>(match.cases[1])
            match.cases.forEach { assertEquals(graph.expression(matchId).type, graph.expression(it).type) }
            assertNull(success.guard)
            assertNull(failure.guard)
            val typed = success.patterns.single()
            assertEquals(CfirSourcePatternKind.TYPE, typed.kind)
            assertTrue(typed.slots.isEmpty())
            assertFalse(typed.matchBeforeRuntime)
            assertTrue(typed.needRuntimeTypeCheck)
            val inner = typed.patterns.single()
            assertEquals(typed.types.single(), inner.types.single())
            assertEquals(leaf.symbol.toLookupTag(), (typed.types.single() as ConeClassLikeType).lookupTag)
            assertEquals(CfirSourcePatternKind.WILDCARD, failure.patterns.single().kind)
            assertEquals(graph.expression(selector).type, failure.patterns.single().types.single())
            assertTrue(failure.patterns.single().slots.isEmpty())
            assertTrue(failure.patterns.single().patterns.isEmpty())
            listOf(success.body, failure.body).forEach { caseBody ->
                assertEquals(graph.expression(matchId).type, graph.expression(caseBody).type)
                assertTrue(owned(caseBody).none { graph.expression(it).payload is SourcePayload.Return })
            }
            if (position == 0) {
                assertTrue(graph.expression(matchId).type.isBoolean)
                assertEquals(CfirSourcePatternKind.WILDCARD, inner.kind)
                assertTrue(inner.slots.isEmpty())
                assertTrue(inner.patterns.isEmpty())
                assertEquals("true", payload<SourcePayload.Literal>(onlyExpression(success.body)).text)
                assertEquals("false", payload<SourcePayload.Literal>(onlyExpression(failure.body)).text)
            } else {
                val bound = binding(inner)
                generated(bound, inspect)
                assertEquals(typed.types.single(), bound.returnTypeRef.coneType)
                some(onlyExpression(success.body), bound, graph.expression(matchId).type)
                val noneId = onlyExpression(failure.body)
                val none = payload<SourcePayload.Reference>(noneId)
                val noneTarget = assertInstanceOf(CfirSourceReferenceTarget.Declaration::class.java, none.target).declaration
                assertEquals("None", (noneTarget as CfirCallableDeclaration).symbol.name.asString())
                assertSame(coreConstructor("None"), noneTarget)
                assertEquals(listOf(typed.types.single()), none.instantiatedTypes)
                assertEquals(graph.expression(matchId).type, graph.expression(noneId).type)
                assertNotSame(inspect.valueParameters.single(), bound)
                val option = graph.expression(matchId).type as ConeEnumType
                assertEquals("Option", option.lookupTag.classId.shortClassName.asString())
                assertEquals(typed.types.single(), option.typeArguments.single().type)
            }
            assertNotSame(match.cases[0], match.cases[1])
            assertEquals(1, owned(matchId).count { graph.expression(it).payload is SourcePayload.Reference && target(it) === inspect.valueParameters.single() }, "selector 只求值一次")
        }
        val select = returned(body(method(owner, "select")), method(owner, "select"))
        val selected = payload<SourcePayload.Match>(select.value)
        assertNotNull(selected.selector)
        assertEquals(2, selected.cases.size)
        selected.cases.forEach { assertNull(payload<SourcePayload.MatchCase>(it).guard) }
        val predicate = method(owner, "predicate")
        val withoutSelector = payload<SourcePayload.Match>(returned(body(predicate), predicate).value)
        assertNull(withoutSelector.selector)
        assertEquals(2, withoutSelector.cases.size)
        withoutSelector.cases.forEach { payload<SourcePayload.MatchCaseOther>(it) }
    }

    /** v113 TryExpr.cpp:63–81、112–224、266–306；普通 Try 最后可空槽见 ExprWriter.cpp:444–474。 */
    private fun exceptionsResources(owner: CfirInterface) {
        val recover = method(owner, "recover")
        val ordinary = payload<SourcePayload.Try>(expressionSlot(block(body(recover)).statements[1]))
        assertTrue(ordinary.resources.isEmpty())
        assertEquals(1, ordinary.catches.size)
        assertNotNull(ordinary.finallyBlock)
        assertEquals(CfirSourcePatternKind.EXCEPTION_TYPE, ordinary.patterns.single().kind)
        assertSame(variable(recover, "error"), binding(ordinary.patterns.single().patterns.single()))
        val without = method(owner, "withoutFinally")
        val withoutReturn = returned(body(without), without)
        val withoutTry = payload<SourcePayload.Try>(withoutReturn.value)
        assertEquals(1, withoutTry.catches.size)
        assertNull(withoutTry.finallyBlock, "普通 try/catch 的末槽是真实缺失，不能造空 finally")
        assertTrue(withoutTry.resources.isEmpty())
        assertEquals("4", payload<SourcePayload.Literal>(onlyExpression(withoutTry.body)).text)
        assertEquals("5", payload<SourcePayload.Literal>(onlyExpression(withoutTry.catches.single())).text)

        val with = method(owner, "withResources")
        val originalTry = normalElements(with).filterIsInstance<CfirTryExpression>().single()
        val outerId = graph.expressionOf(originalTry)
        val outer = payload<SourcePayload.Try>(outerId)
        assertTrue(graph.expression(outerId).type.isUnit)
        assertTrue(outer.resources.isEmpty())
        assertSame(graph.expressionOf(originalTry.catches.single().body), outer.catches.single())
        assertSame(graph.expressionOf(originalTry.finallyBlock!!), outer.finallyBlock)
        val first = originalTry.resources[0]
        val second = originalTry.resources[1]
        assertEquals(listOf("first", "second"), listOf(first, second).map { it.name.asString() })
        val firstLayer = resourceLayer(outer.body, first, with)
        val secondLayer = resourceLayer(firstLayer.second.body, second, with)
        assertNotSame(firstLayer.first, secondLayer.first, "每层独立 freshExc，不能按同名合并")
        val resourceClass = file.declarations.filterIsInstance<CfirClass>().single { it.name.asString() == "SourceResource" }
        for ((resource, layer) in listOf(first to firstLayer, second to secondLayer)) {
            assertSame(graph.expressionOf(resource.initializer!!), initializer(resource), "资源 initializer 只进入 SOURCE 一次")
            val resourceCall = payload<SourcePayload.Call>(initializer(resource))
            assertEquals(CfirSourceCallKind.OBJECT_CREATION, resourceCall.kind)
            val initializerTarget = target(resourceCall.base)
            assertTrue(resourceClass.declarations.any { it === initializerTarget }, "必须保留真实 constructor 身份")
            assertResourceFinally(layer.second, layer.first, resource, resourceClass)
        }
        val all = owned(body(with))
        val tries = all.filter { graph.expression(it).payload is SourcePayload.Try }
        assertEquals(5, tries.size, "本函数为外 Try + 每资源保护 Try/close Try；整个官方文件共七个")
        tries.forEach { assertTrue(payload<SourcePayload.Try>(it).resources.isEmpty()) }
        assertTrue(owned(secondLayer.second.body).none { graph.expression(it).payload is SourcePayload.Return }, "资源正文只需 Unit，不生成函数 Return")
        assertTrue(graph.expression(secondLayer.second.body).type.isUnit)

        // 原 fixture 的精确 Unit 注解 + 赋值尾：选择 InsertUnit，不能把 Block 改成 Nothing。
        // 此 normal 方法的 SOURCE 产品存在，不等于 CJO 选择应导出其普通成员正文。
        val close = resourceClass.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "close" }
        val closeBody = body(close)
        val closeSlots = block(closeBody).statements
        assertEquals(2, closeSlots.size)
        assertTrue(graph.expression(closeBody).type.isUnit)
        payload<SourcePayload.Assignment>(expressionSlot(closeSlots[0]))
        val closeReturn = returned(closeBody, close)
        assertTrue(graph.expression(closeReturn.value).type.isUnit)
        assertEquals(CfirLiteralKind.UNIT, payload<SourcePayload.Literal>(closeReturn.value).kind)
        assertTrue(close.returnTypeRef.coneType.isUnit)
    }

    /** 每层顺序 freshExc / 原资源声明 / 保护 Try；catch 保存的 Some 必须借用该 catch binding。 */
    private fun resourceLayer(blockId: CfirSourceExpressionIdentity, resource: CfirVariable, owner: CfirFunction): Pair<CfirVariable, SourcePayload.Try> {
        val slots = block(blockId).statements
        assertEquals(3, slots.size)
        val fresh = (slots[0] as CfirSourceSlot.Declaration).declaration as CfirVariable
        generated(fresh, owner)
        assertTrue(fresh.isVar)
        assertSame(resource, (slots[1] as CfirSourceSlot.Declaration).declaration)
        val noneId = initializer(fresh)
        val none = payload<SourcePayload.Reference>(noneId)
        val noneTarget = assertInstanceOf(CfirSourceReferenceTarget.Declaration::class.java, none.target).declaration
        assertEquals("None", (noneTarget as CfirCallableDeclaration).symbol.name.asString())
        assertSame(coreConstructor("None"), noneTarget)
        assertEquals(fresh.returnTypeRef.coneType, graph.expression(noneId).type)
        val option = fresh.returnTypeRef.coneType as ConeEnumType
        assertEquals(StdlibClassIds.Option, option.lookupTag.classId)
        val exception = option.typeArguments.single().type as ConeClassLikeType
        assertEquals(StdlibClassIds.Exception, exception.lookupTag.classId)
        assertEquals(listOf(exception), none.instantiatedTypes)
        val protected = payload<SourcePayload.Try>(expressionSlot(slots[2]))
        assertEquals(1, protected.catches.size)
        assertEquals(CfirSourcePatternKind.EXCEPTION_TYPE, protected.patterns.single().kind)
        val caught = binding(protected.patterns.single().patterns.single())
        generated(caught, owner)
        val saveId = onlyExpression(protected.catches.single())
        val save = payload<SourcePayload.Assignment>(saveId)
        assertEquals(CfirSourceOperator.ASSIGN, save.operator)
        assertFalse(save.isCompound)
        reference(save.left, fresh)
        some(save.right, caught, fresh.returnTypeRef.coneType)
        assertTrue(graph.expression(saveId).type.isUnit)
        return fresh to protected
    }

    /** close 失败：Some 分支抛 saved，None 分支抛 closeExc；两个 match 的 saved 是不同声明。 */
    private fun assertResourceFinally(protected: SourcePayload.Try, fresh: CfirVariable, resource: CfirVariable, resourceClass: CfirClass) {
        val finallyId = checkNotNull(protected.finallyBlock)
        val finalSlots = block(finallyId).statements
        assertEquals(2, finalSlots.size)
        val closeTry = payload<SourcePayload.Try>(expressionSlot(finalSlots[0]))
        assertNull(closeTry.finallyBlock)
        val conditional = payload<SourcePayload.Conditional>(onlyExpression(closeTry.body))
        assertNull(conditional.elseBranch)
        val not = payload<SourcePayload.Unary>(conditional.condition)
        assertEquals(CfirSourceOperator.NOT, not.operator)
        assertResourceCall(not.operand, resource, resourceClass, "isClosed")
        assertResourceCall(onlyExpression(conditional.thenBranch), resource, resourceClass, "close")
        val closeException = binding(closeTry.patterns.single().patterns.single())
        assertTrue(graph.generatedDeclarations.containsKey(closeException))
        val savedOnClose = resourceMatch(onlyExpression(closeTry.catches.single()), fresh, closeException)
        val savedAfterClose = resourceMatch(expressionSlot(finalSlots[1]), fresh, null)
        val caught = binding(protected.patterns.single().patterns.single())
        val bindings = listOf(caught, closeException, savedOnClose, savedAfterClose)
        assertEquals(bindings.size, identitySet<CfirVariable>().apply { addAll(bindings) }.size, "正文异常、close 异常、两个 saved binding 均独立")
    }

    private fun resourceMatch(identity: CfirSourceExpressionIdentity, fresh: CfirVariable, closeException: CfirVariable?): CfirVariable {
        val match = payload<SourcePayload.Match>(identity)
        reference(checkNotNull(match.selector), fresh)
        assertEquals(2, match.cases.size)
        val someCase = payload<SourcePayload.MatchCase>(match.cases[0])
        val noneCase = payload<SourcePayload.MatchCase>(match.cases[1])
        assertNull(someCase.guard)
        assertNull(noneCase.guard)
        val somePattern = someCase.patterns.single()
        val nonePattern = noneCase.patterns.single()
        assertEquals(CfirSourcePatternKind.ENUM, somePattern.kind)
        assertEquals(CfirSourcePatternKind.ENUM, nonePattern.kind)
        val someRef = payload<SourcePayload.Reference>(expressionSlot(somePattern.slots.single()))
        val noneRef = payload<SourcePayload.Reference>(expressionSlot(nonePattern.slots.single()))
        val someTarget = assertInstanceOf(CfirSourceReferenceTarget.Declaration::class.java, someRef.target).declaration
        val noneTarget = assertInstanceOf(CfirSourceReferenceTarget.Declaration::class.java, noneRef.target).declaration
        assertEquals("Some", (someTarget as CfirCallableDeclaration).symbol.name.asString())
        assertEquals("None", (noneTarget as CfirCallableDeclaration).symbol.name.asString())
        assertSame(coreConstructor("Some"), someTarget)
        assertSame(coreConstructor("None"), noneTarget)
        assertTrue(nonePattern.patterns.isEmpty())
        val saved = binding(somePattern.patterns.single())
        assertTrue(graph.generatedDeclarations.containsKey(saved))
        assertEquals(listOf(saved.returnTypeRef.coneType), someRef.instantiatedTypes)
        assertEquals(someRef.instantiatedTypes, noneRef.instantiatedTypes)
        val constructorType = graph.expression(expressionSlot(somePattern.slots.single())).type as ConeFunctionType
        assertEquals(listOf(saved.returnTypeRef.coneType), constructorType.parameterTypes)
        assertEquals(fresh.returnTypeRef.coneType, constructorType.returnType)
        assertEquals(fresh.returnTypeRef.coneType, graph.expression(expressionSlot(nonePattern.slots.single())).type)
        val thrown = onlyExpression(someCase.body)
        assertTrue(graph.expression(thrown).type.isNothing)
        reference(payload<SourcePayload.Throw>(thrown).exception, saved)
        val noneBody = onlyExpression(noneCase.body)
        if (closeException != null) {
            assertTrue(graph.expression(noneBody).type.isNothing)
            reference(payload<SourcePayload.Throw>(noneBody).exception, closeException)
        } else {
            assertTrue(graph.expression(noneBody).type.isUnit)
            assertEquals(CfirLiteralKind.UNIT, payload<SourcePayload.Literal>(noneBody).kind)
        }
        return saved
    }

    private fun assertResourceCall(identity: CfirSourceExpressionIdentity, resource: CfirVariable, owner: CfirClass, name: String) {
        val call = payload<SourcePayload.Call>(identity)
        assertTrue(call.arguments.isEmpty())
        assertEquals(CfirSourceCallKind.DECLARED, call.kind)
        val member = payload<SourcePayload.Member>(call.base)
        assertSame(owner.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == name },
            assertInstanceOf(CfirSourceReferenceTarget.Declaration::class.java, member.reference.target).declaration)
        reference(member.receiver, resource)
        if (name == "close") assertTrue(graph.expression(identity).type.isUnit)
        else assertTrue(graph.expression(identity).type.isBoolean)
    }

    private fun some(identity: CfirSourceExpressionIdentity, bound: CfirVariable, resultType: ConeCangJieType) {
        val call = payload<SourcePayload.Call>(identity)
        assertEquals(CfirSourceCallKind.DECLARED, call.kind)
        val ref = payload<SourcePayload.Reference>(call.base)
        val referenceTarget = assertInstanceOf(CfirSourceReferenceTarget.Declaration::class.java, ref.target).declaration
        assertEquals("Some", (referenceTarget as CfirCallableDeclaration).symbol.name.asString())
        assertSame(coreConstructor("Some"), referenceTarget)
        assertTrue(referenceTarget.symbol.isBound)
        assertEquals(listOf(bound.returnTypeRef.coneType), ref.instantiatedTypes)
        assertEquals(resultType, graph.expression(identity).type)
        reference(argument(call.arguments.single()), bound)
    }

    /** 所有 Call 实参是独立 wrapper，类型来自应用后的 callee，而非未替换的原始参数声明。 */
    private fun assertAppliedCall(identity: CfirSourceExpressionIdentity) {
        val call = payload<SourcePayload.Call>(identity)
        val signature = graph.expression(call.base).type as ConeFunctionType
        assertEquals(signature.returnType, graph.expression(identity).type,
            "SOURCE call kind=${call.kind}, context=${graph.expression(identity).context.symbol}, " +
                "span=${graph.expression(identity).span}, base=${graph.expression(call.base).payload}")
        assertEquals(signature.parameterTypes.size, call.arguments.size)
        call.arguments.zip(signature.parameterTypes).forEach { (id, type) ->
            assertEquals(type, graph.expression(id).type)
            val wrapper = payload<SourcePayload.Argument>(id)
            assertFalse(wrapper.isDefaultValue)
            assertFalse(wrapper.withInout)
            val value = checkNotNull(wrapper.value)
            assertNotSame(id, value, "FuncArg wrapper 保留独立身份")
        }
    }

    private fun nativeAssign(identity: CfirSourceExpressionIdentity, variable: CfirVariable, op: CfirSourceOperator, rhs: String) {
        val assign = payload<SourcePayload.Assignment>(identity)
        assertEquals(op, assign.operator)
        assertTrue(assign.isCompound)
        assertTrue(graph.expression(identity).type.isUnit)
        reference(assign.left, variable)
        val literal = payload<SourcePayload.Literal>(assign.right)
        assertEquals(CfirLiteralKind.INT, literal.kind)
        assertEquals(rhs, literal.text)
        assertEquals(variable.returnTypeRef.coneType, graph.expression(assign.right).type)
    }

    private fun generated(declaration: CfirDeclaration, owner: CfirFunction) {
        val context = graph.generatedDeclarations.getValue(declaration)
        assertSame(owner, context.owner)
        assertSame(file, context.file)
        assertSame(declaration, declaration.symbol.cfir)
    }

    private fun reference(identity: CfirSourceExpressionIdentity, declaration: CfirDeclaration, message: String = "解析引用必须保留正式声明身份") {
        val referenceTarget = payload<SourcePayload.Reference>(identity).target
        assertSame(declaration, assertInstanceOf(CfirSourceReferenceTarget.Declaration::class.java, referenceTarget).declaration, message)
        if (declaration is CfirVariable) assertEquals(declaration.returnTypeRef.coneType, graph.expression(identity).type)
        assertSame(declaration, declaration.symbol.cfir)
    }

    private fun target(identity: CfirSourceExpressionIdentity): CfirDeclaration = when (val value = graph.expression(identity).payload) {
        is SourcePayload.Reference -> {
            val referenceTarget = value.target
            assertInstanceOf(CfirSourceReferenceTarget.Declaration::class.java, referenceTarget).declaration
        }
        is SourcePayload.Member -> {
            val referenceTarget = value.reference.target
            assertInstanceOf(CfirSourceReferenceTarget.Declaration::class.java, referenceTarget).declaration
        }
        else -> error("应为已解析 Reference/Member，实际为 $value")
    }

    private fun binding(pattern: CfirSourcePattern): CfirVariable {
        assertEquals(CfirSourcePatternKind.VARIABLE, pattern.kind)
        assertTrue(pattern.patterns.isEmpty())
        val variable = (pattern.slots.single() as CfirSourceSlot.Declaration).declaration as CfirVariable
        assertEquals(pattern.types.first(), variable.returnTypeRef.coneType)
        return variable
    }

    private fun method(owner: CfirInterface, name: String): CfirNamedFunction = owner.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == name }
    private fun variable(owner: CfirFunction, name: String): CfirVariable = normalElements(owner).filterIsInstance<CfirVariable>().single { it.symbol.name.asString() == name }
    private fun body(function: CfirFunction): CfirSourceExpressionIdentity = graph.functionBodies.getValue(function).also {
        assertSame(graph.expressionOf(checkNotNull(function.body)), it, "函数根与原 body 的 SOURCE 身份一致")
    }
    private fun initializer(variable: CfirVariable): CfirSourceExpressionIdentity = graph.initializers.getValue(variable)
    private fun block(identity: CfirSourceExpressionIdentity): SourcePayload.Block = payload(identity)
    private fun onlyExpression(blockId: CfirSourceExpressionIdentity): CfirSourceExpressionIdentity = expressionSlot(block(blockId).statements.single())
    private fun expressionSlot(slot: CfirSourceSlot): CfirSourceExpressionIdentity = (slot as CfirSourceSlot.Expression).identity
    private fun argument(identity: CfirSourceExpressionIdentity): CfirSourceExpressionIdentity = checkNotNull(payload<SourcePayload.Argument>(identity).value)
    private fun returned(blockId: CfirSourceExpressionIdentity, function: CfirFunction): SourcePayload.Return =
        payload<SourcePayload.Return>(expressionSlot(block(blockId).statements.last())).also { assertSame(function, it.function) }

    /** 查正常 harness 加载的真实 std.core 符号，不能仅凭名字接受伪造 Some/None。 */
    private fun coreConstructor(name: String): CfirEnumConstructor {
        val option = checkNotNull(session.symbolProvider.getClassLikeSymbolByClassId(StdlibClassIds.Option)).cfir as CfirEnum
        return option.declarations.filterIsInstance<CfirEnumConstructor>().single { it.name.asString() == name }
    }

    private inline fun <reified T : SourcePayload> payload(identity: CfirSourceExpressionIdentity): T {
        val value = graph.expression(identity).payload
        assertTrue(value is T, "应为 ${T::class.simpleName}，实际为 $value")
        return value as T
    }

    /** 混合声明、initializer、lambda body、模式表达式均可达；Jump.loop/map 不作为拥有边递归。 */
    private fun owned(root: CfirSourceExpressionIdentity): List<CfirSourceExpressionIdentity> {
        val visited = identitySet<CfirSourceExpressionIdentity>()
        val result = mutableListOf<CfirSourceExpressionIdentity>()
        fun visitDeclaration(declaration: CfirDeclaration, visit: (CfirSourceExpressionIdentity) -> Unit) {
            if (declaration is CfirVariable) graph.initializers[declaration]?.let(visit)
            if (declaration is CfirFunction) graph.functionBodies[declaration]?.let(visit)
        }
        fun patternDeclarations(pattern: CfirSourcePattern): List<CfirDeclaration> =
            pattern.slots.filterIsInstance<CfirSourceSlot.Declaration>().map { it.declaration } + pattern.patterns.flatMap(::patternDeclarations)
        fun visit(identity: CfirSourceExpressionIdentity) {
            if (!visited.add(identity)) return
            result += identity
            val payload = graph.expression(identity).payload
            payload.expressionChildren().forEach(::visit)
            if (payload is SourcePayload.Block) payload.statements.filterIsInstance<CfirSourceSlot.Declaration>().forEach { visitDeclaration(it.declaration, ::visit) }
            payload.patterns().flatMap(::patternDeclarations).forEach { visitDeclaration(it, ::visit) }
        }
        visit(root)
        return result
    }
}

/**
 * 验证独立 SOURCE 不改变正常分析域：全部 owned 节点/边、类型对象、声明 phase、函数 body/CFG、
 * 参数签名、initializer、resource 列表与 Return/Jump/ref 目标在生产及只读验收后保持身份。
 */
private class NormalSourceSnapshot(private val file: CfirFile) {
    private val elements = normalElements(file)
    private val checks = buildList<() -> Unit> {
        for (element in elements) {
            val source = element.source
            val children = directChildren(element)
            add {
                assertSame(source, element.source)
                val actual = directChildren(element)
                assertEquals(children.size, actual.size)
                children.zip(actual).forEach { (before, after) -> assertSame(before, after, "normal owned 子边不得被 canonical 改写") }
            }
            if (element is CfirExpression) {
                val type = element.coneTypeOrNull
                add { assertSame(type, element.coneTypeOrNull, "normal type 对象保持身份") }
            }
            if (element is CfirDeclaration) {
                val phase = element.resolvePhase
                val symbol = element.symbol
                add {
                    assertEquals(phase, element.resolvePhase)
                    assertSame(symbol, element.symbol)
                }
            }
            if (element is CfirCallableDeclaration) {
                val typeRef = element.returnTypeRef
                val status = element.status
                add {
                    assertSame(typeRef, element.returnTypeRef)
                    assertSame(status, element.status)
                }
            }
            if (element is CfirFunction) {
                val body = element.body
                val cfg = element.controlFlowGraphReference
                val graph = (cfg as? CfirControlFlowGraphReferenceImpl)?.controlFlowGraph
                val nodes = graph?.nodes?.toList()
                val nodeElements = nodes?.map { it.fir }
                add {
                    assertSame(body, element.body)
                    assertSame(cfg, element.controlFlowGraphReference)
                    if (graph != null) {
                        val actual = graph.nodes.toList()
                        assertEquals(nodes!!.size, actual.size)
                        nodes.zip(actual).forEach { (before, after) -> assertSame(before, after) }
                        nodeElements!!.zip(actual).forEach { (before, after) -> assertSame(before, after.fir) }
                    }
                }
            }
            if (element is CfirVariable) {
                val initializer = element.initializer
                add { assertSame(initializer, element.initializer) }
            }
            if (element is CfirJump<*>) {
                val target = element.target
                val labeled = target.labeledElement
                add {
                    assertSame(target, element.target)
                    assertSame(labeled, element.target.labeledElement)
                }
            }
            if (element is CfirResolvedNamedReference) {
                val target = element.resolvedSymbol
                add { assertSame(target, element.resolvedSymbol) }
            }
            if (element is CfirTryExpression) {
                val resources = element.resources.toList()
                add {
                    assertEquals(resources.size, element.resources.size)
                    resources.zip(element.resources).forEach { (before, after) -> assertSame(before, after) }
                }
            }
        }
    }

    fun assertUnchanged() {
        val actual = normalElements(file)
        assertEquals(elements.size, actual.size)
        elements.zip(actual).forEach { (before, after) -> assertSame(before, after) }
        checks.forEach { it() }
    }
}

private fun directChildren(element: CfirElement): List<CfirElement> = buildList {
    element.acceptChildren(object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) { add(element) }
    })
}

private fun normalElements(root: CfirElement): List<CfirElement> {
    val seen = identitySet<CfirElement>()
    val result = mutableListOf<CfirElement>()
    root.accept(object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            if (!seen.add(element)) return
            result += element
            element.acceptChildren(this)
        }
    })
    return result
}

private fun <T : Any> identitySet(): MutableSet<T> = Collections.newSetFromMap(IdentityHashMap<T, Boolean>())
