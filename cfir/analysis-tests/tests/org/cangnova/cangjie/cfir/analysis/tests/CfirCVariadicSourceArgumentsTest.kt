package org.cangnova.cangjie.cfir.analysis.tests

import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCfirDiagnosticTestBase
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpMode
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvedArgumentList
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedBuiltinOperatorReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysis
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportSourcePayload
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportTypePayload
import org.cangnova.cangjie.cfir.resolve.source.CfirSourceGraphProducer
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.source.CfirSourceCallKind
import org.cangnova.cangjie.cfir.source.CfirSourceExpressionPayload
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeCStringType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.*
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
import org.cangnova.cangjie.source.text
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
import org.cangnova.cangjie.test.frontend.cfirDiagnosticCollectorService
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.compilerConfigurationProvider
import org.cangnova.cangjie.test.services.sourceFileProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Collections
import java.util.HexFormat
import java.util.IdentityHashMap

/** PSI：从真实 foreign 声明及默认正文验收 C ellipsis 的完成实参与 SOURCE 调用。 */
class CfirCVariadicSourceArgumentsTest : AbstractCfirCVariadicSourceArgumentsTest(CfirParser.Psi)

/** LightTree 使用同一官方核验源码、诊断位置及完成角色合同。 */
class CfirCVariadicSourceArgumentsLightTreeTest : AbstractCfirCVariadicSourceArgumentsTest(CfirParser.LightTree)

/**
 * 框架沿固定 Kotlin 3c7a5041 的 AbstractFirDiagnosticTestBase/FirAnalysisHandler 和
 * FirCallCompletionResultsWriterTransformer 的正式完成参数列表路径。
 * 仓颉差异来自固定 native 14276740 的 ParseDecl、PreCheck、TypeCheckCall、
 * TypeCheckUtil，详见 docs/cjmp-source-helper-20261117/c-variadic-source-authority-20261119.md。
 * 五份独立输入已实际使用 SDK 1.1.3 的 staticlib/JSON diagnostics/dump-ast 取证；
 * 正例成功，四个负例分别只有 arity、CType mismatch、直接／延迟 Unit 实参错误。
 */
abstract class AbstractCfirCVariadicSourceArgumentsTest(parser: CfirParser) : AbstractCfirDiagnosticTestBase(parser) {
    private lateinit var case: CVariadicSourceCase

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            LANGUAGE_VERSION with "1.1.3"
            CJMP_MODE with CfirCjmpMode.NONE
            RUN_PIPELINE_TILL with TestPhase.FRONTEND
            +WITH_STDLIB
        }
        builder.firHandlersStep {
            useHandlers({ services: TestServices -> CVariadicSourceArgumentsHandler(services, parser, case) })
        }
    }

    @Test
    fun completedArgumentsPreserveFixedExtrasAndDeclaredSourceKind() = runCase(CVariadicSourceCase.COMPLETED)

    @Test
    fun missingFixedArgumentsRemainArityErrors() = runCase(CVariadicSourceCase.MISSING_FIXED)

    @Test
    fun ordinaryClassExtraDoesNotSatisfyCType() = runCase(CVariadicSourceCase.NON_CTYPE)

    @Test
    fun directUnitExtraHasItsOwnCallDiagnostic() = runCase(CVariadicSourceCase.UNIT_EXTRA)

    /** C01：Unit 只能从正常嵌套泛型调用完成后裁决，不能仅检查直接 literal。 */
    @Test
    fun c01DeferredUnitExtraIsRejectedAfterNestedGenericCompletion() = runCase(CVariadicSourceCase.DEFERRED_UNIT)

    private fun runCase(value: CVariadicSourceCase) {
        case = value
        assertEquals(value.nativeSourceSha256, cVariadicSourceSha256(cVariadicSourceText(value).toByteArray(Charsets.UTF_8)),
            "inline 标记以外的原源码必须与官方 cjc 输入逐字一致")
        runTest(value.source.toString())
    }
}

/** 负例坐标为官方 JSON MainHint 的半开范围，不由本库错误树或当前诊断反算。 */
private enum class CVariadicSourceCase(
    val fileName: String,
    val nativeSourceSha256: String,
    val diagnostic: String? = null,
    val argumentCount: Int = 0,
    val diagnosticLine: Int = 0,
    val beginColumn: Int = 0,
    val endColumn: Int = 0,
) {
    COMPLETED("completedArguments", "EA54E0DD04F79C3967B4E4DA8D5E0BB85AB059F54492FBA050ADA7897E3B16D2"),
    MISSING_FIXED("missingFixed", "799C7E0C427D4792319B71D34DBF6E9D476CB8029D62BA47001112E8D75D5A33",
        "WRONG_NUMBER_OF_ARGUMENTS", 2, 7, 40, 55),
    NON_CTYPE("nonCType", "F3420FA5BBC9F074EF4489C05F18578C208481C9717DAF1CCB12B05F20BD5ACC",
        "ARGUMENT_TYPE_MISMATCH", 4, 9, 62, 67),
    UNIT_EXTRA("unitExtra", "1D10322A3107019F1406B0150ACC97B577857FBB04069C003FCB70814691B900",
        "UNIT_CANNOT_BE_CFUNC_ARGUMENT", 4, 7, 62, 63),
    DEFERRED_UNIT("deferredUnit", "79EF1327DA1DF2E0767275143C23BA55A7109EA00D7189A58B5C781ECFA256C3",
        "UNIT_CANNOT_BE_CFUNC_ARGUMENT", 4, 12, 62, 63);

    val source: Path
        get() = Path.of("cfir/analysis-tests/testData/diagnostics2/common-specific/source-c-ellipsis/$fileName.cj")
}

/** 诊断仍由标准 handler 验收；正例再进入公开 frontend 工厂与生产 SOURCE/capture 链路。 */
@OptIn(CompilerConfiguration.Internals::class)
private class CVariadicSourceArgumentsHandler(
    testServices: TestServices,
    private val parser: CfirParser,
    private val case: CVariadicSourceCase,
) : CfirAnalysisHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer> = emptyList()
    private var checkedModules = 0

    override fun processModule(module: TestModule, info: CfirOutputArtifact) {
        assertEquals(0, checkedModules)
        val part = info.partsForDependsOnModules.single { it.module === module }
        val input = part.firFilesByTestFile.keys.single { !it.isAdditional }
        assertEquals(case.source.toAbsolutePath().normalize(), input.originalFile.toPath().toAbsolutePath().normalize())
        val source = testServices.sourceFileProvider.getContentOfSourceFile(input)
        assertEquals(case.nativeSourceSha256, cVariadicSourceSha256(source.toByteArray(Charsets.UTF_8)))
        if (case != CVariadicSourceCase.COMPLETED) {
            val file = part.firFilesByTestFile.getValue(input)
            val diagnostics = testServices.cfirDiagnosticCollectorService.getFrontendDiagnosticsForModule(info).getValue(file)
            // inline 名称不带前缀；factoryName 是带 CFIR_ 的完整内部 ID，精确比较该 API 表示。
            assertEquals(setOf("CFIR_" + checkNotNull(case.diagnostic)), diagnostics.map { it.factoryName }.toSet())
            val lineStart = source.split('\n').take(case.diagnosticLine - 1).sumOf { it.length + 1 }
            diagnostics.forEach { diagnostic ->
                assertEquals(lineStart + case.beginColumn - 1, diagnostic.firstRange.startOffset)
                assertEquals(lineStart + case.endColumn - 1, diagnostic.firstRange.endOffset)
            }
            val call = cVariadicSourceElements(file).filterIsInstance<CfirFunctionCall>().single {
                (it.calleeReference as? CfirNamedReference)?.name?.asString() == "cEllipsisFixed3"
            }
            assertEquals(case.argumentCount, call.argumentList.arguments.size, "错误调用也保留真实 fixed/extra 实参")
            if (case == CVariadicSourceCase.DEFERRED_UNIT) assertDeferredUnitCompletion(file, call)
            checkedModules++
            return
        }
        val disposable = Disposer.newDisposable("c-ellipsis-source-completion")
        try {
            val moduleConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
            val diagnostics = DiagnosticsCollectorImpl()
            val messages = CVariadicSourceMessages()
            val configuration = CompilerConfiguration().apply {
                moduleName = module.name
                languageVersionSettings = moduleConfiguration.languageVersionSettings
                useLightTree = parser == CfirParser.LightTree
                cjmpMode = CfirCjmpMode.NONE
                diagnosticsCollector = diagnostics
                messageCollector = messages
                moduleConfiguration.classpathRoots.forEach { addClasspathRoot(it.path) }
                addCangJieSourceRoot(input.originalFile.toPath().toAbsolutePath().normalize().toString())
            }
            val artifact = checkNotNull(CfirFrontendPipelinePhase.executePhase(ConfigurationPipelineArtifact(configuration, disposable))) {
                "合法 C ellipsis SOURCE 必须真正完成 frontend，不能注入候选或拼造 ArgList"
            }
            assertTrue(diagnostics.diagnostics.isEmpty(), diagnostics.diagnostics.joinToString("\n") { it.renderMessage() })
            assertTrue(messages.entries.none { it.first.isError || it.first.isWarning }, messages.entries.joinToString("\n"))
            val output = artifact.frontendOutput.outputs.single()
            assertNotSame(part.session, output.session)
            assertCompletedSource(output.fir.single())
            checkedModules++
        } finally {
            Disposer.dispose(disposable)
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        assertEquals(1, checkedModules, "不能跳过未完成的 C ellipsis 调用或错误诊断")
    }

    /** 官方 4_sema_ast 中第四个 FuncArg 和其嵌套 preserve 调用均已是 Unit。 */
    private fun assertDeferredUnitCompletion(file: CfirFile, outer: CfirFunctionCall) {
        val preserve = file.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "preserve" }
        assertEquals(1, preserve.typeParameters.size)
        val nested = outer.argumentList.arguments[3] as CfirFunctionCall
        assertSame(preserve, (nested.calleeReference as CfirResolvedNamedReference).resolvedSymbol.cfir)
        assertEquals(UNIT, cVariadicPrimitiveKind(checkNotNull(nested.coneTypeOrNull)))
        val arguments = nested.argumentList as CfirResolvedArgumentList
        assertEquals(1, arguments.arguments.size)
        assertEquals(1, arguments.mapping.size)
        assertSame(arguments.arguments.single(), arguments.mapping.keys.single())
        assertSame(preserve.valueParameters.single(), arguments.mapping.values.single())
        assertNull(arguments.cVariadicArguments, "内层是普通泛型调用，不能借用外层 C ellipsis 角色")
        assertFalse(arguments.usesCangjieVariadicCall)
        assertEquals(UNIT, cVariadicPrimitiveKind(checkNotNull(arguments.arguments.single().coneTypeOrNull)))
        val applied = nested.calleeReference as CfirResolvedAppliedCallableReference
        assertEquals(UNIT, cVariadicPrimitiveKind(checkNotNull(applied.substitutedReturnType)))
        assertEquals(listOf(UNIT), applied.substitutedParameterTypes.map { cVariadicPrimitiveKind(it) })
    }

    private fun assertCompletedSource(file: CfirFile) {
        val foreign = file.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "cEllipsisFixed3" }
        assertEquals(3, foreign.valueParameters.size, "ellipsis 没有 fake formal")
        assertTrue(foreign.hasVariableLenArg)
        val owner = file.declarations.filterIsInstance<CfirInterface>().single { it.name.asString() == "CEllipsisSource" }
        val methods = owner.declarations.filterIsInstance<CfirNamedFunction>().associateBy { it.name.asString() }
        assertEquals(setOf("zero", "one", "heterogeneous", "nestedLiterals"), methods.keys)
        assertTrue(methods.values.all { it.status.isDefault })
        val canonical = CfirSourceGraphProducer.produce(listOf(file))
        val analysis = CfirExportAnalysis.analyze(listOf(file), canonicalSources = listOf(canonical))
        val signature = analysis.declarationTypeOf(foreign) as ConeFunctionType
        assertCSignature(signature)
        val frozen = analysis.typeElement(signature, foreign).payload as CfirExportTypePayload.Function
        assertTrue(frozen.isC)
        assertTrue(frozen.hasVariableLenArg)
        assertEquals(listOf(INT32, INT64, INT32), frozen.parameters.map { cVariadicPrimitiveKind(it) })
        assertEquals(INT32, cVariadicPrimitiveKind(frozen.returnType))
        val expectedKinds = linkedMapOf(
            "zero" to listOf("Int32", "Int64", "Int32"),
            "one" to listOf("Int32", "Int64", "Int32", "Int32"),
            "heterogeneous" to listOf("Int32", "Int64", "Int32", "Int8", "Bool", "Float32", "CString", "CPointer<Int32>"),
            "nestedLiterals" to listOf("Int32", "Int64", "Int32", "Int64", "Bool", "Float64"),
        )
        for ((name, kinds) in expectedKinds) {
            val method = methods.getValue(name)
            val call = cVariadicSourceElements(method).filterIsInstance<CfirFunctionCall>().single {
                (it.calleeReference as? CfirNamedReference)?.name?.asString() == "cEllipsisFixed3"
            }
            assertSame(foreign, (call.calleeReference as CfirResolvedNamedReference).resolvedSymbol.cfir)
            val arguments = call.argumentList as CfirResolvedArgumentList
            val completed = arguments.arguments
            val roles = checkNotNull(arguments.cVariadicArguments) { "零 extra 也必须发布正式 C ellipsis 角色" }
            assertFalse(arguments.usesCangjieVariadicCall)
            assertEquals(3, roles.fixedParameterCount)
            assertEquals(kinds.size, completed.size, "$name 的成功 ArgList 不得丢弃 extras")
            assertEquals(kinds.size - 3, roles.extraArguments.size)
            assertEquals(3, arguments.mapping.size)
            assertEquals(3, arguments.mappingIncludingContextArguments.size)
            arguments.mapping.entries.forEachIndexed { index, binding ->
                assertSame(completed[index], binding.key)
                assertSame(foreign.valueParameters[index], binding.value)
            }
            roles.extraArguments.forEachIndexed { index, extra ->
                assertSame(completed[index + 3], extra)
                assertFalse(arguments.mapping.containsKey(extra))
                assertFalse(arguments.mappingIncludingContextArguments.containsKey(extra))
            }
            assertEquals(kinds, completed.map { cVariadicSourceTypeName(checkNotNull(it.coneTypeOrNull)) })
            assertEquals(INT32, cVariadicPrimitiveKind(checkNotNull(call.coneTypeOrNull)))
            val captured = analysis.sourceElement(arguments).payload as CfirExportSourcePayload.ResolvedArguments
            assertFalse(captured.usesCangjieVariadicCall)
            assertEquals(completed.size, captured.arguments.size)
            captured.arguments.forEachIndexed { index, argument -> assertSame(completed[index], argument) }
            assertEquals(3, captured.valueBindings.size)
            assertEquals(3, captured.allBindings.size)
            captured.valueBindings.forEachIndexed { index, binding ->
                assertSame(completed[index], binding.argument)
                assertSame(foreign.valueParameters[index], binding.parameter)
            }
            captured.allBindings.forEachIndexed { index, binding ->
                assertSame(completed[index], binding.argument)
                assertSame(foreign.valueParameters[index], binding.parameter)
            }
            val capturedRoles = checkNotNull(captured.cVariadicArguments)
            assertEquals(3, capturedRoles.fixedParameterCount)
            capturedRoles.extraArguments.forEachIndexed { index, extra -> assertSame(completed[index + 3], extra) }
            assertEquals(roles.extraArguments.size, capturedRoles.extraArguments.size)
            val sourceCall = canonical.expression(canonical.expressionOf(call)).payload as CfirSourceExpressionPayload.Call
            assertEquals(CfirSourceCallKind.DECLARED, sourceCall.kind)
            assertEquals(completed.size, sourceCall.arguments.size)
            sourceCall.arguments.forEachIndexed { index, identity ->
                val argument = canonical.expression(identity).payload as CfirSourceExpressionPayload.Argument
                assertFalse(argument.isDefaultValue)
                assertFalse(argument.withInout)
                assertSame(canonical.expressionOf(completed[index]), argument.value)
            }
            assertCSignature(canonical.expression(sourceCall.base).type as ConeFunctionType)
        }
        assertNestedLiteralCompletion(methods.getValue("nestedLiterals"))
        analysis.validateSourceUnchanged()
    }

    /** SOURCE 定型不执行 ABI 默认 promotions：Int8/Bool/Float32 仍保存自己的源码类型。 */
    private fun assertCSignature(type: ConeFunctionType) {
        assertTrue(type.isCFunc)
        assertTrue(type.hasVariableLenArg)
        assertEquals(3, type.parameterTypes.size)
        assertEquals(listOf(INT32, INT64, INT32), type.parameterTypes.map { cVariadicPrimitiveKind(it) })
        assertEquals(INT32, cVariadicPrimitiveKind(type.returnType))
    }

    private fun assertNestedLiteralCompletion(method: CfirNamedFunction) {
        val all = cVariadicSourceElements(method)
        val expected = mapOf("1" to INT32, "2" to INT64, "3" to INT64, "4" to INT32,
            "5" to INT64, "6" to INT64, "7" to INT64, "true" to BOOLEAN, "8.5" to FLOAT64)
        val literals = all.filterIsInstance<CfirLiteralExpression>()
        assertEquals(expected.size, literals.size)
        literals.forEach { literal ->
            val spelling = checkNotNull(literal.source?.text).toString()
            assertEquals(expected.getValue(spelling), cVariadicPrimitiveKind(checkNotNull(literal.coneTypeOrNull)))
        }
        val operators = all.filterIsInstance<CfirResolvedBuiltinOperatorReference>()
        assertEquals(3, operators.size, "三个嵌套运算都必须从正常 completion 发布已绑定的内建签名")
        operators.forEach { reference ->
            assertEquals(INT64, reference.signature.receiverKind)
            assertEquals(listOf(INT64), reference.signature.parameterKinds)
            assertEquals(INT64, reference.signature.returnKind)
        }
    }
}

private class CVariadicSourceMessages : MessageCollector {
    val entries = mutableListOf<Pair<CompilerMessageSeverity, String>>()
    override fun clear() = entries.clear()
    override fun hasErrors(): Boolean = entries.any { it.first.isError }
    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        entries += severity to message
    }
}

private fun cVariadicSourceTypeName(type: ConeCangJieType): String = when (type) {
    is ConePrimitiveType -> cVariadicPrimitiveKind(type).typeName
    is ConeCStringType -> "CString"
    is ConePointerType -> "CPointer<" + cVariadicSourceTypeName(type.pointeeType) + ">"
    else -> error("官方 fixture 的实参类型必须准确保留，不能把未完成类型当作 CType 成功")
}

private fun cVariadicPrimitiveKind(type: ConeCangJieType): PrimitiveTypeKind {
    val kind = (type as ConePrimitiveType).kind
    assertFalse(kind == IDEAL_INT || kind == IDEAL_FLOAT, "正常完成后的 literal/operator 不得残留 Ideal 类型")
    return kind
}

private fun cVariadicSourceElements(root: CfirElement): List<CfirElement> {
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

private fun cVariadicSourceText(case: CVariadicSourceCase): String =
    Regex("<![^>]*!>|<!>").replace(Files.readString(case.source, Charsets.UTF_8), "")

private fun cVariadicSourceSha256(bytes: ByteArray): String =
    HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
