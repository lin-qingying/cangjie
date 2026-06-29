package org.cangnova.cangjie.analysis.test.framework.base

import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

/**
 * 手动（非生成）Analysis API 测试的基类（对齐 Kotlin 的 AbstractAnalysisApiExecutionTest）。
 *
 * 测试方法名必须等于 testData 目录中的文件名（无扩展名）。
 *
 * 测试方法可声明以下参数类型，由框架自动注入：
 * - [CjFile]（主文件）
 * - [TestServices]
 *
 * @param testDirPathString testData 目录路径（相对于项目根目录）
 */
@ExtendWith(AnalysisApiExecutionTestExtension::class)
abstract class AbstractAnalysisApiExecutionTest(
    /**
     * 当前手写测试使用的 testData 目录路径。
     */
    val testDirPathString: String,
) : AbstractAnalysisApiBasedTest() {

    /**
     * 由 JUnit extension 调用的底层执行入口。
     */
    @Deprecated("Handled by the test infrastructure. Avoid calling directly")
    fun performTest(path: String, block: (TestServices, CjFile?, CjTestModule) -> Unit) {
        runTest(path) { testServices ->
            val (mainFile, mainModule) = findMainFileAndModule(testServices)
            block(testServices, mainFile, mainModule)
        }
    }
}

/**
 * 为手写 Analysis API 测试提供 testData 执行和参数注入的 JUnit5 extension。
 */
internal class AnalysisApiExecutionTestExtension :
    BeforeTestExecutionCallback, AfterTestExecutionCallback, ParameterResolver {

    /**
     * Extension 支持自动注入的参数类型。
     */
    private companion object {
        private val SUPPORTED_PARAMETER_TYPES = listOf(
            TestServices::class.java,
            CjFile::class.java,
            CjTestModule::class.java,
        )
    }

    /**
     * 单个测试方法执行期间缓存的参数状态。
     */
    private class State(
        /**
         * 当前测试服务容器。
         */
        val testServices: TestServices,
        /**
         * 当前测试主文件。
         */
        val mainFile: CjFile?,
        /**
         * 当前测试主模块。
         */
        val mainModule: CjTestModule,
    )

    /**
     * 当前线程正在执行的测试状态。
     */
    private val cachedState = ThreadLocal<State>()

    /**
     * 判断当前参数是否可由 Analysis API 测试 extension 注入。
     */
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        val parameterType = parameterContext.parameter.type
        return SUPPORTED_PARAMETER_TYPES.any { it.isAssignableFrom(parameterType) }
    }

    /**
     * 根据参数类型返回当前测试状态中的对应对象。
     */
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any? {
        val state = cachedState.get() ?: error("State is not cached yet")
        val parameterType = parameterContext.parameter.type
        return when {
            TestServices::class.java.isAssignableFrom(parameterType) -> state.testServices
            CjFile::class.java.isAssignableFrom(parameterType) -> state.mainFile
            CjTestModule::class.java.isAssignableFrom(parameterType) -> state.mainModule
            else -> error("Unsupported parameter type $parameterType")
        }
    }

    /**
     * 在测试方法执行前解析 testData 并缓存参数状态。
     */
    override fun beforeTestExecution(context: ExtensionContext) {
        val testInstance = context.requiredTestInstance as AbstractAnalysisApiExecutionTest
        val testFilePath = getTestFilePath(testInstance.testDirPathString, context.requiredTestMethod.name)

        @Suppress("DEPRECATION")
        testInstance.performTest(testFilePath.toString()) { testServices, mainFile, mainModule ->
            require(cachedState.get() == null)
            cachedState.set(State(testServices, mainFile, mainModule))
        }
    }

    /**
     * 在测试方法结束后清空线程本地状态。
     */
    override fun afterTestExecution(context: ExtensionContext?) {
        cachedState.remove()
    }

    /**
     * 根据测试目录和方法名定位实际 testData 文件。
     */
    private fun getTestFilePath(testDirPathString: String, testFileName: String): Path {
        val workspaceRoot = locateWorkspaceRoot(Paths.get("").toAbsolutePath().normalize())
        val candidates = listOf(
            Paths.get(testDirPathString, "$testFileName.cj"),
        )

        candidates.firstOrNull { candidate -> candidate.exists() }?.let { return it }
        candidates
            .asSequence()
            .map { candidate -> workspaceRoot.resolve(candidate).normalize() }
            .firstOrNull { candidate -> candidate.exists() }
            ?.let { return it }

        error("Cannot find test file $testFileName.cj in $testDirPathString")
    }

    /**
     * 统一从当前工作目录向上定位仓库根目录。
     *
     * Gradle 在不同任务和不同 IDE 启动方式下，测试进程的 cwd 可能是仓库根、模块根或临时目录。
     * 测试框架不应该把 testData 定位语义绑定到这些偶然差异上，因此这里显式以 `settings.gradle.kts`
     * 作为仓库根锚点。
     */
    private fun locateWorkspaceRoot(start: Path): Path {
        return generateSequence(start) { current -> current.parent }
            .firstOrNull { candidate -> candidate.resolve("settings.gradle.kts").isRegularFile() }
            ?: start
    }
}
