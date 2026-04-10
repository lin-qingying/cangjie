import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named

plugins {
    `java-library`
    id("com.gradleup.shadow")
}

description = "供非受控宿主进程嵌入使用的仓颉前端公开工件，对宿主敏感依赖执行 shaded/relocated 隔离。"

/**
 * 编译器前端完整一方模块列表（与 :prepare:frontend 保持一致）。
 */
val frontendProjectPaths = listOf(
    // 编译器核心
    ":compiler:frontend",
    ":compiler:phaser",
    ":compiler:config",
    // CFIR 全系列
    ":cfir:entrypoint",
    ":cfir:cfir-common",
    ":cfir:cfir-tree",
    ":cfir:cfir-cones",
    ":cfir:providers",
    ":cfir:resolve",
    ":cfir:semantics",
    ":cfir:checkers",
    ":cfir:diagnostic-renderers",
    ":cfir:cfir-serialization",
    ":cfir:raw-cfir:raw-cfir-common",
    ":cfir:raw-cfir:psi2cfir",
    ":cfir:raw-cfir:light-tree2cfir",
    // 基础设施
    ":common",
    ":common:diagnostics",
    ":util",
    ":psi",
    ":resolution.common",
    // 宏
    ":macro:macro-common",
    // FlatBuffers 生成
    ":flatbuffers-gen",
)

dependencies {
    // Shadow 插件通过 implementation 配置自动拉取传递依赖并打包
    implementation(project(":compiler:frontend"))
    implementation(project(":dependencies:intellij-core"))
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    isZip64 = true

    // 屏蔽签名文件与原始注解包，避免合并后出现签名失效与重复资源。
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("META-INF/maven/**")
    exclude("org/jetbrains/annotations/**")

    mergeServiceFiles()
    configureCangjieEmbeddableRelocation()

    manifest.attributes["Implementation-Title"] = "cangjie-frontend-embeddable"
    manifest.attributes["Implementation-Version"] = project.version.toString()
}

tasks.named<Jar>("sourcesJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    isZip64 = true
    for (projectPath in frontendProjectPaths) {
        val projectTasks = project(projectPath).tasks
        if (projectTasks.names.any { it == "compileKotlin" }) {
            dependsOn(projectTasks.named("compileKotlin").map { it.dependsOn })
        }
    }
    from({
        frontendProjectPaths
            .map(::project)
            .filter { it.plugins.hasPlugin("java-base") }
            .distinctBy { it.path }
            .map { it.extensions.getByType<JavaPluginExtension>().sourceSets.getByName("main").allSource }
    })
}
