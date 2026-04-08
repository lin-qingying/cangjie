import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// common: 编译器基础设施（名称系统、内置类型、描述符）

plugins {
    kotlin("jvm")
}

description = "Shared Cangjie frontend language model and core infrastructure."

dependencies {
    implementation(project(":util"))
    compileOnly(intellijCore())

}
