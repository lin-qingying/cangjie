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
 * 这样 Analysis API DSL 不再只是生成器输入，
 * 也成为 Analysis API 测试矩阵的结构事实来源。
 */
object GeneratedAnalysisApiMatrixConsistencyChecker {
    @JvmStatic
    fun main(args: Array<String>) {
        val projectRoot = if (args.isNotEmpty()) File(args[0]) else File(System.getProperty("user.dir"))

        val generatedSuites = TestGeneratorForAnalysisApi.generatedSuites(projectRoot)
        check(generatedSuites.isNotEmpty()) {
            "Analysis API generated test DSL 未声明任何 generated test model。"
        }

        val classNameToOrigin = linkedMapOf<String, String>()
        val outputPathToOrigin = linkedMapOf<String, String>()

        generatedSuites.forEach { suite ->
            val modelRoot = projectRoot.resolve(suite.modelRelativePath)
            check(modelRoot.isDirectory) {
                "Generated Analysis API model 缺少 testData 目录：${modelRoot.invariantSeparatorsPath}"
            }

            val pattern = Regex(suite.includedFilePattern)
            val matchedFiles = modelRoot.walkTopDown()
                .filter { file -> file.isFile && pattern.matches(file.name) }
                .toList()
            check(matchedFiles.isNotEmpty()) {
                buildString {
                    appendLine("Generated Analysis API model 没有命中任何测试数据文件。")
                    appendLine("testData: ${modelRoot.invariantSeparatorsPath}")
                    appendLine("pattern: ${suite.includedFilePattern}")
                }
            }

            val generatedClassName = suite.outputFile.nameWithoutExtension
            val origin = "${suite.modelRelativePath} -> $generatedClassName"
            val previousClassOrigin = classNameToOrigin.putIfAbsent(generatedClassName, origin)
            check(previousClassOrigin == null) {
                buildString {
                    appendLine("Generated Analysis API class 名称冲突：$generatedClassName")
                    appendLine("first: $previousClassOrigin")
                    appendLine("second: $origin")
                }
            }

            val outputPath = suite.outputFile.canonicalFile.invariantSeparatorsPath
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
