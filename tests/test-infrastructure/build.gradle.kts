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
    api(project(":common:diagnostics"))

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testFixturesApi(project(":compiler:frontend"))
    testFixturesApi(project(":cfir:entrypoint"))
    testFixturesApi(project(":cfir:cfir-cones"))


    testFixturesApi(project(":compiler:config"))
    testFixturesApi(project(":psi"))
    testFixturesApi(intellijCore())
    testFixturesApi(libs.junit4)


    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testFixturesApi(libs.junit.platform.launcher)
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xnested-type-aliases"))
}
