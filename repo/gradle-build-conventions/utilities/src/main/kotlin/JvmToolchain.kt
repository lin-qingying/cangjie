@file:JvmName("JvmToolchain")

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaCompiler
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.gradle.kotlin.dsl.getByType

/**
 * 仓库构建支持的 JDK 主版本。
 *
 * 枚举同时承载 Gradle toolchain 使用的语言版本号，以及旧式任务命名中使用的目标名称。
 */
enum class JdkMajorVersion(
    /**
     * 传递给 Gradle Java toolchain 的语言版本号。
     */
    val majorVersion: Int,
    /**
     * 构建任务、配置名或环境描述中展示的目标版本名称。
     */
    val targetName: String = majorVersion.toString(),
) {
    /**
     * JDK 8 工具链，Gradle 目标名称沿用传统的 `1.8` 写法。
     */
    JDK_1_8(8, targetName = "1.8"),
    /**
     * JDK 11 工具链。
     */
    JDK_11_0(11),
    /**
     * JDK 17 工具链，是当前仓库默认编译工具链。
     */
    JDK_17_0(17),
    /**
     * JDK 21 工具链，用于需要新运行时验证的模块。
     */
    JDK_21_0(21);

    /**
     * 与枚举项名称一致的环境变量/配置标识名称。
     */
    val envName = name
}

/**
 * 仓库默认 JVM toolchain。
 */
val DEFAULT_JVM_TOOLCHAIN = JdkMajorVersion.JDK_17_0

/**
 * 为当前项目配置仓库默认 JVM toolchain。
 */
fun Project.configureJvmDefaultToolchain() {
    configureJvmToolchain(DEFAULT_JVM_TOOLCHAIN)
}

/**
 * 为当前项目配置 Java toolchain，并统一 JavaCompile 的注解处理和源码编码策略。
 */
fun Project.configureJvmToolchain(jdkVersion: JdkMajorVersion) {
    configureJavaOnlyToolchain(jdkVersion)

    tasks
        .matching { it is JavaCompile }
        .configureEach {
            with(this as JavaCompile) {
                options.compilerArgs.add("-proc:none")
                options.encoding = "UTF-8"
            }
        }
}

/**
 * 将 [jdkVersion] 写入 Gradle Java toolchain 规格。
 */
fun JavaToolchainSpec.setupToolchain(jdkVersion: JdkMajorVersion) {
    languageVersion.set(JavaLanguageVersion.of(jdkVersion.majorVersion))
}

/**
 * 仅配置 Java 插件暴露的 toolchain，不额外修改编译任务参数。
 */
fun Project.configureJavaOnlyToolchain(jdkVersion: JdkMajorVersion) {
    plugins.withId("java-base") {
        val javaExtension = extensions.getByType<JavaPluginExtension>()
        javaExtension.toolchain {
            setupToolchain(jdkVersion)
        }
    }
}

/**
 * 为单个 [JavaCompile] 任务绑定指定 JDK 的编译器。
 */
fun JavaCompile.configureTaskToolchain(jdkVersion: JdkMajorVersion) {
    javaCompiler.set(project.getToolchainCompilerFor(jdkVersion))
}

/**
 * 获取指定 JDK toolchain 对应的 Java 编译器 Provider。
 */
private fun Project.getToolchainCompilerFor(jdkVersion: JdkMajorVersion): Provider<JavaCompiler> {
    val service = extensions.getByType<JavaToolchainService>()
    return service.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(jdkVersion.majorVersion))
    }
}

/**
 * 获取指定 JDK toolchain 对应的 Java 启动器 Provider。
 */
fun Project.getToolchainLauncherFor(jdkVersion: JdkMajorVersion): Provider<JavaLauncher> {
    val service = extensions.getByType<JavaToolchainService>()
    return service.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(jdkVersion.majorVersion))
    }
}

/**
 * 获取指定 JDK toolchain 的安装目录路径。
 */
fun Project.getToolchainJdkHomeFor(jdkVersion: JdkMajorVersion): Provider<String> {
    return getToolchainLauncherFor(jdkVersion).map {
        it.metadata.installationPath.asFile.absolutePath
    }
}
