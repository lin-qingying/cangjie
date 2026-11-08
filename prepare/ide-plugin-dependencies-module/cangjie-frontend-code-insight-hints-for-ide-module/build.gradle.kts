plugins {
    kotlin("jvm")
}

description = "IDE 插件依赖：源码桥接 :code-insight:hints。"

/*
 * 只 `api` hints 自身：它的 `implementation` 依赖（analysis-api / psi / common）
 * 不出现在消费者的编译类路径上，与 prepare 侧「只打 hints jar」的取舍保持一致。
 */
dependencies {
    api(project(":code-insight:hints"))
}

configurations.configureEach {
    exclude(group = "com.jetbrains.intellij.platform")
    exclude(group = "com.jetbrains.intellij")
    exclude(group = "org.jetbrains.intellij")
    exclude(group = "com.intellij.platform")
}
