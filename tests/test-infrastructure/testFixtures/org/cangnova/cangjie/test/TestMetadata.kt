package org.cangnova.cangjie.test

/**
 * 用于 IDE 工具（例如测试数据导航）定位关联的 testData。
 *
 * 这是 Kotlin 编译器测试框架 `org.jetbrains.kotlin.test.TestMetadata` 的轻量对齐实现。
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class TestMetadata(val value: String)

