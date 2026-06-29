package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.test.WrappedException
import org.cangnova.cangjie.test.model.AfterAnalysisChecker
import org.cangnova.cangjie.test.model.FrontendKinds
import org.cangnova.cangjie.test.model.TestArtifactKind
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.moduleStructure
import java.io.File

/**
 * 表示 `AbstractFailingFacadeSuppressor`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
abstract class AbstractFailingFacadeSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {

    /**
     * 提供 `testFile` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    protected abstract fun testFile(): File

    /**
     * 保存 `facadeKind`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    protected abstract val facadeKind: TestArtifactKind<*>

    /**
     * 保存 `order`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    override val order: Order
        get() = Order.P5

    /**
     * 执行 `suppressIfNeeded` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val failFile = testFile().parentFile.resolve("${testFile().nameWithoutExtension}.fail").takeIf { it.exists() }
            ?: return failedAssertions
        val (suppressible, notSuppressible) = failedAssertions.partition {
            when (it) {
                is WrappedException.FromFacade -> it.facade.outputKind == facadeKind
                is WrappedException.FromHandler -> it.handler.artifactKind == facadeKind
                is WrappedException.FromMetaInfoHandler -> true
                else -> false
            }
        }

        return when {
            suppressible.isNotEmpty() -> notSuppressible

            // LL FIR is a Kotlin-specific concept and is not used in this project.
            testServices.moduleStructure.originalTestDataFiles.first().isLLCfirTestData -> failedAssertions
            else -> failedAssertions + AssertionError("Fail file exists but no exceptions was thrown. Please remove ${failFile.name}").wrap()
        }
    }
}

/**
 * 表示 `CfirFailingTestSuppressor`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
class CfirFailingTestSuppressor(testServices: TestServices) : AbstractFailingFacadeSuppressor(testServices) {

    /**
     * 执行 `testFile` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    override fun testFile(): File {
        return testServices.moduleStructure.originalTestDataFiles.first().cfirTestDataFile
    }

    /**
     * 保存 `facadeKind`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    override val facadeKind: TestArtifactKind<*>
        get() = FrontendKinds.CFIR
}

/**
 * 保存 `CFIR_PREFIX`，供CFIR 前端测试在测试执行期间读取或传递。
 */
private const val CFIR_PREFIX = ".cfir"

/**
 * 保存 `File.isLLCfirTestData`，供CFIR 前端测试在测试执行期间读取或传递。
 */
private val File.isLLCfirTestData: Boolean
    get() = false

/**
 * 保存 `File.cfirTestDataFile`，供CFIR 前端测试在测试执行期间读取或传递。
 */
private val File.cfirTestDataFile: File
    get() {
        val extensionWithDot = ".$extension"
        if (name.endsWith("$CFIR_PREFIX$extensionWithDot")) return this

        val candidate = parentFile.resolve("${nameWithoutExtension}$CFIR_PREFIX$extensionWithDot")
        return if (candidate.exists()) candidate else this
    }
