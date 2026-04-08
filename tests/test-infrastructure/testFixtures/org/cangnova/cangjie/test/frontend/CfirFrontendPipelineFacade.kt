package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.frontend.pipeline.ConfigurationPipelineArtifact
import org.cangnova.cangjie.frontend.pipeline.CheckCompilationErrors
import org.cangnova.cangjie.frontend.pipeline.FrontendPipelineArtifact
import org.cangnova.cangjie.frontend.pipeline.PipelinePhase
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.test.frontend.FrontendDirectives.CHECK_COMPILER_OUTPUT
import org.cangnova.cangjie.test.model.FrontendFacade
import org.cangnova.cangjie.test.model.FrontendKinds
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.ServiceRegistrationData
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.frontendBasedFacadesMarkerRegistrationData
import org.cangnova.cangjie.test.services.compilerConfigurationProvider
import org.cangnova.cangjie.test.services.moduleStructure
import org.cangnova.cangjie.test.services.sourceFileProvider

abstract class CfirFrontendPipelineFacade<Phase, OutputPipelineArtifact>(
    testServices: TestServices,
    private val phase: Phase,
) : FrontendFacade<CfirOutputArtifact>(testServices, FrontendKinds.CFIR)
        where OutputPipelineArtifact : FrontendPipelineArtifact,
              Phase : PipelinePhase<ConfigurationPipelineArtifact, OutputPipelineArtifact> {

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(frontendBasedFacadesMarkerRegistrationData)

    override fun shouldTransform(module: TestModule): Boolean {
        return shouldRunFirFrontendFacade(module, testServices)
    }

    override fun analyze(module: TestModule): CfirOutputArtifact? {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val input = ConfigurationPipelineArtifact(
            configuration = configuration,
            rootDisposable = testServices.compilerConfigurationProvider.testRootDisposable,
        )

        val output = phase.executePhase(input)
            ?: return processErrorFromFrontendPhase(configuration, testServices)

        val firOutputs = output.frontendOutput.outputs
        val testFirOutputs = getPartsForDependsOnModules(module, firOutputs)
        return CfirFrontendPipelineOutputArtifact(output, testFirOutputs)
    }

    open fun getPartsForDependsOnModules(
        module: TestModule,
        firOutputs: List<org.cangnova.cangjie.cfir.pipeline.SingleModuleFrontendOutput>,
    ): List<CfirOutputPartForDependsOnModule> {
        val modulesFromTheSameStructure = testServices.moduleStructure.modules.associateBy { "<${it.name}>" }
        return firOutputs.map {
            val sessionModuleName = "<${it.session.moduleData.name.asString()}>"
            val correspondingModule = modulesFromTheSameStructure[sessionModuleName] ?: module
            it.toTestOutputPart(correspondingModule, testServices)
        }
    }
}

class CfirFrontendPipelineOutputArtifact<A : FrontendPipelineArtifact>(
    val frontendArtifact: A,
    partsForDependsOnModules: List<CfirOutputPartForDependsOnModule>,
) : CfirOutputArtifact(partsForDependsOnModules) {
    override val allFirFiles: Collection<org.cangnova.cangjie.cfir.declarations.CfirFile>
        get() = frontendArtifact.frontendOutput.outputs.flatMap { it.fir }
}

fun org.cangnova.cangjie.cfir.pipeline.SingleModuleFrontendOutput.toTestOutputPart(
    correspondingModule: TestModule,
    testServices: TestServices,
): CfirOutputPartForDependsOnModule {
    val testFilePerFirFile = correspondingModule.files.mapNotNull { testFile ->
        val firFile = fir.firstOrNull { firFile ->
            val path = testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(testFile).canonicalPath
            val normalizedPath = path.replace('\\', '/')
            normalizedPath == firFile.sourceFile?.path
        } ?: return@mapNotNull null
        testFile to firFile
    }
    return CfirOutputPartForDependsOnModule(
        module = correspondingModule,
        session = session,
        scopeSession = scopeSession,
        firAnalyzerFacade = CfirPipelineAnalyzerFacade(this),
        firFilesByTestFile = testFilePerFirFile.toMap()
    )
}

fun processErrorFromFrontendPhase(configuration: CompilerConfiguration, testServices: TestServices): Nothing? {
    if (CheckCompilationErrors.CheckDiagnosticCollector.checkHasErrorsAndReportToMessageCollector(configuration)) {
        if (CHECK_COMPILER_OUTPUT in testServices.moduleStructure.allDirectives) {
            return null
        }
    }
    error("Frontend phase returned null and there are no errors in diagnostic/message collectors")
}

private fun shouldRunFirFrontendFacade(@Suppress("UNUSED_PARAMETER") module: TestModule, @Suppress("UNUSED_PARAMETER") testServices: TestServices): Boolean = true
