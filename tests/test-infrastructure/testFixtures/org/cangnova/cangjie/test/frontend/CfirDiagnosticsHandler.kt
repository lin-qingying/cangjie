package org.cangnova.cangjie.test.frontend

import com.intellij.openapi.util.TextRange
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
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
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
    protected val File.nameWithoutFirExtension: String
        get() = nameWithoutExtension.removeSuffix(".fir")
}

class CfirDiagnosticsHandler(
    testServices: TestServices,
) : CfirAnalysisHandler(testServices) {
    private val globalMetadataInfoHandler: GlobalMetadataInfoHandler
        get() = testServices.globalMetadataInfoHandler

    private val diagnosticsService: DiagnosticsService
        get() = testServices.diagnosticsService

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(DiagnosticsDirectives, CfirDiagnosticsDirectives)

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::DiagnosticsService), service(::CfirDiagnosticCollectorService))

    override val additionalAfterAnalysisCheckers: List<Constructor<AfterAnalysisChecker>>
        get() = emptyList()

    private val fullDiagnosticsRenderer = FullDiagnosticsRenderer(DiagnosticsDirectives.RENDER_DIAGNOSTICS_FULL_TEXT)

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        fullDiagnosticsRenderer.assertCollectedDiagnostics(testServices, ".cfir.diag.txt")
    }

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
            override fun visitElement(element: CfirElement) {
                if (element is CfirExpression) {
                    consumer.reportExpressionTypeDiagnostic(element)
                }
                if (shouldRenderDynamic && element is CfirResolvable) {
                    reportDynamic(element)
                }
                element.acceptChildren(this)
            }

            private fun reportDynamic(element: CfirResolvable) {
                val reference = element.calleeReference as? CfirNamedReference ?: return
                val resolved = reference as? CfirResolvedNamedReference ?: return
                val origin = resolved.resolvedSymbol.cfir.origin
                if (origin.toString() == "DynamicScope") {
                    consumer.report(CjDebugInfoDiagnostics.DYNAMIC, reference.source)
                }
            }

            override fun visitFunctionCall(functionCall: CfirFunctionCall) {
                val reference = functionCall.calleeReference as? CfirNamedReference ?: return
                consumer.reportCallDiagnostic(functionCall, reference)
                consumer.reportContainingCallableOwner(functionCall, reference)
                super.visitFunctionCall(functionCall)
            }

            override fun visitNamedAccessExpression(propertyAccess: CfirNamedAccessExpression) {
                val reference = propertyAccess.calleeReference as? CfirNamedReference ?: return
                consumer.reportContainingCallableOwner(propertyAccess, reference)
                super.visitNamedAccessExpression(propertyAccess)
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

    private fun DebugDiagnosticConsumer.reportExpressionTypeDiagnostic(element: CfirExpression) {
        report(CjDebugInfoDiagnostics.EXPRESSION_TYPE, element) {
            element.coneTypeOrNull?.toString() ?: "<unknown type>"
        }
    }

    private fun DebugDiagnosticConsumer.reportCallDiagnostic(element: CfirElement, reference: CfirNamedReference) {
        report(CjDebugInfoDiagnostics.CALL, element) {
            val resolvedSymbol = (reference as? CfirResolvedNamedReference)?.resolvedSymbol
            val fqName = resolvedSymbol?.fqNameForDebug()
            renderCallInfo(fqName, getTypeOfCall(reference, resolvedSymbol))
        }
    }

    private fun DebugDiagnosticConsumer.reportContainingCallableOwner(element: CfirElement, reference: CfirNamedReference) {
        report(CjDebugInfoDiagnostics.CALLABLE_OWNER, element) {
            val resolvedSymbol = (reference as? CfirResolvedNamedReference)?.resolvedSymbol
            val callable = resolvedSymbol?.cfir as? CfirCallableDeclaration ?: return@report ""
            "${callable.symbol} in ${callable.moduleData.name.asString()}"
        }
    }

    private fun getTypeOfCall(reference: CfirNamedReference, resolvedSymbol: CfirSymbol<*>?): String {
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

    private fun CfirSymbol<*>.fqNameForDebug(): String? = when (this) {
        is CfirCallableSymbol<*> -> callableNameForDebug(cfir)
        else -> cfir.toString()
    }

    private fun callableNameForDebug(callable: CfirCallableDeclaration): String = when (callable) {
        is CfirFunction -> callable.symbol.name.asString()
        is CfirProperty -> callable.name.asString()
        is CfirFieldVariable -> callable.name.asString()
        is CfirPatternVariable -> "<pattern>"
        is CfirVariable -> callable.symbol.toString()
        is CfirValueParameter -> callable.name.asString()
        is CfirMainFunction -> "main"
        is CfirMacroDeclaration -> callable.symbol.name.asString()
        is CfirConstructor -> "<init>"
        else -> callable.symbol.toString()
    }
}

class FullDiagnosticsRenderer(private val directive: org.cangnova.cangjie.test.directives.model.SimpleDirective) {
    private val dumper: MultiModuleInfoDumper = MultiModuleInfoDumper(moduleHeaderTemplate = "// -- Module: <%s> --")

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

    private data class RenderedDiagnostic(
        val start: Int,
        val end: Int,
        val severity: String,
        val message: String,
    )
}

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

private fun diagnosticRanges(diagnostic: CjDiagnostic): List<com.intellij.openapi.util.TextRange> {
    return when (diagnostic) {
        is CjDiagnosticWithSource -> diagnostic.textRanges
        else -> listOf(diagnostic.firstRange)
    }
}

private object CfirMetaInfoUtils {
    val renderDiagnosticNoArgs: DiagnosticCodeMetaInfoRenderConfiguration =
        DiagnosticCodeMetaInfoRenderConfiguration().apply { renderParams = false }

    val renderDiagnosticWithArgs: DiagnosticCodeMetaInfoRenderConfiguration =
        DiagnosticCodeMetaInfoRenderConfiguration().apply { renderParams = true }
}

private enum class TypeOfCall(val nameToRender: String) {
    UNRESOLVED("unresolved"),
    VARIABLE_THROUGH_INVOKE("variable&invoke"),
    PROPERTY_GETTER("property getter"),
    FUNCTION("function"),
    OTHER("other"),
}

private fun renderCallInfo(fqName: String?, typeCall: String): String {
    return "fqName: ${fqName ?: "fqName is unknown"}; typeCall: $typeCall"
}

private class DebugDiagnosticConsumer(
    private val result: MutableList<CjDiagnostic>,
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

    @OptIn(InternalDiagnosticFactoryMethod::class)
    fun report(factory: CjDiagnosticFactory0, sourceElement: CjSourceElement?) {
        if (sourceElement == null || sourceElement.kind !in allowedKindsForDebugInfo) return
        if (sourceElement.elementType == CjNodeTypes.LAMBDA_ARGUMENT || sourceElement.elementType == CjNodeTypes.BLOCK) return

        val availableDiagnostics = diagnosedRangesToDiagnosticNames[sourceElement.startOffset..sourceElement.endOffset]
        if (availableDiagnostics == null || factory.name !in availableDiagnostics) return

        val diagnostic = factory.on(sourceElement, null, DiagnosticContext.Default) ?: return
        result.add(diagnostic)
    }

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

private object CjDebugInfoDiagnostics : CjDiagnosticsContainer() {
    val DYNAMIC by debugInfo0()
    val EXPRESSION_TYPE by debugInfo1()
    val CALL by debugInfo1()
    val CALLABLE_OWNER by debugInfo1()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = Renderers

    private object Renderers : BaseDiagnosticRendererFactory() {
        override val MAP: CjDiagnosticFactoryToRendererMap by CjDiagnosticFactoryToRendererMap("DebugInfo") { map ->
            map.put(DYNAMIC, "")
            map.put(EXPRESSION_TYPE, "{0}", TO_STRING)
            map.put(CALL, "{0}", TO_STRING)
            map.put(CALLABLE_OWNER, "{0}", TO_STRING)
        }
    }

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

private fun CjSourceElement.toCjPsiSourceElement(kind: org.cangnova.cangjie.source.CjSourceElementKind): CjPsiSourceElement? {
    return (this as? CjPsiSourceElement)?.psi?.toCjPsiSourceElement(kind)
}
