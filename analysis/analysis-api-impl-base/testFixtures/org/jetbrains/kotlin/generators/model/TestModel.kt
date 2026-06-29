package org.jetbrains.kotlin.generators.model

import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.utils.Printer

/**
 * 测试生成模型的公共基类。
 *
 * 测试类和测试方法都需要提供生成名称、关联测试数据路径以及 JUnit 标签；
 * 生成器据此统一输出 `@TestMetadata`、`@Tag` 和嵌套类结构。
 */
sealed class TestEntityModel {
    /**
     * 生成到 Java 源码中的类名或方法名。
     */
    abstract val name: String

    /**
     * 写入 `@TestMetadata` 的测试数据路径；为 null 时不生成该注解。
     */
    abstract val dataString: String?

    /**
     * 写入 JUnit5 `@Tag` 的标签集合。
     */
    abstract val tags: List<String>
}

/**
 * 生成测试套件中的测试类模型。
 *
 * 一个模型可以表示实际测试类，也可以表示嵌套目录生成出的内部测试类。
 */
abstract class TestClassModel : TestEntityModel() {
    /**
     * 当前测试类下的嵌套测试类模型。
     */
    abstract val innerTestClasses: Collection<TestClassModel>

    /**
     * 当前测试类直接生成的测试方法模型。
     */
    abstract val methods: Collection<MethodModel<*>>

    /**
     * 当前类是否没有需要生成的测试方法和有效子类。
     */
    abstract val isEmpty: Boolean

    /**
     * 写入 `@TestDataPath` 的根路径；为 null 时不生成该注解。
     */
    abstract val dataPathRoot: String?

    /**
     * 附加在当前测试类上的自定义注解模型。
     */
    abstract val annotations: Collection<AnnotationModel>

    /**
     * 当前测试类、方法及所有嵌套测试类生成源码所需的导入类型集合。
     */
    val imports: Set<Class<*>>
        get() {
            return mutableSetOf<Class<*>>().also { allImports ->
                annotations.flatMapTo(allImports) { it.imports() }
                methods.flatMapTo(allImports) { it.imports() }
                innerTestClasses.flatMapTo(allImports) { it.imports }
            }
        }
}

/**
 * 生成测试套件中的测试方法模型。
 *
 * 泛型参数将模型类型与对应的 [MethodGenerator] 绑定，避免具体生成器处理错误模型。
 */
abstract class MethodModel<M : MethodModel<M>> : TestEntityModel() {
    /**
     * 输出该方法签名和方法体的具体生成器。
     */
    abstract val generator: MethodGenerator<M>

    /**
     * 当前方法是否应标记为真正的 JUnit 测试方法。
     */
    open val isTestMethod: Boolean get() = true

    /**
     * 当前方法是否需要复制到目录递归产生的内部测试类中。
     */
    open val shouldBeGeneratedForInnerTestClass: Boolean get() = true

    /**
     * 当前方法生成源码时需要额外导入的类型集合。
     */
    open fun imports(): Collection<Class<*>> = emptyList()

    /**
     * 将当前模型交给绑定的生成器输出方法体。
     */
    fun generateBody(p: Printer) {
        @Suppress("UNCHECKED_CAST")
        generator.generateBody(this as M, p)
    }

    /**
     * 将当前模型交给绑定的生成器输出方法签名。
     */
    fun generateSignature(p: Printer) {
        @Suppress("UNCHECKED_CAST")
        generator.generateSignature(this as M, p)
    }
}
