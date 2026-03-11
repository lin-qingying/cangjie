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

enum class JdkMajorVersion(
    val majorVersion: Int,
    val targetName: String = majorVersion.toString(),
) {
    JDK_1_8(8, targetName = "1.8"),
    JDK_11_0(11),
    JDK_17_0(17),
    JDK_21_0(21);

    val envName = name
}

val DEFAULT_JVM_TOOLCHAIN = JdkMajorVersion.JDK_17_0

fun Project.configureJvmDefaultToolchain() {
    configureJvmToolchain(DEFAULT_JVM_TOOLCHAIN)
}

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

fun JavaToolchainSpec.setupToolchain(jdkVersion: JdkMajorVersion) {
    languageVersion.set(JavaLanguageVersion.of(jdkVersion.majorVersion))
}

fun Project.configureJavaOnlyToolchain(jdkVersion: JdkMajorVersion) {
    plugins.withId("java-base") {
        val javaExtension = extensions.getByType<JavaPluginExtension>()
        javaExtension.toolchain {
            setupToolchain(jdkVersion)
        }
    }
}

fun JavaCompile.configureTaskToolchain(jdkVersion: JdkMajorVersion) {
    javaCompiler.set(project.getToolchainCompilerFor(jdkVersion))
}

private fun Project.getToolchainCompilerFor(jdkVersion: JdkMajorVersion): Provider<JavaCompiler> {
    val service = extensions.getByType<JavaToolchainService>()
    return service.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(jdkVersion.majorVersion))
    }
}

fun Project.getToolchainLauncherFor(jdkVersion: JdkMajorVersion): Provider<JavaLauncher> {
    val service = extensions.getByType<JavaToolchainService>()
    return service.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(jdkVersion.majorVersion))
    }
}

fun Project.getToolchainJdkHomeFor(jdkVersion: JdkMajorVersion): Provider<String> {
    return getToolchainLauncherFor(jdkVersion).map {
        it.metadata.installationPath.asFile.absolutePath
    }
}
