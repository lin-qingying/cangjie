import org.gradle.api.Project
import org.gradle.kotlin.dsl.project

/**
 * IntelliJ Platform 依赖管理（对齐 Kotlin K2 编译器模式）。
 *
 * 核心依赖通过聚合模块 :dependencies:intellij-core 统一管理，
 * 各模块使用 compileOnly(intellijCore()) 引用。
 *
 * @see <a href="https://github.com/JetBrains/kotlin/blob/master/repo/gradle-build-conventions/buildsrc-compat/src/main/kotlin/intellijDependencies.kt">
 *   Kotlin 参考实现</a>
 */

val Project.intellijSdkVersion: String
    get() = property("intellijSdkVersion") as String

/** IntelliJ Platform 核心聚合依赖（对齐 Kotlin :dependencies:intellij-core） */
fun Project.intellijCore() = dependencies.project(":dependencies:intellij-core")

// ===== 单独引用（用于只需要部分依赖的模块） =====

fun Project.intellijPlatformUtil() = "com.jetbrains.intellij.platform:util:$intellijSdkVersion"
fun Project.intellijPlatformUtilBase() = "com.jetbrains.intellij.platform:util-base:$intellijSdkVersion"
fun Project.intellijUtilRt() = "com.jetbrains.intellij.platform:util-rt:$intellijSdkVersion"
fun Project.intellijAnalysis() = "com.jetbrains.intellij.platform:analysis:$intellijSdkVersion"
fun Project.intellijJDom() = "com.jetbrains.intellij.platform:util-jdom:$intellijSdkVersion"
fun Project.intellijTestFramework() = "com.jetbrains.intellij.platform:test-framework:$intellijSdkVersion"
