import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
}

description = "Analysis API performance and statistics test entry point for the Cangjie frontend."

allprojects {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.optIn.addAll(
            listOf(
                "org.cangnova.cangjie.analysis.api.CaPlatformInterface",
                "org.cangnova.cangjie.analysis.api.CaImplementationDetail",
                "org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsOnlyApi",
            )
        )
    }
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

dependencies {
    // 统计后端：OpenTelemetry API 与对齐 Kotlin analysis-api-platform-interface 的声明方式一致。
    testFixturesApi(libs.opentelemetry.api)
    // 统计测试宿主：standalone CFIR Analysis API 与其测试基类。
    testFixturesApi(project(":analysis:analysis-api"))
    testFixturesApi(project(":analysis:analysis-api-platform-interface"))
    testFixturesApi(project(":analysis:analysis-api-cfir"))
    testFixturesApi(project(":analysis:analysis-api-standalone"))
    testFixturesApi(project(":analysis:low-level-api-cfir"))
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-impl-base")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-cfir")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-standalone")))
    testFixturesApi(intellijCore())
    testFixturesApi(libs.junit.jupiter)
    testFixturesRuntimeOnly(libs.junit.platform.launcher)
    // SDK：让统计指标真正进入 MeterProvider；InMemoryMetricReader 供断言，logging/OTLP 导出器供本地观察。
    testFixturesImplementation(libs.opentelemetry.sdk)
    testFixturesImplementation(libs.opentelemetry.sdk.metrics)
    testFixturesImplementation(libs.opentelemetry.sdk.testing)
    testFixturesImplementation(libs.opentelemetry.exporter.otlp)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        workingDir = rootDir
    }
}

/**
 * 开启 OTLP 导出的系统属性名，值是 gRPC endpoint。须与
 * `CaPerformanceTestTelemetry.OTLP_ENDPOINT_PROPERTY` 一致。
 */
val caPerformanceOtlpEndpoint = "cangjie.performance.otlp.endpoint"

// 把 OTLP endpoint 转发进测试 JVM。
//
// `Test` 任务另起 JVM，Gradle 命令行上的 `-D` 只进 Gradle 自身进程，不转发就永远到不了
// 测试进程——`CaPerformanceTestTelemetry` 读的那个系统属性会恒为 null，OTLP 导出这条路等于
// 没接上。要用就显式传：
//
//   ./gradlew :analysis:analysis-performance-test:test \
//       -Dcangjie.performance.otlp.endpoint=http://localhost:4317
tasks.withType<Test>().configureEach {
    providers.systemProperty(caPerformanceOtlpEndpoint).orNull?.let { endpoint ->
        systemProperty(caPerformanceOtlpEndpoint, endpoint)
    }
}
