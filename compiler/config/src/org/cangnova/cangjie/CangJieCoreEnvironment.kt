package org.cangnova.cangjie

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Path

/**
 * 仓颉核心环境运行模式。
 */
sealed interface CangJieCoreEnvironmentMode {
    /**
     * 面向命令行、LSP 等真实宿主的生产模式。
     */
    data object Production : CangJieCoreEnvironmentMode

    /**
     * 面向测试的模式，会使用单测 application 并启用写动作检测。
     */
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
    /**
     * 当前核心环境持有的项目级环境。
     */
    val projectEnvironment: CangjieCoreProjectEnvironment,
) {
    /**
     * 当前核心环境暴露的 IntelliJ project。
     */
    val project: Project
        get() = projectEnvironment.project

    /**
     * 当前核心环境暴露的 application 级环境。
     */
    val applicationEnvironment: CangjieCoreApplicationEnvironment
        get() = projectEnvironment.environment as CangjieCoreApplicationEnvironment

    companion object {
        /**
         * 创建用于单元测试的核心环境。
         */
        fun createForTests(
            parentDisposable: Disposable,
        ): CangJieCoreEnvironment = create(parentDisposable, CangJieCoreEnvironmentMode.UnitTest)

        /**
         * 按指定运行模式创建完整的 application/project 两级核心环境。
         */
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

        /**
         * 填充 IntelliJ standalone/headless 启动所需的基础系统属性。
         */
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

        /**
         * IntelliJ 读取 IDE home 位置的系统属性名。
         */
        private const val IDEA_HOME_PATH_PROPERTY = "idea.home.path"

        /**
         * IntelliJ 读取配置目录位置的系统属性名。
         */
        private const val IDEA_CONFIG_PATH_PROPERTY = "idea.config.path"

        /**
         * IntelliJ 读取 system 目录位置的系统属性名。
         */
        private const val IDEA_SYSTEM_PATH_PROPERTY = "idea.system.path"

        /**
         * 允许平台插件在 standalone 环境中通过兼容构建号检查的系统属性名。
         */
        private const val IDEA_PLUGINS_COMPATIBLE_BUILD_PROPERTY = "idea.plugins.compatible.build"

        /**
         * 要求平台忽略 disabled plugins 列表的系统属性名。
         */
        private const val IDEA_IGNORE_DISABLED_PLUGINS_PROPERTY = "idea.ignore.disabled.plugins"

        /**
         * AWT headless 模式系统属性名。
         */
        private const val JAVA_AWT_HEADLESS_PROPERTY = "java.awt.headless"

        /**
         * 当前进程内临时 IntelliJ home/config/system 根目录。
         */
        private val processTmpRoot: Path by lazy {
            Files.createTempDirectory("cangjie-test-intellij-home")
        }

        /**
         * 写入最小 `build.txt` 和 `idea.properties` 的临时 IDE home。
         */
        private val ideaHomePath: Path by lazy {
            val home = Files.createDirectories(processTmpRoot.resolve("idea-home"))
            val bin = Files.createDirectories(home.resolve("bin"))
            Files.writeString(home.resolve("build.txt"), "IC-999.SNAPSHOT")
            Files.writeString(bin.resolve("idea.properties"), "idea.config.path=\n")
            home
        }
    }
}
