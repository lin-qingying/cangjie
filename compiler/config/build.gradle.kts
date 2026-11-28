plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(intellijCore())
    // Core 的后台宏写屏障直接使用平台 locking-impl，不依赖 ide-impl 的实现依赖传递。
    compileOnly("com.jetbrains.intellij.platform:locking-impl:${property("intellijSdkVersion")}")
    implementation(project(":common:diagnostics"))
    implementation(project(":psi"))

    testImplementation(intellijCore())
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}
