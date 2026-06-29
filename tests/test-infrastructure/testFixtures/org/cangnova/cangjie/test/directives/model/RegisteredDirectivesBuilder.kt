package org.cangnova.cangjie.test.directives.model

import kotlin.collections.get

/**
 * 表示 `RegisteredDirectivesBuilder`，承载测试指令中的配置数据、测试产物或处理步骤。
 */
class RegisteredDirectivesBuilder private constructor(
    /**
     * 保存 `simpleDirectives`，供测试指令在测试执行期间读取或传递。
     */
    private val simpleDirectives: MutableList<SimpleDirective>,
    /**
     * 保存 `stringDirectives`，供测试指令在测试执行期间读取或传递。
     */
    private val stringDirectives: MutableMap<StringDirective, List<String>>,
    /**
     * 保存 `valueDirectives`，供测试指令在测试执行期间读取或传递。
     */
    private val valueDirectives: MutableMap<ValueDirective<*>, List<Any>>
) {
    constructor() : this(mutableListOf(), mutableMapOf(), mutableMapOf())

    constructor(old: RegisteredDirectives) : this() {
        for (directive in old) {
            when (directive) {
                is SimpleDirective -> +directive
                is StringDirective -> directive with old[directive]
                is ValueDirective<*> -> {
                    // no way to call with
                    valueDirectives[directive] = old[directive]
                }
            }
        }
    }

    /**
     * 提供 `unaryPlus` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    operator fun SimpleDirective.unaryPlus() {
        simpleDirectives += this
    }

    /**
     * 提供 `unaryMinus` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    operator fun SimpleDirective.unaryMinus() {
        simpleDirectives.remove(this)
    }

    /**
     * 提供 `with` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    infix fun StringDirective.with(value: String) {
        with(listOf(value))
    }

    /**
     * 提供 `with` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    infix fun StringDirective.with(values: List<String>) {
        stringDirectives.putWithExistsCheck(this, values)
    }

    /**
     * 提供 `plus` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    operator fun StringDirective.plus(value: String) {
        val previous = stringDirectives[this] ?: listOf()
        stringDirectives[this] = previous + value
    }

    /**
     * 提供 `unaryMinus` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    operator fun StringDirective.unaryMinus() {
        stringDirectives.remove(this)
    }

    /**
     * 提供 `with` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    infix fun <T : Any> ValueDirective<T>.with(value: T) {
        with(listOf(value))
    }

    /**
     * 提供 `with` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    infix fun <T : Any> ValueDirective<T>.with(values: List<T>) {
        valueDirectives.putWithExistsCheck(this, values)
    }

    /**
     * 提供 `ValueDirective` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    operator fun ValueDirective<*>.unaryMinus() {
        valueDirectives.remove(this)
    }

    /**
     * 提供 `MutableMap` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    private fun <K : Directive, V> MutableMap<K, V>.putWithExistsCheck(key: K, value: V) {
        val alreadyRegistered = get(key)
        if (alreadyRegistered == null) {
            put(key, value)
        } else if (alreadyRegistered is List<Any?> && value is List<Any?>) {
            @Suppress("UNCHECKED_CAST")
            put(key, (alreadyRegistered + value) as V)
        } else {
            error("Default values for $key directive already registered")
        }
    }

    /**
     * 执行 `build` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    fun build(): RegisteredDirectives {
        return RegisteredDirectivesImpl(simpleDirectives, stringDirectives, valueDirectives)
    }
}
