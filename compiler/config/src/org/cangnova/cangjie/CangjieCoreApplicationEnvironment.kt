package org.cangnova.cangjie

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockApplication
import com.intellij.openapi.Disposable

/**
 * 仓颉核心应用环境。
 *
 * 对齐 Kotlin `KotlinCoreApplicationEnvironment` 的职责：
 * - 承载 headless Application 容器
 * - 统一装配 IntelliJ PSI / Symbol / Target 所需的平台能力
 * - 统一装配仓颉语言自己的 FileType / ParserDefinition 基础设施
 */
class CangjieCoreApplicationEnvironment private constructor(
    parentDisposable: Disposable,
    private val environmentMode: CangJieCoreEnvironmentMode,
) : CoreApplicationEnvironment(parentDisposable, environmentMode == CangJieCoreEnvironmentMode.UnitTest) {
    init {
        CangJieHeadlessPlatformBootstrap.initializeApplicationEnvironment(this)
    }

    override fun createApplication(parentDisposable: Disposable): MockApplication {
        val application = super.createApplication(parentDisposable)
        return if (application.isUnitTestMode) {
            CangJieCoreUnitTestApplication(parentDisposable)
        } else {
            application
        }
    }

    companion object {
        fun create(
            parentDisposable: Disposable,
            environmentMode: CangJieCoreEnvironmentMode = CangJieCoreEnvironmentMode.UnitTest,
        ): CangjieCoreApplicationEnvironment {
            return CangjieCoreApplicationEnvironment(parentDisposable, environmentMode)
        }
    }
}
