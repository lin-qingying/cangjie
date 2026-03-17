package org.cangnova.cangjie.cli.pipeline

import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cli.compiler.VfsBasedProjectEnvironment
import org.cangnova.cangjie.cli.compiler.findFileByPath
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.deserialization.SingleModuleDataProvider
import org.cangnova.cangjie.cfir.entrypoint.session.CfirDefaultSessionFactory
import org.cangnova.cangjie.cfir.extensions.CfirExtensionRegistrar
import org.cangnova.cangjie.cfir.pipeline.AllModulesFrontendOutput
import org.cangnova.cangjie.cfir.pipeline.CfirSessionProducer
import org.cangnova.cangjie.cfir.pipeline.CfirSessionConstructionUtils
import org.cangnova.cangjie.cfir.pipeline.buildCfirFromCjFiles
import org.cangnova.cangjie.cfir.pipeline.resolveAndCheckCfir
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.CompilerConfigurationKey
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

/**
 * CFIR 前端管线阶段
 */
object CfirFrontendPipelinePhase : PipelinePhase<ConfigurationPipelineArtifact, DefaultCfirFrontendPipelineArtifact>(
    "CfirFrontendPipelinePhase"
) {
    override fun executePhase(input: ConfigurationPipelineArtifact): DefaultCfirFrontendPipelineArtifact {
        val (configuration, rootDisposable) = input
        val (environment, sourceFiles, allSources) = createEnvironmentAndSources(configuration, rootDisposable)
        val rootModuleName = Name.identifier(configuration.moduleName ?: "main")
        val factory = CfirDefaultSessionFactory()
        val sessionFactoryContext = CfirDefaultSessionFactory.Context()
        val extensionRegistrars = emptyList<CfirExtensionRegistrar>()

        val sessionsWithSources = CfirSessionConstructionUtils.prepareSessions(
            files = sourceFiles,
            configuration = configuration,
            rootModuleName = rootModuleName,
            createSharedLibrarySession = {
                factory.createSharedLibrarySession(
                    mainModuleName = rootModuleName,
                    extensionRegistrars = extensionRegistrars,
                    languageVersionSettings = configuration.languageVersionSettings,
                    context = sessionFactoryContext,
                )
            },
            createLibrarySession = { sharedLibrarySession ->
                factory.createLibrarySession(
                    sharedLibrarySession = sharedLibrarySession,
                    moduleDataProvider = SingleModuleDataProvider(sharedLibrarySession.moduleData),
                    extensionRegistrars = extensionRegistrars,
                    languageVersionSettings = configuration.languageVersionSettings,
                    context = sessionFactoryContext,
                )
            },
            createSourceSession = CfirSessionProducer { files, moduleData, _, sessionConfigurator ->
                factory.createSourceSession(
                    moduleData = moduleData,
                    extensionRegistrars = extensionRegistrars,
                    configuration = configuration,
                    context = sessionFactoryContext,
                    init = sessionConfigurator,
                )
            },
        )

        val outputs = sessionsWithSources.map { (session, sources) ->
            val rawFirFiles = session.buildCfirFromCjFiles(sources)
            resolveAndCheckCfir(session, rawFirFiles)
        }
        val frontendOutput = AllModulesFrontendOutput(outputs)
        return DefaultCfirFrontendPipelineArtifact(frontendOutput, configuration, environment, allSources)
    }

    private data class EnvironmentAndSources(
        val environment: VfsBasedProjectEnvironment,
        val sourceFiles: List<CjFile>,
        val allSources: List<CjSourceFile>,
    )

    private fun createEnvironmentAndSources(
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
    ): EnvironmentAndSources {
        val coreEnvironment = CangJieCoreEnvironment.create(rootDisposable, CangJieCoreEnvironmentMode.Production)
        val project = coreEnvironment.projectEnvironment.project
        val environment = VfsBasedProjectEnvironment(
            project = project,
            knownFileSystems = listOf(StandardFileSystems.local(), StandardFileSystems.jar()),
        )

        val configuredSourcePaths = configuration.getList(CLI_SOURCE_FILE_PATHS)
        val normalizedSourcePaths = configuredSourcePaths.map { path -> File(path).absolutePath }
        val psiManager = PsiManager.getInstance(project)
        val sourceFiles = normalizedSourcePaths
            .mapNotNull(environment::findFileByPath)
            .mapNotNull { psiManager.findFile(it) as? CjFile }
        val allSources = sourceFiles.map(::PsiBackedCjSourceFile)
        return EnvironmentAndSources(environment, sourceFiles, allSources)
    }

    private class PsiBackedCjSourceFile(
        private val psiFile: CjFile,
    ) : CjSourceFile {
        override val name: String
            get() = psiFile.name

        override val path: String?
            get() = psiFile.virtualFile?.path

        override fun getContentsAsStream(): InputStream {
            val virtualFile = psiFile.virtualFile ?: return ByteArrayInputStream(psiFile.text.toByteArray())
            return virtualFile.inputStream
        }
    }

    private val CLI_SOURCE_FILE_PATHS: CompilerConfigurationKey<List<String>> =
        CompilerConfigurationKey.create("CLI_SOURCE_FILE_PATHS")
}

