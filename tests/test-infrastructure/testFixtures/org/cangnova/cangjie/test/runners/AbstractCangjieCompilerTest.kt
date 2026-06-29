package org.cangnova.cangjie.test.runners

import com.intellij.testFramework.TestDataFile
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.test.CangJieTestInfo
import org.cangnova.cangjie.test.Constructor
import org.cangnova.cangjie.test.NonGroupingTestRunner
import org.cangnova.cangjie.test.TestInfrastructureInternals
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder
import org.cangnova.cangjie.test.builders.nonGroupingPhaseTestRunner
import org.cangnova.cangjie.test.model.ResultingArtifact
import org.cangnova.cangjie.test.services.MetaInfosCleanupPreprocessor
import org.cangnova.cangjie.test.services.SourceFilePreprocessor
import org.cangnova.cangjie.test.services.TemporaryDirectoryManager
import org.cangnova.cangjie.test.services.impl.JUnit5Assertions
import org.cangnova.cangjie.test.services.impl.TemporaryDirectoryManagerImpl
import org.cangnova.cangjie.test.toCangJieTestInfo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInfo

/**
 * 表示 `AbstractCangjieCompilerTest`，承载测试运行器中的配置数据、测试产物或处理步骤。
 */
abstract class AbstractCangjieCompilerTest {

    /**
     * 维护 `previousSlowAssertions`，供测试运行器在测试执行期间读取或传递。
     */
    private var previousSlowAssertions: Boolean = false

    companion object {
        val defaultPreprocessors: List<Constructor<SourceFilePreprocessor>> = listOf(
            ::MetaInfosCleanupPreprocessor,

            )
    }

    /**
     * 保存 `configuration`，供测试运行器在测试执行期间读取或传递。
     */
    @OptIn(TestInfrastructureInternals::class)
    protected open val configuration: TestConfigurationBuilder.() -> Unit = {
        assertions = JUnit5Assertions
        useSourcePreprocessor(*defaultPreprocessors.toTypedArray())

        useAdditionalService<TemporaryDirectoryManager>(::TemporaryDirectoryManagerImpl)
        startingArtifactFactory = { ResultingArtifact.Source() }
        @OptIn(TestInfrastructureInternals::class)
        testInfo = this@AbstractCangjieCompilerTest.testInfo

        configureInternal(this)
    }

    /**
     * 提供 `configure` 对应的测试运行器流程，维持测试框架的阶段契约。
     */
    abstract fun configure(builder: TestConfigurationBuilder)

    /**
     * 提供 `configureInternal` 对应的测试运行器流程，维持测试框架的阶段契约。
     */
    @TestInfrastructureInternals
    protected open fun configureInternal(builder: TestConfigurationBuilder) {
        configure(builder)
    }

    /**
     * 保存 `testInfo`，供测试运行器在测试执行期间读取或传递。
     */
    private lateinit var testInfo: CangJieTestInfo

    /**
     * 保存 `testRunner`，供测试运行器在测试执行期间读取或传递。
     */
    lateinit var testRunner: NonGroupingTestRunner
        private set

    /**
     * 提供 `runTest` 对应的测试运行器流程，维持测试框架的阶段契约。
     */
    open fun runTest(@TestDataFile filePath: String) {
        initTestRunner(filePath).runTest(filePath)
    }

    /**
     * 执行 `initTestInfo` 对应的测试运行器流程，维持测试框架的阶段契约。
     */
    @BeforeEach
    fun initTestInfo(testInfo: TestInfo) {
        previousSlowAssertions = AbstractTypeChecker.RUN_SLOW_ASSERTIONS
        AbstractTypeChecker.RUN_SLOW_ASSERTIONS = java.lang.Boolean.getBoolean("cangjie.slow.assertions")
        initTestInfo(testInfo.toCangJieTestInfo())
    }

    /**
     * 执行 `restoreSlowAssertionsFlag` 对应的测试运行器流程，维持测试框架的阶段契约。
     */
    @AfterEach
    fun restoreSlowAssertionsFlag() {
        AbstractTypeChecker.RUN_SLOW_ASSERTIONS = previousSlowAssertions
    }

    /**
     * 执行 `initTestInfo` 对应的测试运行器流程，维持测试框架的阶段契约。
     */
    fun initTestInfo(testInfo: CangJieTestInfo) {
        this.testInfo = testInfo
    }

    /**
     * 执行 `initTestRunner` 对应的测试运行器流程，维持测试框架的阶段契约。
     */
    fun initTestRunner(@TestDataFile filePath: String): NonGroupingTestRunner {
        return nonGroupingPhaseTestRunner(filePath, configuration).also { testRunner = it }
    }

    /**
     * 执行 `initTestRunnerAndCreateModuleStructure` 对应的测试运行器流程，维持测试框架的阶段契约。
     */
    fun initTestRunnerAndCreateModuleStructure(@TestDataFile filePath: String) {
        initTestRunner(filePath).prepareModuleStructure(filePath)
    }
}
