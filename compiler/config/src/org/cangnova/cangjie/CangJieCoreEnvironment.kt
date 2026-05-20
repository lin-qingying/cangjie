package org.cangnova.cangjie

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Path

sealed interface CangJieCoreEnvironmentMode {
    data object Production : CangJieCoreEnvironmentMode
    data object UnitTest : CangJieCoreEnvironmentMode
}

/**
 * 仓颉 headless 核心环境入口。
 *
 * 这里负责：
 * - 统一创建 application / project 两级环境
 * - 维护 IntelliJ headless 容器运行所需的基础系统属性
 *
 * 真正的平台能力注册已经下沉到 `CangjieCoreApplicationEnvironment`
 * 与 `CangjieCoreProjectEnvironment`，避免继续由入口类拼装细节。
 */
class CangJieCoreEnvironment private constructor(
    val projectEnvironment: CangjieCoreProjectEnvironment,
) {
    val project: Project
        get() = projectEnvironment.project

    val applicationEnvironment: CangjieCoreApplicationEnvironment
        get() = projectEnvironment.environment as CangjieCoreApplicationEnvironment

    companion object {
        fun createForTests(
            parentDisposable: Disposable,
        ): CangJieCoreEnvironment = create(parentDisposable, CangJieCoreEnvironmentMode.UnitTest)

        fun create(
            parentDisposable: Disposable,
            mode: CangJieCoreEnvironmentMode,
        ): CangJieCoreEnvironment {
            ensureIdeaStandaloneProperties()
            val applicationEnvironment = CangjieCoreApplicationEnvironment.create(
                parentDisposable = parentDisposable,
                environmentMode = mode,
            )
            val projectEnvironment = CangjieCoreProjectEnvironment(parentDisposable, applicationEnvironment)
            return CangJieCoreEnvironment(projectEnvironment)
        }

        private fun ensureIdeaStandaloneProperties() {
            // 这个核心环境面向 CLI / LSP / 单测等无界面宿主，必须显式声明 AWT headless。
            // 否则 IntelliJ 253+ 的 JBUIScale 会把纯 PSI/高亮测试误判成带界面启动流程，
            // 在 `LoadingState.APP_STARTED` 之前访问 UI 默认值时直接触发 "Must be precomputed"。
            System.setProperty(JAVA_AWT_HEADLESS_PROPERTY, "true")
            if (System.getProperty(IDEA_HOME_PATH_PROPERTY).isNullOrBlank()) {
                System.setProperty(IDEA_HOME_PATH_PROPERTY, ideaHomePath.toString())
            }
            if (System.getProperty(IDEA_CONFIG_PATH_PROPERTY).isNullOrBlank()) {
                System.setProperty(IDEA_CONFIG_PATH_PROPERTY, Files.createDirectories(processTmpRoot.resolve("config")).toString())
            }
            if (System.getProperty(IDEA_SYSTEM_PATH_PROPERTY).isNullOrBlank()) {
                System.setProperty(IDEA_SYSTEM_PATH_PROPERTY, Files.createDirectories(processTmpRoot.resolve("system")).toString())
            }
            if (System.getProperty(IDEA_PLUGINS_COMPATIBLE_BUILD_PROPERTY).isNullOrBlank()) {
                System.setProperty(IDEA_PLUGINS_COMPATIBLE_BUILD_PROPERTY, "999.SNAPSHOT")
            }
            if (System.getProperty(IDEA_IGNORE_DISABLED_PLUGINS_PROPERTY).isNullOrBlank()) {
                System.setProperty(IDEA_IGNORE_DISABLED_PLUGINS_PROPERTY, "true")
            }
        }

        private const val IDEA_HOME_PATH_PROPERTY = "idea.home.path"
        private const val IDEA_CONFIG_PATH_PROPERTY = "idea.config.path"
        private const val IDEA_SYSTEM_PATH_PROPERTY = "idea.system.path"
        private const val IDEA_PLUGINS_COMPATIBLE_BUILD_PROPERTY = "idea.plugins.compatible.build"
        private const val IDEA_IGNORE_DISABLED_PLUGINS_PROPERTY = "idea.ignore.disabled.plugins"
        private const val JAVA_AWT_HEADLESS_PROPERTY = "java.awt.headless"

        private val processTmpRoot: Path by lazy {
            Files.createTempDirectory("cangjie-test-intellij-home")
        }

        private val ideaHomePath: Path by lazy {
            val home = Files.createDirectories(processTmpRoot.resolve("idea-home"))
            val bin = Files.createDirectories(home.resolve("bin"))
            Files.writeString(home.resolve("build.txt"), "IC-999.SNAPSHOT")
            Files.writeString(bin.resolve("idea.properties"), "idea.config.path=\n")
            home
        }
    }
}
