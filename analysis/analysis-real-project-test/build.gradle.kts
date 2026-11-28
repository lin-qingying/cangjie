// 真实项目对照测试：先用 cjpm 编译确认真实项目无错误，再在同一项目上运行 CFIR 分析。
// 框架类放在 testFixtures，测试模块（cfir/lsp/ide）可通过 testFixtures 依赖复用。
import java.time.Duration

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

dependencies {
    testFixturesCompileOnly(intellijCore())

    // 被测入口：headless 模块图 + standalone session。
    testFixturesApi(project(":analysis:analysis-api-standalone"))
    // standalone 手写测试所需的 configurator / 平台服务注册。
    testFixturesApi(testFixtures(project(":analysis:analysis-api-standalone")))
    // headless 测试基座（AbstractAnalysisApiExecutionTest、TestServices、指令框架）。
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))
    // 反编译服务注册（真实项目会加载 SDK 的 builtins/.cjo）。
    testFixturesApi(testFixtures(project(":analysis:analysis-api-impl-base")))
    // 平台级测试基座（Project/PSI fixture、TestWithDisposable 等）。
    testFixturesApi(testFixtures(project(":tests:test-infrastructure")))

    // 测试运行期直接使用 PSI/lexer 类（headless bootstrap 不会自动带入实现模块）。
    testFixturesApi(project(":psi"))
    testFixturesApi(project(":common"))
    testFixturesApi(project(":common:diagnostics"))
    testFixturesApi(project(":util"))

    // standalone 会话在运行期会触达 CFIR 实现模块；这些依赖在 analysis 模块里是 implementation，
    // 不会传递到消费方，必须在框架层显式声明。
    testFixturesApi(project(":cfir:cfir-common"))
    testFixturesApi(project(":cfir:cfir-cones"))
    testFixturesApi(project(":cfir:cfir-tree"))
    testFixturesApi(project(":cfir:resolve"))
    testFixturesApi(project(":cfir:checkers"))
    testFixturesApi(project(":cfir:entrypoint"))
    testFixturesApi(project(":cfir:cfir-serialization"))
    testFixturesApi(project(":cfir:raw-cfir:raw-cfir-common"))
    testFixturesApi(project(":cfir:raw-cfir:psi2cfir"))
    testFixturesApi(project(":analysis:analysis-api"))
    testFixturesApi(project(":analysis:analysis-api-cfir"))
    testFixturesApi(project(":analysis:analysis-api-impl-base"))
    testFixturesApi(project(":analysis:low-level-api-cfir"))
    testFixturesApi(project(":analysis:decompiled"))
    testFixturesApi(project(":analysis:stubs"))
    testFixturesApi(project(":compiler:frontend"))
    testFixturesApi(project(":macro:macro-common"))

    testFixturesApi(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        workingDir = rootDir
        maxHeapSize = "4g"
        timeout.set(Duration.ofMinutes(30))
        val realProjectRepo = providers.gradleProperty("realProjectRepo").orNull.orEmpty()
        val realProjectRef = providers.gradleProperty("realProjectRef").orNull.orEmpty()
        val cangjieHome = providers.gradleProperty("cangjieHome").orNull.orEmpty()
        val realProjectRefresh = providers.gradleProperty("realProjectRefresh").orNull.orEmpty().ifBlank { "true" }
        val realProjectConfig = providers.gradleProperty("realProjectConfig").orNull.orEmpty()
        val realProjectFilter = providers.gradleProperty("realProjectFilter").orNull.orEmpty()
        systemProperty("realProject.repo", realProjectRepo)
        systemProperty("realProject.ref", realProjectRef)
        systemProperty("cangjie.home", cangjieHome)
        systemProperty("realProject.refresh", realProjectRefresh)
        systemProperty("realProject.config", realProjectConfig)
        systemProperty("realProject.filter", realProjectFilter)
        systemProperty(
            "realProject.root",
            layout.buildDirectory.dir("real-projects").get().asFile.absolutePath,
        )
        // Gradle 不会自动跟踪 systemProperty，显式声明输入，切换项目/提交时会重新执行。
        inputs.property("realProject.repo", realProjectRepo)
        inputs.property("realProject.ref", realProjectRef)
        inputs.property("cangjie.home", cangjieHome)
        inputs.property("realProject.refresh", realProjectRefresh)
        inputs.property("realProject.config", realProjectConfig)
        inputs.property("realProject.filter", realProjectFilter)
        // 分支内容可能在 ref 不变的情况下前进；外部 git 状态不能由 Gradle 输入表达，
        // 因此该任务既不跳过也不进构建缓存。
        outputs.upToDateWhen { false }
        outputs.doNotCacheIf("real-project test depends on external git state") { true }
    }
}
