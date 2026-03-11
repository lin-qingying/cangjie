/*
 * util 模块：编译器基础工具类。
 */

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(intellijCore())
    testImplementation(libs.junit.jupiter)
}
