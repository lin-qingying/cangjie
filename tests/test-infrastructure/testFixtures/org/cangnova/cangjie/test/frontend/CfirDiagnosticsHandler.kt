package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory0
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory1
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactoryToRendererMap
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticsContainer
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithSource
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.InternalDiagnosticFactoryMethod
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.cfir.diagnostics.SourceElementPositioningStrategies
import org.cangnova.cangjie.cfir.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.cangnova.cangjie.cfir.diagnostics.rendering.CjDiagnosticRenderers.TO_STRING
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.test.Constructor
import org.cangnova.cangjie.test.CfirParser
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.CjQualifiedExpression
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.cangnova.cangjie.test.directives.DiagnosticsDirectives
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.model.AfterAnalysisChecker
import org.cangnova.cangjie.test.model.FrontendKinds
import org.cangnova.cangjie.test.model.FrontendOutputHandler
import org.cangnova.cangjie.test.model.TestFile
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.DiagnosticsService
import org.cangnova.cangjie.test.services.GlobalMetadataInfoHandler
import org.cangnova.cangjie.test.services.ServiceRegistrationData
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.diagnosticsService
import org.cangnova.cangjie.test.services.globalMetadataInfoHandler
import org.cangnova.cangjie.test.services.service
import org.cangnova.cangjie.test.codeMetaInfo.model.DiagnosticCodeMetaInfo
import org.cangnova.cangjie.test.codeMetaInfo.renderConfigurations.DiagnosticCodeMetaInfoRenderConfiguration
import org.cangnova.cangjie.test.directives.model.singleOrZeroValue
import org.cangnova.cangjie.test.services.assertions
import org.cangnova.cangjie.test.services.moduleStructure
import org.cangnova.cangjie.test.util.MultiModuleInfoDumper
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.toCjPsiSourceElement
import org.cangnova.cangjie.utils.DummyDelegate
import java.io.File
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * 表示 `CfirAnalysisHandler`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
abstract class CfirAnalysisHandler(
    testServices: TestServices,
    failureDisablesNextSteps: Boolean = false,
    doNotRunIfThereWerePreviousFailures: Boolean = false,
) : FrontendOutputHandler<CfirOutputArtifact>(
    testServices,
    FrontendKinds.CFIR,
    failureDisablesNextSteps,
    doNotRunIfThereWerePreviousFailures,
) {
    /**
     * 保存 `File.nameWithoutFirExtension`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    protected val File.nameWithoutFirExtension: String
        get() = nameWithoutExtension.removeSuffix(".fir")
}

/**
 * 表示 `CfirDiagnosticsHandler`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
class CfirDiagnosticsHandler(
    testServices: TestServices,
) : CfirAnalysisHandler(testServices) {
    /**
     * 保存 `globalMetadataInfoHandler`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    private val globalMetadataInfoHandler: GlobalMetadataInfoHandler
        get() = testServices.globalMetadataInfoHandler

    /**
     * 保存 `diagnosticsService`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    private val diagnosticsService: DiagnosticsService
        get() = testServices.diagnosticsService

    /**
     * 保存 `directiveContainers`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(DiagnosticsDirectives, CfirDiagnosticsDirectives)

    /**
     * 保存 `additionalServices`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::DiagnosticsService), service(::CfirDiagnosticCollectorService))

    /**
     * 保存 `additionalAfterAnalysisCheckers`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    override val additionalAfterAnalysisCheckers: List<Constructor<AfterAnalysisChecker>>
        get() = emptyList()

    /**
     * 保存 `fullDiagnosticsRenderer`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    private val fullDiagnosticsRenderer = FullDiagnosticsRenderer(DiagnosticsDirectives.RENDER_DIAGNOSTICS_FULL_TEXT)

    /**
     * 执行 `processAfterAllModules` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        fullDiagnosticsRenderer.assertCollectedDiagnostics(testServices, ".cfir.diag.txt")
    }

    /**
     * 执行 `processModule` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    override fun processModule(module: TestModule, info: CfirOutputArtifact) {
        val frontendDiagnosticsPerFile = testServices.cfirDiagnosticCollectorService.getFrontendDiagnosticsForModule(info)

        for (part in info.partsForDependsOnModules) {
            val currentModule = part.module
            val lightTreeComparingModeEnabled = CfirDiagnosticsDirectives.COMPARE_WITH_LIGHT_TREE in currentModule.directives
            val lightTreeEnabled = currentModule.directives.singleOrZeroValue(CfirDiagnosticsDirectives.CFIR_PARSER) == CfirParser.LightTree
            val forceRenderArguments = CfirDiagnosticsDirectives.RENDER_DIAGNOSTIC_ARGUMENTS in currentModule.directives

            for (file in currentModule.files) {
                val cfirFile = info.mainFirFilesByTestFile[file] ?: continue

                val diagnostics = frontendDiagnosticsPerFile[cfirFile]
                    .orEmpty()
                    .filter { diagnostic ->
                        diagnosticsService.shouldRenderDiagnostic(
                            currentModule,
                            diagnostic.factoryName,
                            diagnostic.severity,
                        )
                    }

                val diagnosticsMetaInfos = diagnostics.diagnosticCodeMetaInfos(
                    module = currentModule,
                    file = file,
                    diagnosticsService = diagnosticsService,
                    globalMetadataInfoHandler = globalMetadataInfoHandler,
                    lightTreeEnabled = lightTreeEnabled,
                    lightTreeComparingModeEnabled = lightTreeComparingModeEnabled,
                    forceRenderArguments = forceRenderArguments,
                )

                globalMetadataInfoHandler.addMetadataInfosForFile(file, diagnosticsMetaInfos)
                collectDebugInfoDiagnostics(
                    module = currentModule,
                    testFile = file,
                    cfirFile = cfirFile,
                    lightTreeEnabled = lightTreeEnabled,
                    lightTreeComparingModeEnabled = lightTreeComparingModeEnabled,
                )
                fullDiagnosticsRenderer.storeFullDiagnosticRender(currentModule, diagnostics, file)
            }
        }
    }

    /**
     * 提供 `collectDebugInfoDiagnostics` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    private fun collectDebugInfoDiagnostics(
        module: TestModule,
        testFile: TestFile,
        cfirFile: CfirFile,
        lightTreeEnabled: Boolean,
        lightTreeComparingModeEnabled: Boolean,
    ) {
        val result = mutableListOf<CjDiagnostic>()

        val diagnosedRangesToDiagnosticNames = globalMetadataInfoHandler.getExistingMetaInfosForFile(testFile)
            .groupBy(keySelector = { it.start..it.end }, valueTransform = { it.tag })
            .mapValues { (_, value) -> value.toSet() }

        val consumer = DebugDiagnosticConsumer(result, diagnosedRangesToDiagnosticNames)
        val shouldRenderDynamic = DiagnosticsDirectives.MARK_DYNAMIC_CALLS in module.directives

        object : CfirDefaultVisitorVoid() {
            /**
             * 执行 `visitElement` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
             */
            override fun visitElement(element: CfirElement) {
                if (element is CfirExpression) {
                    consumer.reportExpressionTypeDiagnostic(element)
                }
                if (shouldRenderDynamic && element is CfirResolvable) {
                    reportDynamic(element)
                }
                element.acceptChildren(this)
            }

            /**
             * 提供 `reportDynamic` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
             */
            private fun reportDynamic(element: CfirResolvable) {
                val reference = element.calleeReference as? CfirNamedReference ?: return
                val resolved = reference as? CfirResolvedNamedReference ?: return
                val origin = resolved.resolvedSymbol.cfir.origin
                if (origin.toString() == "DynamicScope") {
                    consumer.report(CjDebugInfoDiagnostics.DYNAMIC, reference.source)
                }
            }

            /**
             * 执行 `visitFunctionCall` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
             */
            override fun visitFunctionCall(functionCall: CfirFunctionCall) {
                val reference = functionCall.calleeReference as? CfirNamedReference ?: return
                consumer.reportCallDiagnostic(functionCall, reference)
                consumer.reportContainingCallableOwner(functionCall, reference)
                super.visitFunctionCall(functionCall)
            }

            /**
             * 执行 `visitNamedAccessExpression` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
             */
            override fun visitNamedAccessExpression(namedAccessExpression: CfirNamedAccessExpression) {
                val reference = namedAccessExpression.calleeReference as? CfirNamedReference ?: return
                consumer.reportContainingCallableOwner(namedAccessExpression, reference)
                super.visitNamedAccessExpression(namedAccessExpression)
            }
        }.let(cfirFile::accept)

        val codeMetaInfos = result.flatMap { diagnostic ->
            diagnostic.toMetaInfos(
                module = module,
                file = testFile,
                globalMetadataInfoHandler = globalMetadataInfoHandler,
                lightTreeEnabled = lightTreeEnabled,
                lightTreeComparingModeEnabled = lightTreeComparingModeEnabled,
                forceRenderArguments = true,
            )
        }

        globalMetadataInfoHandler.addMetadataInfosForFile(testFile, codeMetaInfos)
    }

    /**
     * 提供 `reportExpressionTypeDiagnostic` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    private fun DebugDiagnosticConsumer.reportExpressionTypeDiagnostic(element: CfirExpression) {
        report(CjDebugInfoDiagnostics.EXPRESSION_TYPE, element) {
            element.coneTypeOrNull?.toString() ?: "<unknown type>"
        }
    }

    /**
     * 提供 `reportCallDiagnostic` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    private fun DebugDiagnosticConsumer.reportCallDiagnostic(element: CfirElement, reference: CfirNamedReference) {
        report(CjDebugInfoDiagnostics.CALL, element) {
            val resolvedSymbol = (reference as? CfirResolvedNamedReference)?.resolvedSymbol
            val fqName = resolvedSymbol?.fqNameForDebug()
            renderCallInfo(fqName, getTypeOfCall(reference, resolvedSymbol))
        }
    }

    /**
     * 提供 `reportContainingCallableOwner` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    private fun DebugDiagnosticConsumer.reportContainingCallableOwner(element: CfirElement, reference: CfirNamedReference) {
        report(CjDebugInfoDiagnostics.CALLABLE_OWNER, element) {
            val resolvedSymbol = (reference as? CfirResolvedNamedReference)?.resolvedSymbol
            val callable = resolvedSymbol?.cfir as? CfirCallableDeclaration ?: return@report ""
            "${callable.symbol} in ${callable.moduleData.name.asString()}"
        }
    }

    /**
     * 提供 `getTypeOfCall` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    private fun getTypeOfCall(reference: CfirNamedReference, resolvedSymbol: CfirBasedSymbol<*>?): String {
        if (resolvedSymbol == null) return TypeOfCall.UNRESOLVED.nameToRender

        if ((resolvedSymbol as? CfirFunctionSymbol<*>)?.name == Name.identifier("invoke")
            && reference.name != Name.identifier("invoke")
        ) {
            return TypeOfCall.VARIABLE_THROUGH_INVOKE.nameToRender
        }

        return when (resolvedSymbol) {
            is CfirPropertySymbol -> TypeOfCall.PROPERTY_GETTER.nameToRender
            is CfirFunctionSymbol<*> -> buildString {
                if (resolvedSymbol.cfir.status.isOperator) append("operator ")
                append(TypeOfCall.FUNCTION.nameToRender)
            }

            else -> TypeOfCall.OTHER.nameToRender
        }
    }

    /**
     * 提供 `CfirBasedSymbol` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    private fun CfirBasedSymbol<*>.fqNameForDebug(): String? = when (this) {
        is CfirCallableSymbol<*> -> callableNameForDebug(cfir)
        else -> cfir.toString()
    }

    /**
     * 提供 `callableNameForDebug` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    private fun callableNameForDebug(callable: CfirCallableDeclaration): String = when (callable) {

        is CfirValueParameter -> callable.name.asString()
        is CfirMainFunction -> "main"
        is CfirMacroDeclaration -> callable.symbol.name.asString()
        is CfirConstructor -> "<init>"
        is CfirFunction -> callable.symbol.name.asString()
        is CfirProperty -> callable.name.asString()
        is CfirFieldVariable -> callable.name.asString()
        is CfirPatternVariable -> "<pattern>"
        is CfirVariable -> callable.symbol.toString()
        else -> callable.symbol.toString()
    }
}

/**
 * 表示 `FullDiagnosticsRenderer`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
class FullDiagnosticsRenderer(private val directive: org.cangnova.cangjie.test.directives.model.SimpleDirective) {
    /**
     * 保存 `dumper`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    private val dumper: MultiModuleInfoDumper = MultiModuleInfoDumper(moduleHeaderTemplate = "// -- Module: <%s> --")

    /**
     * 执行 `assertCollectedDiagnostics` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    fun assertCollectedDiagnostics(testServices: TestServices, expectedExtension: String) {
        val directives = testServices.moduleStructure.allDirectives
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val expectedFile = testDataFile.parentFile.resolve("${testDataFile.nameWithoutExtension.removeSuffix(".cfir")}$expectedExtension")

        if (directive !in directives) {
            if (DiagnosticsDirectives.RENDER_ALL_DIAGNOSTICS_FULL_TEXT !in directives) {
                testServices.assertions.assertFileDoesntExist(expectedFile) { directive.name }
            }
            return
        }

        if (dumper.isEmpty() && !expectedFile.exists()) {
            return
        }

        testServices.assertions.assertEqualsToFile(expectedFile, dumper.generateResultingDump())
    }

    /**
     * 执行 `storeFullDiagnosticRender` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    fun storeFullDiagnosticRender(module: TestModule, diagnostics: List<CjDiagnostic>, file: TestFile) {
        if (directive !in module.directives) return
        if (diagnostics.isEmpty()) return

        val rendered = diagnostics
            .flatMap { diagnostic ->
                diagnosticRanges(diagnostic).map { range ->
                    RenderedDiagnostic(
                        start = range.startOffset,
                        end = range.endOffset,
                        severity = diagnostic.severity.name.lowercase(),
                        message = diagnostic.renderMessage(),
                    )
                }
            }
            .sortedWith(compareBy<RenderedDiagnostic> { it.start }.thenBy { it.message })

        if (rendered.isEmpty()) return

        val builder = dumper.builderForModule(module)
        builder.appendLine(
            rendered.joinToString(separator = "\n\n") {
                "/${file.name}:${it.start}..${it.end}: ${it.severity}: ${it.message}"
            }
        )
    }

    /**
     * 表示 `RenderedDiagnostic`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
     */
    private data class RenderedDiagnostic(
        /**
         * 保存 `start`，供CFIR 前端测试在测试执行期间读取或传递。
         */
        val start: Int,
        /**
         * 保存 `end`，供CFIR 前端测试在测试执行期间读取或传递。
         */
        val end: Int,
        /**
         * 保存 `severity`，供CFIR 前端测试在测试执行期间读取或传递。
         */
        val severity: String,
        /**
         * 保存 `message`，供CFIR 前端测试在测试执行期间读取或传递。
         */
        val message: String,
    )
}

/**
 * 执行 `diagnosticCodeMetaInfos` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
 */
fun List<CjDiagnostic>.diagnosticCodeMetaInfos(
    module: TestModule,
    file: TestFile,
    diagnosticsService: DiagnosticsService,
    globalMetadataInfoHandler: GlobalMetadataInfoHandler,
    lightTreeEnabled: Boolean,
    lightTreeComparingModeEnabled: Boolean,
    forceRenderArguments: Boolean = false,
): List<DiagnosticCodeMetaInfo> {
    return flatMap { diagnostic ->
        if (!diagnosticsService.shouldRenderDiagnostic(module, diagnostic.factoryName, diagnostic.severity)) {
            emptyList()
        } else {
            diagnostic.toMetaInfos(
                module,
                file,
                globalMetadataInfoHandler,
                lightTreeEnabled,
                lightTreeComparingModeEnabled,
                forceRenderArguments,
            )
        }
    }
}

/**
 * 执行 `toMetaInfos` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
 */
fun CjDiagnostic.toMetaInfos(
    module: TestModule,
    file: TestFile,
    globalMetadataInfoHandler: GlobalMetadataInfoHandler,
    lightTreeEnabled: Boolean,
    lightTreeComparingModeEnabled: Boolean,
    forceRenderArguments: Boolean = false,
): List<DiagnosticCodeMetaInfo> {
    return diagnosticRanges(this).map { range ->
        val metaInfo = DiagnosticCodeMetaInfo(
            range = range,
            renderConfiguration = CfirMetaInfoUtils.renderDiagnosticNoArgs,
            diagnostic = this,
        )

        val shouldRenderArguments = forceRenderArguments ||
            globalMetadataInfoHandler.getExistingMetaInfosForActualMetadata(file, metaInfo).any { it.description != null }

        if (shouldRenderArguments) {
            metaInfo.replaceRenderConfiguration(CfirMetaInfoUtils.renderDiagnosticWithArgs)
        }

        if (lightTreeComparingModeEnabled) {
            metaInfo.attributes += if (lightTreeEnabled) PsiLightTreeMetaInfoProcessor.LT else PsiLightTreeMetaInfoProcessor.PSI
        }

        if (file !in module.files) {
            metaInfo.attributes += "DEP"
        }

        metaInfo
    }
}

/**
 * 提供 `diagnosticRanges` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
 */
private fun diagnosticRanges(diagnostic: CjDiagnostic): List<com.intellij.openapi.util.TextRange> {
    return when (diagnostic) {
        is CjDiagnosticWithSource -> diagnostic.textRanges
        else -> listOf(diagnostic.firstRange)
    }
}

/**
 * 提供 `CfirMetaInfoUtils` 单例，集中承载CFIR 前端测试的共享状态、常量或默认行为。
 */
private object CfirMetaInfoUtils {
    /**
     * 保存 `renderDiagnosticNoArgs`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val renderDiagnosticNoArgs: DiagnosticCodeMetaInfoRenderConfiguration =
        DiagnosticCodeMetaInfoRenderConfiguration().apply { renderParams = false }

    /**
     * 保存 `renderDiagnosticWithArgs`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val renderDiagnosticWithArgs: DiagnosticCodeMetaInfoRenderConfiguration =
        DiagnosticCodeMetaInfoRenderConfiguration().apply { renderParams = true }
}

/**
 * 表示 `TypeOfCall`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
private enum class TypeOfCall(val nameToRender: String) {
    UNRESOLVED("unresolved"),
    VARIABLE_THROUGH_INVOKE("variable&invoke"),
    PROPERTY_GETTER("property getter"),
    FUNCTION("function"),
    OTHER("other"),
}

/**
 * 提供 `renderCallInfo` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
 */
private fun renderCallInfo(fqName: String?, typeCall: String): String {
    return "fqName: ${fqName ?: "fqName is unknown"}; typeCall: $typeCall"
}

/**
 * 表示 `DebugDiagnosticConsumer`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
private class DebugDiagnosticConsumer(
    /**
     * 保存 `result`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    private val result: MutableList<CjDiagnostic>,
    /**
     * 保存 `diagnosedRangesToDiagnosticNames`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    private val diagnosedRangesToDiagnosticNames: Map<IntRange, Set<String>>,
) {
    private companion object {
        private val allowedKindsForDebugInfo = setOf(
            CjRealSourceElementKind,
            CjFakeSourceElementKind.ReferenceInAtomicQualifiedAccess,
            CjFakeSourceElementKind.SmartCastExpression,
            CjFakeSourceElementKind.DelegatingConstructorCall,
            CjFakeSourceElementKind.ArrayAccessNameReference,
            CjFakeSourceElementKind.DesugaredPlusAssign,
            CjFakeSourceElementKind.DesugaredMinusAssign,
            CjFakeSourceElementKind.DesugaredTimesAssign,
            CjFakeSourceElementKind.DesugaredDivAssign,
            CjFakeSourceElementKind.DesugaredRemAssign,
            CjFakeSourceElementKind.DesugaredPrefixDec,
            CjFakeSourceElementKind.DesugaredPrefixInc,
            CjFakeSourceElementKind.DesugaredPostfixDec,
            CjFakeSourceElementKind.DesugaredPostfixInc,
        )
    }

    /**
     * 执行 `report` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    @OptIn(InternalDiagnosticFactoryMethod::class)
    fun report(factory: CjDiagnosticFactory0, sourceElement: CjSourceElement?) {
        if (sourceElement == null || sourceElement.kind !in allowedKindsForDebugInfo) return
        if (sourceElement.elementType == CjNodeTypes.LAMBDA_ARGUMENT || sourceElement.elementType == CjNodeTypes.BLOCK) return

        val availableDiagnostics = diagnosedRangesToDiagnosticNames[sourceElement.startOffset..sourceElement.endOffset]
        if (availableDiagnostics == null || factory.name !in availableDiagnostics) return

        val diagnostic = factory.on(sourceElement, null, DiagnosticContext.Default) ?: return
        result.add(diagnostic)
    }

    /**
     * 执行 `report` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    @OptIn(InternalDiagnosticFactoryMethod::class)
    fun report(factory: CjDiagnosticFactory1<String>, element: CfirElement, argumentFactory: () -> String) {
        val sourceElement = element.source?.takeIf { it.kind in allowedKindsForDebugInfo } ?: return
        if (sourceElement.elementType == CjNodeTypes.LAMBDA_ARGUMENT || sourceElement.elementType == CjNodeTypes.BLOCK) return

        val positionedElement = factory.getPositionedElement(sourceElement)
        val availableDiagnostics = diagnosedRangesToDiagnosticNames[positionedElement.startOffset..positionedElement.endOffset]
        if (availableDiagnostics == null || factory.name !in availableDiagnostics) return

        val diagnostic = factory.on(positionedElement, argumentFactory(), null, DiagnosticContext.Default) ?: return
        result.add(diagnostic)
    }

    /**
     * 提供 `getPositionedElement` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    private fun CjDiagnosticFactory1<String>.getPositionedElement(sourceElement: CjSourceElement): CjSourceElement {
        val elementType = sourceElement.elementType
        if (this === CjDebugInfoDiagnostics.CALL &&
            (elementType == CjNodeTypes.DOT_QUALIFIED_EXPRESSION || elementType == CjNodeTypes.SAFE_ACCESS_EXPRESSION)
        ) {
            if (sourceElement is CjPsiSourceElement) {
                val selector = (sourceElement.psi as? CjQualifiedExpression)?.selectorExpression
                return selector?.toCjPsiSourceElement(sourceElement.kind) ?: sourceElement
            }
            if (sourceElement is CjLightSourceElement) {
                return sourceElement
            }
        }
        return sourceElement
    }
}

/**
 * 提供 `CjDebugInfoDiagnostics` 单例，集中承载CFIR 前端测试的共享状态、常量或默认行为。
 */
private object CjDebugInfoDiagnostics : CjDiagnosticsContainer() {
    /**
     * 保存 `DYNAMIC`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val DYNAMIC by debugInfo0()
    /**
     * 保存 `EXPRESSION_TYPE`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val EXPRESSION_TYPE by debugInfo1()
    /**
     * 保存 `CALL`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val CALL by debugInfo1()
    /**
     * 保存 `CALLABLE_OWNER`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val CALLABLE_OWNER by debugInfo1()

    /**
     * 执行 `getRendererFactory` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    override fun getRendererFactory(): BaseDiagnosticRendererFactory = Renderers

    /**
     * 提供 `Renderers` 单例，集中承载CFIR 前端测试的共享状态、常量或默认行为。
     */
    private object Renderers : BaseDiagnosticRendererFactory() {
        /**
         * 保存 `MAP`，供CFIR 前端测试在测试执行期间读取或传递。
         */
        override val MAP: CjDiagnosticFactoryToRendererMap by CjDiagnosticFactoryToRendererMap("DebugInfo") { map ->
            map.put(DYNAMIC, "")
            map.put(EXPRESSION_TYPE, "{0}", TO_STRING)
            map.put(CALL, "{0}", TO_STRING)
            map.put(CALLABLE_OWNER, "{0}", TO_STRING)
        }
    }

    /**
     * 提供 `debugInfo0` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    private fun debugInfo0() = object {
        operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, CjDiagnosticFactory0> {
            return DummyDelegate(
                CjDiagnosticFactory0(
                    "DEBUG_INFO_${prop.name}",
                    Severity.INFO,
                    SourceElementPositioningStrategies.DEFAULT,
                    CjElement::class,
                    this@CjDebugInfoDiagnostics.getRendererFactory(),
                )
            )
        }
    }

    /**
     * 提供 `debugInfo1` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    private fun debugInfo1() = object {
        operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, CjDiagnosticFactory1<String>> {
            return DummyDelegate(
                CjDiagnosticFactory1(
                    "DEBUG_INFO_${prop.name}",
                    Severity.INFO,
                    SourceElementPositioningStrategies.DEFAULT,
                    CjElement::class,
                    this@CjDebugInfoDiagnostics.getRendererFactory(),
                )
            )
        }
    }
}

/**
 * 提供 `toCjPsiSourceElement` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
 */
private fun CjSourceElement.toCjPsiSourceElement(kind: org.cangnova.cangjie.source.CjSourceElementKind): CjPsiSourceElement? {
    return (this as? CjPsiSourceElement)?.psi?.toCjPsiSourceElement(kind)
}
