package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.chir.cfir2chir.Cfir2ChirConversionException
import org.cangnova.cangjie.chir.cfir2chir.DefaultCfir2ChirConverter
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.enableChirJvmCodegen
import org.cangnova.cangjie.config.jvmClassOutputDirectory
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.cfir.pipeline.SingleModuleFrontendOutput
import org.cangnova.cangjie.jvm.codegen.api.ChirJvmCodegenInput
import org.cangnova.cangjie.jvm.codegen.api.writeClasses
import org.cangnova.cangjie.jvm.codegen.diagnostics.JvmCodegenException
import org.cangnova.cangjie.jvm.codegen.pipeline.DefaultChirToJvmCodeGenerator
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import java.nio.file.Files
import java.nio.file.Path

/**
 * CFIR2CHIR → JVM codegen 管线阶段（核心管线可选的 CFIR2CHIR → CODEGEN 阶段）。
 *
 * 消费已完成 resolve + check 的 CFIR 前端产物，按包转换为 CHIR 并生成 JVM class：
 * - 每个包的文件集喂给 [DefaultCfir2ChirConverter]，产出 CHIR package；
 * - CHIR package 经 [DefaultChirToJvmCodeGenerator] 生成 classfile；
 * - 配置了输出目录时写出 class 文件，否则只在内存中产出（产物计数进 message collector）。
 *
 * 诊断门卫：有 error 诊断的编译不进后端（对齐官方"报错不 codegen"）。
 * 配置门卫：`enableChirJvmCodegen` 未开时本阶段是明确空操作，原样返回前端产物。
 *
 * 转换失败（CFIR 尚未消化的语义、CHIR 校验错误）按阶段失败处理并给出定位信息。
 */
object ChirJvmCodegenPipelinePhase : PipelinePhase<DefaultCfirFrontendPipelineArtifact, DefaultCfirFrontendPipelineArtifact>(
    name = "ChirJvmCodegenPipelinePhase",
    postActions = setOf(CheckCompilationErrors.CheckDiagnosticCollector),
) {
    /**
     * 执行后端阶段；未启用时原样返回，失败返回 `null` 由管线按阶段失败处理。
     */
    override fun executePhase(input: DefaultCfirFrontendPipelineArtifact): DefaultCfirFrontendPipelineArtifact? {
        val configuration = input.configuration
        if (!configuration.enableChirJvmCodegen) return input
        // 诊断门卫：报错编译不进后端（官方 cjc 行为）。
        if (CheckCompilationErrors.CheckDiagnosticCollector.checkHasErrors(configuration)) return input

        val outputDirectory = configuration.jvmClassOutputDirectory?.let(Path::of)
        return try {
            val generatedCount = input.frontendOutput.outputs.sumOf { moduleOutput ->
                generateAndWriteModule(configuration, moduleOutput, outputDirectory)
            }
            if (outputDirectory == null) {
                configuration.messageCollector.report(
                    CompilerMessageSeverity.INFO,
                    "CHIR → JVM codegen produced $generatedCount class(es) in memory",
                )
            } else {
                configuration.messageCollector.report(
                    CompilerMessageSeverity.INFO,
                    "CHIR → JVM codegen wrote $generatedCount class(es) to $outputDirectory",
                )
            }
            input
        } catch (failure: Cfir2ChirConversionException) {
            configuration.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "CFIR to CHIR conversion failed: ${failure.message}",
            )
            null
        } catch (failure: JvmCodegenException) {
            // CHIR 校验失败（消息以 invalid CHIR 开头）附上完整 CHIR dump：
            // 占位降级的定位需要看到表达式的 attributes 与 block 结构。
            val chirDump = if (failure.message?.startsWith("invalid CHIR") == true) {
                "\nCHIR dump:\n" + input.frontendOutput.outputs.joinToString("\n") { moduleOutput ->
                    moduleOutput.fir.groupBy { it.packageDirective.packageFqName.asString() }
                        .values.joinToString("\n") { files ->
                            runCatching {
                                org.cangnova.cangjie.chir.core.printer.ChirPrinter.print(
                                    org.cangnova.cangjie.chir.cfir2chir.DefaultCfir2ChirConverter().convert(files),
                                )
                            }.getOrElse { "chir dump failed: $it" }
                        }
                }
            } else {
                ""
            }
            configuration.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "JVM codegen failed: ${failure.message ?: failure::class.simpleName}$chirDump",
            )
            null
        } catch (failure: Throwable) {
            configuration.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "JVM codegen failed: ${failure.message ?: failure::class.simpleName}",
            )
            null
        }
    }

    /**
     * 转换单个前端输出并按包写出 class 文件；返回生成的 class 数量。
     */
    private fun generateAndWriteModule(
        configuration: CompilerConfiguration,
        moduleOutput: SingleModuleFrontendOutput,
        outputDirectory: Path?,
    ): Int {
        val packages = moduleOutput.fir.groupBy { it.packageDirective.packageFqName.asString() }
        return packages.values.sumOf { files ->
            val chirPackage = DefaultCfir2ChirConverter().convert(files)
            val jvmOutput = DefaultChirToJvmCodeGenerator().generate(ChirJvmCodegenInput(chirPackage))
            if (outputDirectory != null) {
                jvmOutput.writeClasses(outputDirectory)
            }
            jvmOutput.classes.size
        }
    }
}
