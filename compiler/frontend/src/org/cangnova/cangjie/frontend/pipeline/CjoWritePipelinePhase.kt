package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.cfir.pipeline.SingleModuleFrontendOutput
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.serialization.cjo.CfirCjoPackageMetadataProducer
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageWriter
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.cjoOutputDirectory
import org.cangnova.cangjie.config.cjoOutputFile
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpChirOutput
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysisSettings
import org.cangnova.cangjie.cfir.resolve.providers.dependenciesSymbolProvider
import org.cangnova.cangjie.cfir.resolve.source.CfirSourceGraphProducer
import org.cangnova.cangjie.cfir.serialization.cjo.CjoExportOptions
import org.cangnova.cangjie.cfir.serialization.cjo.CjoExportSourceSnapshots
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilySnapshot
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.cjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.config.compileCjd
import org.cangnova.cangjie.config.enableCompileTest
import org.cangnova.cangjie.config.nativeMetadataExportConfiguration
import org.cangnova.cangjie.name.FqName
import java.nio.file.Files
import java.nio.file.Path

/**
 * 写出 live CJO 包级元数据的管线阶段（对应核心管线的 SAVE_CJO 阶段）。
 *
 * 此前这一步是 [CfirFrontendPipelinePhase] 里的私有方法：既不出现在阶段耗时报告里，
 * 写失败也只能看到"前端阶段失败"这一条线索，与真实失败点（产物生成还是文件写出）对不上。
 * 独立成阶段后耗时由阶段采集器自动记录，写失败单独定位。
 *
 * 未配置 CJO 输出（`--output` / `--output-dir` 都没给）时它是一次明确的空操作：
 * 不产出文件，原样返回前端产物。
 */
object CjoWritePipelinePhase : PipelinePhase<DefaultCfirFrontendPipelineArtifact, DefaultCfirFrontendPipelineArtifact>(
    name = "CjoWritePipelinePhase",
) {
    /**
     * 按包写出 `.cjo`；写失败返回 `null`，由管线按阶段失败处理。
     *
     * 本阶段不改变前端产物，成功时返回同一个实例。
     */
    override fun executePhase(input: DefaultCfirFrontendPipelineArtifact): DefaultCfirFrontendPipelineArtifact? {
        if (!writeLiveCjoOutputs(input.configuration, input.frontendOutput.outputs)) return null
        return input
    }

    /**
     * Kotlin FIR writes library metadata after frontend analysis.  CFIR keeps
     * the same phase boundary: this producer consumes resolved CFIR and writes
     * one package artifact per package when an explicit output directory is set.
     */
    private fun writeLiveCjoOutputs(
        configuration: CompilerConfiguration,
        outputs: List<SingleModuleFrontendOutput>,
    ): Boolean {
        val outputDirectory = configuration.cjoOutputDirectory?.let(Path::of)
            ?: configuration.cjoOutputFile?.let { product ->
                val productPath = Path.of(product)
                if (Files.isDirectory(productPath)) productPath else productPath.parent ?: Path.of(".")
            }
        if (outputDirectory == null) return true
        return try {
            Files.createDirectories(outputDirectory)
            val packages = outputs
                .flatMap { output -> output.fir.map { output.session to it } }
                .groupBy { (_, file) -> file.packageDirective.packageFqName.asString() }
            packages.forEach { (packageName, outputFiles) ->
                val sessions = outputFiles.map { it.first }.distinct()
                val files = outputFiles.map { it.second }
                val specificSessions = sessions.filter { it.cjmpSettings.mode == CfirCjmpMode.SPECIFIC }
                val families = specificSessions.map { it.cjmpFamilyAggregate.ensureFamilyComplete(FqName(packageName)) }
                val family = when (families.size) {
                    0 -> null
                    1 -> families.single()
                    else -> CfirCjmpFamilySnapshot.combine(
                        FqName(packageName),
                        families,
                        families.flatMap { it.packageDeclarationsForExport() }.distinctBy { it.symbol },
                    )
                }
                val loadedPackages = CjoExportSourceSnapshots.uniqueLoadedPackages(specificSessions.zip(families).flatMap { (session, snapshot) ->
                    CjoExportSourceSnapshots.loadedPackages(session.dependenciesSymbolProvider, snapshot)
                })
                val metadata = CfirCjoPackageMetadataProducer.produce(
                    files, family, loadedPackages,
                    CjoExportOptions(
                        exportForIncremental = false,
                        exportContent = configuration.nativeMetadataExportConfiguration.importSourceCode,
                        serializingCommon = configuration.cjmpChirOutput,
                        isMacroPackage = files.map { it.packageDirective.isMacroPackage }.distinct().single(),
                        analysisSettings = CfirExportAnalysisSettings(
                            noSubPackages = configuration.nativeMetadataExportConfiguration.noSubPackages,
                            exportForTest = configuration.nativeMetadataExportConfiguration.exportForTest,
                            canInline = configuration.nativeMetadataExportConfiguration.canInlineSource(configuration.enableCompileTest),
                            compileCjd = configuration.compileCjd,
                        ),
                    ),
                    canonicalSources = listOf(CfirSourceGraphProducer.produce(files, family)),
                )
                val target = outputDirectory.resolve(CjoConstants.packageNameToPath(packageName))
                Files.createDirectories(target.parent)
                CjoPackageWriter.write(target, metadata)
            }
            true
        } catch (failure: Throwable) {
            configuration.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Cannot produce live CJO metadata: ${failure.message ?: failure::class.simpleName}",
            )
            false
        }
    }
}