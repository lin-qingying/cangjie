package org.jetbrains.kotlin.generators.model

/**
 * 将测试类模型的大部分行为委派给另一个模型的基类。
 *
 * 生成器在只需要替换类名、注解或标签等少量属性时使用该类，避免复制完整的目录扫描模型。
 */
abstract class DelegatingTestClassModel(private val delegate: TestClassModel) : TestClassModel() {
    /**
     * 委派模型提供的生成类名。
     */
    override val name: String
        get() = delegate.name

    /**
     * 委派模型提供的内部测试类集合。
     */
    override val innerTestClasses: Collection<TestClassModel>
        get() = delegate.innerTestClasses

    /**
     * 委派模型提供的测试方法集合。
     */
    override val methods: Collection<MethodModel<*>>
        get() = delegate.methods

    /**
     * 委派模型提供的空类判断结果。
     */
    override val isEmpty: Boolean
        get() = delegate.isEmpty

    /**
     * 委派模型提供的测试数据根路径注解值。
     */
    override val dataPathRoot: String?
        get() = delegate.dataPathRoot

    /**
     * 委派模型提供的测试数据元信息路径。
     */
    override val dataString: String?
        get() = delegate.dataString

    /**
     * 委派模型提供的类级注解集合。
     */
    override val annotations: Collection<AnnotationModel>
        get() = delegate.annotations

    /**
     * 委派模型提供的 JUnit5 标签集合。
     */
    override val tags: List<String>
        get() = delegate.tags
}
