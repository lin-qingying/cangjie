package org.cangnova.cangjie.analysis.api.impl.base.test

import java.io.File

/**
 * Analysis API generated test 矩阵完整性校验器。
 *
 * 它校验的是注册表结构本身，而不仅仅是 tests-gen 文件有没有更新：
 * 1. 每个 model 声明的 testData 目录必须存在。
 * 2. 每个 model 支持的 module kind 都必须至少能展开出一个可用 variant。
 * 3. 每个 `model × module kind` 都必须至少命中一个测试数据文件。
 * 4. 所有展开后的 generated class 名称与输出路径必须全局唯一。
 *
 * 这样 `GeneratedAnalysisApiTestRegistry` 不再只是生成器输入表，
 * 也成为 Analysis API 测试矩阵的结构事实来源。
 */
object GeneratedAnalysisApiMatrixConsistencyChecker {
    @JvmStatic
    fun main(args: Array<String>) {
        val projectRoot = if (args.isNotEmpty()) File(args[0]) else File(System.getProperty("user.dir"))

        check(GeneratedAnalysisApiTestRegistry.candidateVariants.isNotEmpty()) {
            "GeneratedAnalysisApiTestRegistry 未声明任何 candidate variant。"
        }
        check(GeneratedAnalysisApiTestRegistry.models.isNotEmpty()) {
            "GeneratedAnalysisApiTestRegistry 未声明任何 generated test model。"
        }

        val classNameToOrigin = linkedMapOf<String, String>()
        val outputPathToOrigin = linkedMapOf<String, String>()

        GeneratedAnalysisApiTestRegistry.models.forEach { model ->
            val modelRoot = projectRoot.resolve(model.modelRelativePath)
            check(modelRoot.isDirectory) {
                "Generated Analysis API model 缺少 testData 目录：${modelRoot.invariantSeparatorsPath}"
            }
            check(model.supportedModuleKinds.isNotEmpty()) {
                "Generated Analysis API model 未声明任何 module kind：${model.baseName} -> ${model.modelRelativePath}"
            }

            model.supportedModuleKinds.forEach { moduleKind ->
                val variants = GeneratedAnalysisApiTestRegistry.supportedVariantsFor(moduleKind)
                check(variants.isNotEmpty()) {
                    "module kind ${moduleKind.name} 在注册表中没有任何可用 variant。"
                }

                val pattern = Regex(model.includedFilePattern(moduleKind))
                val matchedFiles = modelRoot.walkTopDown()
                    .filter { file -> file.isFile && pattern.matches(file.name) }
                    .toList()
                check(matchedFiles.isNotEmpty()) {
                    buildString {
                        appendLine("Generated Analysis API model 没有命中任何测试数据文件。")
                        appendLine("model: ${model.baseName}")
                        appendLine("module kind: ${moduleKind.name}")
                        appendLine("testData: ${modelRoot.invariantSeparatorsPath}")
                        appendLine("pattern: ${model.includedFilePattern(moduleKind)}")
                    }
                }

                variants.forEach { variant ->
                    val generatedClassName = variant.generatedClassName(moduleKind, model.baseName)
                    val origin = "${model.baseName}@${model.modelRelativePath}[${moduleKind.name}:${variant.frontend.name}/${variant.analysisApiMode.name}/${variant.analysisSessionMode.name}]"
                    val previousClassOrigin = classNameToOrigin.putIfAbsent(generatedClassName, origin)
                    check(previousClassOrigin == null) {
                        buildString {
                            appendLine("Generated Analysis API class 名称冲突：$generatedClassName")
                            appendLine("first: $previousClassOrigin")
                            appendLine("second: $origin")
                        }
                    }

                    val outputPath = projectRoot.resolve(
                        "${TestGeneratorForAnalysisApi.generatedOutputRoot}/$generatedClassName.kt",
                    ).canonicalFile.invariantSeparatorsPath
                    val previousPathOrigin = outputPathToOrigin.putIfAbsent(outputPath, origin)
                    check(previousPathOrigin == null) {
                        buildString {
                            appendLine("Generated Analysis API 输出路径冲突：$outputPath")
                            appendLine("first: $previousPathOrigin")
                            appendLine("second: $origin")
                        }
                    }
                }
            }
        }
    }
}
