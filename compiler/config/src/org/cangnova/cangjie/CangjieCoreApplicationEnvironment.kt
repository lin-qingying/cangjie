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
    /**
     * 当前核心环境运行模式，决定 Application 是否按单测模式创建。
     */
    private val environmentMode: CangJieCoreEnvironmentMode,
) : CoreApplicationEnvironment(parentDisposable, environmentMode == CangJieCoreEnvironmentMode.UnitTest) {
    init {
        CangJieHeadlessPlatformBootstrap.initializeApplicationEnvironment(this)
    }

    /**
     * 创建 IntelliJ mock application，并在单测模式下替换为支持写动作状态跟踪的实现。
     */
    override fun createApplication(parentDisposable: Disposable): MockApplication {
        val application = super.createApplication(parentDisposable)
        return if (application.isUnitTestMode) {
            CangJieCoreUnitTestApplication(parentDisposable)
        } else {
            application
        }
    }

    companion object {
        /**
         * 创建仓颉核心 application 环境，并在构造期间完成 headless 平台能力装配。
         */
        fun create(
            parentDisposable: Disposable,
            environmentMode: CangJieCoreEnvironmentMode = CangJieCoreEnvironmentMode.UnitTest,
        ): CangjieCoreApplicationEnvironment {
            return CangjieCoreApplicationEnvironment(parentDisposable, environmentMode)
        }
    }
}
