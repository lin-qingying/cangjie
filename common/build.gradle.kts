// common: 编译器基础设施（名称系统、内置类型、描述符）

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":util"))
    compileOnly(intellijCore())

}
