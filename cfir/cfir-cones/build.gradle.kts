// CFIR Cones: 类型系统核心（ConeCangjieType 及其子类）
dependencies {
    api(project(":cfir:cfir-common"))
    api(project(":common"))

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
