

package org.jetbrains.kotlin.generators.dsl.junit5

import org.jetbrains.kotlin.generators.InconsistencyChecker
import org.jetbrains.kotlin.generators.allowGenerationOnTeamCity
import org.jetbrains.kotlin.generators.dsl.TestGroupSuite
import org.jetbrains.kotlin.generators.dsl.forEachTestClassParallel
import org.jetbrains.kotlin.generators.model.TestInfraRevision
import org.jetbrains.kotlin.generators.skipTestAllFilesCheck
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

/**
 * 使用 JUnit5 生成测试套件，并从命令行参数读取 dry-run、TeamCity 写入和覆盖检查开关。
 *
 * @param args 生成器主函数接收到的命令行参数。
 * @param mainClassName 生成文件头中记录的生成入口类名。
 * @param init 测试套件 DSL 配置。
 */
fun generateTestGroupSuiteWithJUnit5(
    args: Array<String>,
    mainClassName: String? = TestGeneratorUtil.getMainClassName(),
    init: TestGroupSuite.() -> Unit,
) {
    generateTestGroupSuiteWithJUnit5(
        dryRun = InconsistencyChecker.hasDryRunArg(args),
        allowGenerationOnTeamCity = args.allowGenerationOnTeamCity(),
        skipTestAllFilesCheck = args.skipTestAllFilesCheck(),
        mainClassName,
        init
    )
}

/**
 * 使用 JUnit5 生成测试套件。
 *
 * @param dryRun 为 true 时只检查生成内容是否与磁盘一致，不写入文件。
 * @param allowGenerationOnTeamCity 为 true 时允许在 TeamCity 上实际写入生成文件。
 * @param skipTestAllFilesCheck 为 true 时默认不生成测试数据覆盖检查方法。
 * @param mainClassName 生成文件头中记录的生成入口类名。
 * @param init 测试套件 DSL 配置。
 */
fun generateTestGroupSuiteWithJUnit5(
    dryRun: Boolean = false,
    allowGenerationOnTeamCity: Boolean = false,
    skipTestAllFilesCheck: Boolean = false,
    mainClassName: String? = TestGeneratorUtil.getMainClassName(),
    init: TestGroupSuite.() -> Unit,
) {
    val suite = TestGroupSuite(TestInfraRevision.StandardJUnit5, skipTestAllFilesCheck).apply(init)
    suite.forEachTestClassParallel { testClass ->
        val (changed, testSourceFilePath) = TestGeneratorForJUnit5
            .generateAndSave(testClass, dryRun, allowGenerationOnTeamCity, mainClassName)
        if (changed) {
            InconsistencyChecker.inconsistencyChecker(dryRun).add(testSourceFilePath)
        }
    }
}
