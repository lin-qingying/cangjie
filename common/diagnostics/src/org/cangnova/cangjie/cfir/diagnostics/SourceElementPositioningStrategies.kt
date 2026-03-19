package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.psi.PsiElement

object SourceElementPositioningStrategies {
    val DEFAULT: AbstractSourceElementPositioningStrategy = OffsetsOnlyPositioningStrategy()
    val VARIABLE_INITIALIZER: AbstractSourceElementPositioningStrategy = DEFAULT
    val PATTERN_VARIABLE_INITIALIZER: AbstractSourceElementPositioningStrategy = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.INITIALIZER_EQ,
        PositioningStrategies.INITIALIZER_EQ
    )

    val OPERATOR = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.OPERATOR,
        PositioningStrategies.OPERATOR
    )
    val REFERENCED_NAME_BY_QUALIFIED = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.REFERENCED_NAME_BY_QUALIFIED,
        PositioningStrategies.REFERENCED_NAME_BY_QUALIFIED
    )

}
