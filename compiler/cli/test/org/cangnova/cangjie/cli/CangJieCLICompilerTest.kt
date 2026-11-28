package org.cangnova.cangjie.cli

import PackageFormat.Package
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.create
import org.cangnova.cangjie.config.diagnosticsCollector
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.config.packagePaths
import org.cangnova.cangjie.config.PositionalArtifactInput
import org.cangnova.cangjie.config.PositionalArtifactKind
import org.cangnova.cangjie.config.positionalArtifactInputs
import org.cangnova.cangjie.config.positionalChirInputs
import org.cangnova.cangjie.frontend.pipeline.cjmpLegacyChirInputs
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpModuleDebug
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpModuleOptLevel
import org.cangnova.cangjie.cfir.session.CfirCjmpOptimizationLevel
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.declaration.ChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirStructDeclaration
import org.cangnova.cangjie.chir.core.type.ChirTypeDeclarationId
import org.cangnova.cangjie.chir.cjmp.CjmpLegacyChirPackageLinker
import org.cangnova.cangjie.chir.cjmp.CjmpChirActualizationPlan
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113SemanticPackage
import org.cangnova.cangjie.codegen.api.ChirToLlvmCodeGenerator
import org.cangnova.cangjie.codegen.api.ChirCodegenOutput
import org.cangnova.cangjie.codegen.ir.LlvmModuleArtifact
import org.cangnova.cangjie.codegen.pipeline.DefaultChirToLlvmCodeGenerator
import org.cangnova.cangjie.arguments.description.cangjieCompilerArguments
import org.cangnova.cangjie.arguments.dsl.base.CangJieCompilerArguments
import org.cangnova.cangjie.arguments.dsl.base.CangJieCompilerArgumentsLevel
import org.cangnova.cangjie.arguments.dsl.base.ExperimentalArgumentApi
import org.cangnova.cangjie.cli.common.arguments.parseCommandLineArguments
import org.cangnova.cangjie.cli.common.CliInputPathInspection
import org.cangnova.cangjie.config.addCangJieSourceRoots
import org.cangnova.cangjie.config.cangjieSourceRoots
import org.cangnova.cangjie.frontend.pipeline.AbstractFrontendPipeline
import org.cangnova.cangjie.frontend.pipeline.ArgumentsPipelineArtifact
import org.cangnova.cangjie.frontend.pipeline.CfirFrontendPipelinePhase
import org.cangnova.cangjie.frontend.pipeline.CjmpLegacyChirInput
import org.cangnova.cangjie.frontend.pipeline.ConfigurationPipelineArtifact
import org.cangnova.cangjie.frontend.pipeline.DefaultCfirFrontendPipelineArtifact
import org.cangnova.cangjie.frontend.pipeline.PipelineContext
import org.cangnova.cangjie.frontend.pipeline.PipelinePhase
import org.cangnova.cangjie.frontend.arguments.CommonCompilerArguments
import org.cangnova.cangjie.frontend.arguments.Freezable
import org.cangnova.cangjie.phaser.CompilerPhase
import org.cangnova.cangjie.phaser.PhaseConfig
import org.cangnova.cangjie.phaser.PhaserState
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64

class CangJieCLICompilerTest {
    @TempDir
    lateinit var tempDir: Path

    private class ProbeArguments : CommonCompilerArguments() {
        override fun copyOf(): Freezable = ProbeArguments().also { copy ->
            copy.cjmpCommonPart = cjmpCommonPart.copyOf()
            copy.cjmpCommonPartChir = cjmpCommonPartChir.copyOf()
            copy.cjmpCompileCommon = cjmpCompileCommon
            copy.debug = debug
            copy.optimizationLevel = optimizationLevel
        }
    }

    private class ProbePipeline : AbstractFrontendPipeline<ProbeArguments>() {
        var receivedArguments: ProbeArguments? = null
        var receivedSourcePaths: List<String> = emptyList()
        var receivedPositionalChirPaths: List<Path> = emptyList()
        var receivedLegacyChirInputs: List<CjmpLegacyChirInput> = emptyList()
        var receivedPackagePaths: List<Path> = emptyList()
        var receivedArtifactKinds: List<PositionalArtifactKind> = emptyList()
        var receivedArtifactPaths: List<Path> = emptyList()
        var receivedCjmpModuleOptions: Pair<Boolean, CfirCjmpOptimizationLevel>? = null

        override fun createCompoundPhase(
            arguments: ProbeArguments,
        ): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<ProbeArguments>, *> {
            receivedArguments = arguments
            return object : PipelinePhase<ArgumentsPipelineArtifact<ProbeArguments>, ArgumentsPipelineArtifact<ProbeArguments>>(
                name = "CangJieCLICompilerTestPhase",
            ) {
                override fun executePhase(input: ArgumentsPipelineArtifact<ProbeArguments>): ArgumentsPipelineArtifact<ProbeArguments> {
                    receivedSourcePaths = input.configuration.cangjieSourceRoots.map { it.path }
                    receivedPositionalChirPaths = input.configuration.positionalChirInputs
                    receivedLegacyChirInputs = input.configuration.cjmpLegacyChirInputs
                    receivedPackagePaths = input.configuration.packagePaths
                    receivedArtifactKinds = input.configuration.positionalArtifactInputs.map { it.kind }
                    receivedArtifactPaths = input.configuration.positionalArtifactInputs.map { it.resolvedPath }
                    receivedCjmpModuleOptions = input.configuration.cjmpModuleDebug to input.configuration.cjmpModuleOptLevel
                    return input
                }
            }
        }
    }

    private class ProbeCompiler(
        pipeline: ProbePipeline,
    ) : CangJieFrontendCLICompiler<ProbeArguments>(
        pipeline,
        legacyCjmpCompilationDriver = LegacyCjmpCompilationDriver { _, _, _, _ -> true },
    ) {
        override fun createArguments(): ProbeArguments = ProbeArguments()

        override fun configureSourceArguments(
            freeArguments: List<String>,
            configuration: CompilerConfiguration,
        ): Boolean {
            configuration.addCangJieSourceRoots(freeArguments)
            return true
        }
    }

    private class CfirCliPipeline(
        private val rootDisposable: Disposable,
    ) : AbstractFrontendPipeline<ProbeArguments>() {
        var artifact: DefaultCfirFrontendPipelineArtifact? = null
            private set

        override fun createCompoundPhase(
            arguments: ProbeArguments,
        ): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<ProbeArguments>, *> =
            object : CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<ProbeArguments>, DefaultCfirFrontendPipelineArtifact> {
                override fun invoke(
                    phaseConfig: PhaseConfig,
                    phaserState: PhaserState,
                    context: PipelineContext,
                    input: ArgumentsPipelineArtifact<ProbeArguments>,
                ): DefaultCfirFrontendPipelineArtifact = CfirFrontendPipelinePhase.invoke(
                    phaseConfig,
                    phaserState,
                    context,
                    ConfigurationPipelineArtifact(input.configuration, rootDisposable),
                ).also { artifact = it }

                override fun getNamedSubphases(startDepth: Int) = CfirFrontendPipelinePhase.getNamedSubphases(startDepth)
            }
    }

    private class CfirCliCompiler(
        pipeline: CfirCliPipeline,
        legacyCjmpCompilationDriver: LegacyCjmpCompilationDriver = DefaultLegacyCjmpCompilationDriver(),
    ) : CangJieFrontendCLICompiler<ProbeArguments>(
        pipeline,
        legacyCjmpCompilationDriver = legacyCjmpCompilationDriver,
    ) {
        override fun createArguments(): ProbeArguments = ProbeArguments()

        override fun configureSourceArguments(
            freeArguments: List<String>,
            configuration: CompilerConfiguration,
        ): Boolean {
            configuration.moduleName = "cjmp_cli_options"
            configuration.addCangJieSourceRoots(freeArguments)
            return true
        }
    }

    private data class CfirCliResult(
        val frontendSucceeded: Boolean?,
        val artifactProduced: Boolean,
        val messages: List<Pair<CompilerMessageSeverity, String>>,
        val diagnosticFactories: List<String>,
        val diagnosticMessages: List<String>,
    )

    /** 参数/路径 probe 同样使用生产契约要求的真实诊断收集器。 */
    private fun newCompilerConfiguration(): CompilerConfiguration = CompilerConfiguration.create()

    private class RecordingMessageCollector : MessageCollector {
        val messages = mutableListOf<Pair<CompilerMessageSeverity, String>>()

        override fun clear() = messages.clear()

        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: CompilerMessageSourceLocation?,
        ) {
            messages += severity to message
        }

        override fun hasErrors(): Boolean = messages.any { (severity, _) -> severity.isError }
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `frontend pipeline execution returns its phase artifact`() {
        val pipeline = ProbePipeline()
        val arguments = ProbeArguments()
        val configuration = newCompilerConfiguration()
        val result = pipeline.executeWithArtifact(arguments, configuration)
        val artifact = requireNotNull(result.artifact) as ArgumentsPipelineArtifact<*>

        assertTrue(result.succeeded)
        assertTrue(pipeline.receivedArguments === arguments)
        assertTrue(artifact.arguments === arguments)
        assertTrue(artifact.configuration === configuration)
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `text cjmp options reach compiler execution`() {
        val commonPipeline = ProbePipeline()
        val commonCompiler = ProbeCompiler(commonPipeline)
        val sourcePath = Files.createFile(tempDir.resolve("source.cj"))
        val unusedInput = Files.createFile(tempDir.resolve("literal-argument"))
        val commonMessages = RecordingMessageCollector()
        val commonResult = commonCompiler.exec(
            arrayOf(
                "-Xcjmp-compile-common",
                "-g",
                "-O2",
                "--language-version=1.2.0-alpha.20",
                sourcePath.toString(),
                "--",
                unusedInput.toString(),
            ),
            newCompilerConfiguration().apply { messageCollector = commonMessages },
        )

        val commonArguments = requireNotNull(commonPipeline.receivedArguments)
        assertTrue(commonResult.parsing.issues.isEmpty(), "common options parse: ${commonResult.parsing.issues}")
        assertEquals(true, commonResult.frontendSucceeded)
        assertTrue(commonArguments.cjmpCompileCommon)
        assertTrue(commonArguments.debug)
        assertEquals("2", commonArguments.optimizationLevel)
        assertEquals(true to CfirCjmpOptimizationLevel.O2, commonPipeline.receivedCjmpModuleOptions)
        assertEquals(listOf(sourcePath.toString()), commonPipeline.receivedSourcePaths)
        assertEquals(listOf(CompilerMessageSeverity.WARNING), commonMessages.messages.map { it.first })

        val specificPipeline = ProbePipeline()
        val cjoPaths = listOf(Files.createFile(tempDir.resolve("common.cjo")).toString())
        val chirPaths = listOf(copyOfficialRawChir("common.chir").toString())
        val specificMessages = RecordingMessageCollector()
        val specificConfiguration = newCompilerConfiguration().apply { messageCollector = specificMessages }
        val specificResult = ProbeCompiler(specificPipeline).exec(
            arrayOf(
                "-Xcjmp-common-part=${cjoPaths.joinToString(File.pathSeparator)}",
                "-Xcjmp-common-part-chir=${chirPaths.joinToString(File.pathSeparator)}",
                "-g",
                "-O2",
                "--language-version=1.2.0-alpha.20",
                sourcePath.toString(),
            ),
            specificConfiguration,
        )

        assertTrue(specificResult.parsing.issues.isEmpty(), "specific options parse: ${specificResult.parsing.issues}")
        assertEquals(true, specificResult.frontendSucceeded, "specific options enter the pipeline: ${specificMessages.messages}")
        val specificArguments = requireNotNull(specificPipeline.receivedArguments)
        assertArrayEquals(cjoPaths.toTypedArray(), specificArguments.cjmpCommonPart)
        assertArrayEquals(chirPaths.toTypedArray(), specificArguments.cjmpCommonPartChir)
        assertFalse(specificArguments.cjmpCompileCommon)
        assertTrue(specificArguments.debug)
        assertEquals("2", specificArguments.optimizationLevel)
        assertEquals(true to CfirCjmpOptimizationLevel.O2, specificPipeline.receivedCjmpModuleOptions)
        assertEquals(listOf(sourcePath.toString()), specificPipeline.receivedSourcePaths)
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `legacy positional chir is kept separate from source roots`() {
        val pipeline = ProbePipeline()
        val sourcePath = Files.createFile(tempDir.resolve("source.cj"))
        val chirPath = copyOfficialRawChir("common.chir")
        val cjoPath = Files.createFile(tempDir.resolve("common.cjo"))

        val result = ProbeCompiler(pipeline).exec(
            arrayOf(
                "-Xcjmp-common-part=$cjoPath",
                "--language-version=1.1.3",
                sourcePath.toString(),
                chirPath.toString(),
            ),
            newCompilerConfiguration(),
        )

        assertTrue(result.parsing.issues.isEmpty(), "legacy positional CHIR parse: ${result.parsing.issues}")
        assertEquals(true, result.frontendSucceeded)
        assertEquals(listOf(sourcePath.toString()), pipeline.receivedSourcePaths)
        assertEquals(listOf(chirPath), pipeline.receivedPositionalChirPaths)
        assertEquals(listOf(chirPath), pipeline.receivedLegacyChirInputs.map { it.sourcePath })
        assertEquals("cjmp_raw_chir_v113", pipeline.receivedLegacyChirInputs.single().semanticPackage.metadata.name)
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `alpha nineteen still routes positional chir through the legacy driver`() {
        val pipeline = ProbePipeline()
        val sourcePath = Files.createFile(tempDir.resolve("alpha19-source.cj"))
        val chirPath = copyOfficialRawChir("alpha19-common.chir")
        val cjoPath = Files.createFile(tempDir.resolve("alpha19-common.cjo"))

        val result = ProbeCompiler(pipeline).exec(
            arrayOf(
                "-Xcjmp-common-part=$cjoPath",
                "--language-version=1.2.0-alpha.19",
                sourcePath.toString(),
                "--",
                chirPath.toString(),
            ),
            newCompilerConfiguration(),
        )

        assertTrue(result.parsing.issues.isEmpty(), "alpha.19 legacy CHIR parse: ${result.parsing.issues}")
        assertEquals(true, result.frontendSucceeded)
        assertArrayEquals(arrayOf(cjoPath.toString()), requireNotNull(pipeline.receivedArguments).cjmpCommonPart)
        assertTrue(requireNotNull(pipeline.receivedArguments).cjmpCommonPartChir.isEmpty())
        assertEquals(listOf(sourcePath.toString()), pipeline.receivedSourcePaths)
        assertEquals(listOf(chirPath), pipeline.receivedPositionalChirPaths)
        assertEquals(listOf(chirPath), pipeline.receivedLegacyChirInputs.map { it.sourcePath })
        assertTrue(pipeline.receivedPackagePaths.isEmpty())
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `legacy positional chir accepts a symbolic link to a chir file`() {
        val chirTarget = copyOfficialRawChir("linked-target.chir")
        val chirLink = tempDir.resolve("linked-input.chir")
        val cjoPath = Files.createFile(tempDir.resolve("linked-common.cjo"))
        try {
            Files.createSymbolicLink(chirLink, chirTarget.fileName)
        } catch (failure: Exception) {
            assumeTrue(false, "symbolic links are unavailable in this test environment: ${failure.message}")
        }

        val pipeline = ProbePipeline()
        val sourcePath = Files.createFile(tempDir.resolve("linked-source.cj"))
        val messages = RecordingMessageCollector()
        val result = ProbeCompiler(pipeline).exec(
            arrayOf(
                "-Xcjmp-common-part=$cjoPath",
                "--language-version=1.1.3",
                sourcePath.toString(),
                "--",
                chirLink.toString(),
            ),
            newCompilerConfiguration().apply { messageCollector = messages },
        )

        assertTrue(result.parsing.issues.isEmpty(), "symlinked CHIR parse: ${result.parsing.issues}")
        assertEquals(true, result.frontendSucceeded)
        assertEquals(listOf(sourcePath.toString()), pipeline.receivedSourcePaths)
        val resolvedChir = pipeline.receivedPositionalChirPaths.single()
        assertTrue(resolvedChir.isAbsolute)
        assertTrue(Files.isSameFile(resolvedChir, chirTarget), "the driver input resolves to the linked CHIR file")
        assertTrue(messages.messages.isEmpty())
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `legacy positional chir rejects multiple inputs before entering the frontend pipeline`() {
        val pipeline = ProbePipeline()
        val sourcePath = Files.createFile(tempDir.resolve("source.cj"))
        val firstChir = copyOfficialRawChir("first.chir")
        val secondChir = copyOfficialRawChir("second.chir")
        val cjoPath = Files.createFile(tempDir.resolve("common.cjo"))
        val messages = RecordingMessageCollector()

        val result = ProbeCompiler(pipeline).exec(
            arrayOf(
                "-Xcjmp-common-part=$cjoPath",
                "--language-version=1.1.3",
                sourcePath.toString(),
                firstChir.toString(),
                secondChir.toString(),
            ),
            newCompilerConfiguration().apply { messageCollector = messages },
        )

        assertTrue(result.parsing.issues.isEmpty(), "legacy positional CHIR parse: ${result.parsing.issues}")
        assertEquals(false, result.frontendSucceeded)
        assertNull(pipeline.receivedArguments)
        assertTrue(pipeline.receivedPositionalChirPaths.isEmpty())
        assertTrue(pipeline.receivedLegacyChirInputs.isEmpty())
        assertEquals(listOf(CompilerMessageSeverity.ERROR), messages.messages.map { it.first })
        assertTrue(messages.messages.single().second.isNotBlank())
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `legacy positional chir must resolve to a readable non-directory path`() {
        val pipeline = ProbePipeline()
        val sourcePath = Files.createFile(tempDir.resolve("source.cj"))
        val messages = RecordingMessageCollector()
        val missingChir = tempDir.resolve("missing.chir")

        val result = ProbeCompiler(pipeline).exec(
            arrayOf("--language-version=1.1.3", sourcePath.toString(), missingChir.toString()),
            newCompilerConfiguration().apply { messageCollector = messages },
        )

        assertTrue(result.parsing.issues.isEmpty(), "legacy positional CHIR parse: ${result.parsing.issues}")
        assertEquals(false, result.frontendSucceeded)
        assertNull(pipeline.receivedArguments)
        assertEquals(listOf(CompilerMessageSeverity.ERROR), messages.messages.map { it.first })
        assertTrue(messages.messages.single().second.contains(missingChir.toString()))
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `legacy positional chir reports an invalid path before frontend`() {
        val pipeline = ProbePipeline()
        val sourcePath = Files.createFile(tempDir.resolve("source.cj"))
        val messages = RecordingMessageCollector()

        val result = ProbeCompiler(pipeline).exec(
            arrayOf("--language-version=1.1.3", sourcePath.toString(), "\u0000invalid.chir"),
            newCompilerConfiguration().apply { messageCollector = messages },
        )

        assertTrue(result.parsing.issues.isEmpty(), "legacy positional CHIR parse: ${result.parsing.issues}")
        assertEquals(false, result.frontendSucceeded)
        assertNull(pipeline.receivedArguments)
        assertEquals(listOf(CompilerMessageSeverity.ERROR), messages.messages.map { it.first })
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `legacy positional chir imports the full graph before frontend and rejects nested corruption`() {
        val pipeline = ProbePipeline()
        val sourcePath = Files.createFile(tempDir.resolve("source.cj"))
        val cjoPath = Files.createFile(tempDir.resolve("common.cjo"))
        val validChir = copyOfficialRawChir("valid-common.chir")
        val validResult = ProbeCompiler(pipeline).exec(
            arrayOf(
                "-Xcjmp-common-part=$cjoPath",
                "--language-version=1.1.3",
                sourcePath.toString(),
                validChir.toString(),
            ),
            newCompilerConfiguration(),
        )

        assertEquals(true, validResult.frontendSucceeded)
        val imported = pipeline.receivedLegacyChirInputs.single().semanticPackage
        assertTrue(imported.tableCount > 0)
        assertTrue(imported.values.isNotEmpty())
        assertTrue(imported.expressions.isNotEmpty())

        val invalidChir = copyOfficialRawChir("invalid-common.chir")
        val invalidBytes = Files.readAllBytes(invalidChir)
        overwriteFirstUnionTableOffset(invalidBytes)
        Files.write(invalidChir, invalidBytes)
        val messages = RecordingMessageCollector()
        val invalidConfiguration = newCompilerConfiguration().apply { messageCollector = messages }
        val invalidResult = ProbeCompiler(ProbePipeline()).exec(
            arrayOf(
                "-Xcjmp-common-part=$cjoPath",
                "--language-version=1.1.3",
                sourcePath.toString(),
                invalidChir.toString(),
            ),
            invalidConfiguration,
        )

        assertEquals(false, invalidResult.frontendSucceeded)
        assertTrue(messages.messages.single().second.contains("invalid-common.chir"))
        assertTrue(invalidConfiguration.cjmpLegacyChirInputs.isEmpty())
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `legacy positional chir requires the official raw phase`() {
        val pipeline = ProbePipeline()
        val sourcePath = Files.createFile(tempDir.resolve("source.cj"))
        val cjoPath = Files.createFile(tempDir.resolve("common.cjo"))
        val optimizedChir = copyOfficialRawChir("optimized.chir")
        val optimizedBytes = Files.readAllBytes(optimizedChir)
        overwriteRootByte(optimizedBytes, fieldVtableOffset = 28, value = 1)
        Files.write(optimizedChir, optimizedBytes)
        val messages = RecordingMessageCollector()

        val result = ProbeCompiler(pipeline).exec(
            arrayOf(
                "-Xcjmp-common-part=$cjoPath",
                "--language-version=1.1.3",
                sourcePath.toString(),
                optimizedChir.toString(),
            ),
            newCompilerConfiguration().apply { messageCollector = messages },
        )

        assertEquals(false, result.frontendSucceeded)
        assertNull(pipeline.receivedArguments)
        assertTrue(messages.messages.single().second.contains("RAW"), messages.messages.single().second)
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `legacy common cjo requires a positional chir input`() {
        val pipeline = ProbePipeline()
        val sourcePath = Files.createFile(tempDir.resolve("source.cj"))
        val cjoPath = Files.createFile(tempDir.resolve("common-only.cjo"))
        val messages = RecordingMessageCollector()

        val result = ProbeCompiler(pipeline).exec(
            arrayOf("-Xcjmp-common-part=$cjoPath", "--language-version=1.1.3", sourcePath.toString()),
            newCompilerConfiguration().apply { messageCollector = messages },
        )

        assertTrue(result.parsing.issues.isEmpty(), "legacy common CJO parse: ${result.parsing.issues}")
        assertEquals(false, result.frontendSucceeded)
        assertNull(pipeline.receivedArguments)
        assertEquals(listOf(CompilerMessageSeverity.ERROR), messages.messages.map { it.first })
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `legacy positional chir requires a common cjo input`() {
        val pipeline = ProbePipeline()
        val sourcePath = Files.createFile(tempDir.resolve("specific-source.cj"))
        val chirPath = Files.createFile(tempDir.resolve("common.chir"))
        val messages = RecordingMessageCollector()

        val result = ProbeCompiler(pipeline).exec(
            arrayOf("--language-version=1.1.3", sourcePath.toString(), chirPath.toString()),
            newCompilerConfiguration().apply { messageCollector = messages },
        )

        assertTrue(result.parsing.issues.isEmpty(), "legacy positional CHIR parse: ${result.parsing.issues}")
        assertEquals(false, result.frontendSucceeded)
        assertNull(pipeline.receivedArguments)
        assertEquals(listOf(CompilerMessageSeverity.ERROR), messages.messages.map { it.first })
        assertTrue(messages.messages.single().second.contains("common-part CJO"))
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `dedicated common part chir protocol ignores positional chir`() {
        val pipeline = ProbePipeline()
        val sourcePath = Files.createFile(tempDir.resolve("source.cj"))
        val chirPath = Files.createFile(tempDir.resolve("modern-ignored.chir"))
        val commonCjoPath = Files.createFile(tempDir.resolve("modern-common.cjo"))
        val commonChirPath = Files.createFile(tempDir.resolve("modern-common.chir"))
        val messages = RecordingMessageCollector()

        val result = ProbeCompiler(pipeline).exec(
            arrayOf(
                "-Xcjmp-common-part=$commonCjoPath",
                "-Xcjmp-common-part-chir=$commonChirPath",
                "--language-version=1.2.0-alpha.20",
                sourcePath.toString(),
                chirPath.toString(),
            ),
            newCompilerConfiguration().apply { messageCollector = messages },
        )

        assertTrue(result.parsing.issues.isEmpty(), "modern positional CHIR parse: ${result.parsing.issues}")
        assertEquals(true, result.frontendSucceeded)
        val arguments = requireNotNull(pipeline.receivedArguments)
        assertArrayEquals(arrayOf(commonCjoPath.toString()), arguments.cjmpCommonPart)
        assertArrayEquals(arrayOf(commonChirPath.toString()), arguments.cjmpCommonPartChir)
        assertEquals(listOf(sourcePath.toString()), pipeline.receivedSourcePaths)
        assertTrue(pipeline.receivedPositionalChirPaths.isEmpty())
        assertEquals(listOf(CompilerMessageSeverity.WARNING), messages.messages.map { it.first })
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `dedicated common part protocol rejects positional directories before frontend`() {
        val missingPipeline = ProbePipeline()
        val sourcePath = Files.createFile(tempDir.resolve("source.cj"))
        val missingMessages = RecordingMessageCollector()
        val missingChir = tempDir.resolve("missing-modern.chir")

        val missingResult = ProbeCompiler(missingPipeline).exec(
            arrayOf("--language-version=1.2.0-alpha.20", sourcePath.toString(), missingChir.toString()),
            newCompilerConfiguration().apply { messageCollector = missingMessages },
        )

        assertTrue(missingResult.parsing.issues.isEmpty())
        assertEquals(false, missingResult.frontendSucceeded)
        assertNull(missingPipeline.receivedArguments)
        assertEquals(listOf(CompilerMessageSeverity.ERROR), missingMessages.messages.map { it.first })
        assertTrue(missingMessages.messages.single().second.contains(missingChir.toString()))

        val directoryPipeline = ProbePipeline()
        val directoryMessages = RecordingMessageCollector()
        val chirDirectory = Files.createDirectory(tempDir.resolve("directory.chir"))
        val directoryConfiguration = newCompilerConfiguration().apply { messageCollector = directoryMessages }
        val directoryResult = ProbeCompiler(directoryPipeline).exec(
            arrayOf("--language-version=1.2.0-alpha.20", sourcePath.toString(), chirDirectory.toString()),
            directoryConfiguration,
        )

        assertTrue(directoryResult.parsing.issues.isEmpty())
        assertEquals(false, directoryResult.frontendSucceeded)
        assertNull(directoryPipeline.receivedArguments)
        assertEquals(listOf(chirDirectory), directoryConfiguration.packagePaths)
        assertEquals(listOf(CompilerMessageSeverity.ERROR), directoryMessages.messages.map { it.first })
        assertTrue(directoryMessages.messages.single().second.contains(chirDirectory.toString()))
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `alpha cjo directory with trailing separator uses no-extension directory handling`() {
        val pipeline = ProbePipeline()
        val sourcePath = Files.createFile(tempDir.resolve("source.cj"))
        val cjoDirectory = Files.createDirectory(tempDir.resolve("package.cjo"))
        val positionalPath = cjoDirectory.toString() + File.separator
        val messages = RecordingMessageCollector()
        val configuration = newCompilerConfiguration().apply { messageCollector = messages }

        val result = ProbeCompiler(pipeline).exec(
            arrayOf("--language-version=1.2.0-alpha.20", sourcePath.toString(), positionalPath),
            configuration,
        )

        assertTrue(result.parsing.issues.isEmpty())
        assertEquals(false, result.frontendSucceeded)
        assertNull(pipeline.receivedArguments)
        assertEquals(listOf(cjoDirectory), configuration.packagePaths)
        assertTrue(configuration.positionalArtifactInputs.isEmpty())
        assertEquals(listOf(CompilerMessageSeverity.ERROR), messages.messages.map { it.first })
        assertTrue(messages.messages.single().second.contains(positionalPath))
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `alpha positional directory and unused file follow no-extension handling`() {
        val pipeline = ProbePipeline()
        val sourcePath = Files.createFile(tempDir.resolve("source.cj"))
        val packagePath = Files.createDirectory(tempDir.resolve("pkg"))
        val messages = RecordingMessageCollector()
        val configuration = newCompilerConfiguration().apply { messageCollector = messages }

        val result = ProbeCompiler(pipeline).exec(
            arrayOf(
                "--language-version=1.2.0-alpha.20",
                sourcePath.toString(),
                packagePath.toString(),
            ),
            configuration,
        )

        assertTrue(result.parsing.issues.isEmpty())
        assertEquals(false, result.frontendSucceeded)
        assertNull(pipeline.receivedArguments)
        assertEquals(listOf(packagePath), configuration.packagePaths)
        assertEquals(listOf(CompilerMessageSeverity.ERROR), messages.messages.map { it.first })
        assertTrue(messages.messages.single().second.contains(packagePath.toString()))
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `unused positional file is warned and does not become a source root`() {
        val pipeline = ProbePipeline()
        val sourcePath = Files.createFile(tempDir.resolve("source.cj"))
        val unusedFile = Files.createFile(tempDir.resolve("notes.txt"))
        val messages = RecordingMessageCollector()

        val result = ProbeCompiler(pipeline).exec(
            arrayOf("--language-version=1.2.0-alpha.20", sourcePath.toString(), unusedFile.toString()),
            newCompilerConfiguration().apply { messageCollector = messages },
        )

        assertTrue(result.parsing.issues.isEmpty())
        assertEquals(true, result.frontendSucceeded)
        assertEquals(listOf(sourcePath.toString()), pipeline.receivedSourcePaths)
        assertEquals(listOf(CompilerMessageSeverity.WARNING), messages.messages.map { it.first })
        assertTrue(messages.messages.single().second.contains(unusedFile.toString()))
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `fallback positional invalid path reports invalid path diagnostic`() {
        val pipeline = ProbePipeline()
        val sourcePath = Files.createFile(tempDir.resolve("source.cj"))
        val invalidPath = "\u0000invalid-input"
        val messages = RecordingMessageCollector()

        val result = ProbeCompiler(pipeline).exec(
            arrayOf("--language-version=1.2.0-alpha.20", sourcePath.toString(), "--", invalidPath),
            newCompilerConfiguration().apply { messageCollector = messages },
        )

        assertTrue(result.parsing.issues.isEmpty())
        assertEquals(false, result.frontendSucceeded)
        assertNull(pipeline.receivedArguments)
        assertEquals(listOf(CompilerMessageSeverity.ERROR), messages.messages.map { it.first })
        assertEquals(
            CangJieParsingBundle.message("cli.error.positional.input.invalid.path", invalidPath),
            messages.messages.single().second,
        )
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `fallback positional missing path reports not found diagnostic`() {
        val pipeline = ProbePipeline()
        val sourcePath = Files.createFile(tempDir.resolve("source.cj"))
        val missingPath = tempDir.resolve("missing-input.txt")
        val messages = RecordingMessageCollector()

        val result = ProbeCompiler(pipeline).exec(
            arrayOf("--language-version=1.2.0-alpha.20", sourcePath.toString(), "--", missingPath.toString()),
            newCompilerConfiguration().apply { messageCollector = messages },
        )

        assertTrue(result.parsing.issues.isEmpty())
        assertEquals(false, result.frontendSucceeded)
        assertNull(pipeline.receivedArguments)
        assertEquals(listOf(CompilerMessageSeverity.ERROR), messages.messages.map { it.first })
        assertEquals(
            CangJieParsingBundle.message("cli.error.positional.input.not.found", missingPath),
            messages.messages.single().second,
        )
    }

    @Test
    fun `fallback positional path failures map to distinct diagnostics`() {
        assertEquals(
            "cli.error.positional.input.invalid.path",
            positionalInputPathFailureMessageKey(CliInputPathInspection.InvalidPath("invalid")),
        )
        assertEquals(
            "cli.error.positional.input.not.found",
            positionalInputPathFailureMessageKey(CliInputPathInspection.Missing("missing")),
        )
        assertEquals(
            "cli.error.positional.input.inaccessible",
            positionalInputPathFailureMessageKey(CliInputPathInspection.Inaccessible("inaccessible")),
        )
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `failed argument parsing clears positional state before returning`() {
        val pipeline = ProbePipeline()
        val configuration = newCompilerConfiguration().apply {
            positionalChirInputs = listOf(tempDir.resolve("stale.chir"))
            positionalArtifactInputs = listOf(
                PositionalArtifactInput(
                    PositionalArtifactKind.CJO,
                    "stale.cjo",
                    tempDir.resolve("stale.cjo"),
                ),
            )
            packagePaths = listOf(tempDir.resolve("stale-package"))
        }

        val result = ProbeCompiler(pipeline).exec(arrayOf("--unknown-option"), configuration)

        assertTrue(result.parsing.hasErrors)
        assertEquals(null, result.frontendSucceeded)
        assertTrue(configuration.positionalChirInputs.isEmpty())
        assertTrue(configuration.positionalArtifactInputs.isEmpty())
        assertTrue(configuration.packagePaths.isEmpty())
        assertNull(pipeline.receivedArguments)
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `legacy dispatcher keeps CHIR extension matching case sensitive and falls back for other files`() {
        val pipeline = ProbePipeline()
        val sourcePath = Files.createFile(tempDir.resolve("legacy-source.cj"))
        val uppercaseChir = Files.createFile(tempDir.resolve("legacy-common.CHIR"))
        val unusedFile = Files.createFile(tempDir.resolve("legacy-notes.txt"))
        val messages = RecordingMessageCollector()

        val result = ProbeCompiler(pipeline).exec(
            arrayOf(
                "--language-version=1.1.3",
                sourcePath.toString(),
                uppercaseChir.toString(),
                unusedFile.toString(),
            ),
            newCompilerConfiguration().apply { messageCollector = messages },
        )

        assertTrue(result.parsing.issues.isEmpty())
        assertEquals(true, result.frontendSucceeded)
        assertEquals(listOf(sourcePath.toString()), pipeline.receivedSourcePaths)
        assertTrue(pipeline.receivedPositionalChirPaths.isEmpty())
        assertTrue(pipeline.receivedPackagePaths.isEmpty())
        assertEquals(
            listOf(CompilerMessageSeverity.WARNING, CompilerMessageSeverity.WARNING),
            messages.messages.map { it.first },
        )
        assertTrue(messages.messages.first().second.contains(uppercaseChir.toString()))
        assertTrue(messages.messages.last().second.contains(unusedFile.toString()))
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `alpha source suffix directory is rejected as a source file`() {
        val pipeline = ProbePipeline()
        val sourceDirectory = Files.createDirectory(tempDir.resolve("source.cj"))
        val messages = RecordingMessageCollector()

        val result = ProbeCompiler(pipeline).exec(
            arrayOf("--language-version=1.2.0-alpha.20", sourceDirectory.toString()),
            newCompilerConfiguration().apply { messageCollector = messages },
        )

        assertTrue(result.parsing.issues.isEmpty())
        assertEquals(false, result.frontendSucceeded)
        assertNull(pipeline.receivedArguments)
        assertEquals(listOf(CompilerMessageSeverity.ERROR), messages.messages.map { it.first })
        assertTrue(messages.messages.single().second.contains(sourceDirectory.toString()))
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `alpha artifact extensions retain their dedicated input roles`() {
        val pipeline = ProbePipeline()
        val sourcePath = Files.createFile(tempDir.resolve("source.cj"))
        val objectPath = Files.createFile(tempDir.resolve("object.o"))
        val archivePath = Files.createFile(tempDir.resolve("archive.a"))
        val coffObjectPath = Files.createFile(tempDir.resolve("object.obj"))
        val bitcodePath = Files.createFile(tempDir.resolve("module.bc"))
        val cjoPath = Files.createFile(tempDir.resolve("module.cjo"))

        val result = ProbeCompiler(pipeline).exec(
            arrayOf(
                "--language-version=1.2.0-alpha.20",
                sourcePath.toString(),
                objectPath.toString(),
                archivePath.toString(),
                coffObjectPath.toString(),
                bitcodePath.toString(),
                cjoPath.toString(),
            ),
            newCompilerConfiguration(),
        )

        assertTrue(result.parsing.issues.isEmpty())
        assertEquals(true, result.frontendSucceeded)
        assertEquals(listOf(sourcePath.toString()), pipeline.receivedSourcePaths)
        assertEquals(
            listOf(
                PositionalArtifactKind.OBJECT,
                PositionalArtifactKind.ARCHIVE,
                PositionalArtifactKind.COFF_OBJECT,
                PositionalArtifactKind.LLVM_BITCODE,
                PositionalArtifactKind.CJO,
            ),
            pipeline.receivedArtifactKinds,
        )
        assertEquals(
            listOf(objectPath, archivePath, coffObjectPath, bitcodePath, cjoPath),
            pipeline.receivedArtifactPaths,
        )
        assertTrue(pipeline.receivedPackagePaths.isEmpty())
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `alpha positional CJO input enforces the single-input rule`() {
        val pipeline = ProbePipeline()
        val firstCjo = Files.createFile(tempDir.resolve("first.cjo"))
        val secondCjo = Files.createFile(tempDir.resolve("second.cjo"))
        val messages = RecordingMessageCollector()

        val result = ProbeCompiler(pipeline).exec(
            arrayOf("--language-version=1.2.0-alpha.20", firstCjo.toString(), secondCjo.toString()),
            newCompilerConfiguration().apply { messageCollector = messages },
        )

        assertTrue(result.parsing.issues.isEmpty())
        assertEquals(false, result.frontendSucceeded)
        assertNull(pipeline.receivedArguments)
        assertEquals(listOf(CompilerMessageSeverity.ERROR), messages.messages.map { it.first })
        assertTrue(messages.messages.single().second.contains(secondCjo.toString()))
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `positional chir rejects input paths above the driver path limit`() {
        val pipeline = ProbePipeline()
        val sourcePath = Files.createFile(tempDir.resolve("source.cj"))
        val cjoPath = Files.createFile(tempDir.resolve("common.cjo"))
        val messages = RecordingMessageCollector()
        val tooLongPath = "x".repeat(5_000) + ".chir"

        val result = ProbeCompiler(pipeline).exec(
            arrayOf("-Xcjmp-common-part=$cjoPath", "--language-version=1.1.3", sourcePath.toString(), tooLongPath),
            newCompilerConfiguration().apply { messageCollector = messages },
        )

        assertTrue(result.parsing.issues.isEmpty())
        assertEquals(false, result.frontendSucceeded)
        assertNull(pipeline.receivedArguments)
        assertEquals(listOf(CompilerMessageSeverity.ERROR), messages.messages.map { it.first })
        assertTrue(messages.messages.single().second.contains(tooLongPath))
    }

    @Test
    fun `parser handles scalar options and reports missing values`() {
        val arguments = ProbeArguments()
        val parsed = parseCommandLineArguments(
            args = listOf("--output=module.cjo", "--language-version", "1.1.0", "-d", "-Xcjmp-compile-common=false"),
            result = arguments,
        )

        assertEquals("module.cjo", arguments.outputFile)
        assertEquals("1.1.0", arguments.languageVersion)
        assertTrue(arguments.compileCjd)
        assertTrue(!arguments.cjmpCompileCommon)
        assertTrue(parsed.issues.isEmpty())

        val missingValue = parseCommandLineArguments(
            args = listOf("--output"),
            result = ProbeArguments(),
        )
        assertTrue(missingValue.hasErrors)
        assertTrue(missingValue.issues.any { it.message == "No value passed for argument --output" })
    }

    @Test
    fun `parser follows official attached optimization option syntax`() {
        val expectedValues = listOf(
            "-O0" to "0",
            "-O1" to "1",
            "-O2" to "2",
            "-O3" to "3",
            "-Os" to "s",
            "-Oz" to "z",
            "-O" to "1",
        )

        for ((option, expectedValue) in expectedValues) {
            val arguments = ProbeArguments()
            val parsed = parseCommandLineArguments(listOf(option), arguments)
            assertTrue(parsed.issues.isEmpty(), "$option parses without a diagnostic")
            assertEquals(expectedValue, arguments.optimizationLevel, option)
        }

        val normalizedLevels = mapOf(
            "0" to CfirCjmpOptimizationLevel.O0,
            "1" to CfirCjmpOptimizationLevel.O1,
            "2" to CfirCjmpOptimizationLevel.O2,
            "3" to CfirCjmpOptimizationLevel.O3,
            "8" to CfirCjmpOptimizationLevel.O3,
            "s" to CfirCjmpOptimizationLevel.O_S,
            "z" to CfirCjmpOptimizationLevel.O_Z,
        )
        normalizedLevels.forEach { (value, expected) ->
            assertEquals(expected, CfirCjmpOptimizationLevel.fromCommandLineValue(value), value)
        }
        assertNull(CfirCjmpOptimizationLevel.fromCommandLineValue("-1"))
        assertNull(CfirCjmpOptimizationLevel.fromCommandLineValue("invalid"))

        val separatedValue = parseCommandLineArguments(listOf("-O", "2"), ProbeArguments())
        assertTrue(separatedValue.issues.isEmpty(), "bare -O selects O1 and leaves the next token as a source argument")
        assertEquals(listOf("2"), separatedValue.freeArguments)

        val equalsValue = parseCommandLineArguments(listOf("-O=2"), ProbeArguments())
        assertTrue(equalsValue.hasErrors, "the official attached form does not use an equals separator")
    }

    @Test
    @OptIn(ExperimentalArgumentApi::class)
    fun `attached short option values do not change long option value consumption`() {
        val optimizationOption = cangjieCompilerArguments.topLevel.nestedLevels.single()
            .arguments.single { it.name == "O" }
            .copy(name = "optimization-level", shortName = "O")
        val schema = CangJieCompilerArguments(
            topLevel = CangJieCompilerArgumentsLevel(
                name = "test-compiler",
                arguments = setOf(optimizationOption),
                nestedLevels = emptySet(),
            ),
        )

        val longOptionArguments = ProbeArguments()
        val longOptionResult = parseCommandLineArguments(
            args = listOf("--optimization-level", "2"),
            result = longOptionArguments,
            schema = schema,
        )
        assertTrue(longOptionResult.issues.isEmpty())
        assertTrue(longOptionResult.freeArguments.isEmpty())
        assertEquals("2", longOptionArguments.optimizationLevel)

        val shortOptionArguments = ProbeArguments()
        val shortOptionResult = parseCommandLineArguments(
            args = listOf("-O2"),
            result = shortOptionArguments,
            schema = schema,
        )
        assertTrue(shortOptionResult.issues.isEmpty())
        assertEquals("2", shortOptionArguments.optimizationLevel)
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `pipeline rejects unsupported optimization suffix before entering phases`() {
        val pipeline = ProbePipeline()
        val messages = org.cangnova.cangjie.messages.MessageCollector.NONE
        val configuration = newCompilerConfiguration().apply { messageCollector = messages }

        val compiler = ProbeCompiler(pipeline)
        val result = compiler.exec(arrayOf("-Oinvalid", "source.cj"), configuration)

        assertEquals(false, result.frontendSucceeded)
        assertTrue(result.parsing.issues.isEmpty(), "the generic argument parser preserves option payload for domain validation")
        assertEquals(null, pipeline.receivedArguments, "an invalid CJMP option does not construct the frontend phase")
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `CLI options write and validate common CJO compilation settings`() {
        val packageName = "cjmp_cli_options"
        val commonSource = Files.writeString(
            tempDir.resolve("common.cj"),
            "package $packageName\npublic common func platform(): Int64",
        )
        val commonOutput = Files.createDirectories(tempDir.resolve("common-output"))
        val commonResult = executeCfirCli(
            listOf(
                "-Xcjmp-compile-common",
                "-g",
                "-O2",
                "--language-version=1.2.0-alpha.20",
                "--output-dir=$commonOutput",
                commonSource.toString(),
            ),
        )

        assertEquals(true, commonResult.frontendSucceeded, "common CLI compilation completes: ${commonResult.messages}")
        assertTrue(commonResult.artifactProduced, "the real CFIR frontend phase ran")
        assertFalse(commonResult.messages.any { it.first.isError }, "common compilation has no CLI errors")

        val commonCjo = commonOutput.resolve(CjoConstants.packageNameToPath(packageName))
        assertTrue(Files.isRegularFile(commonCjo), "common CLI compilation writes its CJO")
        val commonHeader = CjoPackageHeader.fromPackage(
            Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(commonCjo))),
        )
        assertEquals(true, commonHeader.options?.debug)
        assertEquals(CfirCjmpOptimizationLevel.O2.cjoValue, commonHeader.options?.optLevel)

        val commonChir = Files.createFile(tempDir.resolve("common.chir"))
        val specificSource = Files.writeString(
            tempDir.resolve("specific.cj"),
            "package $packageName\npublic specific func platform(): Int64 { 42 }",
        )
        val defaultCommonOutput = Files.createDirectories(tempDir.resolve("default-common-output"))
        val defaultCommonResult = executeCfirCli(
            listOf(
                "-Xcjmp-compile-common",
                "--language-version=1.2.0-alpha.20",
                "--output-dir=$defaultCommonOutput",
                commonSource.toString(),
            ),
        )
        assertEquals(
            true,
            defaultCommonResult.frontendSucceeded,
            "default CLI options compile common CJO: ${defaultCommonResult.messages}",
        )
        val defaultCommonCjo = defaultCommonOutput.resolve(CjoConstants.packageNameToPath(packageName))
        val defaultCommonHeader = CjoPackageHeader.fromPackage(
            Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(defaultCommonCjo))),
        )
        assertEquals(false, defaultCommonHeader.options?.debug)
        assertEquals(CfirCjmpOptimizationLevel.O0.cjoValue, defaultCommonHeader.options?.optLevel)

        val defaultSpecificResult = executeCfirCli(
            listOf(
                "-Xcjmp-common-part=$defaultCommonCjo",
                "-Xcjmp-common-part-chir=$commonChir",
                "--language-version=1.2.0-alpha.20",
                specificSource.toString(),
            ),
        )
        assertEquals(
            true,
            defaultSpecificResult.frontendSucceeded,
            "default CLI options load the default CJO: ${defaultSpecificResult.messages}",
        )
        assertFalse(defaultSpecificResult.diagnosticFactories.contains("CFIR_CJMP_COMMON_CJO_DEBUG_MISMATCH"))
        assertFalse(defaultSpecificResult.diagnosticFactories.contains("CFIR_CJMP_COMMON_CJO_OPT_LEVEL_MISMATCH"))

        val matchingResult = executeCfirCli(
            listOf(
                "-Xcjmp-common-part=$commonCjo",
                "-Xcjmp-common-part-chir=$commonChir",
                "-g",
                "-O2",
                "--language-version=1.2.0-alpha.20",
                specificSource.toString(),
            ),
        )
        assertEquals(true, matchingResult.frontendSucceeded, "matching common options load successfully: ${matchingResult.messages}")
        assertTrue(matchingResult.artifactProduced)
        assertFalse(matchingResult.diagnosticFactories.contains("CFIR_CJMP_COMMON_CJO_OPT_LEVEL_MISMATCH"))

        val mismatchResult = executeCfirCli(
            listOf(
                "-Xcjmp-common-part=$commonCjo",
                "-Xcjmp-common-part-chir=$commonChir",
                "-g",
                "-O1",
                "--language-version=1.2.0-alpha.20",
                specificSource.toString(),
            ),
        )
        assertTrue(
            mismatchResult.diagnosticFactories.contains("CFIR_CJMP_COMMON_CJO_OPT_LEVEL_MISMATCH"),
            "the common CJO load gate compares the CLI-selected optimization level: ${mismatchResult.diagnosticFactories}",
        )

        val debugMismatchResult = executeCfirCli(
            listOf(
                "-Xcjmp-common-part=$commonCjo",
                "-Xcjmp-common-part-chir=$commonChir",
                "-O2",
                "--language-version=1.2.0-alpha.20",
                specificSource.toString(),
            ),
        )
        assertTrue(
            debugMismatchResult.diagnosticFactories.contains("CFIR_CJMP_COMMON_CJO_DEBUG_MISMATCH"),
            "omitting CLI -g rejects a common CJO compiled with debug enabled: ${debugMismatchResult.diagnosticFactories}",
        )
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `CLI language version controls the generic cjmp parser boundary`() {
        val packageName = "cjmp_cli_alpha_generic"
        val commonSource = Files.writeString(
            tempDir.resolve("generic-common.cj"),
            "package $packageName\npublic common func identity<T>(value: T): T { value }",
        )
        val parserDiagnostic = CangJieParsingBundle.message(
            "parsing.error.cjmp.generic.declaration.unsupported",
            "common",
        )

        for (version in listOf("1.1.0-alpha.65", "1.1.0-alpha.66")) {
            val output = Files.createDirectories(tempDir.resolve("generic-common-$version"))
            val commonResult = executeCfirCli(
                listOf(
                    "-Xcjmp-compile-common",
                    "--language-version=$version",
                    "--output-dir=$output",
                    commonSource.toString(),
                ),
            )

            if (version == "1.1.0-alpha.65") {
                assertTrue(
                    commonResult.messages.any { (severity, message) -> severity.isError && message == parserDiagnostic },
                    "alpha.65 reports the versioned parser diagnostic: ${commonResult.messages}",
                )
                assertTrue(commonResult.diagnosticFactories.isEmpty(), "alpha.65 must stop before CFIR")
                assertFalse(
                    Files.exists(output.resolve(CjoConstants.packageNameToPath(packageName))),
                    "alpha.65 must not write a CJO after the parser error",
                )
                continue
            }

            assertEquals(true, commonResult.frontendSucceeded, "alpha.66 common CLI: ${commonResult.messages}")
            val commonCjo = output.resolve(CjoConstants.packageNameToPath(packageName))
            assertTrue(Files.isRegularFile(commonCjo), "alpha.66 writes the generic common CJO")

            val specificSource = Files.writeString(
                tempDir.resolve("generic-specific-$version.cj"),
                "package $packageName\npublic platform func identity<U>(value: U): U { value }",
            )
            val specificResult = executeCfirCli(
                listOf(
                    "-Xcjmp-common-part=$commonCjo",
                    "--language-version=$version",
                    specificSource.toString(),
                ),
            )

            assertEquals(true, specificResult.frontendSucceeded, "alpha.66 specific CLI: ${specificResult.messages}")
            assertTrue(specificResult.artifactProduced)
            assertTrue(specificResult.diagnosticFactories.isEmpty(), "alpha.66 generic counterpart diagnostics")
        }
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `CLI language version controls cjmp parameter default boundary`() {
        val packageName = "cjmp_cli_alpha_default"
        val commonSource = Files.writeString(
            tempDir.resolve("default-common.cj"),
            "package $packageName\npublic common func value(x!: Int64 = 1): Int64 { x }",
        )
        val parserDiagnostic = CangJieParsingBundle.message(
            "parsing.error.cjmp.platform.parameter.default.unsupported",
            "function",
        )
        val commonPackages = mutableMapOf<String, Path>()

        for (version in listOf("1.1.0-alpha.65", "1.1.0-alpha.66")) {
            val output = Files.createDirectories(tempDir.resolve("default-common-$version"))
            val commonResult = executeCfirCli(
                listOf(
                    "-Xcjmp-compile-common",
                    "--language-version=$version",
                    "--output-dir=$output",
                    commonSource.toString(),
                ),
            )
            assertEquals(true, commonResult.frontendSucceeded, "$version common CLI: ${commonResult.messages}")
            val commonCjo = output.resolve(CjoConstants.packageNameToPath(packageName))
            assertTrue(Files.isRegularFile(commonCjo), "$version writes its common CJO")
            commonPackages[version] = commonCjo
        }

        val alpha65PlatformSource = Files.writeString(
            tempDir.resolve("default-platform-alpha65.cj"),
            "package $packageName\npublic platform func value(x!: Int64 = 2): Int64 { x }",
        )
        val alpha65Result = executeCfirCli(
            listOf(
                "-Xcjmp-common-part=${commonPackages.getValue("1.1.0-alpha.65")}",
                "--language-version=1.1.0-alpha.65",
                alpha65PlatformSource.toString(),
            ),
        )
        assertTrue(
            alpha65Result.messages.any { (severity, message) -> severity.isError && message == parserDiagnostic },
            "alpha.65 rejects the platform default in parsing: ${alpha65Result.messages}",
        )
        assertTrue(alpha65Result.diagnosticFactories.isEmpty(), "alpha.65 must stop before CFIR")
        assertFalse(alpha65Result.artifactProduced, "alpha.65 must not produce a CFIR artifact after the parser error")

        val alpha66PlatformSource = Files.writeString(
            tempDir.resolve("default-platform-alpha66.cj"),
            "package $packageName\npublic platform func value(x!: Int64): Int64 { x }",
        )
        val alpha66OneSidedResult = executeCfirCli(
            listOf(
                "-Xcjmp-common-part=${commonPackages.getValue("1.1.0-alpha.66")}",
                "--language-version=1.1.0-alpha.66",
                alpha66PlatformSource.toString(),
            ),
        )
        assertEquals(true, alpha66OneSidedResult.frontendSucceeded, "alpha.66 accepts one-sided default: ${alpha66OneSidedResult.messages}")
        assertTrue(alpha66OneSidedResult.artifactProduced)
        assertTrue(alpha66OneSidedResult.diagnosticFactories.isEmpty(), "alpha.66 one-sided default diagnostics")

        val alpha66BothSidesSource = Files.writeString(
            tempDir.resolve("default-platform-both-alpha66.cj"),
            "package $packageName\npublic platform func value(x!: Int64 = 2): Int64 { x }",
        )
        val alpha66BothSidesResult = executeCfirCli(
            listOf(
                "-Xcjmp-common-part=${commonPackages.getValue("1.1.0-alpha.66")}",
                "--language-version=1.1.0-alpha.66",
                alpha66BothSidesSource.toString(),
            ),
        )
        assertTrue(
            "CFIR_CJMP_PARAMETER_DEFAULT_VALUE_BOTH_SIDES" in alpha66BothSidesResult.diagnosticFactories,
            "alpha.66 diagnoses defaults declared on both sides: ${alpha66BothSidesResult.diagnosticMessages}",
        )
        assertTrue(
            alpha66BothSidesResult.messages.none { (severity, _) -> severity.isError },
            "alpha.66 accepts platform defaults syntactically before semantic CJMP checking: ${alpha66BothSidesResult.messages}",
        )
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `CLI language version controls inferred return post match checking`() {
        val packageName = "cjmp_cli_alpha_return"
        val commonSource = Files.writeString(
            tempDir.resolve("return-common.cj"),
            """package $packageName
                |public common func inferredSame() { 1 }
                |public common func inferredMismatch() { 1 }
                |""".trimMargin(),
        )
        val specificSource = Files.writeString(
            tempDir.resolve("return-specific.cj"),
            """package $packageName
                |public platform func inferredSame() { 1 }
                |public platform func inferredMismatch() { "wrong" }
                |""".trimMargin(),
        )

        for (version in listOf("1.1.0-alpha.66", "1.1.0-alpha.68")) {
            val output = Files.createDirectories(tempDir.resolve("return-common-$version"))
            val commonResult = executeCfirCli(
                listOf(
                    "-Xcjmp-compile-common",
                    "--language-version=$version",
                    "--output-dir=$output",
                    commonSource.toString(),
                ),
            )
            assertEquals(true, commonResult.frontendSucceeded, "$version common CLI: ${commonResult.messages}")
            val commonCjo = output.resolve(CjoConstants.packageNameToPath(packageName))
            assertTrue(Files.isRegularFile(commonCjo), "$version writes its common CJO")

            val specificResult = executeCfirCli(
                listOf(
                    "-Xcjmp-common-part=$commonCjo",
                    "--language-version=$version",
                    specificSource.toString(),
                ),
            )
            val factories = specificResult.diagnosticFactories
            val expectedFactories = if (version == "1.1.0-alpha.66") {
                listOf("CFIR_NOT_MATCHED")
            } else {
                listOf("CFIR_RETURN_TYPE_INCOMPATIBLE")
            }
            assertEquals(expectedFactories, factories.sorted(), "$version diagnoses only the mismatched inferred return")
            assertTrue(
                specificResult.diagnosticMessages.all { "inferredMismatch" in it },
                "$version must not diagnose the compatible inferredSame pair: ${specificResult.diagnosticMessages}",
            )
        }
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `legacy positional CHIR compiles a specific call through LLVM and writes bitcode`() {
        val packageName = "cjmp_raw_chir_v113"
        val commonCjo = Files.write(
            tempDir.resolve(CjoConstants.packageNameToPath(packageName)),
            officialRawCommonCjoBytes(),
        )

        val specificSource = Files.writeString(
            tempDir.resolve("specific.cj"),
            """package $packageName
                |public specific func Value(input: Int64): Int64 { 314 }
                |public func consumeCommon(input: Int64): Int64 { Value(input) }
                |""".trimMargin(),
        )
        val rawCommonChir = copyOfficialRawChir("legacy-common.chir")
        val llvmOutput = Files.createDirectories(tempDir.resolve("legacy-specific-output"))
        val specificResult = executeCfirCli(
            listOf(
                "-Xcjmp-common-part=$commonCjo",
                "--language-version=1.1.3",
                "--output=${llvmOutput.resolve("merged.bc")}",
                specificSource.toString(),
                rawCommonChir.toString(),
            ),
        )

        assertEquals(true, specificResult.frontendSucceeded, "specific LLVM compilation: ${specificResult.messages}")
        val bitcodePath = llvmOutput.resolve("merged.bc")
        val bitcode = Files.readAllBytes(bitcodePath)
        assertTrue(bitcode.size >= 4)
        assertArrayEquals(byteArrayOf(0x42, 0x43, 0xC0.toByte(), 0xDE.toByte()), bitcode.copyOfRange(0, 4))

        val llvmIr = Files.readString(llvmOutput.resolve("merged.ll"))
        assertTrue(llvmIr.contains("consumeCommon"), "LLVM IR contains the specific caller")
        assertTrue(llvmIr.contains("ret i64 314"), "LLVM IR contains the unique specific Value implementation body")
        val implementationSymbol = Regex("(?m)^define\\s+[^\\n]*@([^\\s(]+)\\(")
            .findAll(llvmIr)
            .map { it.groupValues[1] }
            .single { symbol -> llvmIr.substringAfter("@$symbol(").substringBefore("}").contains("ret i64 314") }
        val caller = llvmIr.substringAfter("consumeCommon").substringBefore("}")
        assertTrue(caller.contains("@$implementationSymbol("), "specific caller targets the actualized implementation")
        assertTrue(
            llvmIr.contains("@\"std.core:Array.tt\" = external global %TypeTemplate"),
            "official common CHIR generic Array type uses its nominal TypeTemplate",
        )
        assertTrue(
            llvmIr.contains("call ptr @CJ_MCC_GetOrCreateTypeInfo(ptr @\"std.core:Array.tt\", i32 1, ptr "),
            "generic Array TypeInfo is constructed through the runtime template ABI",
        )
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `legacy positional CHIR actualizes generic nominal owners through the production driver`() {
        val packageName = "cjmp_generic_owner_v113"
        val fixtureRoot = repositoryRoot().toPath().resolve(
            "chir/chir-tree/testData/chirParity/official-generated-chir/cjmpGenericOwner",
        )
        val commonCjo = Files.copy(
            fixtureRoot.resolve("$packageName.cjo"),
            tempDir.resolve(CjoConstants.packageNameToPath(packageName)),
        )
        val commonCjoHeader = CjoPackageHeader.fromPackage(
            Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(commonCjo))),
        )
        assertTrue(commonCjoHeader.isCjmpCommonPart, "official common CJO must carry CJMP declaration attributes")
        val commonChir = Files.copy(
            fixtureRoot.resolve("$packageName.chir"),
            tempDir.resolve("$packageName.chir"),
        )
        val specificSource = Files.copy(fixtureRoot.resolve("specific.cj"), tempDir.resolve("specific.cj"))
        val outputDirectory = Files.createDirectories(tempDir.resolve("generic-cjmp-output"))
        var linkedPackage: ChirPackage? = null
        val defaultCodeGenerator = DefaultChirToLlvmCodeGenerator()
        val productionDriver = DefaultLegacyCjmpCompilationDriver(
            codeGenerator = ChirToLlvmCodeGenerator { input ->
                linkedPackage = input.chirPackage
                defaultCodeGenerator.generate(input)
            },
        )
        val driver = LegacyCjmpCompilationDriver { arguments, configuration, frontendArtifact, legacyChirInput ->
            arguments.outputDirectory = outputDirectory.toString()
            productionDriver.compile(arguments, configuration, frontendArtifact, legacyChirInput)
        }

        val result = executeCfirCli(
            listOf(
                "-Xcjmp-common-part=$commonCjo",
                "--language-version=1.1.3",
                "--output=${outputDirectory.resolve("generic-cjmp.bc")}",
                specificSource.toString(),
                commonChir.toString(),
            ),
            legacyCjmpCompilationDriver = driver,
        )

        assertEquals(
            true,
            result.frontendSucceeded,
            "specific generic CJMP compilation: messages=${result.messages}, diagnostics=${result.diagnosticMessages}",
        )
        val actualized = checkNotNull(linkedPackage)
        val boxes = actualized.typeDefinitions
            .filterIsInstance<ChirClassDeclaration>()
            .filter { it.typeDeclarationId == ChirTypeDeclarationId(packageName, "Box") }
        assertEquals(
            1,
            boxes.size,
            "the linked package has one definition for $packageName.Box",
        )
        val box = boxes.single()
        assertEquals(listOf("U"), box.typeParameters)
        val transforms = box.memberDeclarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .filter { it.name == "transform" }
        assertEquals(1, transforms.size, "actualized Box.transform declarations: ${transforms.map { it.semanticId }}")
        val transform = transforms.single()
        assertEquals(true, transform.hasBody)
        assertEquals(1, transform.genericTypeParameters.size)
        val bitcodePath = outputDirectory.resolve("generic-cjmp.bc")
        val bitcode = Files.readAllBytes(bitcodePath)
        assertTrue(bitcode.size > 4, "the production LLVM backend writes a non-empty module")
        assertArrayEquals(byteArrayOf(0x42, 0x43, 0xC0.toByte(), 0xDE.toByte()), bitcode.copyOfRange(0, 4))
        assertTrue(Files.isRegularFile(outputDirectory.resolve("generic-cjmp.ll")))
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `legacy linker failure skips llvm backend and leaves no output artifacts`() {
        val packageName = "cjmp_raw_chir_v113"
        val commonCjo = Files.write(
            tempDir.resolve(CjoConstants.packageNameToPath(packageName)),
            officialRawCommonCjoBytes(),
        )
        val specificSource = Files.writeString(
            tempDir.resolve("specific-link-failure.cj"),
            """package $packageName
                |public specific func Value(input: Int64): Int64 { 0 }
                |public func consumeCommon(input: Int64): Int64 { Value(input) }
                |""".trimMargin(),
        )
        val commonChir = copyOfficialRawChir("legacy-link-failure-common.chir")
        val outputDirectory = Files.createDirectories(tempDir.resolve("legacy-link-failure-output"))
        var codegenInvocations = 0
        val driver = DefaultLegacyCjmpCompilationDriver(
            linker = object : CjmpLegacyChirPackageLinker {
                override fun link(
                    commonSemanticPackage: OfficialChirV113SemanticPackage,
                    specificPackage: ChirPackage,
                    actualizedIdentities: Set<ChirSemanticId>,
                ): ChirPackage = error("intentional linker rejection")

                override fun link(
                    commonSemanticPackage: OfficialChirV113SemanticPackage,
                    specificPackage: ChirPackage,
                    plan: CjmpChirActualizationPlan,
                ): ChirPackage = error("intentional linker rejection")
            },
            codeGenerator = ChirToLlvmCodeGenerator {
                codegenInvocations++
                error("LLVM backend must not run after linker rejection")
            },
        )

        val result = executeCfirCli(
            listOf(
                "-Xcjmp-common-part=$commonCjo",
                "--language-version=1.1.3",
                "--output-dir=$outputDirectory",
                specificSource.toString(),
                commonChir.toString(),
            ),
            legacyCjmpCompilationDriver = driver,
        )

        assertEquals(0, codegenInvocations)
        assertTrue(result.messages.any { (_, message) -> "intentional linker rejection" in message }, result.messages.toString())
        assertTrue(
            Files.list(outputDirectory).use { files ->
                files.noneMatch { path ->
                    path.fileName.toString().endsWith(".ll") || path.fileName.toString().endsWith(".bc")
                }
            },
        )
    }

    private fun copyOfficialRawChir(fileName: String): Path = copyOfficialChirFixture(
        fixtureRelativePath = "cjmpRawCommon/cjmp_raw_chir_v113.chir",
        targetName = fileName,
    )

    private fun officialRawCommonCjoBytes(): ByteArray {
        val encoded = javaClass.getResourceAsStream("/cjmpRawCommon/cjmp_raw_chir_v113.cjo.b64")
            ?.use { input -> input.readBytes().toString(Charsets.US_ASCII) }
            ?: error("missing official common CJO fixture")
        return Base64.getMimeDecoder().decode(encoded)
    }

    private fun copyOfficialChirFixture(fixtureRelativePath: String, targetName: String): Path {
        val root = repositoryRoot().toPath()
        val fixture = root.resolve("chir/chir-tree/testData/chirParity/official-generated-chir")
            .resolve(fixtureRelativePath)
        check(Files.isRegularFile(fixture)) { "missing official CHIR fixture: $fixture" }
        return Files.copy(fixture, tempDir.resolve(targetName))
    }

    private fun overwriteFirstUnionTableOffset(bytes: ByteArray) {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val rootPosition = buffer.getInt(0)
        val vtablePosition = rootPosition - buffer.getInt(rootPosition)
        val fieldOffset = buffer.getShort(vtablePosition + 12).toInt() and 0xFFFF
        check(fieldOffset != 0) { "fixture has no type table vector" }
        val vectorPointerPosition = rootPosition + fieldOffset
        val vectorPosition = vectorPointerPosition + buffer.getInt(vectorPointerPosition)
        check(buffer.getInt(vectorPosition) > 0) { "fixture type table vector is empty" }
        buffer.putInt(vectorPosition + Int.SIZE_BYTES, Int.MAX_VALUE)
    }

    private fun overwriteRootByte(bytes: ByteArray, fieldVtableOffset: Int, value: Int) {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val rootPosition = buffer.getInt(0)
        val vtablePosition = rootPosition - buffer.getInt(rootPosition)
        var fieldOffset = buffer.getShort(vtablePosition + fieldVtableOffset).toInt() and 0xFFFF
        if (fieldOffset == 0) {
            val objectSize = buffer.getShort(vtablePosition + Short.SIZE_BYTES).toInt() and 0xFFFF
            val vtableLength = buffer.getShort(vtablePosition).toInt() and 0xFFFF
            val presentFieldOffsets = (2 * Short.SIZE_BYTES until vtableLength step Short.SIZE_BYTES)
                .map { offset -> buffer.getShort(vtablePosition + offset).toInt() and 0xFFFF }
                .filter { it != 0 }
            val unusedLeadingByte = 6
            check(unusedLeadingByte < objectSize && presentFieldOffsets.none { it <= unusedLeadingByte }) {
                "fixture has no unused table byte for root field at vtable offset $fieldVtableOffset"
            }
            buffer.putShort(vtablePosition + fieldVtableOffset, unusedLeadingByte.toShort())
            fieldOffset = unusedLeadingByte
        }
        buffer.put(rootPosition + fieldOffset, value.toByte())
    }

    private fun repositoryRoot(): File {
        var directory = File(System.getProperty("user.dir")).absoluteFile
        while (true) {
            if (File(directory, "settings.gradle.kts").isFile) return directory
            directory = directory.parentFile ?: error("repository root not found")
        }
    }

    @OptIn(CompilerConfiguration.Internals::class)
    private fun executeCfirCli(
        arguments: List<String>,
        legacyCjmpCompilationDriver: LegacyCjmpCompilationDriver = DefaultLegacyCjmpCompilationDriver(),
    ): CfirCliResult {
        val messageCollector = RecordingMessageCollector()
        val diagnosticsCollector = DiagnosticsCollectorImpl()
        val configuration = CompilerConfiguration.create(diagnosticsCollector, messageCollector)
        val disposable = Disposer.newDisposable("cjmp-cli-options")
        val pipeline = CfirCliPipeline(disposable)
        return try {
            val result = CfirCliCompiler(pipeline, legacyCjmpCompilationDriver).exec(arguments, configuration)
            CfirCliResult(
                frontendSucceeded = result.frontendSucceeded,
                artifactProduced = pipeline.artifact != null,
                messages = messageCollector.messages.toList(),
                diagnosticFactories = diagnosticsCollector.diagnostics.map { it.factoryName },
                diagnosticMessages = diagnosticsCollector.diagnostics.map { diagnostic ->
                    "${diagnostic.factoryName}: ${diagnostic.renderMessage()}"
                },
            )
        } finally {
            Disposer.dispose(disposable)
        }
    }
}
