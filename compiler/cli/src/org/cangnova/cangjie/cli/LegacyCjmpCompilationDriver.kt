/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cangnova.cangjie.cli

import org.cangnova.cangjie.chir.cfir2chir.DefaultCfir2ChirConverter
import org.cangnova.cangjie.chir.cjmp.CfirCjmpChirSemanticIdentityProviderFactory
import org.cangnova.cangjie.chir.cjmp.CjmpLegacyChirPackageLinker
import org.cangnova.cangjie.chir.cjmp.DefaultCjmpLegacyChirPackageLinker
import org.cangnova.cangjie.codegen.api.ChirCodegenInput
import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.api.ChirToLlvmCodeGenerator
import org.cangnova.cangjie.codegen.io.LlvmArtifactWriter
import org.cangnova.cangjie.codegen.pipeline.DefaultChirToLlvmCodeGenerator
import org.cangnova.cangjie.cfir.pipeline.SingleModuleFrontendOutput
import org.cangnova.cangjie.cfir.session.cjmpFamilyAggregate
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.frontend.arguments.CommonCompilerArguments
import org.cangnova.cangjie.frontend.pipeline.CjmpLegacyChirInput
import org.cangnova.cangjie.frontend.pipeline.DefaultCfirFrontendPipelineArtifact
import org.cangnova.cangjie.frontend.pipeline.PipelineArtifact
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import java.nio.file.Path

/** 旧版 positional RAW CHIR 的编译调度边界。 */
public fun interface LegacyCjmpCompilationDriver {
    /** 完成 specific 前端产物、官方 common CHIR、actualization 与 LLVM 后端的连接。 */
    public fun compile(
        arguments: CommonCompilerArguments,
        configuration: CompilerConfiguration,
        frontendArtifact: PipelineArtifact?,
        legacyChirInput: CjmpLegacyChirInput,
    ): Boolean
}

/**
 * 旧版 CJMP 生产 driver。
 *
 * 此处集中承接前端结果、CFIR matching 身份、官方 common RAW CHIR 与 LLVM 后端之间的阶段边界；
 * 每个阶段成功后才会启动下一阶段，避免错误包进入后端或提前写出 LLVM 工件。
 */
public class DefaultLegacyCjmpCompilationDriver(
    private val linker: CjmpLegacyChirPackageLinker = DefaultCjmpLegacyChirPackageLinker(),
    private val codeGenerator: ChirToLlvmCodeGenerator = DefaultChirToLlvmCodeGenerator(),
    private val artifactWriter: LlvmArtifactWriter = LlvmArtifactWriter(),
) : LegacyCjmpCompilationDriver {
    override fun compile(
        arguments: CommonCompilerArguments,
        configuration: CompilerConfiguration,
        frontendArtifact: PipelineArtifact?,
        legacyChirInput: CjmpLegacyChirInput,
    ): Boolean = try {
        compileValidated(arguments, configuration, frontendArtifact, legacyChirInput)
        true
    } catch (exception: Exception) {
        reportFailure(configuration, exception)
    } catch (error: LinkageError) {
        reportFailure(configuration, error)
    }

    private fun compileValidated(
        arguments: CommonCompilerArguments,
        configuration: CompilerConfiguration,
        frontendArtifact: PipelineArtifact?,
        legacyChirInput: CjmpLegacyChirInput,
    ) {
        val artifact = frontendArtifact as? DefaultCfirFrontendPipelineArtifact
            ?: error("Legacy CJMP compilation requires the completed CFIR frontend artifact")
        val frontendOutput: SingleModuleFrontendOutput = artifact.frontendOutput.outputs.singleOrNull()
            ?: error("Legacy CJMP compilation requires exactly one frontend module")
        val files = frontendOutput.fir
        require(files.isNotEmpty()) { "Legacy CJMP compilation requires at least one CFIR file" }

        val packageNames = files.map { it.packageDirective.packageFqName }.distinct()
        require(packageNames.size == 1) { "Legacy CJMP compilation requires one specific package" }
        val packageFqName = packageNames.single()
        val family = frontendOutput.session.cjmpFamilyAggregate.ensureFamilyComplete(packageFqName)
        val identityProvider = CfirCjmpChirSemanticIdentityProviderFactory.create(
            families = listOf(family),
            dependencies = frontendOutput.session.symbolProvider,
        )
        val specificPackage = DefaultCfir2ChirConverter(identityProvider).convert(files)
        val commonPackageName = legacyChirInput.semanticPackage.metadata.name
        require(specificPackage.name == commonPackageName) {
            "Official common package $commonPackageName does not match specific package ${specificPackage.name}"
        }

        val actualizationPlan = CfirCjmpChirSemanticIdentityProviderFactory.createActualizationPlan(
            families = listOf(family),
            dependencies = frontendOutput.session.symbolProvider,
            specificIdentityProvider = identityProvider,
        )
        val linkedPackage = linker.link(
            commonSemanticPackage = legacyChirInput.semanticPackage,
            specificPackage = specificPackage,
            plan = actualizationPlan,
        )
        val optimization = cjmpCodegenOptimization(arguments.optimizationLevel)
        val codegenOutput = codeGenerator.generate(
            ChirCodegenInput(
                chirPackage = linkedPackage,
                options = CodegenOptions(
                    enabled = true,
                    failOnUnavailable = true,
                    validateChirBeforeLowering = true,
                    verifyBeforeWrite = true,
                    emitBitcode = true,
                    emitObjectCode = false,
                    optimizeLlvmModule = optimization.optimizeLlvmModule,
                    optimizationLevel = optimization.level,
                ),
            ),
        )
        require(codegenOutput.modules.isNotEmpty()) { "LLVM codegen produced no modules" }

        val outputTarget = outputTarget(arguments)
        val namedOutput = outputTarget.moduleBaseName?.let { moduleBaseName ->
            codegenOutput.copy(modules = codegenOutput.modules.map { it.copy(name = moduleBaseName) })
        } ?: codegenOutput
        val writtenArtifacts = artifactWriter.write(
            output = namedOutput,
            outputDirectory = outputTarget.directory,
            emitBitcode = true,
            emitObjectCode = false,
        )
        check(writtenArtifacts.size == codegenOutput.modules.size && writtenArtifacts.all { it.bitcodePath != null }) {
            "LLVM codegen did not write bitcode for every module"
        }
    }

    private fun outputTarget(arguments: CommonCompilerArguments): LlvmOutputTarget {
        val outputDirectory = arguments.outputDirectory?.let(Path::of)
        val requestedOutput = arguments.outputFile?.let(Path::of)
        val resolvedOutput = when {
            requestedOutput == null -> null
            requestedOutput.isAbsolute -> requestedOutput
            outputDirectory != null -> outputDirectory.resolve(requestedOutput)
            else -> requestedOutput
        }
        val directory = resolvedOutput?.parent ?: outputDirectory ?: Path.of(".")
        val fileName = resolvedOutput?.fileName?.toString()
        val extensionSeparator = fileName?.lastIndexOf('.') ?: -1
        val moduleBaseName = when {
            fileName == null -> null
            extensionSeparator > 0 -> fileName.substring(0, extensionSeparator)
            else -> fileName
        }
        return LlvmOutputTarget(directory, moduleBaseName)
    }

    private fun reportFailure(configuration: CompilerConfiguration, failure: Throwable): Boolean {
        val detail = failure.message?.takeIf(String::isNotBlank) ?: failure.javaClass.simpleName
        configuration.messageCollector.report(
            CompilerMessageSeverity.ERROR,
            CangJieParsingBundle.message("cli.error.positional.cjmp.compilation.failed", detail),
        )
        return false
    }

    private data class LlvmOutputTarget(
        val directory: Path,
        val moduleBaseName: String?,
    )
}
