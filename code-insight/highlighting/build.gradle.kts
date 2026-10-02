plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

dependencies {
    compileOnly(intellijCore())

    implementation(project(":common"))
    implementation(project(":psi"))

    testImplementation(intellijCore())
    testImplementation(kotlin("test"))
    testImplementation(testFixtures(project(":tests:test-infrastructure")))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    /*
     * 本模块的测试在无界面宿主下解析 PSI 并创建平台高亮 key。必须显式声明 AWT headless：
     * 否则 IntelliJ 253+ 的 JBUIScale 会把纯 PSI/高亮测试误判成带界面启动流程，
     * 在 `LoadingState.APP_STARTED` 之前访问 UI 默认值时抛 "Must be precomputed"。
     *
     * 该失败发生在 `HighlightInfoType` 的类初始化上，Kotlin/Java 一旦类初始化抛错便把该类
     * 永久标记为 erroneous，之后同一 JVM 内任何触达它的测试都会变成 `NoClassDefFoundError`。
     * 因此必须在 JVM 启动参数层面声明——任何测试内引导都晚于类初始化，且只对单个测试类有效。
     */
    systemProperty("java.awt.headless", "true")
}
