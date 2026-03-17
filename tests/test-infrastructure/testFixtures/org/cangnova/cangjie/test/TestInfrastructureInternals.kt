package org.cangnova.cangjie.test

/**
 * 标记测试基础设施内部 API 的注解
 *
 * 对应 Kotlin K2 的 TestInfrastructureInternals
 */
@RequiresOptIn("This is internal test infrastructure API and should not be used in user code")
@Retention(AnnotationRetention.BINARY)
annotation class TestInfrastructureInternals
