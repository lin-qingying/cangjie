package org.cangjie.test.services.impl

import org.cangjie.test.directives.model.*

class RegisteredDirectivesParser(private val container: DirectivesContainer) {
    companion object {
        private val DIRECTIVE_PATTERN = Regex("""^//\s*([A-Z0-9_]+)(:[ \t]*(.*))? *$""")
        private val SPACES_PATTERN = Regex("""[,]?[ \t]+""")
        private const val NAME_GROUP = 1
        private const val VALUES_GROUP = 3

        fun parseDirective(line: String): RawDirective? {
            val result = DIRECTIVE_PATTERN.matchEntire(line)?.groupValues ?: return null
            val name = result.getOrNull(NAME_GROUP) ?: return null
            val rawValue = result.getOrNull(VALUES_GROUP)
            val values = rawValue
                ?.split(SPACES_PATTERN)
                ?.filter { it.isNotBlank() }
                ?.takeIf { it.isNotEmpty() }
            return RawDirective(name, values, rawValue)
        }
    }

    data class RawDirective(val name: String, val values: List<String>?, val rawValue: String?)
    data class ParsedDirective(val directive: Directive, val values: List<*>)

    private val simpleDirectives = mutableListOf<SimpleDirective>()
    private val stringValueDirectives = mutableMapOf<StringDirective, MutableList<String>>()
    private val valueDirectives = mutableMapOf<ValueDirective<*>, MutableList<Any>>()

    /**
     * @return 若该行包含“已注册且可识别”的指令，则返回 true。
     */
    fun parse(line: String): Boolean {
        val rawDirective = parseDirective(line) ?: return false
        val parsedDirective = convertToRegisteredDirective(rawDirective) ?: return false
        addParsedDirective(parsedDirective)
        return true
    }

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

    fun convertToRegisteredDirective(rawDirective: RawDirective): ParsedDirective? {
        val (name, rawValues, rawValueString) = rawDirective
        val directive = container[name] ?: return null

        val values: List<*> = when (directive) {
            is SimpleDirective -> {
                require(rawValues == null) { "指令 $directive 不应带参数，但实际传入：${rawValues?.joinToString(", ")}" }
                emptyList<Any?>()
            }

            is StringDirective -> {
                when (directive.multiLine) {
                    true -> listOfNotNull(rawValueString)
                    false -> rawValues ?: emptyList()
                }
            }

            is ValueDirective<*> -> {
                require(rawValues != null) { "指令 $directive 必须至少提供一个值" }
                rawValues.map {
                    directive.parser.invoke(it) ?: error("$it 不是 $directive 的合法值")
                }
            }
        }
        return ParsedDirective(directive, values)
    }

    fun build(): RegisteredDirectives {
        return RegisteredDirectivesImpl(simpleDirectives, stringValueDirectives, valueDirectives)
    }
}

