import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
}

description = "Low-level CFIR support layer for the Cangjie Analysis API."

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-reflect") {
        isTransitive = false
    }

    implementation(project(":common"))
    implementation(project(":util"))
    api(project(":common:diagnostics"))
    api(project(":psi"))

    api(project(":cfir:cfir-common"))
    api(project(":cfir:cfir-cones"))
    api(project(":cfir:cfir-tree"))
    api(project(":cfir:resolve"))
    api(project(":cfir:providers"))
    api(project(":cfir:semantics"))
    api(project(":cfir:checkers"))
    api(project(":cfir:entrypoint"))

    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-platform-interface"))
    implementation(project(":analysis:analysis-api-impl-base"))
    implementation(project(":analysis:analysis-internal-utils"))
    implementation(project(":analysis:decompiled"))
    implementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    implementation(project(":analysis:decompiled:decompiler-to-psi"))
    implementation(project(":analysis:stubs"))
    implementation(project(":analysis:light-declarations"))
    implementation(project(":analysis:symbol-light-declarations"))

    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("io.opentelemetry:opentelemetry-api:1.39.0")

    compileOnly(intellijCore())

    testFixturesApi(project(":analysis:analysis-api"))
    testFixturesApi(project(":analysis:analysis-api-platform-interface"))
    testFixturesApi(project(":analysis:analysis-api-cfir"))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-impl-base")))
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))
    testFixturesApi(testFixtures(project(":cfir:analysis-tests")))
    testFixturesApi(testFixtures(project(":tests:test-infrastructure")))
    testFixturesApi(project(":psi"))
    testFixturesApi("org.opentest4j:opentest4j:1.3.0")
    testFixturesApi(libs.junit.jupiter)
    testFixturesRuntimeOnly(libs.junit.platform.launcher)
    testFixturesCompileOnly(intellijTestFramework()) {
        isTransitive = false
    }
    testFixturesImplementation(testFixtures(project(":analysis:decompiled:decompiler-to-psi")))
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "org.cangnova.cangjie.cfir.symbols.SymbolInternals",
            "org.cangnova.cangjie.cfir.declarations.DirectDeclarationsAccess",
            "org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals",
            "org.cangnova.cangjie.analysis.api.CaIdeApi",
            "org.cangnova.cangjie.analysis.api.CaImplementationDetail",
        )
    }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        workingDir = rootDir
        val updateTestData = System.getProperty("update.test.data")
        if (updateTestData != null) {
            systemProperty("update.test.data", updateTestData)
        }
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.optIn.addAll(
        listOf(
            "org.cangnova.cangjie.cfir.symbols.SymbolInternals",
            "org.cangnova.cangjie.cfir.declarations.DirectDeclarationsAccess",
            "org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals",
            "org.cangnova.cangjie.analysis.api.CaIdeApi",
            "org.cangnova.cangjie.analysis.api.CaImplementationDetail",
        )
    )
}

tasks.register("analysisLowLevelApiCfirAllTests") {
    dependsOn(":analysis:low-level-api-cfir:test")
}
