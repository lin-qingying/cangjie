package org.cangnova.cangjie.cfir.render

import org.cangnova.cangjie.cfir.types.ConeAttribute
import org.cangnova.cangjie.utils.ifNotEmpty

abstract class ConeAttributeRenderer {
    abstract fun render(attributes: Iterable<ConeAttribute<*>>): String

    object ToString : ConeAttributeRenderer() {
        override fun render(attributes: Iterable<ConeAttribute<*>>): String {
            return attributes.sortedBy { it.key.qualifiedName }.joinToString(separator = " ", postfix = " ")
        }
    }

    object ForReadability : ConeAttributeRenderer() {
        override fun render(attributes: Iterable<ConeAttribute<*>>): String {
            return attributes.mapNotNull { attribute -> attribute.renderForReadability()?.let { attribute to it } }
                .sortedBy { (attribute, _) -> attribute.key.qualifiedName }
                .ifNotEmpty {
                    joinToString(separator = " ", postfix = " ") { (_, output) ->
                        output
                    }
                } ?: ""
        }
    }

    object None : ConeAttributeRenderer() {
        override fun render(attributes: Iterable<ConeAttribute<*>>): String {
            return ""
        }
    }
}
