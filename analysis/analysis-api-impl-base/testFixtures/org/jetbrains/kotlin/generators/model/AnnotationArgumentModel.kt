package org.jetbrains.kotlin.generators.model

class AnnotationArgumentModel(
    val name: String = DEFAULT_NAME,
    val value: Any,
) {
    companion object {
        const val DEFAULT_NAME = "value"
    }
}
