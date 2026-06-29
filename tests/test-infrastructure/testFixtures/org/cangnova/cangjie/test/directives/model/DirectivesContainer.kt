package org.cangnova.cangjie.test.directives.model

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * 表示 `DirectivesContainer`，承载测试指令中的配置数据、测试产物或处理步骤。
 */
sealed class DirectivesContainer {
    /**
     * 提供 `Empty` 单例，集中承载测试指令的共享状态、常量或默认行为。
     */
    object Empty : SimpleDirectivesContainer()

    /**
     * 提供 `get` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    abstract operator fun get(name: String): Directive?
    /**
     * 提供 `contains` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    abstract operator fun contains(directive: Directive): Boolean
}

/**
 * 表示 `SimpleDirectivesContainer`，承载测试指令中的配置数据、测试产物或处理步骤。
 */
abstract class SimpleDirectivesContainer : DirectivesContainer() {
    /**
     * 保存 `registeredDirectives`，供测试指令在测试执行期间读取或传递。
     */
    private val registeredDirectives: MutableMap<String, Directive> = mutableMapOf()

    /**
     * 提供 `get` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    override operator fun get(name: String): Directive? = registeredDirectives[name]

    /**
     * 提供 `directive` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    protected fun directive(
        description: String,
        applicability: DirectiveApplicability = DirectiveApplicability.Global,
    ): DirectiveDelegateProvider<SimpleDirective> {
        return DirectiveDelegateProvider { SimpleDirective(it, description, applicability) }
    }

    /**
     * 提供 `stringDirective` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    protected fun stringDirective(
        description: String,
        applicability: DirectiveApplicability = DirectiveApplicability.Global,
        multiLine: Boolean = false,
    ): DirectiveDelegateProvider<StringDirective> {
        return DirectiveDelegateProvider { StringDirective(it, description, applicability, multiLine) }
    }

    /**
     * 提供 `valueDirective` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    protected fun <T : Any> valueDirective(
        description: String,
        applicability: DirectiveApplicability = DirectiveApplicability.Global,
        parser: (String) -> T?,
    ): DirectiveDelegateProvider<ValueDirective<T>> {
        return DirectiveDelegateProvider { ValueDirective(it, description, applicability, parser) }
    }

    /**
     * 提供 `>` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    protected inline fun <reified T : Enum<T>> enumDirective(
        description: String,
        applicability: DirectiveApplicability = DirectiveApplicability.Global,
        noinline additionalParser: ((String) -> T?)? = null,
    ): DirectiveDelegateProvider<ValueDirective<T>> {
        val possibleValues = enumValues<T>()
        val parser: (String) -> T? = { value -> possibleValues.firstOrNull { it.name == value } ?: additionalParser?.invoke(value) }
        return valueDirective(description, applicability, parser)
    }

    /**
     * 提供 `registerDirective` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    protected fun registerDirective(directive: Directive) {
        registeredDirectives[directive.name] = directive
    }

    /**
     * 执行 `contains` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    override fun contains(directive: Directive): Boolean = directive in registeredDirectives.values

    protected inner class DirectiveDelegateProvider<T : Directive>(val directiveConstructor: (String) -> T) {
        operator fun provideDelegate(
            thisRef: SimpleDirectivesContainer,
            property: KProperty<*>,
        ): ReadOnlyProperty<SimpleDirectivesContainer, T> {
            val directive = directiveConstructor(property.name).also { thisRef.registerDirective(it) }
            return ReadOnlyProperty { _, _ -> directive }
        }
    }
}

/**
 * 表示 `ComposedDirectivesContainer`，承载测试指令中的配置数据、测试产物或处理步骤。
 */
class ComposedDirectivesContainer(private val containers: Collection<DirectivesContainer>) : DirectivesContainer() {
    constructor(vararg containers: DirectivesContainer) : this(containers.toList())

    /**
     * 执行 `get` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    override fun get(name: String): Directive? {
        for (container in containers) {
            container[name]?.let { return it }
        }
        return null
    }

    /**
     * 执行 `contains` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    override fun contains(directive: Directive): Boolean = containers.any { directive in it }
}
