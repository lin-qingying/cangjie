package org.cangnova.cangjie.cfir.analysis.tests

import PackageFormat.*
import PackageFormat.Package
import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.Disposer
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCfirDiagnosticTestBase
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.defaultAssignmentSyntaxFacts
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpMode
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysis
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysisSettings
import org.cangnova.cangjie.cfir.resolve.source.CfirSourceGraphProducer
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.serialization.cjo.CjoDeclarationExportSelection
import org.cangnova.cangjie.cfir.serialization.cjo.CjoExportOptions
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSourceContentSlot
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSourceSaveStage
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.addClasspathRoot
import org.cangnova.cangjie.config.classpathRoots
import org.cangnova.cangjie.config.cjoOutputDirectory
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
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.source.CjSourceElement
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
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat

/** PSI 的真实 SOURCE→frontend→CJO 正文图验收；四个输入已经由官方 v1.1.3 编译、解码。 */
class CfirSourceExpressionExportTest : AbstractCfirSourceExpressionExportTest(CfirParser.Psi)

/** LightTree 复用同一份原生源码、诊断 harness 和官方结构断言。 */
class CfirSourceExpressionExportLightTreeTest : AbstractCfirSourceExpressionExportTest(CfirParser.LightTree)

/**
 * 官方证据见 source-expression-export/FIXTURE.md 和 build/source-expression-export-v113 的 JSON。
 * 期望来自固定 v113 产物，数字索引只在实际图内解释；不从本仓 actual 生成期望。
 * 此处只配置语言版本、标准库和普通包模式，DEFAULT 完全由真实前端解析接口正文获得。
 */
abstract class AbstractCfirSourceExpressionExportTest(parser: CfirParser) : AbstractCfirDiagnosticTestBase(parser) {
    private lateinit var fixture: SourceExpressionFixture
    private var defaultFactsCase = false

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            LANGUAGE_VERSION with "1.1.3"
            CJMP_MODE with CfirCjmpMode.NONE
            RUN_PIPELINE_TILL with TestPhase.FRONTEND
            +WITH_STDLIB
        }
        builder.firHandlersStep {
            useHandlers({ services: TestServices ->
                if (defaultFactsCase) SourceDefaultAssignmentFactsHandler(services, parser)
                else SourceExpressionCjoHandler(services, parser, fixture)
            })
        }
    }

    @Test
    fun declarationsCalls() = runCase(SourceExpressionFixture.DECLARATIONS_CALLS)

    @Test
    fun controlFlow() = runCase(SourceExpressionFixture.CONTROL_FLOW)

    @Test
    fun operatorsConversions() = runCase(SourceExpressionFixture.OPERATORS_CONVERSIONS)

    @Test
    fun exceptionsResources() = runCase(SourceExpressionFixture.EXCEPTIONS_RESOURCES)

    /** 新 v113 正例逐条区分缺省、非空 Invalid 和上界/接口实例化；原 SOURCE14 保持原断言。 */
    @Test
    fun referenceInstantiation() = runCase(SourceExpressionFixture.REFERENCE_INSTANTIATION)

    @Test
    fun referenceDerivedInstantiation() = runCase(SourceExpressionFixture.REFERENCE_DERIVED_INSTANTIATION)

    /** 官方八个 const-owner defaults；来源 facts 与槽选择独立于完整 SOURCE body writer。 */
    @Test
    fun originalDefaultAssignmentFactsAndFinalSelection() {
        defaultFactsCase = true
        val path = Path.of("cfir/analysis-tests/testData/diagnostics2/common-specific/source-expression-export/defaultFacts.cj")
        val digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)))
        assertEquals("a9dfb1d5611ecc9da2ca302ccaa0361eff545090bbc53b22303e59d3eeaa2116", digest, "必须逐字复制 authority.json 已验证的 defaultFacts.cj")
        runTest(path.toString())
    }

    private fun runCase(case: SourceExpressionFixture) {
        fixture = case
        val path = Path.of("cfir/analysis-tests/testData/diagnostics2/common-specific/source-expression-export/${case.fileName}")
        val digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)))
        assertEquals(case.sourceSha256, digest, "输入必须保持官方成功编译的原始源码；改变输入须重新取得官方证据")
        runTest(path.toString())
    }
}

/**
 * SOURCE-only handler 使用相同公共 frontend 工厂、模块标准库根与 1.1.3 语言配置。
 * 它没有 CJO 输出路径；公开分析和 FINAL 参数槽选择不能借由 body codec、helper 或常量求值补事实。
 */
@OptIn(CompilerConfiguration.Internals::class)
private class SourceDefaultAssignmentFactsHandler(
    testServices: TestServices,
    private val parser: CfirParser,
) : CfirAnalysisHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer> = emptyList()
    private var checkedModules = 0

    override fun processModule(module: TestModule, info: CfirOutputArtifact) {
        assertEquals(0, checkedModules)
        val part = info.partsForDependsOnModules.single { it.module === module }
        val input = part.firFilesByTestFile.keys.filter { !it.isAdditional }.single()
        assertTrue(testServices.globalMetadataInfoHandler.getExistingMetaInfosForFile(input).isEmpty(), "官方 SOURCE 正例没有错误或警告")
        val original = input.originalFile.toPath().toAbsolutePath().normalize()
        assertEquals(Files.readString(original, Charsets.UTF_8), testServices.sourceFileProvider.getContentOfSourceFile(input))
        val disposable = Disposer.newDisposable("source-default-assignment-facts")
        try {
            val moduleConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
            val diagnostics = DiagnosticsCollectorImpl()
            val messages = SourceExpressionMessages()
            val configuration = CompilerConfiguration().apply {
                moduleName = module.name
                languageVersionSettings = moduleConfiguration.languageVersionSettings
                useLightTree = parser == CfirParser.LightTree
                cjmpMode = module.directives[CJMP_MODE].single()
                diagnosticsCollector = diagnostics
                messageCollector = messages
                moduleConfiguration.classpathRoots.forEach { addClasspathRoot(it.path) }
                addCangJieSourceRoot(original.toString())
            }
            assertNull(configuration.cjoOutputDirectory, "八槽 SOURCE 事实测试不触发正文写出")
            val artifact = CfirFrontendPipelinePhase.executePhase(ConfigurationPipelineArtifact(configuration, disposable))
            assertTrue(diagnostics.diagnostics.isEmpty(), diagnostics.diagnostics.joinToString("\n") { "${it.factoryName}: ${it.renderMessage()}" })
            assertTrue(messages.entries.none { it.first.isWarning || it.first.isError }, messages.entries.toString())
            val output = checkNotNull(artifact) { "原默认参数正例必须完成正式 SOURCE frontend" }.frontendOutput.outputs.single()
            assertNotSame(part.session, output.session)
            assertOriginalSourceFacts(output.fir.single())
            checkedModules++
        } finally {
            Disposer.dispose(disposable)
        }
    }

    private fun assertOriginalSourceFacts(file: CfirFile) {
        assertEquals("source_default_facts", file.packageDirective.packageFqName.asString())
        val owner = file.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "constDefaults" }
        assertTrue(owner.status.isConst, "owner 的 const 合法性不能提升原 assignment.isConst")
        val parameters = owner.valueParameters
        assertEquals(OriginalDefaultFact.entries.map { it.parameterName }, parameters.map { it.name.asString() })
        val settings = CfirExportAnalysisSettings(canInline = false)
        val canonical = CfirSourceGraphProducer.produce(listOf(file))
        val analysis = CfirExportAnalysis.analyze(listOf(file), settings = settings, canonicalSources = listOf(canonical))
        assertTrue(analysis.statusOf(owner).isConst)
        assertEquals(parameters, analysis.valueParametersOf(owner))
        val syntaxBefore = parameters.map { parameter ->
            checkNotNull(parameter.defaultAssignmentSyntaxFacts) { "${parameter.name}: raw builder 未保存 defaultAssignmentSyntaxFacts" }
        }
        val factsBefore = parameters.map { analysis.defaultAssignmentFactsOf(it) }
        parameters.zip(OriginalDefaultFact.entries).forEach { (parameter, expected) ->
            assertRawSyntax(parameter, expected)
            val syntax = checkNotNull(parameter.defaultAssignmentSyntaxFacts)
            assertEquals(expected.isLiteral, syntax.isLiteral, "${expected.parameterName}: 原 literal 身份")
            assertEquals(expected.isConst, syntax.literalConst, "${expected.parameterName}: 原 literal/Paren const 来源")
            val facts = analysis.defaultAssignmentFactsOf(parameter)
            assertEquals(expected.isLiteral, facts.assignmentIsLiteral)
            assertEquals(expected.isConst, facts.assignmentIsConst)
            assertFalse(facts.parameterIsToolAdded, "普通源参数没有节点 TOOL_ADD 位")
            assertFalse(analysis.precedingFactsOf(parameter).isToolAdded)
            val root = checkNotNull(analysis.defaultValueOf(parameter))
            assertSame(parameter.defaultValue, root, "冻结根必须是原 assignment，不是 helper 的正文或计算结果")
            val frozen = analysis.sourceElement(root)
            assertSame(root, frozen.identity)
            assertSame(root.source, frozen.source)
            assertEquals(PrimitiveTypeKind.INT64, (checkNotNull(frozen.type) as ConePrimitiveType).kind)
            assertTrue(analysis.sourceElement(parameter).children.any { it === root }, "冻结参数保留原 default assignment 边")
            val walked = mutableListOf<org.cangnova.cangjie.cfir.CfirElement>()
            analysis.walkSource(root, parameter) { node, _ -> walked += node.identity }
            assertTrue(walked.any { it === root })
            frozen.children.forEach { child -> assertTrue(walked.any { it === child }, "来源遍历不得漏掉原 root 的子边") }
        }
        // PARAMETER_DEFAULT 采用独立原生 Gate；关闭正文导出也必须保留原来的三个默认槽。
        for (exportContent in listOf(true, false)) {
            val selection = CjoDeclarationExportSelection.create(
                analysis,
                CjoExportOptions(exportContent = exportContent, analysisSettings = settings),
                isSuppressed = { analysis.isDoNotExport(it) },
                isMatchedCommon = { false }, // 本次是真实 NONE 包，没有 common 对应声明。
            )
            val selectedNames = mutableListOf<String>()
            parameters.zip(OriginalDefaultFact.entries).forEach { (parameter, expected) ->
                val selected = selection.selectSourceContent(parameter, CjoSourceSaveStage.FINAL)
                val content = selection.sourceContent(parameter)
                assertSame(selected, content, "首次 select 之后 sourceContent 只读已保存决定")
                assertEquals(CjoSourceSaveStage.FINAL, selection.sourceSaveStageOf(parameter))
                if (expected.hasRoot) {
                    val saved = checkNotNull(content) { "${expected.parameterName}: 原生 defaultVal 应有根" }
                    assertEquals(CjoSourceContentSlot.PARAMETER_DEFAULT, saved.slot)
                    assertEquals(CjoSourceSaveStage.FINAL, saved.stage)
                    assertSame(parameter, saved.owner)
                    assertSame(canonical.expressionOf(checkNotNull(analysis.defaultValueOf(parameter))), saved.root)
                    selectedNames += parameter.name.asString()
                } else {
                    assertNull(content, "${expected.parameterName}: const owner/helper 或可求值性不能扩大原 assignment Gate")
                }
                assertSame(content, selection.selectSourceContent(parameter, CjoSourceSaveStage.FINAL))
                assertSame(content, selection.sourceContent(parameter))
            }
            assertEquals(listOf("literal", "parenthesized", "negativeLiteral"), selectedNames)
        }
        parameters.forEachIndexed { index, parameter ->
            assertSame(syntaxBefore[index], parameter.defaultAssignmentSyntaxFacts, "选择阶段不得重写 raw 来源 payload")
            assertEquals(factsBefore[index], analysis.defaultAssignmentFactsOf(parameter))
        }
        val frozenOuter = file.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "frozenOuter" }
        assertTrue(analysis.isFrozenOf(frozenOuter), "header 必须保留实际 @Frozen 来源")
        assertFalse(analysis.statusOf(frozenOuter).isConst)
        val offset = frozenOuter.valueParameters.single { it.name.asString() == "offset" }
        assertFalse(analysis.defaultAssignmentFactsOf(offset).assignmentIsConst, "Frozen owner 也不能提升复合默认表达式的原 const 位")
        assertFalse(analysis.defaultAssignmentFactsOf(offset).assignmentIsLiteral)
        analysis.validateSourceUnchanged()
    }

    /** 两条 raw 使用同一源码树契约：直接负号的 numeric 子节点与带括号的一元运算必须可区分。 */
    private fun assertRawSyntax(parameter: CfirValueParameter, expected: OriginalDefaultFact) {
        val source = checkNotNull(parameter.source)
        assertEquals(CjNodeTypes.VALUE_PARAMETER, source.elementType)
        withSyntaxChildren(source, source.lighterASTNode) { children ->
            val equalsIndex = children.indexOfFirst { it.tokenType == CjTokens.EQ }
            assertTrue(equalsIndex >= 0)
            val root = children.drop(equalsIndex + 1).filter { it.tokenType != TokenType.WHITE_SPACE }.single()
            assertEquals(expected.syntaxKind, root.tokenType, "${expected.parameterName}: 原 assignment 语法形状")
            assertEquals(expected.expressionText, source.treeStructure.toString(root).toString())
            if (expected.operandKind != null) {
                withSyntaxChildren(source, root) { operands ->
                    val operand = operands.single { it.tokenType == expected.operandKind }
                    assertEquals(expected.operandText, source.treeStructure.toString(operand).toString())
                }
            }
        }
    }

    private inline fun withSyntaxChildren(source: CjSourceElement, node: LighterASTNode, action: (List<LighterASTNode>) -> Unit) {
        val reference = Ref<Array<LighterASTNode>>()
        val count = source.treeStructure.getChildren(node, reference)
        val children = checkNotNull(reference.get())
        try {
            action(children.take(count))
        } finally {
            source.treeStructure.disposeChildren(children, count)
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        assertEquals(1, checkedModules, "八个原 default assignment 必须通过真实 SOURCE 事实与选择验收")
    }
}

/** 原始 assignment 标志来自固定 v113 producer；defaultVal 是否有根来自 authority.json 的八行 SDK actual。 */
private enum class OriginalDefaultFact(
    val parameterName: String,
    val expressionText: String,
    val syntaxKind: IElementType,
    val isConst: Boolean,
    val isLiteral: Boolean,
    val hasRoot: Boolean,
    val operandKind: IElementType? = null,
    val operandText: String? = null,
) {
    LITERAL("literal", "7", CjNodeTypes.INTEGER_CONSTANT, true, true, true),
    PARENTHESIZED("parenthesized", "(7)", CjNodeTypes.PARENTHESIZED, true, false, true, CjNodeTypes.INTEGER_CONSTANT, "7"),
    NEGATIVE_LITERAL("negativeLiteral", "-7", CjNodeTypes.PREFIX_EXPRESSION, true, true, true, CjNodeTypes.INTEGER_CONSTANT, "7"),
    UNARY_EXPRESSION("unaryExpression", "-(7)", CjNodeTypes.PREFIX_EXPRESSION, false, false, false, CjNodeTypes.PARENTHESIZED, "(7)"),
    COMPOUND("compound", "1 + 2", CjNodeTypes.BINARY_EXPRESSION, false, false, false),
    COMPOUND_PAREN("compoundParen", "(1 + 2)", CjNodeTypes.PARENTHESIZED, false, false, false, CjNodeTypes.BINARY_EXPRESSION, "1 + 2"),
    INVOCATION("invocation", "fixedValue()", CjNodeTypes.CALL_EXPRESSION, false, false, false),
    CONSTANT_REFERENCE("constantReference", "globalValue", CjNodeTypes.REFERENCE_EXPRESSION, false, false, false),
}

/** 固定 SDK 成功产物的输入身份、表规模和接口物理成员；不是 SOURCE 选择器。 */
private enum class SourceExpressionFixture(
    val fileName: String,
    val packageName: String,
    val ownerName: String,
    val methods: List<String>,
    val declarationCount: Int,
    val expressionCount: Int,
    val sourceSha256: String,
) {
    DECLARATIONS_CALLS(
        "declarationsCalls.cj", "source_declarations_calls", "DeclarationsCalls",
        listOf("evaluate", "implicitValue", "explicitUnit"), 20, 32,
        "bf8f88e2398ba1f562f848ab0814f68cef90c7803b96c3b1bc6394d145ad9002",
    ),
    CONTROL_FLOW(
        "controlFlow.cj", "source_control_flow", "ControlFlow", listOf("walk", "choose"), 10, 73,
        "1d83b1b49f11f98adcb5c5a9e025dc1993ae396436633983624b5960c811930d",
    ),
    OPERATORS_CONVERSIONS(
        "operatorsConversions.cj", "source_operators_conversions", "OperatorsConversions",
        listOf("calculate", "inspect", "select", "predicate"), 22, 93,
        "5b237eb7082ec4d061f9cff7c341dfc8c39ee0af81018db489f0d5be0d2acb98",
    ),
    EXCEPTIONS_RESOURCES(
        "exceptionsResources.cj", "source_exceptions_resources", "ExceptionsResources",
        listOf("recover", "withResources", "withoutFinally"), 29, 163,
        "4a017a550e5cd25d828c2750872bf2c1053746bd5e064a0252f7de4cc49d4858",
    ),
    REFERENCE_INSTANTIATION(
        "referenceInstantiation.cj", "source_reference_instantiation", "ReferenceInstantiation",
        listOf("nonGeneric", "genericStructure", "genericCalls", "interfaceMatch", "genericInterfaceMatch",
            "concreteInterfaceMatch", "upperBound", "genericUpperBound", "concreteGenericUpperBound", "classUpperBound"),
        72, 154, "42aedd8b24eefeb2943813a02164f85738844f545fd6874bacea8fe8435aa09c",
    ),
    REFERENCE_DERIVED_INSTANTIATION(
        "referenceDerivedInstantiation.cj", "source_reference_derived_instantiation", "ReferenceDerivedInstantiation",
        listOf("derived", "parent", "derivedUpperBound", "parentUpperBound", "genericDerived", "genericDerivedUpperBound"),
        37, 84, "3246761fdf6b394c6a098d64f9ecd234be8062a130cafdc58b86cc8011733c3c",
    ),
}

/**
 * 沿用 StoredMemberInstantiationCjoHandler 的正式配置与 executePhase 调用。
 * 诊断先由原 inline harness 核验，再以新 session 编译同一原始文件并读取正式 writer 的 CJO；
 * SDK/builtin/stdlib 均通过现有工厂与模块环境配置加载，不制造 CFIR、helper 或导出属性。
 */
@OptIn(CompilerConfiguration.Internals::class)
private class SourceExpressionCjoHandler(
    testServices: TestServices,
    private val parser: CfirParser,
    private val fixture: SourceExpressionFixture,
) : CfirAnalysisHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer> = emptyList()
    private var checkedModules = 0

    override fun processModule(module: TestModule, info: CfirOutputArtifact) {
        assertEquals(0, checkedModules, "每个正例只编译一个独立包")
        val part = info.partsForDependsOnModules.single { it.module === module }
        val file = part.firFilesByTestFile.keys.filter { !it.isAdditional }.single()
        assertTrue(testServices.globalMetadataInfoHandler.getExistingMetaInfosForFile(file).isEmpty(), "官方正例没有内联诊断")
        val original = file.originalFile.toPath().toAbsolutePath().normalize()
        assertEquals(Files.readString(original, Charsets.UTF_8), testServices.sourceFileProvider.getContentOfSourceFile(file))
        val root = Files.createTempDirectory("source-expression-export-")
        val disposable = Disposer.newDisposable("source-expression-export")
        try {
            val moduleConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
            val diagnostics = DiagnosticsCollectorImpl()
            val messages = SourceExpressionMessages()
            val configuration = CompilerConfiguration().apply {
                moduleName = module.name
                languageVersionSettings = moduleConfiguration.languageVersionSettings
                useLightTree = parser == CfirParser.LightTree
                cjmpMode = module.directives[CJMP_MODE].single()
                diagnosticsCollector = diagnostics
                messageCollector = messages
                moduleConfiguration.classpathRoots.forEach { addClasspathRoot(it.path) }
                addCangJieSourceRoot(original.toString())
                cjoOutputDirectory = root.toString()
            }
            val artifact = CfirFrontendPipelinePhase.executePhase(ConfigurationPipelineArtifact(configuration, disposable))
            assertTrue(diagnostics.diagnostics.isEmpty(), diagnostics.diagnostics.joinToString("\n") { "${it.factoryName}: ${it.renderMessage()}" })
            assertFalse(messages.hasErrors(), messages.entries.joinToString("\n"))
            assertTrue(messages.entries.none { it.first.isWarning || it.first.isError }, messages.entries.toString())
            val output = checkNotNull(artifact) { "合法 SOURCE 必须完成正式 frontend 与 SAVE_CJO" }.frontendOutput.outputs.single()
            assertNotSame(part.session, output.session, "不能复用诊断 harness 的 SOURCE provider")
            val owner = output.fir.single().declarations.filterIsInstance<CfirInterface>().single { it.name.asString() == fixture.ownerName }
            val members = owner.declarations.filterIsInstance<CfirNamedFunction>()
            assertEquals(fixture.methods, members.map { it.name.asString() })
            assertTrue(members.all { it.status.isDefault }, "DEFAULT 必须来自真实接口正文语义")
            val cjo = root.resolve(CjoConstants.packageNameToPath(fixture.packageName))
            assertTrue(Files.isRegularFile(cjo), "只能读取正式 MetadataProducer/PackageWriter 实际写出的 CJO")
            val bytes = Files.readAllBytes(cjo)
            val graph = SourceExpressionGraph(Package.getRootAsPackage(ByteBuffer.wrap(bytes)))
            graph.assertFixture(fixture)
            CjoSourceGraphConsumptionAssertions.assertConsumed(configuration, disposable, root, cjo, owner, fixture.methods)
            assertArrayEquals(bytes, Files.readAllBytes(cjo), "结构检查不得改变产物")
            checkedModules++
        } finally {
            Disposer.dispose(disposable)
            root.toFile().deleteRecursively()
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        assertEquals(1, checkedModules, "每个原生正例必须实际完成 SOURCE→CJO，不跳过未建模的 IR")
    }
}

private class SourceExpressionMessages : MessageCollector {
    val entries = mutableListOf<Pair<CompilerMessageSeverity, String>>()
    override fun clear() = entries.clear()
    override fun hasErrors(): Boolean = entries.any { it.first.isError }
    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        entries += severity to message
    }
}

/**
 * 只读验收原生表及图边。UInt 始终表示当前实际表的 1-based ID；比较共享身份时直接比较 ID。
 * Block 和 VarPattern 的声明索引与普通表达式索引分别解释；Jump.mapExpr 只验证回边，不树递归。
 */
private class SourceExpressionGraph(private val pkg: Package) {
    private val declarationIds = (1..pkg.allDeclsLength).map { it.toUInt() }
    private val expressionIds = (1..pkg.allExprsLength).map { it.toUInt() }

    fun assertFixture(fixture: SourceExpressionFixture) {
        assertEquals(fixture.packageName, pkg.fullPkgName)
        val owner = declaration(fixture.ownerName, DeclKind.InterfaceDecl)
        val ownerInfo = declInfo(owner, DeclInfo.InterfaceInfo, InterfaceInfo())
        val methods = (0 until ownerInfo.bodyLength).map { ownerInfo.body(it) }
        assertEquals(fixture.methods, methods.map { decl(it).identifier })
        methods.forEach {
            assertTrue(hasAttribute(it, Attribute.DEFAULT))
            kind(body(it), ExprKind.Block)
            type(body(it), TypeKind.Nothing)
        }
        assertAll(
            { assertEquals(fixture.declarationCount, pkg.allDeclsLength, "固定 v113 正例完整声明闭包") },
            { assertEquals(fixture.expressionCount, pkg.allExprsLength, "固定 v113 正例完整正文图，包含 wrapper 与展开节点") },
            { assertGraphIntegrity() },
            {
                when (fixture) {
                    SourceExpressionFixture.DECLARATIONS_CALLS -> assertDeclarationsCalls()
                    SourceExpressionFixture.CONTROL_FLOW -> assertControlFlow()
                    SourceExpressionFixture.OPERATORS_CONVERSIONS -> assertOperatorsConversions()
                    SourceExpressionFixture.EXCEPTIONS_RESOURCES -> assertExceptionsResources()
                    SourceExpressionFixture.REFERENCE_INSTANTIATION -> assertReferenceInstantiation()
                    SourceExpressionFixture.REFERENCE_DERIVED_INSTANTIATION -> assertDerivedReferenceInstantiation()
                }
            },
        )
    }

    /** 官方 attempt-02 为154个表达式/72个声明，全部69条Ref分别为53/5/11三种状态。 */
    private fun assertReferenceInstantiation() {
        // Native SetDeclTy/SaveNominalTy：声明self与应用后的GenericCarrier<Int64>是不同角色。
        for ((name, kind) in listOf("GenericCarrier" to DeclKind.StructDecl, "GenericContract" to DeclKind.InterfaceDecl)) {
            val owner = declaration(name, kind)
            val generic = checkNotNull(decl(owner).generic)
            val selfType = semaType(decl(owner).type)
            assertEquals(1, selfType.typeArgsLength)
            val parameter = semaType(selfType.typeArgs(0))
            assertEquals(TypeKind.Generic, parameter.kind)
            val parameterInfo = checkNotNull(parameter.info(GenericTyInfo()) as? GenericTyInfo)
            assertEquals(generic.typeParameters(0), checkNotNull(parameterInfo.declPtr).index)
            val selfInfo = checkNotNull(selfType.info(CompositeTyInfo()) as? CompositeTyInfo)
            assertEquals(owner, checkNotNull(selfInfo.declPtr).index)
            assertFalse(selfInfo.isThisTy)
        }
        val references = expressionIds.filter { expr(it).infoType == ExprInfo.ReferenceInfo }.map { id ->
            id to exprInfo(id, ExprInfo.ReferenceInfo, ReferenceInfo())
        }
        assertEquals(69, references.size)
        assertEquals(53, references.count { it.second.matchedParentTy == 0u })
        val invalid = references.filter { it.second.matchedParentTy != 0u && semaType(it.second.matchedParentTy).kind == TypeKind.Invalid }
        assertEquals(5, invalid.size)
        assertEquals(1, invalid.map { it.second.matchedParentTy }.toSet().size, "真实InvalidTy共享一个已完成类型身份")
        val invalidType = semaType(invalid.first().second.matchedParentTy)
        assertEquals(SemaTyInfo.NONE, invalidType.infoType)
        assertEquals(0, invalidType.typeArgsLength)
        assertEquals(11, references.count { it.second.matchedParentTy != 0u && semaType(it.second.matchedParentTy).kind != TypeKind.Invalid })

        fun parentName(info: ReferenceInfo): String {
            if (info.matchedParentTy == 0u) return "Absent"
            val parent = semaType(info.matchedParentTy)
            if (parent.kind == TypeKind.Invalid) return "Invalid"
            assertEquals(SemaTyInfo.CompositeTyInfo, parent.infoType)
            val composite = checkNotNull(parent.info(CompositeTyInfo()) as? CompositeTyInfo)
            val target = checkNotNull(composite.declPtr)
            assertEquals(-2, target.pkgId)
            return checkNotNull(decl(target.index).identifier)
        }
        fun named(name: String): List<Pair<UInt, ReferenceInfo>> = references.filter { it.second.reference == name }
        fun parents(name: String, vararg expected: String) {
            assertEquals(expected.sorted(), named(name).map { parentName(it.second) }.sorted(), "${name}的全部调用/函数值引用")
            named(name).forEach { (_, reference) ->
                val target = checkNotNull(reference.target)
                assertEquals(-2, target.pkgId)
                assertEquals(name, decl(target.index).identifier)
            }
        }
        parents("ordinary", "Absent")
        parents("tag", "Absent")
        parents("plain", "Absent", "Absent", "PlainParent")
        parents("payload", "Absent")
        parents("echo", "Absent", "Invalid")
        parents("constant", "Invalid")
        parents("identity", "Absent", "Invalid", "Invalid")
        parents("generic", "Invalid")
        parents("accepted", "PlainContract", "PlainContract", "PlainContract")
        parents("fixed", "GenericContract", "GenericContract", "GenericContract")
        parents("supplied", "GenericContract", "GenericContract", "GenericContract", "GenericContract")
        named("identity").plus(named("generic")).forEach { (_, reference) ->
            assertEquals(1, reference.instTysLength)
            assertEquals(TypeKind.Int64, semaType(reference.instTys(0)).kind)
        }
        val genericOwner = checkNotNull(decl(declaration("genericUpperBound", DeclKind.FuncDecl)).generic)
        val u = (0 until genericOwner.typeParametersLength).map { genericOwner.typeParameters(it) }
            .single { decl(it).identifier == "U" }
        val genericParents = named("supplied").map { semaType(it.second.matchedParentTy) }
        assertEquals(3, genericParents.count { semaType(it.typeArgs(0)).kind == TypeKind.Int64 })
        val genericParent = genericParents.single { semaType(it.typeArgs(0)).kind == TypeKind.Generic }
        val genericArgument = checkNotNull(semaType(genericParent.typeArgs(0)).info(GenericTyInfo()) as? GenericTyInfo)
        assertEquals(-2, checkNotNull(genericArgument.declPtr).pkgId)
        assertEquals(u, checkNotNull(genericArgument.declPtr).index, "匹配父类型保留函数U，不能换成接口自身T")
        val payload = named("payload").single()
        assertEquals(TypeKind.Int64, semaType(expr(payload.first).type).kind)
        assertEquals(TypeKind.Generic, semaType(decl(checkNotNull(payload.second.target).index).type).kind,
            "泛型字段声明类型与应用后MemberAccess类型各自保留")
    }

    /** 固定v113：相同非泛型plain目标，在派生receiver、上界调用和函数值中拥有不同parent。 */
    private fun assertDerivedReferenceInstantiation() {
        val references = expressionIds.filter { expr(it).infoType == ExprInfo.ReferenceInfo }.map { id ->
            id to exprInfo(id, ExprInfo.ReferenceInfo, ReferenceInfo())
        }
        assertEquals(42, references.size)
        references.forEach { assertEquals(0, it.second.instTysLength) }
        val plain = references.filter { it.second.reference == "plain" }
        assertEquals(12, plain.size)
        assertEquals(1, plain.map { checkNotNull(it.second.target).index }.toSet().size, "调用与函数值共享原始非泛型成员身份")
        val callBases = expressionIds.filter { expr(it).kind == ExprKind.CallExpr }.map { operands(it).first() }.toSet()
        val calls = plain.filter { it.first in callBases }
        val values = plain.filter { it.first !in callBases }
        assertEquals(6, calls.size)
        assertEquals(6, values.size)
        assertEquals(1, calls.count { it.second.matchedParentTy == 0u })
        assertEquals(3, values.count { it.second.matchedParentTy == 0u })
        val invalid = calls.filter { it.second.matchedParentTy != 0u && semaType(it.second.matchedParentTy).kind == TypeKind.Invalid }
        assertEquals(2, invalid.size)
        assertEquals(1, invalid.map { it.second.matchedParentTy }.toSet().size)
        assertEquals(34, references.count { it.second.matchedParentTy == 0u })
        assertTrue(references.filter { it.second.reference != "plain" }.all { it.second.matchedParentTy == 0u })

        fun parent(info: ReferenceInfo): String {
            val type = semaType(info.matchedParentTy)
            assertEquals(TypeKind.Class, type.kind)
            val composite = checkNotNull(type.info(CompositeTyInfo()) as? CompositeTyInfo)
            val target = checkNotNull(composite.declPtr)
            assertEquals(-2, target.pkgId)
            return checkNotNull(decl(target.index).identifier)
        }
        val matchedCalls = calls.filter { it.second.matchedParentTy != 0u && semaType(it.second.matchedParentTy).kind != TypeKind.Invalid }
        assertEquals(listOf("Derived", "Derived", "PlainParent"), matchedCalls.map { parent(it.second) }.sorted())
        val matchedValues = values.filter { it.second.matchedParentTy != 0u }
        assertEquals(listOf("PlainParent", "PlainParent", "PlainParent"), matchedValues.map { parent(it.second) })
        val derivedParents = matchedCalls.filter { parent(it.second) == "Derived" }.map { semaType(it.second.matchedParentTy) }
        assertEquals(1, derivedParents.count { semaType(it.typeArgs(0)).kind == TypeKind.Int64 })
        val generic = derivedParents.single { semaType(it.typeArgs(0)).kind == TypeKind.Generic }
        val argument = checkNotNull(semaType(generic.typeArgs(0)).info(GenericTyInfo()) as? GenericTyInfo)
        val upperBound = checkNotNull(decl(declaration("genericDerivedUpperBound", DeclKind.FuncDecl)).generic)
        val u = (0 until upperBound.typeParametersLength).map { upperBound.typeParameters(it) }.single { decl(it).identifier == "U" }
        assertEquals(u, checkNotNull(argument.declPtr).index, "Derived<U>中的U属于当前上界函数")
    }

    private fun assertDeclarationsCalls() {
        val evaluate = declaration("evaluate", DeclKind.FuncDecl)
        val evaluateParams = parameters(funcBody(evaluate))
        assertEquals(listOf("receiver", "input"), evaluateParams.map { decl(it).identifier })
        val slots = block(body(evaluate), d("step"), d("local"), d("nested", DeclKind.FuncDecl), d("transform"), e(ExprKind.AssignExpr), e(ExprKind.ReturnExpr))
        val helper = declaration("privateStep", DeclKind.FuncDecl)
        assertTrue(hasAttribute(helper, Attribute.PRIVATE))
        assertFalse(hasAttribute(helper, Attribute.DEFAULT))
        assertTrue(decl(helper).isTopLevel)
        assertEquals(0u, body(helper), "普通 private 被引用只提升声明，不扩大正文选择")
        reference(initializer(slots[0]), helper, "privateStep")
        reference(initializer(slots[1]), evaluateParams[1], "input")
        assertTrue(declInfo(slots[1], DeclInfo.VarInfo, VarInfo()).isVar)
        assertEquals(0u, body(slots[2]), "本官方产物的 nested 正文不导出")
        val lambda = initializer(slots[3])
        kind(lambda, ExprKind.LambdaExpr)
        val lambdaBody = exprInfo(lambda, ExprInfo.LambdaInfo, LambdaInfo()).funcBody
        type(lambdaBody.body, TypeKind.Nothing)
        val lambdaParameters = parameters(lambdaBody)
        assertEquals(listOf("value"), lambdaParameters.map { decl(it).identifier })
        val nestedCall = returnValue(block(lambdaBody.body, e(ExprKind.ReturnExpr)).single())
        val nestedArgs = call(nestedCall, ExprKind.RefExpr, 1, CallKind.CallDeclaredFunction)
        reference(operands(nestedCall)[0], slots[2], "nested")
        reference(nestedArgs.single(), lambdaParameters.single(), "value")
        assignment(slots[4], OperatorKind.Assign, false)
        reference(operands(slots[4])[0], slots[1], "local")
        val stepCall = operands(slots[4])[1]
        val stepArgs = call(stepCall, ExprKind.RefExpr, 1, CallKind.CallStructCreation)
        reference(operands(stepCall)[0], slots[0], "step")
        reference(stepArgs.single(), slots[1], "local")
        val addCall = returnValue(slots[5])
        type(addCall, TypeKind.Int64)
        val addArgs = call(addCall, ExprKind.MemberAccess, 1, CallKind.CallDeclaredFunction)
        val add = declaration("add", DeclKind.FuncDecl)
        assertEquals(0u, body(add))
        val access = operands(addCall)[0]
        reference(access, add, "add")
        reference(operands(access).single(), evaluateParams[0], "receiver")
        val transformCall = addArgs.single()
        val transformArgs = call(transformCall, ExprKind.RefExpr, 1, CallKind.CallStructCreation)
        reference(operands(transformCall)[0], slots[3], "transform")
        reference(transformArgs.single(), slots[1], "local")

        val implicit = declaration("implicitValue", DeclKind.FuncDecl)
        val implicitSlots = block(body(implicit), d("copy"), e(ExprKind.ReturnExpr))
        reference(initializer(implicitSlots[0]), parameters(funcBody(implicit)).single(), "input")
        val copy = returnValue(implicitSlots[1])
        type(copy, TypeKind.Int64)
        reference(copy, implicitSlots[0], "copy")
        val unitReturn = block(body(declaration("explicitUnit", DeclKind.FuncDecl)), e(ExprKind.ReturnExpr)).single()
        literal(returnValue(unitReturn), LitConstKind.Unit, "()", TypeKind.Unit)
    }

    private fun assertControlFlow() {
        val walk = declaration("walk", DeclKind.FuncDecl)
        val limit = parameters(funcBody(walk)).single()
        val slots = block(body(walk), d("total"), d("index"), e(ExprKind.WhileExpr), e(ExprKind.DoWhileExpr), e(ExprKind.ForInExpr), e(ExprKind.ReturnExpr))
        literal(initializer(slots[0]), LitConstKind.Integer, "0", TypeKind.Int64)
        literal(initializer(slots[1]), LitConstKind.Integer, "0", TypeKind.Int64)
        val whileOperands = operands(slots[2])
        binary(whileOperands[0], OperatorKind.LT, slots[1], limit)
        val whileSlots = block(whileOperands[1], e(ExprKind.AssignExpr), e(ExprKind.IfExpr), e(ExprKind.IfExpr), e(ExprKind.AssignExpr))
        assignment(whileSlots[0], OperatorKind.AddAssign, true)
        reference(operands(whileSlots[0])[0], slots[1], "index")
        literal(operands(whileSlots[0])[1], LitConstKind.Integer, "1", TypeKind.Int64)
        assertJumpBranch(whileSlots[1], slots[2], false, false)
        assertJumpBranch(whileSlots[2], slots[2], true, false)
        assignment(whileSlots[3], OperatorKind.AddAssign, true)
        reference(operands(whileSlots[3])[0], slots[0], "total")
        reference(operands(whileSlots[3])[1], slots[1], "index")
        // v113 的 DoWhile 顺序是 body、condition，与 While 相反。
        val doOperands = operands(slots[3])
        val decrement = block(doOperands[0], e(ExprKind.AssignExpr)).single()
        assignment(decrement, OperatorKind.SubtractAssign, true)
        reference(operands(decrement)[0], slots[0], "total")
        literal(operands(decrement)[1], LitConstKind.Integer, "1", TypeKind.Int64)
        binary(doOperands[1], OperatorKind.GT, slots[0], limit)
        val forInfo = exprInfo(slots[4], ExprInfo.ForInInfo, ForInInfo())
        assertEquals(ForInKind.Range, forInfo.forInKind)
        val element = binding(checkNotNull(forInfo.pattern), "element")
        val forOperands = operands(slots[4])
        assertEquals(0u, forOperands[1], "缺失 guard 仍占第 2 槽")
        val rangeSlots = block(forOperands[0], d("\$iter-element"), d("\$stop-compiler"), e(ExprKind.AssignExpr), e(ExprKind.BinaryExpr))
        reference(initializer(element), rangeSlots[0], "\$iter-element")
        reference(initializer(rangeSlots[1]), limit, "limit")
        val forSlots = block(forOperands[2], e(ExprKind.IfExpr), e(ExprKind.IfExpr))
        assertJumpBranch(forSlots[0], slots[4], false, true)
        assertJumpBranch(forSlots[1], slots[4], true, false)
        val increment = block(operands(forSlots[0])[2], e(ExprKind.AssignExpr), e(ExprKind.LitConstExpr))[0]
        assignment(increment, OperatorKind.AddAssign, true)
        reference(operands(increment)[0], slots[0], "total")
        reference(operands(increment)[1], element, "element")
        reference(returnValue(slots[5]), slots[0], "total")

        val choose = declaration("choose", DeclKind.FuncDecl)
        val branch = returnValue(block(body(choose), e(ExprKind.ReturnExpr)).single())
        kind(branch, ExprKind.IfExpr)
        type(branch, TypeKind.Int64)
        reference(operands(branch)[0], parameters(funcBody(choose)).single(), "flag")
        literal(block(operands(branch)[1], e(ExprKind.LitConstExpr)).single(), LitConstKind.Integer, "1", TypeKind.Int64)
        literal(block(operands(branch)[2], e(ExprKind.LitConstExpr)).single(), LitConstKind.Integer, "2", TypeKind.Int64)
        assertEquals(4, expressionIds.count { expr(it).kind == ExprKind.JumpExpr })
    }

    private fun assertJumpBranch(branch: UInt, loop: UInt, isBreak: Boolean, hasElse: Boolean) {
        kind(branch, ExprKind.IfExpr)
        assertEquals(3, operands(branch).size)
        if (hasElse) kind(operands(branch)[2], ExprKind.Block) else assertEquals(0u, operands(branch)[2])
        val jump = block(operands(branch)[1], e(ExprKind.JumpExpr)).single()
        type(operands(branch)[1], TypeKind.Nothing)
        assertEquals(isBreak, exprInfo(jump, ExprInfo.JumpInfo, JumpInfo()).isBreak)
        assertEquals(loop, expr(jump).mapExpr, "break/continue 必须指向所在循环的同一实际 ID")
    }

    private fun assertOperatorsConversions() {
        val calculate = declaration("calculate", DeclKind.FuncDecl)
        val calculateParams = parameters(funcBody(calculate))
        val slots = block(body(calculate), d("accumulator"), e(ExprKind.AssignExpr), e(ExprKind.AssignExpr), d("pair"), d("items"), e(ExprKind.CallExpr), d("converted"), e(ExprKind.ReturnExpr))
        reference(initializer(slots[0]), calculateParams[0], "input")
        assignment(slots[1], OperatorKind.Assign, false)
        reference(operands(slots[1])[0], slots[0], "accumulator")
        val negative = operands(slots[1])[1]
        kind(negative, ExprKind.UnaryExpr)
        assertEquals(OperatorKind.Subtract, exprInfo(negative, ExprInfo.UnaryInfo, UnaryInfo()).op)
        reference(operands(negative).single(), slots[0], "accumulator")
        assignment(slots[2], OperatorKind.AddAssign, true)
        literal(operands(slots[2])[1], LitConstKind.Integer, "2", TypeKind.Int64)
        val pair = initializer(slots[3])
        kind(pair, ExprKind.TupleLit)
        reference(operands(pair)[0], slots[0], "accumulator")
        val not = operands(pair)[1]
        kind(not, ExprKind.UnaryExpr)
        assertEquals(OperatorKind.Not, exprInfo(not, ExprInfo.UnaryInfo, UnaryInfo()).op)
        literal(operands(not).single(), LitConstKind.Bool, "false", TypeKind.Bool)
        val array = initializer(slots[4])
        kind(array, ExprKind.ArrayLit)
        val arrayInfo = exprInfo(array, ExprInfo.ArrayInfo, ArrayInfo())
        assertFalse(arrayInfo.isValueArray)
        fullId(checkNotNull(arrayInfo.initFunc))
        assertEquals("_CNat5ArrayIG_E6<init>HA0_G_ll", checkNotNull(arrayInfo.initFunc).decl)
        tupleAccess(operands(array)[0], slots[3], "0")
        arrayAccess(operands(array)[1], calculateParams[1], "values", listOf("0"))
        val setArgs = arrayAccess(slots[5], calculateParams[1], "values", listOf("0", null))
        arrayAccess(setArgs[1], slots[4], "items", listOf("1"))
        val conversion = initializer(slots[6])
        kind(conversion, ExprKind.TypeConvExpr)
        type(conversion, TypeKind.Int32)
        tupleAccess(operands(conversion).single(), slots[3], "0")
        val sum = returnValue(slots[7])
        kind(sum, ExprKind.BinaryExpr)
        assertEquals(OperatorKind.Add, exprInfo(sum, ExprInfo.BinaryInfo, BinaryInfo()).op)
        val widen = operands(sum)[0]
        kind(widen, ExprKind.TypeConvExpr)
        type(widen, TypeKind.Int64)
        reference(operands(widen).single(), slots[6], "converted")
        arrayAccess(operands(sum)[1], slots[4], "items", listOf("0"))

        val inspect = declaration("inspect", DeclKind.FuncDecl)
        val inspectValue = parameters(funcBody(inspect)).single()
        val result = returnValue(block(body(inspect), e(ExprKind.ReturnExpr)).single())
        kind(result, ExprKind.TupleLit)
        assertEquals(2, operands(result).size)
        val isMatch = operands(result)[0]
        val asMatch = operands(result)[1]
        assertConversionMatch(isMatch, inspectValue)
        assertConversionMatch(asMatch, inspectValue)
        type(isMatch, TypeKind.Bool)
        type(asMatch, TypeKind.Enum)
        val isCases = operands(isMatch).drop(1)
        val isPattern = casePattern(isCases[0])
        assertTypePattern(isPattern, "ConversionLeaf")
        assertEquals(PatternKind.WildcardPattern, checkNotNull(isPattern.patterns(0)).kind)
        literal(block(operands(isCases[0])[1], e(ExprKind.LitConstExpr)).single(), LitConstKind.Bool, "true", TypeKind.Bool)
        literal(block(operands(isCases[1])[1], e(ExprKind.LitConstExpr)).single(), LitConstKind.Bool, "false", TypeKind.Bool)
        val asCases = operands(asMatch).drop(1)
        val asPattern = casePattern(asCases[0])
        assertTypePattern(asPattern, "ConversionLeaf")
        val bound = binding(checkNotNull(asPattern.patterns(0)), "v-compiler")
        val some = block(operands(asCases[0])[1], e(ExprKind.CallExpr)).single()
        val someArgs = call(some, ExprKind.RefExpr, 1, CallKind.CallDeclaredFunction)
        externalReference(operands(some)[0], "Some", "_CNat6OptionIG_E4SomeHG_")
        reference(someArgs.single(), bound, "v-compiler")
        externalReference(block(operands(asCases[1])[1], e(ExprKind.RefExpr)).single(), "None", "_CNat6OptionIG_E4NoneKE")

        val select = declaration("select", DeclKind.FuncDecl)
        val selectSlots = block(body(select), d("", DeclKind.VarWithPatternDecl), e(ExprKind.ReturnExpr))
        val destructuring = declInfo(selectSlots[0], DeclInfo.VarWithPatternInfo, VarWithPatternInfo())
        reference(destructuring.initializer, parameters(funcBody(select)).single(), "pair")
        val pattern = checkNotNull(destructuring.irrefutablePattern)
        assertEquals(PatternKind.TuplePattern, pattern.kind)
        assertEquals(2, pattern.patternsLength)
        val left = binding(checkNotNull(pattern.patterns(0)), "left")
        val right = binding(checkNotNull(pattern.patterns(1)), "right")
        assertNotEquals(left, right)
        val match = returnValue(selectSlots[1])
        kind(match, ExprKind.MatchExpr)
        assertTrue(exprInfo(match, ExprInfo.MatchInfo, MatchInfo()).matchMode)
        reference(operands(match)[0], left, "left")
        val firstCase = operands(match)[1]
        val constPattern = casePattern(firstCase)
        assertEquals(PatternKind.ConstPattern, constPattern.kind)
        literal(constPattern.exprs(0), LitConstKind.Integer, "0", TypeKind.Int64)
        reference(block(operands(firstCase)[1], e(ExprKind.RefExpr)).single(), right, "right")
        val lastCase = operands(match)[2]
        assertEquals(PatternKind.WildcardPattern, casePattern(lastCase).kind)
        reference(block(operands(lastCase)[1], e(ExprKind.RefExpr)).single(), left, "left")
        listOf(firstCase, lastCase).forEach { assertEquals(0u, operands(it)[0], "case 缺失 guard 保留槽") }

        val predicate = declaration("predicate", DeclKind.FuncDecl)
        val predicateMatch = returnValue(block(body(predicate), e(ExprKind.ReturnExpr)).single())
        kind(predicateMatch, ExprKind.MatchExpr)
        assertFalse(exprInfo(predicateMatch, ExprInfo.MatchInfo, MatchInfo()).matchMode)
        assertEquals(2, operands(predicateMatch).size, "无 selector 的 match 从 MatchCaseOther 开始")
        val predicateCases = operands(predicateMatch)
        predicateCases.forEach { kind(it, ExprKind.MatchCaseOther) }
        val condition = operands(predicateCases[0])[0]
        kind(condition, ExprKind.BinaryExpr)
        assertEquals(OperatorKind.GT, exprInfo(condition, ExprInfo.BinaryInfo, BinaryInfo()).op)
        reference(operands(condition)[0], parameters(funcBody(predicate)).single(), "input")
        kind(operands(predicateCases[1])[0], ExprKind.WildcardExpr)
        literal(block(operands(predicateCases[0])[1], e(ExprKind.LitConstExpr)).single(), LitConstKind.Integer, "1", TypeKind.Int64)
        literal(block(operands(predicateCases[1])[1], e(ExprKind.LitConstExpr)).single(), LitConstKind.Integer, "0", TypeKind.Int64)
    }

    private fun assertConversionMatch(id: UInt, value: UInt) {
        kind(id, ExprKind.MatchExpr)
        assertTrue(exprInfo(id, ExprInfo.MatchInfo, MatchInfo()).matchMode)
        assertEquals(3, operands(id).size)
        reference(operands(id)[0], value, "value")
        operands(id).drop(1).forEach {
            kind(it, ExprKind.MatchCase)
            assertEquals(0u, operands(it)[0])
        }
        assertEquals(PatternKind.WildcardPattern, casePattern(operands(id)[2]).kind)
    }

    private fun assertTypePattern(pattern: Pattern, className: String) {
        assertEquals(PatternKind.TypePattern, pattern.kind)
        assertTrue(pattern.needRuntimeTypeCheck)
        assertEquals(1, pattern.patternsLength)
        assertEquals(1, pattern.typesLength)
        val ty = semaType(pattern.types(0))
        assertEquals(TypeKind.Class, ty.kind)
        val target = checkNotNull((checkNotNull(ty.info(CompositeTyInfo())) as CompositeTyInfo).declPtr)
        assertEquals(-2, target.pkgId)
        assertEquals(declaration(className, DeclKind.ClassDecl), target.index)
    }

    private fun assertExceptionsResources() {
        val recover = declaration("recover", DeclKind.FuncDecl)
        val recoverSlots = block(body(recover), d("result"), e(ExprKind.TryExpr), e(ExprKind.ReturnExpr))
        val recoverTry = operands(recoverSlots[1])
        val trySlots = block(recoverTry[0], e(ExprKind.IfExpr), e(ExprKind.AssignExpr), e(ExprKind.LitConstExpr))
        reference(operands(trySlots[0])[0], parameters(funcBody(recover)).single(), "fail")
        assertEquals(0u, operands(trySlots[0])[2])
        val thrown = block(operands(trySlots[0])[1], e(ExprKind.ThrowExpr)).single()
        val exception = operands(thrown).single()
        val exceptionArgs = call(exception, ExprKind.RefExpr, 1, CallKind.CallObjectCreation)
        externalReference(operands(exception)[0], "Exception", "_CNat9Exception6<init>HRNat6StringE")
        literal(exceptionArgs.single(), LitConstKind.String, "source body", TypeKind.Struct)
        assertCatchBinding(recoverSlots[1])
        val catchSlots = block(recoverTry[1], e(ExprKind.AssignExpr), e(ExprKind.LitConstExpr))
        reference(operands(catchSlots[0])[0], recoverSlots[0], "result")
        literal(operands(catchSlots[0])[1], LitConstKind.Integer, "2", TypeKind.Int64)
        val finallyAssign = block(recoverTry[2], e(ExprKind.AssignExpr)).single()
        assignment(finallyAssign, OperatorKind.AddAssign, true)
        literal(operands(finallyAssign)[1], LitConstKind.Integer, "10", TypeKind.Int64)
        reference(returnValue(recoverSlots[2]), recoverSlots[0], "result")

        val resourceFunction = declaration("withResources", DeclKind.FuncDecl)
        val resourceSlots = block(body(resourceFunction), d("result"), e(ExprKind.TryExpr), e(ExprKind.ReturnExpr))
        val outerTry = operands(resourceSlots[1])
        // 官方 AfterTypeCheck 将资源移入 Block；先 first，再 second，关闭顺序相反。
        val firstBlock = block(outerTry[0], d("v-freshExc"), d("first"), e(ExprKind.TryExpr))
        val secondBlock = block(operands(firstBlock[2])[0], d("v-freshExc"), d("second"), e(ExprKind.TryExpr))
        val first = firstBlock[1]
        val second = secondBlock[1]
        assertNotEquals(first, second)
        val constructor = declarationIds.single { decl(it).identifier == "init" && funcBody(it).retType == decl(first).type }
        for (resource in listOf(first, second)) {
            val creation = initializer(resource)
            call(creation, ExprKind.RefExpr, 0, CallKind.CallObjectCreation)
            reference(operands(creation)[0], constructor, "SourceResource")
        }
        val resourceTry = secondBlock[2]
        val resourceBody = block(operands(resourceTry)[0], e(ExprKind.AssignExpr), e(ExprKind.IfExpr), e(ExprKind.LitConstExpr))
        val sum = operands(resourceBody[0])[1]
        assertEquals(OperatorKind.Add, exprInfo(sum, ExprInfo.BinaryInfo, BinaryInfo()).op)
        listOf(first, second).forEachIndexed { index, receiver ->
            val readCall = operands(sum)[index]
            call(readCall, ExprKind.MemberAccess, 0, CallKind.CallDeclaredFunction)
            member(operands(readCall)[0], declaration("read", DeclKind.FuncDecl), receiver, decl(receiver).identifier!!)
        }
        assertCloseSequence(operands(resourceTry)[2], second)
        assertCloseSequence(operands(firstBlock[2])[2], first)
        assertCatchBinding(resourceSlots[1])
        kind(outerTry[1], ExprKind.Block)
        kind(outerTry[2], ExprKind.Block)
        reference(returnValue(resourceSlots[2]), resourceSlots[0], "result")
        listOf("read", "isClosed", "close").forEach { assertEquals(0u, body(declaration(it, DeclKind.FuncDecl))) }
        assertEquals(0u, body(constructor))
        val tries = expressionIds.filter { expr(it).kind == ExprKind.TryExpr }
        assertEquals(7, tries.size, "保留资源关闭异常处理与重新抛出形成的全部嵌套 try")
        tries.forEach { assertEquals(0, exprInfo(it, ExprInfo.TryInfo, TryInfo()).resourcesLength, "本批官方 resourceSpec 已全部展开") }
        assertEquals(4, expressionIds.count { expr(it).kind == ExprKind.MatchExpr }, "保留资源异常 Some/None 展开图")
        expressionIds.filter { expr(it).kind == ExprKind.MatchExpr }.forEach { assertResourceExceptionMatch(it) }
        assertEquals(8, expressionIds.count { expr(it).kind == ExprKind.ThrowExpr }, "保留原 throw 和资源关闭的重新抛出")

        val without = declaration("withoutFinally", DeclKind.FuncDecl)
        val withoutTry = returnValue(block(body(without), e(ExprKind.ReturnExpr)).single())
        kind(withoutTry, ExprKind.TryExpr)
        assertEquals(0u, operands(withoutTry)[2], "finally 缺失也必须保留最后一槽")
        literal(block(operands(withoutTry)[0], e(ExprKind.LitConstExpr)).single(), LitConstKind.Integer, "4", TypeKind.Int64)
        literal(block(operands(withoutTry)[1], e(ExprKind.LitConstExpr)).single(), LitConstKind.Integer, "5", TypeKind.Int64)
        assertCatchBinding(withoutTry)
    }

    private fun assertCatchBinding(id: UInt) {
        val info = exprInfo(id, ExprInfo.TryInfo, TryInfo())
        assertEquals(1, info.patternsLength)
        val pattern = checkNotNull(info.patterns(0))
        assertEquals(PatternKind.ExceptTypePattern, pattern.kind)
        val error = binding(checkNotNull(pattern.patterns(0)), "error")
        assertEquals(0u, initializer(error), "catch 绑定由模式建立，不伪造 initializer")
    }

    /** 资源异常的 Some 模式绑定与 rethrow 共用声明 ID；EnumPattern.exprs 则是表达式 ID。 */
    private fun assertResourceExceptionMatch(id: UInt) {
        assertTrue(exprInfo(id, ExprInfo.MatchInfo, MatchInfo()).matchMode)
        assertEquals(3, operands(id).size)
        val someCase = operands(id)[1]
        val noneCase = operands(id)[2]
        assertEquals(0u, operands(someCase)[0])
        assertEquals(0u, operands(noneCase)[0])
        val somePattern = casePattern(someCase)
        val nonePattern = casePattern(noneCase)
        assertEquals(PatternKind.EnumPattern, somePattern.kind)
        assertEquals(PatternKind.EnumPattern, nonePattern.kind)
        assertEquals(1, somePattern.patternsLength)
        assertEquals(0, nonePattern.patternsLength)
        assertEquals(1, somePattern.exprsLength)
        assertEquals(1, nonePattern.exprsLength)
        externalReference(somePattern.exprs(0), "Some", "_CNat6OptionIG_E4SomeHG_")
        externalReference(nonePattern.exprs(0), "None", "_CNat6OptionIG_E4NoneKE")
        val bound = binding(checkNotNull(somePattern.patterns(0)), "v-compiler")
        val rethrow = block(operands(someCase)[1], e(ExprKind.ThrowExpr)).single()
        reference(operands(rethrow).single(), bound, "v-compiler")
    }

    private fun assertCloseSequence(finallyBody: UInt, receiver: UInt) {
        val nestedTry = block(finallyBody, e(ExprKind.TryExpr), e(ExprKind.MatchExpr))[0]
        val branch = block(operands(nestedTry)[0], e(ExprKind.IfExpr)).single()
        val condition = operands(branch)[0]
        kind(condition, ExprKind.UnaryExpr)
        assertEquals(OperatorKind.Not, exprInfo(condition, ExprInfo.UnaryInfo, UnaryInfo()).op)
        val closedCall = operands(condition).single()
        call(closedCall, ExprKind.MemberAccess, 0, CallKind.CallDeclaredFunction)
        member(operands(closedCall)[0], declaration("isClosed", DeclKind.FuncDecl), receiver, decl(receiver).identifier!!)
        val closeCall = block(operands(branch)[1], e(ExprKind.CallExpr)).single()
        call(closeCall, ExprKind.MemberAccess, 0, CallKind.CallDeclaredFunction)
        member(operands(closeCall)[0], declaration("close", DeclKind.FuncDecl), receiver, decl(receiver).identifier!!)
    }

    private fun tupleAccess(id: UInt, receiver: UInt, index: String) {
        kind(id, ExprKind.SubscriptExpr)
        assertTrue(exprInfo(id, ExprInfo.SubscriptInfo, SubscriptInfo()).isTupleAccess)
        reference(operands(id)[0], receiver, "pair")
        literal(operands(id)[1], LitConstKind.Integer, index, TypeKind.Int64)
    }

    private fun arrayAccess(id: UInt, receiver: UInt, name: String, indices: List<String?>): List<UInt> {
        val arguments = call(id, ExprKind.MemberAccess, indices.size, CallKind.CallDeclaredFunction)
        val access = operands(id)[0]
        externalReference(access, "[]", if (indices.size == 1) "_CNat5ArrayIG_EixHl" else "_CNat5ArrayIG_EixHlG_")
        reference(operands(access).single(), receiver, name)
        val info = exprInfo(access, ExprInfo.ReferenceInfo, ReferenceInfo())
        assertTrue(info.matchedParentTy > 0u, "保留官方成员实例化 matchedParentTy，即使该语义类型为 Invalid")
        indices.forEachIndexed { index, value -> if (value != null) literal(arguments[index], LitConstKind.Integer, value, TypeKind.Int64) }
        return arguments
    }

    /** 验证完整实际图的 union、类型、槽及特殊边，遗漏的 pattern/initializer/lambda 图不能当作通过。 */
    private fun assertGraphIntegrity() {
        val seenExpressions = mutableSetOf<UInt>()
        val seenDeclarations = mutableSetOf<UInt>()
        lateinit var visitExpression: (UInt) -> Unit
        lateinit var visitDeclaration: (UInt) -> Unit
        lateinit var visitPattern: (Pattern) -> Unit

        fun visitFunction(body: FuncBody) {
            semaType(body.retType)
            parameters(body).forEach { visitDeclaration(it) }
            if (body.body != 0u) visitExpression(body.body)
        }

        visitPattern = { pattern ->
            assertNotEquals(PatternKind.InvalidPattern, pattern.kind)
            (0 until pattern.typesLength).forEach { semaType(pattern.types(it)) }
            (0 until pattern.patternsLength).forEach { visitPattern(checkNotNull(pattern.patterns(it))) }
            (0 until pattern.exprsLength).forEach {
                if (pattern.kind == PatternKind.VarPattern) visitDeclaration(pattern.exprs(it))
                else visitExpression(pattern.exprs(it))
            }
            if (pattern.kind == PatternKind.VarPattern) {
                assertEquals(1, pattern.exprsLength)
                assertEquals(DeclKind.VarDecl, decl(pattern.exprs(0)).kind)
                assertEquals(decl(pattern.exprs(0)).type, pattern.types(0))
            }
        }
        visitDeclaration = { id ->
            if (seenDeclarations.add(id)) {
                val declaration = decl(id)
                semaType(declaration.type)
                when (declaration.kind) {
                    DeclKind.FuncDecl -> visitFunction(funcBody(id))
                    DeclKind.FuncParam -> assertEquals(0u, declInfo(id, DeclInfo.ParamInfo, ParamInfo()).defaultVal)
                    DeclKind.VarDecl -> initializer(id).takeIf { it != 0u }?.let { visitExpression(it) }
                    DeclKind.VarWithPatternDecl -> {
                        val info = declInfo(id, DeclInfo.VarWithPatternInfo, VarWithPatternInfo())
                        visitPattern(checkNotNull(info.irrefutablePattern))
                        visitExpression(info.initializer)
                    }
                    DeclKind.ClassDecl, DeclKind.InterfaceDecl, DeclKind.StructDecl -> Unit
                    DeclKind.GenericParamDecl -> {
                        val type = semaType(declaration.type)
                        assertEquals(TypeKind.Generic, type.kind)
                        assertEquals(SemaTyInfo.GenericTyInfo, type.infoType)
                        val info = checkNotNull(type.info(GenericTyInfo()) as? GenericTyInfo)
                        assertEquals(-2, checkNotNull(info.declPtr).pkgId)
                        assertEquals(id, checkNotNull(info.declPtr).index)
                        (0 until info.upperBoundsLength).forEach { semaType(info.upperBounds(it)) }
                    }
                    else -> fail<Unit>("此批官方 fixture 没有这种声明：${declaration.kind}")
                }
            }
        }
        visitExpression = { id ->
            if (seenExpressions.add(id)) {
                val expression = expr(id)
                semaType(expression.type)
                val children = operands(id)
                val infoKind = when (expression.kind) {
                    ExprKind.Block -> ExprInfo.BlockInfo
                    ExprKind.InvalidExpr -> ExprInfo.FuncArgInfo
                    ExprKind.RefExpr, ExprKind.MemberAccess -> ExprInfo.ReferenceInfo
                    ExprKind.CallExpr -> ExprInfo.CallInfo
                    ExprKind.ReturnExpr, ExprKind.ThrowExpr, ExprKind.TypeConvExpr, ExprKind.IfExpr,
                    ExprKind.WhileExpr, ExprKind.DoWhileExpr, ExprKind.TupleLit, ExprKind.WildcardExpr,
                    ExprKind.MatchCaseOther -> ExprInfo.NONE
                    ExprKind.LitConstExpr -> ExprInfo.LitConstInfo
                    ExprKind.UnaryExpr -> ExprInfo.UnaryInfo
                    ExprKind.BinaryExpr -> ExprInfo.BinaryInfo
                    ExprKind.AssignExpr -> ExprInfo.AssignInfo
                    ExprKind.SubscriptExpr -> ExprInfo.SubscriptInfo
                    ExprKind.ArrayLit -> ExprInfo.ArrayInfo
                    ExprKind.JumpExpr -> ExprInfo.JumpInfo
                    ExprKind.LambdaExpr -> ExprInfo.LambdaInfo
                    ExprKind.ForInExpr -> ExprInfo.ForInInfo
                    ExprKind.MatchExpr -> ExprInfo.MatchInfo
                    ExprKind.MatchCase -> ExprInfo.MatchCaseInfo
                    ExprKind.TryExpr -> ExprInfo.TryInfo
                    else -> fail<UByte>("此批官方 fixture 没有这种表达式：${expression.kind}，不能以缺失值跳过")
                }
                assertEquals(infoKind, expression.infoType, "Expr#$id union discriminator")
                if (expression.kind == ExprKind.Block) {
                    val info = exprInfo(id, ExprInfo.BlockInfo, BlockInfo())
                    assertEquals(children.size + 1, info.isExprLength, "最后一位是 unsafe 标志")
                    assertFalse(info.isExpr(children.size), "这四个官方输入不含 unsafe Block")
                    children.forEachIndexed { index, child -> if (info.isExpr(index)) visitExpression(child) else visitDeclaration(child) }
                } else {
                    val arity = when (expression.kind) {
                        ExprKind.ReturnExpr, ExprKind.ThrowExpr, ExprKind.TypeConvExpr, ExprKind.UnaryExpr,
                        ExprKind.MemberAccess, ExprKind.InvalidExpr -> 1
                        ExprKind.BinaryExpr, ExprKind.AssignExpr, ExprKind.SubscriptExpr, ExprKind.WhileExpr,
                        ExprKind.DoWhileExpr, ExprKind.MatchCase, ExprKind.MatchCaseOther -> 2
                        ExprKind.IfExpr, ExprKind.ForInExpr -> 3
                        ExprKind.RefExpr, ExprKind.LitConstExpr, ExprKind.JumpExpr, ExprKind.LambdaExpr, ExprKind.WildcardExpr -> 0
                        else -> null // Call、Tuple、Array、Match、Try 的向量长度由各自真实 payload 决定。
                    }
                    if (arity != null) assertEquals(arity, children.size, "Expr#$id ordered slots")
                    children.forEachIndexed { index, child ->
                        val optional = when (expression.kind) {
                            ExprKind.IfExpr -> index == 2
                            ExprKind.ForInExpr -> index == 1
                            ExprKind.MatchCase -> index == 0
                            ExprKind.TryExpr -> index == children.lastIndex
                            else -> false
                        }
                        if (child == 0u) assertTrue(optional, "Expr#$id 非可缺失槽 $index 为 0") else visitExpression(child)
                    }
                }
                when (expression.kind) {
                    ExprKind.RefExpr, ExprKind.MemberAccess -> {
                        val info = exprInfo(id, ExprInfo.ReferenceInfo, ReferenceInfo())
                        val target = checkNotNull(info.target)
                        fullId(target)
                        if (target.pkgId == -2 && decl(target.index).kind in listOf(DeclKind.VarDecl, DeclKind.FuncParam) &&
                            (expression.kind == ExprKind.RefExpr || semaType(decl(target.index).type).kind != TypeKind.Generic)) {
                            assertEquals(decl(target.index).identifier, info.reference)
                            assertEquals(decl(target.index).type, expression.type)
                        }
                        (0 until info.instTysLength).forEach { semaType(info.instTys(it)) }
                        if (info.matchedParentTy != 0u) semaType(info.matchedParentTy)
                    }
                    ExprKind.ReturnExpr, ExprKind.ThrowExpr -> type(id, TypeKind.Nothing)
                    ExprKind.JumpExpr -> {
                        type(id, TypeKind.Nothing)
                        assertTrue(expr(expression.mapExpr).kind in listOf(ExprKind.WhileExpr, ExprKind.DoWhileExpr, ExprKind.ForInExpr))
                    }
                    ExprKind.LambdaExpr -> {
                        val body = exprInfo(id, ExprInfo.LambdaInfo, LambdaInfo()).funcBody
                        kind(body.body, ExprKind.Block)
                        type(body.body, TypeKind.Nothing)
                        visitFunction(body)
                    }
                    ExprKind.CallExpr -> {
                        assertTrue(children.isNotEmpty(), "call 必须有 baseFunc")
                        children.drop(1).forEach { argumentValue(it) }
                    }
                    ExprKind.InvalidExpr -> argumentValue(id)
                    ExprKind.ForInExpr -> visitPattern(checkNotNull(exprInfo(id, ExprInfo.ForInInfo, ForInInfo()).pattern))
                    ExprKind.MatchCase -> {
                        val info = exprInfo(id, ExprInfo.MatchCaseInfo, MatchCaseInfo())
                        assertTrue(info.patternsLength > 0)
                        (0 until info.patternsLength).forEach { visitPattern(checkNotNull(info.patterns(it))) }
                    }
                    ExprKind.MatchExpr -> {
                        val withSelector = exprInfo(id, ExprInfo.MatchInfo, MatchInfo()).matchMode
                        assertTrue(children.size >= 2)
                        (if (withSelector) children.drop(1) else children).forEach {
                            kind(it, if (withSelector) ExprKind.MatchCase else ExprKind.MatchCaseOther)
                        }
                    }
                    ExprKind.TryExpr -> {
                        val info = exprInfo(id, ExprInfo.TryInfo, TryInfo())
                        assertEquals(info.patternsLength + 2, children.size, "try、逐个 catch、最后 finally 槽")
                        (0 until info.patternsLength).forEach { visitPattern(checkNotNull(info.patterns(it))) }
                        (0 until info.resourcesLength).forEach { fullId(checkNotNull(info.resources(it))) }
                    }
                    ExprKind.ArrayLit -> fullId(checkNotNull(exprInfo(id, ExprInfo.ArrayInfo, ArrayInfo()).initFunc))
                }
            }
        }
        declarationIds.forEach { visitDeclaration(it) }
        assertEquals(expressionIds.toSet(), seenExpressions, "正文图还必须覆盖 initializer、lambda、pattern 的非 operands 边")
        expressionIds.filter { expr(it).kind == ExprKind.JumpExpr }.forEach {
            assertTrue(expr(it).mapExpr in seenExpressions, "loop 回边指向已保存的同一个循环")
        }
    }

    private data class Slot(val name: String?, val kind: UShort)
    private fun d(name: String, kind: UShort = DeclKind.VarDecl): Slot = Slot(name, kind)
    private fun e(kind: UShort): Slot = Slot(null, kind)

    private fun block(id: UInt, vararg expected: Slot): List<UInt> {
        kind(id, ExprKind.Block)
        val slots = operands(id)
        val info = exprInfo(id, ExprInfo.BlockInfo, BlockInfo())
        assertEquals(expected.size, slots.size)
        assertEquals(slots.size + 1, info.isExprLength)
        assertFalse(info.isExpr(slots.size))
        expected.forEachIndexed { index, slot ->
            assertEquals(slot.name == null, info.isExpr(index), "Block#$id 槽 $index 的 d/e 索引表")
            if (slot.name == null) kind(slots[index], slot.kind) else {
                assertEquals(slot.kind, decl(slots[index]).kind)
                assertEquals(slot.name, decl(slots[index]).identifier)
            }
        }
        return slots
    }

    private fun decl(id: UInt): Decl {
        assertTrue(id > 0u && id.toLong() <= pkg.allDeclsLength, "声明索引 $id 越界或缺失")
        return checkNotNull(pkg.allDecls(id.toInt() - 1))
    }

    private fun expr(id: UInt): Expr {
        assertTrue(id > 0u && id.toLong() <= pkg.allExprsLength, "表达式索引 $id 越界或缺失")
        return checkNotNull(pkg.allExprs(id.toInt() - 1))
    }

    private fun declaration(name: String, kind: UShort): UInt = declarationIds.single { decl(it).identifier == name && decl(it).kind == kind }
    private fun operands(id: UInt): List<UInt> = expr(id).let { expression -> (0 until expression.operandsLength).map { expression.operands(it) } }
    private fun kind(id: UInt, expected: UShort) = assertEquals(expected, expr(id).kind, "Expr#$id kind")

    private fun semaType(id: UInt): SemaTy {
        assertTrue(id > 0u && id.toLong() <= pkg.allTypesLength, "类型索引 $id 越界或缺失")
        return checkNotNull(pkg.allTypes(id.toInt() - 1))
    }

    private fun type(id: UInt, expected: UShort) = assertEquals(expected, semaType(expr(id).type).kind, "Expr#$id 语义类型")

    private fun <T : com.google.flatbuffers.Table> exprInfo(id: UInt, expected: UByte, table: T): T {
        assertEquals(expected, expr(id).infoType, "Expr#$id info")
        assertNotNull(expr(id).info(table))
        return table
    }

    private fun <T : com.google.flatbuffers.Table> declInfo(id: UInt, expected: UByte, table: T): T {
        assertEquals(expected, decl(id).infoType, "Decl#$id info")
        assertNotNull(decl(id).info(table))
        return table
    }

    private fun funcBody(id: UInt): FuncBody = checkNotNull(declInfo(id, DeclInfo.FuncInfo, FuncInfo()).funcBody)
    private fun body(id: UInt): UInt = funcBody(id).body
    private fun initializer(id: UInt): UInt = declInfo(id, DeclInfo.VarInfo, VarInfo()).initializer

    private fun parameters(body: FuncBody): List<UInt> {
        assertEquals(1, body.paramListsLength)
        val list = checkNotNull(body.paramLists(0))
        assertEquals(list.paramsLength, list.desugarsLength)
        return (0 until list.paramsLength).map {
            assertEquals(0u, list.desugars(it), "这四个官方 fixture 没有默认实参 helper")
            val id = list.params(it)
            assertEquals(DeclKind.FuncParam, decl(id).kind)
            id
        }
    }

    private fun fullId(id: FullId) {
        if (id.pkgId == -2) decl(id.index) else {
            assertTrue(id.pkgId >= 0 && id.pkgId < pkg.importsLength, "外部 FullId 必须使用实际 import 编号")
            assertEquals(0u, id.index)
            assertFalse(id.decl.isNullOrEmpty(), "外部引用必须保留原生声明身份")
        }
    }

    private fun reference(id: UInt, target: UInt, name: String) {
        val info = exprInfo(id, ExprInfo.ReferenceInfo, ReferenceInfo())
        assertEquals(name, info.reference)
        val fullId = checkNotNull(info.target)
        assertEquals(-2, fullId.pkgId, "当前包 FullId 是 -2，不能使用 Position 的 0")
        assertEquals(target, fullId.index, "同一声明的共享身份")
        assertEquals(0, info.instTysLength, "本批当前包引用不含泛型实例化实参")
        assertEquals(0u, info.matchedParentTy)
    }

    private fun externalReference(id: UInt, name: String, nativeName: String) {
        val info = exprInfo(id, ExprInfo.ReferenceInfo, ReferenceInfo())
        assertEquals(name, info.reference)
        val target = checkNotNull(info.target)
        fullId(target)
        assertNotEquals(-2, target.pkgId)
        assertEquals(nativeName, target.decl)
    }

    private fun member(id: UInt, target: UInt, receiver: UInt, receiverName: String) {
        kind(id, ExprKind.MemberAccess)
        reference(id, target, checkNotNull(decl(target).identifier))
        reference(operands(id).single(), receiver, receiverName)
    }

    private fun returnValue(id: UInt): UInt {
        kind(id, ExprKind.ReturnExpr)
        type(id, TypeKind.Nothing)
        return operands(id).single()
    }

    private fun argumentValue(id: UInt): UInt {
        kind(id, ExprKind.InvalidExpr)
        val info = exprInfo(id, ExprInfo.FuncArgInfo, FuncArgInfo())
        assertFalse(info.isDefaultVal)
        assertFalse(info.withInout)
        val value = operands(id).single()
        assertEquals(expr(value).type, expr(id).type, "显式参数 wrapper 保留参数类型")
        return value
    }

    private fun call(id: UInt, baseKind: UShort, argumentCount: Int, callKind: UByte): List<UInt> {
        kind(id, ExprKind.CallExpr)
        val info = exprInfo(id, ExprInfo.CallInfo, CallInfo())
        assertEquals(callKind, info.callKind)
        assertFalse(info.hasSideEffect)
        val slots = operands(id)
        assertEquals(argumentCount + 1, slots.size)
        kind(slots[0], baseKind)
        return slots.drop(1).map { argumentValue(it) }
    }

    private fun assignment(id: UInt, op: UByte, compound: Boolean) {
        kind(id, ExprKind.AssignExpr)
        val info = exprInfo(id, ExprInfo.AssignInfo, AssignInfo())
        assertEquals(op, info.op)
        assertEquals(compound, info.isCompound)
    }

    private fun binary(id: UInt, op: UByte, left: UInt, right: UInt) {
        kind(id, ExprKind.BinaryExpr)
        assertEquals(op, exprInfo(id, ExprInfo.BinaryInfo, BinaryInfo()).op)
        reference(operands(id)[0], left, checkNotNull(decl(left).identifier))
        reference(operands(id)[1], right, checkNotNull(decl(right).identifier))
    }

    private fun literal(id: UInt, constKind: UByte, text: String, typeKind: UShort) {
        kind(id, ExprKind.LitConstExpr)
        type(id, typeKind)
        val info = exprInfo(id, ExprInfo.LitConstInfo, LitConstInfo())
        assertEquals(constKind, info.constKind)
        assertEquals(text, info.strValue)
    }

    private fun casePattern(id: UInt): Pattern {
        kind(id, ExprKind.MatchCase)
        val info = exprInfo(id, ExprInfo.MatchCaseInfo, MatchCaseInfo())
        assertEquals(1, info.patternsLength)
        return checkNotNull(info.patterns(0))
    }

    private fun binding(pattern: Pattern, name: String): UInt {
        assertEquals(PatternKind.VarPattern, pattern.kind)
        assertEquals(1, pattern.exprsLength)
        assertEquals(1, pattern.typesLength)
        val id = pattern.exprs(0)
        assertEquals(DeclKind.VarDecl, decl(id).kind, "VarPattern.exprs 是声明索引，不是表达式索引")
        assertEquals(name, decl(id).identifier)
        assertEquals(decl(id).type, pattern.types(0))
        return id
    }

    private fun hasAttribute(id: UInt, attribute: Attribute): Boolean {
        val word = attribute.ordinal / 64
        assertTrue(word < decl(id).attributesLength, "必须保留官方属性向量")
        return decl(id).attributes(word) and (1uL shl (attribute.ordinal % 64)) != 0uL
    }
}
