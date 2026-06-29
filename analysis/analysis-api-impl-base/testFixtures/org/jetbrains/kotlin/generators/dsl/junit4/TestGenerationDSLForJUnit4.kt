

package org.jetbrains.kotlin.generators.dsl.junit4

import org.jetbrains.kotlin.generators.InconsistencyChecker
import org.jetbrains.kotlin.generators.InconsistencyChecker.Companion.inconsistencyChecker
import org.jetbrains.kotlin.generators.allowGenerationOnTeamCity
import org.jetbrains.kotlin.generators.dsl.TestGroupSuite
import org.jetbrains.kotlin.generators.dsl.forEachTestClassParallel
import org.jetbrains.kotlin.generators.model.TestInfraRevision
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

/**
 * 使用 JUnit4 生成测试套件，并从命令行参数读取 dry-run 与 TeamCity 写入开关。
 *
 * @param args 生成器主函数接收到的命令行参数。
 * @param mainClassName 生成文件头中记录的生成入口类名。
 * @param init 测试套件 DSL 配置。
 */
fun generateTestGroupSuiteWithJUnit4(
    args: Array<String>,
    mainClassName: String? = TestGeneratorUtil.getMainClassName(),
    init: TestGroupSuite.() -> Unit
) {
    generateTestGroupSuiteWithJUnit4(
        dryRun = InconsistencyChecker.hasDryRunArg(args),
        allowGenerationOnTeamCity = args.allowGenerationOnTeamCity(),
        mainClassName,
        init
    )
}

/**
 * 使用 JUnit4 生成测试套件。
 *
 * @param dryRun 为 true 时只检查生成内容是否与磁盘一致，不写入文件。
 * @param allowGenerationOnTeamCity 为 true 时允许在 TeamCity 上实际写入生成文件。
 * @param mainClassName 生成文件头中记录的生成入口类名。
 * @param init 测试套件 DSL 配置。
 */
fun generateTestGroupSuiteWithJUnit4(
    dryRun: Boolean = false,
    allowGenerationOnTeamCity: Boolean = false,
    mainClassName: String? = TestGeneratorUtil.getMainClassName(),
    init: TestGroupSuite.() -> Unit,
) {
    val suite = TestGroupSuite(TestInfraRevision.LegacyJUnit4, defaultSkipTestAllFilesCheck = false).apply(init)
    suite.forEachTestClassParallel { testClass ->
        val (changed, testSourceFilePath) = TestGeneratorForJUnit4
            .generateAndSave(testClass, dryRun, allowGenerationOnTeamCity, mainClassName)
        if (changed) {
            inconsistencyChecker(dryRun).add(testSourceFilePath)
        }
    }
}
