package org.cangnova.cangjie.test.runners
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import com.intellij.testFramework.TestDataFile
import com.intellij.testIntegration.TestFailedLineManager
import org.cangnova.cangjie.test.CangJieTestInfo
import org.cangnova.cangjie.test.NonGroupingTestRunner
import org.cangnova.cangjie.test.TestInfrastructureInternals
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder
import org.cangnova.cangjie.test.builders.nonGroupingPhaseTestRunner
import org.cangnova.cangjie.test.model.ResultingArtifact
import org.cangnova.cangjie.test.toCangJieTestInfo

abstract class AbstractCangjieCompilerTest {
    @OptIn(TestInfrastructureInternals::class)
    protected open val configuration: TestConfigurationBuilder.() -> Unit = {
        startingArtifactFactory = { ResultingArtifact.Source() }
        @OptIn(TestInfrastructureInternals::class)
        testInfo = this@AbstractCangjieCompilerTest.testInfo

        configureInternal(this)
    }

    abstract fun configure(builder: TestConfigurationBuilder)

    @TestInfrastructureInternals
    protected open fun configureInternal(builder: TestConfigurationBuilder) {
        configure(builder)
    }
    private lateinit var testInfo: CangJieTestInfo

    lateinit var testRunner: NonGroupingTestRunner
        private set

    open fun runTest(@TestDataFile filePath: String) {
        initTestRunner(filePath).runTest(filePath)
    }
    @BeforeEach
    fun initTestInfo(testInfo:  TestInfo) {
        initTestInfo(testInfo.toCangJieTestInfo())
    }

    fun initTestInfo(testInfo: CangJieTestInfo) {
        this.testInfo = testInfo
    }

    fun initTestRunner(@TestDataFile filePath: String): NonGroupingTestRunner {
        return nonGroupingPhaseTestRunner(filePath, configuration).also { testRunner = it }
    }

    fun initTestRunnerAndCreateModuleStructure(@TestDataFile filePath: String) {
        initTestRunner(filePath).prepareModuleStructure(filePath)
    }
}
