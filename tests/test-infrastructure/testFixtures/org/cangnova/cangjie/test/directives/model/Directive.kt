package org.cangnova.cangjie.test.directives.model

import org.cangnova.cangjie.test.util.joinToArrayString

// --------------------------- Directive declaration ---------------------------

/**
 * 表示 `DirectiveApplicability`，承载测试指令中的配置数据、测试产物或处理步骤。
 */
enum class DirectiveApplicability(
    /**
     * 保存 `forGlobal`，供测试指令在测试执行期间读取或传递。
     */
    val forGlobal: Boolean = false,
    /**
     * 保存 `forModule`，供测试指令在测试执行期间读取或传递。
     */
    val forModule: Boolean = false,
    /**
     * 保存 `forFile`，供测试指令在测试执行期间读取或传递。
     */
    val forFile: Boolean = false,
) {
    Any(forGlobal = true, forModule = true, forFile = true),
    Global(forGlobal = true, forModule = true),
    Module(forModule = true),
    File(forFile = true),
}

/**
 * 表示 `Directive`，承载测试指令中的配置数据、测试产物或处理步骤。
 */
sealed class Directive(val name: String, val description: String, val applicability: DirectiveApplicability) {
    /**
     * 执行 `toString` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    override fun toString(): String = name
}

/**
 * 表示 `SimpleDirective`，承载测试指令中的配置数据、测试产物或处理步骤。
 */
class SimpleDirective(
    name: String,
    description: String,
    applicability: DirectiveApplicability,
) : Directive(name, description, applicability)

/**
 * 表示 `StringDirective`，承载测试指令中的配置数据、测试产物或处理步骤。
 */
class StringDirective(
    name: String,
    description: String,
    applicability: DirectiveApplicability,
    /**
     * 保存 `multiLine`，供测试指令在测试执行期间读取或传递。
     */
    val multiLine: Boolean,
) : Directive(name, description, applicability)

/**
 * 表示 `ValueDirective`，承载测试指令中的配置数据、测试产物或处理步骤。
 */
class ValueDirective<T : Any>(
    name: String,
    description: String,
    applicability: DirectiveApplicability,
    /**
     * 保存 `parser`，供测试指令在测试执行期间读取或传递。
     */
    val parser: (String) -> T?,
) : Directive(name, description, applicability)

// --------------------------- Registered directive ---------------------------

/**
 * 表示 `RegisteredDirectives`，承载测试指令中的配置数据、测试产物或处理步骤。
 */
abstract class RegisteredDirectives : Iterable<Directive> {
    companion object {
        val Empty: RegisteredDirectives by lazy { RegisteredDirectivesImpl(emptyList(), emptyMap(), emptyMap()) }
    }

    /**
     * 提供 `contains` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    abstract operator fun contains(directive: Directive): Boolean
    /**
     * 提供 `get` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    abstract operator fun get(directive: StringDirective): List<String>
    /**
     * 提供 `get` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    abstract operator fun <T : Any> get(directive: ValueDirective<T>): List<T>
    /**
     * 提供 `isEmpty` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    abstract fun isEmpty(): Boolean
}

/**
 * 表示 `RegisteredDirectivesImpl`，承载测试指令中的配置数据、测试产物或处理步骤。
 */
class RegisteredDirectivesImpl(
    /**
     * 保存 `simpleDirectives`，供测试指令在测试执行期间读取或传递。
     */
    private val simpleDirectives: List<SimpleDirective>,
    /**
     * 保存 `stringDirectives`，供测试指令在测试执行期间读取或传递。
     */
    private val stringDirectives: Map<StringDirective, List<String>>,
    /**
     * 保存 `valueDirectives`，供测试指令在测试执行期间读取或传递。
     */
    private val valueDirectives: Map<ValueDirective<*>, List<Any>>,
) : RegisteredDirectives() {
    /**
     * 执行 `contains` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    override fun contains(directive: Directive): Boolean {
        return when (directive) {
            is SimpleDirective -> directive in simpleDirectives
            is StringDirective -> directive in stringDirectives
            is ValueDirective<*> -> directive in valueDirectives
        }
    }

    /**
     * 执行 `get` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    override fun get(directive: StringDirective): List<String> = stringDirectives[directive] ?: emptyList()

    /**
     * 执行 `get` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    override fun <T : Any> get(directive: ValueDirective<T>): List<T> {
        @Suppress("UNCHECKED_CAST")
        return valueDirectives[directive] as List<T>? ?: emptyList()
    }

    /**
     * 执行 `isEmpty` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    override fun isEmpty(): Boolean =
        simpleDirectives.isEmpty() && stringDirectives.isEmpty() && valueDirectives.isEmpty()
    /**
     * 执行 `toString` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    override fun toString(): String {
        return buildString {
            simpleDirectives.forEach { appendLine("  $it") }
            stringDirectives.forEach { (d, v) -> appendLine("  $d: ${v.joinToArrayString()}") }
            valueDirectives.forEach { (d, v) -> appendLine("  $d: ${v.joinToArrayString()}") }
        }
    }

    /**
     * 执行 `iterator` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    override fun iterator(): Iterator<Directive> {
        return buildList {
            addAll(simpleDirectives)
            addAll(stringDirectives.keys)
            addAll(valueDirectives.keys)
        }.iterator()
    }
}

/**
 * 表示 `ComposedRegisteredDirectives`，承载测试指令中的配置数据、测试产物或处理步骤。
 */
class ComposedRegisteredDirectives(
    /**
     * 保存 `containers`，供测试指令在测试执行期间读取或传递。
     */
    private val containers: List<RegisteredDirectives>,
) : RegisteredDirectives() {
    companion object {
        operator fun invoke(vararg containers: RegisteredDirectives): RegisteredDirectives {
            val notEmptyContainers = containers.filterNot { it.isEmpty() }
            return when (notEmptyContainers.size) {
                0 -> Empty
                1 -> notEmptyContainers.single()
                else -> ComposedRegisteredDirectives(notEmptyContainers)
            }
        }
    }

    /**
     * 执行 `contains` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    override fun contains(directive: Directive): Boolean = containers.any { directive in it }
    /**
     * 执行 `get` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    override fun get(directive: StringDirective): List<String> = containers.flatMap { it[directive] }
    /**
     * 执行 `get` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    override fun <T : Any> get(directive: ValueDirective<T>): List<T> = containers.flatMap { it[directive] }
    /**
     * 执行 `isEmpty` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    override fun isEmpty(): Boolean = containers.all { it.isEmpty() }
    /**
     * 执行 `iterator` 对应的测试指令流程，维持测试框架的阶段契约。
     */
    override fun iterator(): Iterator<Directive> = containers.flatten().iterator()
}

/**
 * 执行 `singleOrZeroValue` 对应的测试指令流程，维持测试框架的阶段契约。
 */
fun RegisteredDirectives.singleOrZeroValue(directive: StringDirective): String? {
    val values = this[directive]
    return when (values.size) {
        0 -> null
        1 -> values.single()
        else -> error("Too many values passed to $directive")
    }
}

/**
 * 执行 `singleValue` 对应的测试指令流程，维持测试框架的阶段契约。
 */
fun RegisteredDirectives.singleValue(directive: StringDirective): String =
    singleOrZeroValue(directive) ?: error("No values passed to $directive")

/**
 * 执行 `singleOrZeroValue` 对应的测试指令流程，维持测试框架的阶段契约。
 */
fun <T : Any> RegisteredDirectives.singleOrZeroValue(directive: ValueDirective<T>): T? {
    val values = this[directive]
    return when (values.size) {
        0 -> null
        1 -> values.single()
        else -> error("Too many values passed to $directive")
    }
}

/**
 * 执行 `singleValue` 对应的测试指令流程，维持测试框架的阶段契约。
 */
fun <T : Any> RegisteredDirectives.singleValue(directive: ValueDirective<T>): T =
    singleOrZeroValue(directive) ?: error("No values passed to $directive")
