import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
}

sourceSets {
    "main" { none() }
    "test" { none() }
    "testFixtures" { projectDefault() }
}

dependencies {
    testFixturesApi(project(":util"))
    testFixturesApi(project(":common"))
    api(project(":cfir:diagnostics"))

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testFixturesApi(project(":compiler:cli"))
    testFixturesApi(project(":cfir:entrypoint"))
    testFixturesApi(project(":cfir:cfir-cones"))
    testFixturesApi(libs.junit.jupiter.api)

    testFixturesApi(project(":compiler:config"))
    testFixturesApi(project(":psi"))
    testFixturesApi(intellijCore())
    testFixturesApi(libs.junit4)
    // TestDataPath 注解仅编译期使用，compileOnly 避免运行时加载 JUnit5TestSessionListener
    testFixturesCompileOnly(intellijTestFramework()) { isTransitive = false }
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xnested-type-aliases"))
}
