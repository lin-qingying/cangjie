package org.cangnova.cangjie.analysis.api.renderer.base.annotations

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationValue

object CaAnnotationValueRenderer {

    fun render(value: CaAnnotationValue): String = buildString {
        renderConstantValue(value)
    }

    private fun StringBuilder.renderConstantValue(value: CaAnnotationValue) {

    }
}