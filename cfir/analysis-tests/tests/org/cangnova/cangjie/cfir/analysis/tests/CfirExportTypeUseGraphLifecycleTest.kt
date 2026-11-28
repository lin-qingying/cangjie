package org.cangnova.cangjie.cfir.analysis.tests

import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCfirDiagnosticTestBase
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpMode
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.resolve.providers.CfirDeclarationExportAnalysis
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysis
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportSourcePayload
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportTypeElement
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportTypePayload
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportTypeUse
import org.cangnova.cangjie.cfir.resolve.source.CfirSourceGraphProducer
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRefOrigin
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRefCopy
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
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
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.MessageCollector
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
import org.cangnova.cangjie.test.services.compilerConfigurationProvider
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

/** PSI：正式 frontend 完成后验收 TypeUse 图，不复用手工构造的类型或 session。 */
class CfirExportTypeUseGraphLifecycleTest : AbstractCfirExportTypeUseGraphLifecycleTest(CfirParser.Psi)

/** LightTree：同一组生命周期合同使用独立的解析和完成输入执行。 */
class CfirExportTypeUseGraphLifecycleLightTreeTest : AbstractCfirExportTypeUseGraphLifecycleTest(CfirParser.LightTree)

/**
 * 框架对位固定 Kotlin 3c7a5041 的 AbstractFirDiagnosticTestBase/FirAnalysisHandler。
 * Alias 合同见 frozen-type-roles-authority-20261118.md 及其 SDK 1.1.3 JSON 证据；
 * 固定 native 14276740 的 TypeManager.cpp:2373–2487 区分当前节点、Paren.inner、
 * 有序实参及无原返回节点，ASTWriter.cpp:1258–1274 单独保存 FuncBody.retType。
 * 新 fixture 经 SDK 1.1.3 的 --dump-ast 验证，Paren、推断返回和继承槽均由前端生成。
 * raw 省略返回标注不代表完成产品没有 Type 节点。固定 TypeChecker.cpp:203–208、
 * 228–238 的 AddRetTypeNode 及 TypeCheckExpr/LambdaExpr.cpp:260–264、343–349 在完成 owner 发布
 * COMPILER_ADD 返回节点；有正文推断函数的 FuncBody 返回槽必须非空且与签名语义一致。
 */
abstract class AbstractCfirExportTypeUseGraphLifecycleTest(parser: CfirParser) : AbstractCfirDiagnosticTestBase(parser) {
    private lateinit var case: TypeUseLifecycleCase

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            LANGUAGE_VERSION with "1.1.3"
            CJMP_MODE with CfirCjmpMode.NONE
            RUN_PIPELINE_TILL with TestPhase.FRONTEND
            +WITH_STDLIB
        }
        builder.firHandlersStep {
            useHandlers({ services: TestServices -> TypeUseLifecycleHandler(services, parser, case) })
        }
    }

    @Test
    fun explicitAliasAndExpressionKeepSeparateRoles() = runCase(TypeUseLifecycleCase.ALIAS_ROLES)

    @Test
    fun sharedTypeParametersKeepOwnerAndRepeatedOrder() = runCase(TypeUseLifecycleCase.GENERIC_ORDER)

    @Test
    fun sourceSemanticAndCompilerAddedNodesKeepTheirRoles() = runCase(TypeUseLifecycleCase.NODE_ORIGINS)

    @Test
    fun parenthesesSelectOnlyTheActualInner() = runCase(TypeUseLifecycleCase.PARENTHESES)

    @Test
    fun omittedReturnPublishesCompletedCompilerAddedType() = runCase(TypeUseLifecycleCase.IMPLICIT_RETURN)

    @Test
    fun inheritedTypeSlotsKeepOrderAndBindings() = runCase(TypeUseLifecycleCase.SUPER_TYPES)

    @Test
    fun replacingReturnNodeLeavesOldGraphFrozen() = runCase(TypeUseLifecycleCase.RETURN_GENERATION)

    @Test
    fun replacingAliasRightHandSideLeavesOldGraphFrozen() = runCase(TypeUseLifecycleCase.ALIAS_GENERATION)

    private fun runCase(value: TypeUseLifecycleCase) {
        case = value
        assertEquals(value.sourceSha256, typeUseLifecycleSha256(Files.readAllBytes(value.source)))
        runTest(value.source.toString())
    }
}

private enum class TypeUseLifecycleCase(val usesLifecycleFixture: Boolean = false) {
    ALIAS_ROLES, GENERIC_ORDER, NODE_ORIGINS(true), PARENTHESES(true),
    IMPLICIT_RETURN(true), SUPER_TYPES(true), RETURN_GENERATION, ALIAS_GENERATION;

    val source: Path
        get() = Path.of("cfir/analysis-tests/testData/diagnostics2/common-specific/source-frozen-types/" +
                if (usesLifecycleFixture) "typeUseGraphLifecycle.cj" else "orderedAliasRoles.cj")

    val sourceSha256: String
        get() = if (usesLifecycleFixture) "7384D24F46FA3EE0EF4FBA49D58BC61AC900DFBFF72621277C50DD14D50B212B"
        else "6B911947B0533ED9D7889C1CCAF820527A0349BF68F90B9B6130E41ACF92C996"
}

/** 保留诊断 harness 的 handler；公开 frontend 工厂提供允许完整 SOURCE 捕获的正常 resolver。 */
@OptIn(CompilerConfiguration.Internals::class)
private class TypeUseLifecycleHandler(
    testServices: TestServices,
    private val parser: CfirParser,
    private val case: TypeUseLifecycleCase,
) : CfirAnalysisHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer> = emptyList()
    private var checkedModules = 0

    override fun processModule(module: TestModule, info: CfirOutputArtifact) {
        assertEquals(0, checkedModules)
        val part = info.partsForDependsOnModules.single { it.module === module }
        val input = part.firFilesByTestFile.keys.single { !it.isAdditional }
        val original = input.originalFile.toPath().toAbsolutePath().normalize()
        assertEquals(case.source.toAbsolutePath().normalize(), original)
        assertEquals(case.sourceSha256, typeUseLifecycleSha256(Files.readAllBytes(original)))
        assertTrue(testServices.globalMetadataInfoHandler.getExistingMetaInfosForFile(input).isEmpty())
        assertEquals(Files.readString(original, Charsets.UTF_8), testServices.sourceFileProvider.getContentOfSourceFile(input))
        val disposable = Disposer.newDisposable("type-use-graph-lifecycle")
        try {
            val moduleConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
            val diagnostics = DiagnosticsCollectorImpl()
            val messages = TypeUseLifecycleMessages()
            val configuration = CompilerConfiguration().apply {
                moduleName = module.name
                languageVersionSettings = moduleConfiguration.languageVersionSettings
                useLightTree = parser == CfirParser.LightTree
                cjmpMode = CfirCjmpMode.NONE
                diagnosticsCollector = diagnostics
                messageCollector = messages
                moduleConfiguration.classpathRoots.forEach { addClasspathRoot(it.path) }
                addCangJieSourceRoot(original.toString())
            }
            val artifact = checkNotNull(CfirFrontendPipelinePhase.executePhase(ConfigurationPipelineArtifact(configuration, disposable))) {
                "官方合法输入必须实际完成 frontend"
            }
            assertTrue(diagnostics.diagnostics.isEmpty(), diagnostics.diagnostics.joinToString("\n") { it.renderMessage() })
            assertTrue(messages.entries.none { it.first.isWarning || it.first.isError }, messages.entries.joinToString("\n"))
            val output = artifact.frontendOutput.outputs.single()
            assertNotSame(part.session, output.session)
            val file = output.fir.single()
            val canonical = CfirSourceGraphProducer.produce(listOf(file))
            val analysis = CfirExportAnalysis.analyze(listOf(file), canonicalSources = listOf(canonical))
            assertSame(canonical, analysis.canonicalSourceGraphs().single())
            assertSame(analysis.typeUseGraph(), analysis.typeUseGraph())
            analysis.validateSourceUnchanged()
            TypeUseLifecycleAssertions(file, analysis).assertCase(case)
            assertEquals(case.sourceSha256, typeUseLifecycleSha256(Files.readAllBytes(original)))
            checkedModules++
        } finally {
            Disposer.dispose(disposable)
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        assertEquals(1, checkedModules, "每个正例必须完成真实 frontend 和全部生命周期断言")
    }
}

private class TypeUseLifecycleMessages : MessageCollector {
    val entries = mutableListOf<Pair<CompilerMessageSeverity, String>>()
    override fun clear() = entries.clear()
    override fun hasErrors(): Boolean = entries.any { it.first.isError }
    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        entries += severity to message
    }
}

/** 只查询捕获产品；名字仅定位 fixture 声明，所有类型参数及 Alias 断言均比较真实声明身份。 */
private class TypeUseLifecycleAssertions(private val file: CfirFile, private val analysis: CfirDeclarationExportAnalysis) {
    private val graph = analysis.typeUseGraph()
    private val elements = typeUseLifecycleElements(file)

    fun assertCase(case: TypeUseLifecycleCase) {
        when (case) {
            TypeUseLifecycleCase.ALIAS_ROLES -> aliasRoles()
            TypeUseLifecycleCase.GENERIC_ORDER -> genericOrder()
            TypeUseLifecycleCase.NODE_ORIGINS -> nodeOrigins()
            TypeUseLifecycleCase.PARENTHESES -> parentheses()
            TypeUseLifecycleCase.IMPLICIT_RETURN -> implicitReturn()
            TypeUseLifecycleCase.SUPER_TYPES -> superTypes()
            TypeUseLifecycleCase.RETURN_GENERATION -> returnGeneration()
            TypeUseLifecycleCase.ALIAS_GENERATION -> aliasGeneration()
        }
        if (case != TypeUseLifecycleCase.RETURN_GENERATION && case != TypeUseLifecycleCase.ALIAS_GENERATION) {
            analysis.validateSourceUnchanged()
        }
    }

    private fun aliasRoles() {
        val alias = findAlias("IntPairChain")
        val function = findFunction("aliasUse")
        val parameter = function.valueParameters.single()
        val parameterUse = graph.reference(parameter.returnTypeRef) as CfirExportTypeUse.Semantic
        val returnUse = graph.reference(function.returnTypeRef) as CfirExportTypeUse.Semantic
        val signature = graph.declarationType(function) as CfirExportTypeUse.Function
        assertSame(parameterUse, graph.declarationType(parameter))
        assertSame(parameterUse, signature.parameters.single())
        assertSame(returnUse, signature.returnType)
        assertSame(returnUse, graph.functionReturn(function))
        listOf(parameterUse, returnUse).forEach { use ->
            assertSame(alias, (use.type.payload as CfirExportTypePayload.Alias).declaration)
            assertTrue((use.type.payload as CfirExportTypePayload.Alias).arguments.isEmpty())
        }
        val result = elements.filterIsInstance<CfirReturnExpression>().single { it.target.labeledElement === function }.result
        val capturedExpression = analysis.sourceElement(result)
        val expressionType = analysis.typeElement(checkNotNull(capturedExpression.type), capturedExpression.context)
        val semanticTuple = analysis.typeElement(parameterUse.type.expandedType, parameterUse.type.domain)
        assertEquals((semanticTuple.payload as CfirExportTypePayload.Tuple).elements,
            (expressionType.payload as CfirExportTypePayload.Tuple).elements, "表达式使用同一展开 tuple 语义")
        assertNotEquals(parameterUse.type.payload, expressionType.payload, "显式 Alias 选择不能污染表达式类型")
        assertNotSame(parameter.returnTypeRef, function.returnTypeRef, "参数和原返回分别拥有真实 SOURCE 节点")
        assertEquals(CfirTypeRefOrigin.SOURCE, parameter.returnTypeRef.typeUseOrigin)
        assertEquals(CfirTypeRefOrigin.SOURCE, function.returnTypeRef.typeUseOrigin)
        val tupleFunction = findFunction("tupleUse")
        val direct = graph.reference(tupleFunction.valueParameters.single().returnTypeRef) as CfirExportTypeUse.Arguments
        assertEquals(2, direct.arguments.size)
        direct.arguments.forEach { primitive(it, PrimitiveTypeKind.INT64) }
        assertEquals((semanticTuple.payload as CfirExportTypePayload.Tuple).elements,
            (direct.type.payload as CfirExportTypePayload.Tuple).elements)
        assertSame(direct, (graph.declarationType(tupleFunction) as CfirExportTypeUse.Function).parameters.single())
    }

    private fun genericOrder() {
        val pair = findAlias("Pair")
        val carrier = file.declarations.filterIsInstance<CfirClassLikeDeclaration>().single { it.name.asString() == "Carrier" }
        val pairOf = findFunction("pairOf")
        val pairT = pair.typeParameters.single()
        val carrierT = carrier.typeParameters.filterIsInstance<CfirTypeParameter>()[0]
        val carrierU = carrier.typeParameters.filterIsInstance<CfirTypeParameter>()[1]
        val functionT = pairOf.typeParameters.single()
        assertNotSame(pairT, carrierT)
        assertNotSame(carrierT, functionT)
        assertSame(pair, analysis.ownerOf(pairT))
        assertSame(carrier, analysis.ownerOf(carrierT))
        assertSame(pairOf, analysis.ownerOf(functionT))
        val rhs = graph.aliasRightHandSide(pair) as CfirExportTypeUse.Arguments
        assertEquals(2, rhs.arguments.size)
        rhs.arguments.forEach { generic(it, pairT) }
        assertSame(pair, (type(graph.declarationType(pair)!!).payload as CfirExportTypePayload.Alias).declaration)
        assertTrue(rhs.type.payload is CfirExportTypePayload.Tuple)
        val choose = graph.declarationType(findFunction("choose")) as CfirExportTypeUse.Function
        assertEquals(3, choose.parameters.size)
        choose.parameters.zip(listOf(carrierT, carrierU, carrierT)).forEach { (use, owner) -> generic(use, owner) }
        generic(choose.returnType, carrierU)
        listOf(findFunction("pair") to carrierT, pairOf to functionT).forEach { (function, parameter) ->
            val use = (graph.declarationType(function) as CfirExportTypeUse.Function).returnType
            val payload = type(use).payload as CfirExportTypePayload.Alias
            assertSame(pair, payload.declaration)
            assertEquals(1, payload.arguments.size)
            val bound = analysis.typeElement(payload.arguments.single(), type(use).domain).payload as CfirExportTypePayload.Generic
            assertSame(parameter, bound.parameter, "不能按同名 T 重绑 owner")
        }
        listOf("orderedSlots" to listOf(PrimitiveTypeKind.INT64, PrimitiveTypeKind.BOOLEAN, PrimitiveTypeKind.INT64),
            "reorderedSlots" to listOf(PrimitiveTypeKind.BOOLEAN, PrimitiveTypeKind.INT64, PrimitiveTypeKind.INT64),
            "repeatedReturn" to listOf(PrimitiveTypeKind.INT64, PrimitiveTypeKind.INT64)).forEach { (name, kinds) ->
            val signature = graph.declarationType(findFunction(name)) as CfirExportTypeUse.Function
            assertEquals(kinds.size, signature.parameters.size)
            signature.parameters.zip(kinds).forEach { (use, kind) -> primitive(use, kind) }
            primitive(signature.returnType, if (name == "repeatedReturn") PrimitiveTypeKind.INT64 else PrimitiveTypeKind.BOOLEAN)
        }
    }

    private fun nodeOrigins() {
        val source = findFunction("explicitUnit").returnTypeRef
        val generated = findFunction("implicitUnit").returnTypeRef
        assertEquals(CfirTypeRefOrigin.SOURCE, source.typeUseOrigin)
        assertEquals(CfirTypeRefOrigin.COMPILER_ADDED, generated.typeUseOrigin)
        assertNotSame(source, generated)
        primitive(graph.reference(source), PrimitiveTypeKind.UNIT)
        primitive(graph.reference(generated), PrimitiveTypeKind.UNIT)
        val inferred = findFunction("inferredTuple")
        val originalTuple = inferred.valueParameters.single().returnTypeRef
        val generatedTuple = inferred.returnTypeRef
        assertEquals(CfirTypeRefOrigin.SOURCE, originalTuple.typeUseOrigin)
        assertEquals(CfirTypeRefOrigin.COMPILER_ADDED, generatedTuple.typeUseOrigin)
        assertNotSame(originalTuple, generatedTuple)
        val originalUse = graph.reference(originalTuple) as CfirExportTypeUse.Arguments
        val generatedUse = graph.reference(generatedTuple) as CfirExportTypeUse.Semantic
        val result = elements.filterIsInstance<CfirReturnExpression>().single { it.target.labeledElement === inferred }.result
        val expression = analysis.sourceElement(result)
        val semanticTuple = analysis.typeElement(checkNotNull(expression.type), expression.context)
        assertEquals((originalUse.type.payload as CfirExportTypePayload.Tuple).elements,
            (semanticTuple.payload as CfirExportTypePayload.Tuple).elements)
        assertEquals((semanticTuple.payload as CfirExportTypePayload.Tuple).elements,
            (generatedUse.type.payload as CfirExportTypePayload.Tuple).elements,
            "SOURCE 语法、表达式语义 Ty 和当前生成 Type 节点分别选择同一 tuple 语义")
        assertNotSame(originalUse, generatedUse)
        val arrayUse = graph.reference(findFunction("arrayUse").valueParameters.single().returnTypeRef) as CfirExportTypeUse.Arguments
        assertEquals(1, arrayUse.arguments.size, "SOURCE Array 的真正语法实参需要递归选择")
        assertEquals(1, (arrayUse.arguments.single() as CfirExportTypeUse.Arguments).arguments.size)
        val array = checkNotNull((arrayUse.type.payload as CfirExportTypePayload.Nominal).declaration)
        assertFalse(analysis.isSourceDeclaration(array), "正常 SDK CJO 导入没有原 SOURCE 类型语法")
        val added = mutableListOf<Pair<CfirTypeRef, CfirExportTypeElement>>()
        analysis.walkSource(array, array) { element, _ ->
            val ref = element.identity as? CfirTypeRef
            val payload = element.payload as? CfirExportSourcePayload.TypeReference
            val resolvedType = element.resolvedType
            if (ref != null && payload?.origin == CfirTypeRefOrigin.COMPILER_ADDED && resolvedType != null) {
                val frozen = analysis.typeElement(resolvedType, element.context)
                assertSame(frozen, (graph.reference(ref) as CfirExportTypeUse.Semantic).type,
                    "当前 COMPILER_ADDED 节点必须直接选择语义 Ty")
                added += ref to frozen
            }
        }
        assertTrue(added.isNotEmpty(), "必须实际覆盖正式 SDK loader 创建的节点，不能手工改 origin")
        val unit = added.first { (it.second.payload as? CfirExportTypePayload.Primitive)?.kind == PrimitiveTypeKind.UNIT }
        assertNotSame(source, unit.first)
        assertNotSame(generated, unit.first)
        assertEquals(CfirTypeRefOrigin.COMPILER_ADDED, unit.first.typeUseOrigin)
        primitive(graph.reference(unit.first), PrimitiveTypeKind.UNIT)
        assertTrue(added.any { (it.second.payload as? CfirExportTypePayload.Nominal)?.arguments?.isNotEmpty() == true },
            "包含有序子 Ty 的 compiler-added 当前节点仍停止，不依据 Cone.children 补语法递归")
    }

    private fun parentheses() {
        val function = findFunction("parenthesized")
        val ref = function.valueParameters.single().returnTypeRef
        val paren = syntax(ref) as CfirExportSourcePayload.ParenthesizedType
        val tuple = syntax(paren.inner) as CfirExportSourcePayload.TupleType
        assertEquals(2, tuple.elements.size)
        assertNotSame(paren.inner, tuple.elements.first(), "真正 inner 是完整 Tuple，不是任意首个 child")
        val selected = graph.reference(ref) as CfirExportTypeUse.Arguments
        assertSame(graph.reference(paren.inner), selected)
        assertEquals(2, selected.arguments.size)
        selected.arguments.zip(tuple.elements).forEach { (use, inner) ->
            assertSame(graph.reference(inner), use)
            assertSame(findAlias("LifecyclePair"), (type(use).payload as CfirExportTypePayload.Alias).declaration)
        }
        assertSame(graph.reference(syntax(function.returnTypeRef).let { (it as CfirExportSourcePayload.ParenthesizedType).inner }),
            graph.functionReturn(function))
        val callback = findFunction("nestedCallback")
        val callbackRef = callback.valueParameters.single().returnTypeRef
        val callbackInner = (syntax(callbackRef) as CfirExportSourcePayload.ParenthesizedType).inner
        val selectedFunction = graph.reference(callbackRef) as CfirExportTypeUse.Function
        assertSame(graph.reference(callbackInner), selectedFunction)
        val owner = file.declarations.filterIsInstance<CfirInterface>().single { it.name.asString() == "LifecycleSource" }
        val parameters = owner.typeParameters.filterIsInstance<CfirTypeParameter>()
        assertEquals(3, selectedFunction.parameters.size)
        selectedFunction.parameters.zip(listOf(parameters[0], parameters[1], parameters[0])).forEach { (use, parameter) -> generic(use, parameter) }
        generic(selectedFunction.returnType, parameters[1])
    }

    private fun implicitReturn() {
        val inferred = findFunction("inferredTuple")
        val signature = graph.declarationType(inferred) as CfirExportTypeUse.Function
        assertEquals(CfirTypeRefOrigin.COMPILER_ADDED, inferred.returnTypeRef.typeUseOrigin)
        val bodyReturn = checkNotNull(graph.functionReturn(inferred)) {
            "raw omission 经正常 BODY 完成后拥有真实生成 Type，FuncBody 返回槽不能为 null"
        }
        val returned = signature.returnType as CfirExportTypeUse.Semantic
        assertSame(returned, bodyReturn)
        assertEquals(2, (returned.type.payload as CfirExportTypePayload.Tuple).elements.size)
        val parameter = signature.parameters.single() as CfirExportTypeUse.Arguments
        assertEquals((parameter.type.payload as CfirExportTypePayload.Tuple).elements,
            (returned.type.payload as CfirExportTypePayload.Tuple).elements)
        assertSame(returned, graph.reference(inferred.returnTypeRef))
        val implicit = findFunction("implicitUnit")
        assertEquals(CfirTypeRefOrigin.COMPILER_ADDED, implicit.returnTypeRef.typeUseOrigin)
        val implicitSignature = graph.declarationType(implicit) as CfirExportTypeUse.Function
        primitive(implicitSignature.returnType, PrimitiveTypeKind.UNIT)
        val implicitBodyReturn = checkNotNull(graph.functionReturn(implicit))
        assertSame(implicitSignature.returnType, implicitBodyReturn)
        assertSame(graph.reference(implicit.returnTypeRef), implicitBodyReturn)
        val explicit = findFunction("explicitUnit")
        assertSame(graph.reference(explicit.returnTypeRef), graph.functionReturn(explicit))
        primitive(checkNotNull(graph.functionReturn(explicit)), PrimitiveTypeKind.UNIT)
    }

    private fun superTypes() {
        val owner = file.declarations.filterIsInstance<CfirInterface>().single { it.name.asString() == "LifecycleSource" }
        val selected = graph.superTypes(owner)
        val parameters = owner.typeParameters.filterIsInstance<CfirTypeParameter>()
        assertEquals(3, selected.size)
        assertEquals(3, owner.superTypeRefs.size)
        selected.zip(owner.superTypeRefs).forEach { (use, ref) -> assertSame(graph.reference(ref), use) }
        selected.zip(listOf("LifecycleFirst", "LifecycleMiddle", "LifecycleLast")).forEach { (use, name) ->
            assertSame(file.declarations.filterIsInstance<CfirInterface>().single { it.name.asString() == name },
                (type(use).payload as CfirExportTypePayload.Nominal).declaration)
        }
        selected.zip(listOf(parameters[0], parameters[1], parameters[0])).forEach { (use, parameter) ->
            generic((use as CfirExportTypeUse.Arguments).arguments.single(), parameter)
        }
    }

    /** 仅替换真实完成节点的身份，Cone、原语法、来源和绑定都原样复制。 */
    private fun returnGeneration() {
        val function = findFunction("aliasUse")
        val original = function.returnTypeRef as CfirResolvedTypeRef
        val source = analysis.sourceElement(original)
        val selected = graph.reference(original)
        val signature = graph.declarationType(function) as CfirExportTypeUse.Function
        val slots = signature.parameters.toList()
        val replacement = buildResolvedTypeRefCopy(original) {}
        assertNotSame(original, replacement)
        assertSame(original.coneType, replacement.coneType)
        assertSame(original.delegatedTypeRef, replacement.delegatedTypeRef)
        function.replaceReturnTypeRef(replacement)
        assertSame(source, analysis.sourceElement(original))
        assertSame(selected, graph.reference(original))
        assertSame(selected, graph.functionReturn(function))
        assertSame(signature, graph.declarationType(function))
        assertSame(selected, signature.returnType)
        assertEquals(slots, signature.parameters)
        assertThrows(NoSuchElementException::class.java) { graph.reference(replacement) }
        assertThrows(IllegalStateException::class.java) { analysis.validateSourceUnchanged() }
        val next = CfirExportAnalysis.analyze(listOf(file), canonicalSources = analysis.canonicalSourceGraphs())
        assertNotSame(graph, next.typeUseGraph())
        assertSame(next.typeUseGraph().reference(replacement), next.typeUseGraph().functionReturn(function))
        next.validateSourceUnchanged()
        assertSame(selected, graph.reference(original))
    }

    private fun aliasGeneration() {
        val alias = findAlias("Pair")
        val original = alias.expandedTypeRef as CfirResolvedTypeRef
        val self = graph.declarationType(alias)
        val rhs = graph.aliasRightHandSide(alias) as CfirExportTypeUse.Arguments
        val slots = rhs.arguments.toList()
        val replacement = buildResolvedTypeRefCopy(original) {}
        assertNotSame(original, replacement)
        assertSame(original.coneType, replacement.coneType)
        alias.replaceExpandedTypeRef(replacement)
        assertSame(self, graph.declarationType(alias))
        assertSame(rhs, graph.aliasRightHandSide(alias))
        assertSame(rhs, graph.reference(original))
        assertEquals(slots, rhs.arguments)
        rhs.arguments.forEach { generic(it, alias.typeParameters.single()) }
        assertThrows(NoSuchElementException::class.java) { graph.reference(replacement) }
        assertThrows(IllegalStateException::class.java) { analysis.validateSourceUnchanged() }
        val next = CfirExportAnalysis.analyze(listOf(file), canonicalSources = analysis.canonicalSourceGraphs())
        assertNotSame(graph, next.typeUseGraph())
        assertSame(next.typeUseGraph().reference(replacement), next.typeUseGraph().aliasRightHandSide(alias))
        next.validateSourceUnchanged()
        assertSame(rhs, graph.aliasRightHandSide(alias))
        assertEquals(slots, rhs.arguments)
    }

    /** 只沿 Resolved 的原型壳寻找该节点的语法角色，Paren 的 inner 由专属字段另行读取。 */
    private fun syntax(ref: CfirTypeRef): CfirExportSourcePayload.TypeReference {
        val payload = analysis.sourceElement(ref).payload as CfirExportSourcePayload.TypeReference
        if (payload is CfirExportSourcePayload.ResolvedType) {
            val prototype = payload.prototype
            if (prototype != null) return syntax(prototype)
        }
        return payload
    }

    private fun generic(use: CfirExportTypeUse, parameter: CfirTypeParameter) {
        assertSame(parameter, (type(use).payload as CfirExportTypePayload.Generic).parameter)
        assertSame(parameter, parameter.symbol.cfir)
    }

    private fun primitive(use: CfirExportTypeUse, kind: PrimitiveTypeKind) {
        assertEquals(kind, (type(use).payload as CfirExportTypePayload.Primitive).kind)
    }

    private fun type(use: CfirExportTypeUse): CfirExportTypeElement = when (use) {
        is CfirExportTypeUse.Semantic -> use.type
        is CfirExportTypeUse.Arguments -> use.type
        is CfirExportTypeUse.Function -> use.type
        CfirExportTypeUse.Invalid -> error("官方合法正例不能产生 Invalid TypeUse")
    }

    private fun findFunction(name: String): CfirNamedFunction =
        elements.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == name }

    private fun findAlias(name: String): CfirTypeAlias =
        file.declarations.filterIsInstance<CfirTypeAlias>().single { it.name.asString() == name }
}

private fun typeUseLifecycleElements(root: CfirElement): List<CfirElement> {
    val seen = Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
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

private fun typeUseLifecycleSha256(bytes: ByteArray): String =
    HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
