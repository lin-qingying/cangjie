plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

dependencies {
    compileOnly(intellijCore())
    compileOnly("com.jetbrains.intellij.platform:analysis:${property("intellijSdkVersion")}") { isTransitive = false }

    implementation(project(":psi"))
    // 导入计划契约直接承载仓颉的 ImportPath（含组织名与别名），不另造平行字符串结构。
    implementation(project(":common"))

    testImplementation(intellijCore())
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
