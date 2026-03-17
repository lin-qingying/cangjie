package org.cangnova.cangjie.test.directives.model

class RegisteredDirectivesBuilder {
    private val simpleDirectives = linkedSetOf<SimpleDirective>()
    private val stringDirectives = linkedMapOf<StringDirective, MutableList<String>>()
    private val valueDirectives = linkedMapOf<ValueDirective<*>, MutableList<Any>>()

    operator fun Directive.unaryPlus() {
        when (this) {
            is SimpleDirective -> simpleDirectives += this
            is StringDirective -> stringDirectives.getOrPut(this) { mutableListOf() }
            is ValueDirective<*> -> valueDirectives.getOrPut(this) { mutableListOf() }
        }
    }

    fun put(directive: StringDirective, value: String) {
        stringDirectives.getOrPut(directive) { mutableListOf() } += value
    }

    fun <T : Any> put(directive: ValueDirective<T>, value: T) {
        @Suppress("UNCHECKED_CAST")
        valueDirectives.getOrPut(directive) { mutableListOf() } += value as Any
    }

    fun build(): RegisteredDirectives {
        return RegisteredDirectivesImpl(
            simpleDirectives = simpleDirectives.toList(),
            stringDirectives = stringDirectives.mapValues { it.value.toList() },
            valueDirectives = valueDirectives.mapValues { it.value.toList() },
        )
    }
}
