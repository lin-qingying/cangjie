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
    // finishedSpans() 的签名暴露 SpanData（sdk-trace），因此 SDK 需作为 api 依赖对测试源集可见。
    testFixturesApi(libs.opentelemetry.sdk)
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

/**
 * 开启 JFR 采集的构建属性名，值为 `.jfr` 输出路径（相对仓库根目录解析）。
 */
val caJfrOutput = "cangjie.jfr.output"

/**
 * JFR 阶段事件是否携带调用栈，须与 `LLFlightRecorder` 读取的系统属性名一致。
 */
val caJfrIncludePhaseTraces = "cangjie.analysis.jfr.includePhaseTraces"

// 把 OTLP endpoint 与 JFR 开关转发进测试 JVM。
//
// `Test` 任务另起 JVM，Gradle 命令行上的 `-D` 只进 Gradle 自身进程，不转发就永远到不了
// 测试进程——`CaPerformanceTestTelemetry` 读的那个系统属性会恒为 null，OTLP 导出这条路等于
// 没接上。要用就显式传：
//
//   ./gradlew :analysis:analysis-performance-test:test \
//       -Dcangjie.performance.otlp.endpoint=http://localhost:4317
//
// JFR 同理，且它用的是构建属性（路径要参与任务输入与配置缓存失效，故走 `-P`）：
//
//   ./gradlew :analysis:analysis-performance-test:test \
//       -Pcangjie.jfr.output=analysis/analysis-performance-test/build/reports/analysis-performance/run.jfr
//
// 分析侧本来就带一套声明级 × 阶段级的 JFR 事件（见 `LLFlightRecorder`，生产路径上一直在
// 发），这里只负责把记录打开。不传 `-Pcangjie.jfr.output` 时测试进程不启用任何采集，
// 事件本身在无记录时是 no-op，因此这条通路默认零开销。
val repositoryRoot = rootDir

tasks.withType<Test>().configureEach {
    providers.systemProperty(caPerformanceOtlpEndpoint).orNull?.let { endpoint ->
        systemProperty(caPerformanceOtlpEndpoint, endpoint)
    }

    // 带栈的阶段事件单独开关：栈采集的开销远大于事件本身，不适合默认打开。
    providers.systemProperty(caJfrIncludePhaseTraces).orNull?.let {
        systemProperty(caJfrIncludePhaseTraces, it)
    }

    // 记录在 JVM 退出时落盘（`dumponexit`），测试进程很短因此不需要 `duration`；
    // `maxsize` / `maxage` 只是兜底，防止异常情况下无限增长。
    providers.gradleProperty(caJfrOutput).orNull?.takeIf { it.isNotBlank() }?.let { output ->
        // 测试进程的 workingDir 是仓库根，这里解析成绝对路径再交给 JVM，
        // 避免产物落到与调用者预期不一致的位置。
        val recording = repositoryRoot.resolve(output).normalize()
        jvmArgs(
            "-XX:StartFlightRecording=" +
                "filename=${recording.path.replace('\\', '/')}," +
                "settings=profile," +
                "maxsize=512m," +
                "maxage=1h," +
                "dumponexit=true"
        )
        // JFR 不会创建父目录，写不出去时它只在 JVM 启动阶段报错并让整个进程起不来，
        // 因此这里必须在 JVM 启动前把目录准备好。
        doFirst { recording.parentFile?.mkdirs() }
    }
}
