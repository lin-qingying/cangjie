import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named

plugins {
    `java-library`
}

description = "供 IntelliJ Platform / IDEA 插件等受控 classpath 场景使用的仓颉前端公开工件，不做 relocation。"

/**
 * 编译器前端完整一方模块列表（闭包传递依赖，手工展开）。
 *
 * 对齐 Kotlin 的 fatJarContents 模式：显式列出需要打入 fat jar 的一方模块，
 * 直接引用各项目 jar 输出，不通过 configuration resolution 拉取传递依赖。
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
    // 三方运行时依赖保持为 POM 依赖，不打入 fat jar
    implementation(libs.guava)
    implementation(libs.flatbuffers.java)
    implementation(libs.kotlinx.collections.immutable)
}

tasks.named<Jar>("jar") {
    frontendProjectPaths.forEach { dependsOn("$it:jar") }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    isZip64 = true
    exclude("META-INF/maven/**")
    from({
        frontendProjectPaths.map { projectPath ->
            zipTree(project(projectPath).tasks.named<Jar>("jar").get().archiveFile.get().asFile)
        }
    })
    manifest.attributes["Implementation-Title"] = "cangjie-frontend"
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
