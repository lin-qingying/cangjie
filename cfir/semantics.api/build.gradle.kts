plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":cfir:cfir-tree"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

kotlin {
    compilerOptions {
        // 对位 Kotlin semantics.api：data class 的 copy 可见性必须遵循其构造器合同。
        freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
    }
}
