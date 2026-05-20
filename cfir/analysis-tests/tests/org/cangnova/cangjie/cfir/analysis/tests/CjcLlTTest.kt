package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCjcLlTDiagnosticsConsistencyTest
import org.cangnova.cangjie.cfir.analysis.tests.golden.CjcProcessRunner
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.nio.file.Path

/**
 * cjc 官方编译器诊断一致性测试（LLT 全量数据）。
 *
 * 入口职责：
 * 1. 枚举 `testData/llt` 下全部 `.cj`；
 * 2. 每个用例调用框架 `runTest`；
 * 3. 具体 CFIR vs CJC 诊断比较由测试基类中挂载的 after-analysis checker 完成。
 */
@Disabled("LLT 官方一致性测试不作为当前 CFIR analysis-tests 门禁执行")
class CjcLlTTest : AbstractCjcLlTDiagnosticsConsistencyTest() {

    private val testDataDir = File("cfir/analysis-tests/testData/llt")

    private val cjcPath: Path by lazy { CjcProcessRunner.findCjcPath() }

    @TestFactory
    fun lltDiagnosticsConsistencyTests(): List<DynamicTest> {
        check(testDataDir.isDirectory) {
            "Test data directory not found: ${testDataDir.absolutePath}"
        }
        check(cjcPath.toFile().exists()) {
            "cjc not found at $cjcPath. Set CANGJIE_HOME environment variable."
        }

        val testFiles = testDataDir.walkTopDown()
            .filter { it.isFile && it.extension == "cj" }
            .sortedBy { it.relativeTo(testDataDir).path }
            .toList()

        check(testFiles.isNotEmpty()) {
            "No .cj test files found in ${testDataDir.absolutePath}"
        }

        return testFiles.map { file ->
            val relativePath = file.relativeTo(testDataDir).path.replace('\\', '/')
            DynamicTest.dynamicTest(relativePath) {
                runTest(file.path.replace('\\', '/'))
            }
        }
    }
}
