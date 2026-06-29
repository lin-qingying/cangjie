package org.cangnova.cangjie.analysis.api.impl.base.test.dsl

import org.jetbrains.annotations.NotNull
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.utils.Printer
import kotlin.reflect.KClass

/**
 * 为 generated test class 生成 `getConfigurator()` 方法的代码生成器。
 *
 * 该生成器把 `AnalysisApiTestConfiguratorFactoryData` 写入生成源码，使每个 generated test
 * 都能在运行时创建与自身 frontend、module kind、session mode 和 API mode 匹配的 configurator。
 */
object FrontendConfiguratorTestGenerator : MethodGenerator<FrontendConfiguratorTestModel>() {
    /**
     * 生成 `getConfigurator()` 方法签名。
     *
     * 方法保持 Java 生成源码风格，带 `@NotNull` 与 `@Override`，以匹配测试基类契约。
     */
    override fun generateSignature(method: FrontendConfiguratorTestModel, p: Printer): Unit = with(p) {
        println("@NotNull")
        println("@Override")
        print("public ${AnalysisApiTestConfigurator::class.simpleName} getConfigurator()")
    }

    /**
     * 生成 `getConfigurator()` 方法体。
     *
     * 方法体调用对应 factory 的 `createConfigurator`，并把当前配置组合序列化成 Java 枚举常量表达式。
     */
    override fun generateBody(method: FrontendConfiguratorTestModel, p: Printer): Unit = with(p) {
        print("return ")
        printWithNoIndent(method.frontendConfiguratorFactoryClass.simpleName)
        printlnWithNoIndent(".INSTANCE.createConfigurator(")
        pushIndent()
        println("new ", AnalysisApiTestConfiguratorFactoryData::class.simpleName, "(")
        pushIndent()
        println(method.data.frontend.asJavaCode(), ",")
        println(method.data.moduleKind.asJavaCode(), ",")
        println(method.data.analysisSessionMode.asJavaCode(), ",")
        println(method.data.analysisApiMode.asJavaCode())
        popIndent()
        println(")")
        popIndent()
        println(");")
    }

    /**
     * 将 Kotlin 枚举值渲染为生成 Java 源码可引用的枚举常量表达式。
     *
     * 例如 `FrontendKind.Cfir` 会被写成 `FrontendKind.Cfir`，供构造
     * `AnalysisApiTestConfiguratorFactoryData` 时直接使用。
     */
    private fun Enum<*>.asJavaCode(): String = "${this::class.simpleName}.${this.name}"
}

/**
 * `getConfigurator()` 方法的生成模型。
 *
 * 该模型记录目标 configurator factory 类型与具体配置组合，并通过 `MethodModel` 接入 Kotlin
 * 测试生成器的源码输出流程。
 */
class FrontendConfiguratorTestModel(
    /**
     * 负责创建当前 generated test configurator 的 factory 类型。
     *
     * 生成器会把该类型加入 imports，并在方法体中调用它的 singleton instance。
     */
    val frontendConfiguratorFactoryClass: KClass<out AnalysisApiTestConfiguratorFactory>,
    /**
     * 当前 generated test 对应的完整配置组合。
     *
     * 该数据会被渲染进生成源码，以便测试运行时恢复 frontend、module kind、session mode 和 API mode。
     */
    val data: AnalysisApiTestConfiguratorFactoryData,
) : MethodModel<FrontendConfiguratorTestModel>() {
    /**
     * 当前模型使用的实际方法生成器。
     *
     * 返回固定 singleton，保证所有 configurator 方法使用同一套源码模板。
     */
    override val generator: MethodGenerator<FrontendConfiguratorTestModel> get() = FrontendConfiguratorTestGenerator
    /**
     * 生成方法的名称。
     *
     * 测试基类通过该方法获取当前 generated test 的 configurator。
     */
    override val name: String get() = "getConfigurator"
    /**
     * 该方法模型不对应单独 testData 字符串。
     *
     * 返回 `null` 表示测试生成器不需要为它生成数据路径元信息。
     */
    override val dataString: String? get() = null
    /**
     * 该方法模型不声明额外 JUnit tags。
     *
     * 返回空列表以保持 configurator 方法只作为辅助方法生成。
     */
    override val tags: List<String> get() = emptyList()
    /**
     * 该方法不是 JUnit 测试方法。
     *
     * 返回 `false` 保证生成器不会给 `getConfigurator()` 添加测试注解。
     */
    override val isTestMethod: Boolean get() = false
    /**
     * 该方法不需要为内部测试类重复生成。
     *
     * configurator 属于外层 generated test class 的公共配置入口。
     */
    override val shouldBeGeneratedForInnerTestClass: Boolean get() = false

    /**
     * 收集生成 `getConfigurator()` 方法所需的 import 类型。
     *
     * imports 包括注解、factory、configurator 数据类以及当前配置组合中实际出现的枚举类型。
     */
    override fun imports(): Collection<Class<*>> {
        return buildList {
            add(NotNull::class.java)
            add(frontendConfiguratorFactoryClass.java)
            add(AnalysisApiTestConfiguratorFactoryData::class.java)
            add(AnalysisApiTestConfigurator::class.java)
            add(data.moduleKind::class.java)
            add(data.frontend::class.java)
            add(data.analysisSessionMode::class.java)
            add(data.analysisApiMode::class.java)
        }
    }
}
