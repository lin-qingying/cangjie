package org.cangnova.cangjie.test.services.impl

import org.cangnova.cangjie.test.Assertions
import org.cangnova.cangjie.test.directives.model.*
import kotlin.invoke
import kotlin.text.get

/**
 * 表示 `RegisteredDirectivesParser`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
class RegisteredDirectivesParser(private val container: DirectivesContainer, private val assertions: Assertions) {
    companion object {
        private val DIRECTIVE_PATTERN = Regex("""^//\s*([A-Z0-9_]+)(:[ \t]*(.*))? *$""")
        val SPACES_PATTERN = Regex("""[,]?[ \t]+""")
        private const val NAME_GROUP = 1
        private const val VALUES_GROUP = 3

        fun parseDirective(line: String): RawDirective? {
            val result = DIRECTIVE_PATTERN.matchEntire(line)?.groupValues ?: return null
            val name = result.getOrNull(NAME_GROUP) ?: return null
            val rawValue = result.getOrNull(VALUES_GROUP)
            val values = rawValue?.split(SPACES_PATTERN)?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
            return RawDirective(name, values, rawValue)
        }
    }

    /**
     * 表示 `RawDirective`，承载测试服务中的配置数据、测试产物或处理步骤。
     */
    data class RawDirective(val name: String, val values: List<String>?, val rawValue: String?)
    /**
     * 表示 `ParsedDirective`，承载测试服务中的配置数据、测试产物或处理步骤。
     */
    data class ParsedDirective(val directive: Directive, val values: List<*>)

    /**
     * 保存 `simpleDirectives`，供测试服务在测试执行期间读取或传递。
     */
    private val simpleDirectives = mutableListOf<SimpleDirective>()
    /**
     * 保存 `stringValueDirectives`，供测试服务在测试执行期间读取或传递。
     */
    private val stringValueDirectives = mutableMapOf<StringDirective, MutableList<String>>()
    /**
     * 保存 `valueDirectives`，供测试服务在测试执行期间读取或传递。
     */
    private val valueDirectives = mutableMapOf<ValueDirective<*>, MutableList<Any>>()

    /**
     * returns true means that line contain directive
     */
    fun parse(line: String): Boolean {
        val rawDirective = parseDirective(line) ?: return false
        val parsedDirective = convertToRegisteredDirective(rawDirective) ?: return false
        addParsedDirective(parsedDirective)
        return true
    }

    /**
     * 执行 `addParsedDirective` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    fun addParsedDirective(parsedDirective: ParsedDirective) {
        val (directive, values) = parsedDirective
        @Suppress("UNCHECKED_CAST")
        when (directive) {
            is SimpleDirective -> simpleDirectives += directive
            is StringDirective -> {
                val list = stringValueDirectives.getOrPut(directive, ::mutableListOf)
                list += values as List<String>
            }
            is ValueDirective<*> -> {
                val list = valueDirectives.getOrPut(directive, ::mutableListOf)
                @Suppress("UNCHECKED_CAST")
                list.addAll(values as List<Any>)
            }
        }
    }

    /**
     * 执行 `convertToRegisteredDirective` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    fun convertToRegisteredDirective(rawDirective: RawDirective): ParsedDirective? {
        val (name, rawValues, rawValueString) = rawDirective
        val directive = container[name] ?: return null

        val values: List<*> = when (directive) {
            is SimpleDirective -> {
                if (rawValues != null) {
                    assertions.fail {
                        "Directive $directive should have no arguments, but ${rawValues.joinToString(", ")} are passed"
                    }
                }
                emptyList<Any?>()
            }

            is StringDirective -> {
                when (directive.multiLine) {
                    true -> listOfNotNull(rawValueString)
                    false -> rawValues ?: emptyList()
                }
            }

            is ValueDirective<*> -> {
                if (rawValues == null) {
                    assertions.fail {
                        "Directive $directive must have at least one value"
                    }
                }
                rawValues.map { directive.extractValue(it) ?: assertions.fail { "$it is not valid value for $directive" } }
            }
        }
        return ParsedDirective(directive, values)
    }

    /**
     * 提供 `extractValue` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    private fun <T : Any> ValueDirective<T>.extractValue(name: String): T? {
        return parser.invoke(name)
    }

    /**
     * 执行 `build` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    fun build(): RegisteredDirectives {
        return RegisteredDirectivesImpl(simpleDirectives, stringValueDirectives, valueDirectives)
    }
}
