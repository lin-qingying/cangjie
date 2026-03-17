import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// CFIR Common: 基础设施（CfirElement、CfirSession、CfirModuleData）

plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":common"))
    api(project(":compiler:config"))
    api(project(":util"))
    compileOnly(intellijCore())
    implementation(libs.guava)
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.add("-Xcontext-parameters")
}
