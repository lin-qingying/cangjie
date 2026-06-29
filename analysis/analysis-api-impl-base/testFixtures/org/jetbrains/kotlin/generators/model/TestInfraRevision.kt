package org.jetbrains.kotlin.generators.model

/**
 * 测试生成基础设施的版本枚举。
 *
 * 该枚举决定生成器输出 JUnit4 旧套件还是 JUnit5 标准嵌套测试结构。
 */
enum class TestInfraRevision {
    /**
     * 旧版 JUnit4 测试生成结构。
     */
    LegacyJUnit4,

    /**
     * 标准 JUnit5 测试生成结构。
     */
    StandardJUnit5,
}
