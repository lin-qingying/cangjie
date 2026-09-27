package org.cangnova.cangjie.cli

import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.cli.common.arguments.parseCommandLineArguments
import org.cangnova.cangjie.config.addCangJieSourceRoots
import org.cangnova.cangjie.config.cangjieSourceRoots
import org.cangnova.cangjie.frontend.pipeline.AbstractFrontendPipeline
import org.cangnova.cangjie.frontend.pipeline.ArgumentsPipelineArtifact
import org.cangnova.cangjie.frontend.pipeline.PipelineContext
import org.cangnova.cangjie.frontend.pipeline.PipelinePhase
import org.cangnova.cangjie.frontend.arguments.CommonCompilerArguments
import org.cangnova.cangjie.frontend.arguments.Freezable
import org.cangnova.cangjie.phaser.CompilerPhase
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class CangJieCLICompilerTest {
    private class ProbeArguments : CommonCompilerArguments() {
        override fun copyOf(): Freezable = ProbeArguments().also { copy ->
            copy.cjmpCommonPart = cjmpCommonPart.copyOf()
            copy.cjmpCommonPartChir = cjmpCommonPartChir.copyOf()
            copy.cjmpCompileCommon = cjmpCompileCommon
        }
    }

    private class ProbePipeline : AbstractFrontendPipeline<ProbeArguments>() {
        var receivedArguments: ProbeArguments? = null
        var receivedSourcePaths: List<String> = emptyList()

        override fun createCompoundPhase(
            arguments: ProbeArguments,
        ): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<ProbeArguments>, *> {
            receivedArguments = arguments
            return object : PipelinePhase<ArgumentsPipelineArtifact<ProbeArguments>, ArgumentsPipelineArtifact<ProbeArguments>>(
                name = "CangJieCLICompilerTestPhase",
            ) {
                override fun executePhase(input: ArgumentsPipelineArtifact<ProbeArguments>): ArgumentsPipelineArtifact<ProbeArguments> {
                    receivedSourcePaths = input.configuration.cangjieSourceRoots.map { it.path }
                    return input
                }
            }
        }
    }

    private class ProbeCompiler(
        pipeline: ProbePipeline,
    ) : CangJieFrontendCLICompiler<ProbeArguments>(pipeline) {
        override fun createArguments(): ProbeArguments = ProbeArguments()

        override fun configureSourceArguments(
            freeArguments: List<String>,
            configuration: CompilerConfiguration,
        ): Boolean {
            configuration.addCangJieSourceRoots(freeArguments)
            return true
        }
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `text cjmp options reach compiler execution`() {
        val pipeline = ProbePipeline()
        val compiler = ProbeCompiler(pipeline)
        val cjoPaths = listOf("common-one.cjo", "common-two.cjo")
        val chirPaths = listOf("common-one.chir", "common-two.chir")

        val result = compiler.exec(
            arrayOf(
                "-Xcjmp-common-part=${cjoPaths.joinToString(File.pathSeparator)}",
                "-Xcjmp-common-part-chir=${chirPaths.joinToString(File.pathSeparator)}",
                "-Xcjmp-compile-common",
                "source.cj",
                "--",
                "literal-argument",
            ),
            CompilerConfiguration(),
        )

        val received = requireNotNull(pipeline.receivedArguments)
        assertArrayEquals(cjoPaths.toTypedArray(), received.cjmpCommonPart)
        assertArrayEquals(chirPaths.toTypedArray(), received.cjmpCommonPartChir)
        assertTrue(received.cjmpCompileCommon)
        assertEquals(listOf("source.cj", "literal-argument"), pipeline.receivedSourcePaths)
        assertTrue(result.frontendSucceeded == true)
        assertTrue(result.parsing.issues.isEmpty())
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
}
