@file:Suppress("unused")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

/** Cangjie embeddable 工件中重定位依赖使用的根包名。 */
const val cangjieEmbeddableRootPackage = "org.cangnova.cangjie"

/** 需要在 embeddable 工件中重定位的第三方包前缀集合。 */
private val cangjieEmbeddablePackagesToRelocate = listOf(
    "com.google",
    "com.sampullara",
    "org.apache",
    "org.jdom",
    "org.picocontainer",
    "org.jline",
    "org.fusesource",
    "net.jpountz",
    "one.util.streamex",
    "it.unimi.dsi.fastutil",
    "kotlinx.collections.immutable",
    "com.fasterxml",
    "org.codehaus",
    "io.opentelemetry",
    "io.vavr",
    "org.antlr",
)

/**
 * 将 embeddable 前端工件中的宿主敏感依赖重定位到 `org.cangnova.cangjie.*` 命名空间下。
 *
 * 设计目标对齐 Kotlin 官方 `compiler-embeddable`：
 * 1. 保持 Cangjie 自身公开 API 包名不变
 * 2. 仅隔离会和宿主 JVM / IDE / 构建系统发生冲突的第三方实现依赖
 * 3. 让最终发布物是真正可嵌入的 shaded/relocated 编译前端，而不是仅仅合并 classpath 的 fat jar
 */
fun ShadowJar.configureCangjieEmbeddableRelocation(withJavaxInject: Boolean = true) {
    relocate("com.intellij", "$cangjieEmbeddableRootPackage.com.intellij") {
        // 这些字符串会被 IntelliJ XML reader 作为服务声明键直接解析，不能被一并改写。
        exclude("com.intellij.projectService")
        exclude("com.intellij.applicationService")
    }

    cangjieEmbeddablePackagesToRelocate.forEach { packageName ->
        relocate(packageName, "$cangjieEmbeddableRootPackage.$packageName")
    }

    if (withJavaxInject) {
        relocate("javax.inject", "$cangjieEmbeddableRootPackage.javax.inject")
    }

    relocate("org.fusesource", "$cangjieEmbeddableRootPackage.org.fusesource") {
        exclude("org.fusesource.jansi.internal.CLibrary")
    }

    /**
     * 关闭 `.kotlin_module` 自动重写，避免 Shadow 当前实现对 Kotlin 模块元数据造成额外漂移。
     * Kotlin 官方 embeddable 构建链路也显式关闭了这一行为。
     */
    enableKotlinModuleRemapping.set(false)
}
