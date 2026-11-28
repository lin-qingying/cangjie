import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    kotlin("jvm")
}

description = "Compiler build identity, independent of language and metadata format versions."

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

// 对位固定 Kotlin compiler/compiler.version：真实构建版本进入专属运行时资源。
tasks.named<ProcessResources>("processResources") {
    val compilerVersion = project.version.toString()
    inputs.property("compilerVersion", compilerVersion)
    filesMatching("META-INF/cangjie/compiler.version") {
        filter<ReplaceTokens>("tokens" to mapOf("snapshot" to compilerVersion))
    }
}
