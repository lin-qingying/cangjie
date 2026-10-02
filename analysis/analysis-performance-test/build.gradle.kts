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
