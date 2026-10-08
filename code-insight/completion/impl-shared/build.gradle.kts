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

    implementation(project(":code-insight:completion:contracts"))
    implementation(project(":psi"))
    implementation(project(":common"))
    implementation(project(":analysis:analysis-api"))
    /*
     * 统计接缝用平台的 OpenTelemetry provider 与 OTel API。
     * 与大仓其它模块一致用 `compileOnly`：两者都由 Analysis 平台制品在运行期提供
     * （`analysis-api-platform-interface` 自己也是这么声明的），这里只借用类型。
     */
    compileOnly(project(":analysis:analysis-api-platform-interface"))
    compileOnly("io.opentelemetry:opentelemetry-api:1.39.0")

    testImplementation(intellijCore())
    testImplementation(project(":analysis:analysis-api-platform-interface"))
    testImplementation("io.opentelemetry:opentelemetry-api:1.39.0")
    testImplementation("com.jetbrains.intellij.platform:analysis:${property("intellijSdkVersion")}") { isTransitive = false }
    testImplementation(testFixtures(project(":tests:test-infrastructure")))
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
