package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.psi.PsiElement

object SourceElementPositioningStrategies {
    val DEFAULT: AbstractSourceElementPositioningStrategy = OffsetsOnlyPositioningStrategy()
    val ACTUAL_DECLARATION_NAME = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.ACTUAL_DECLARATION_NAME,
        PositioningStrategies.ACTUAL_DECLARATION_NAME
    )
    val DECLARATION_START_TO_NAME = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DECLARATION_START_TO_NAME,
        PositioningStrategies.DECLARATION_START_TO_NAME
    )
    val CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS,
        PositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS
    )
    val VISIBILITY_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.VISIBILITY_MODIFIER,
        PositioningStrategies.VISIBILITY_MODIFIER,
    )
    val OVERRIDE_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.OVERRIDE_MODIFIER,
        PositioningStrategies.OVERRIDE_MODIFIER,
    )
    val VARIABLE_INITIALIZER: AbstractSourceElementPositioningStrategy = DEFAULT
    val PATTERN_VARIABLE_INITIALIZER: AbstractSourceElementPositioningStrategy = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.INITIALIZER_EQ,
        PositioningStrategies.INITIALIZER_EQ
    )
    val IMPORT_LAST_NAME = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.IMPORT_LAST_NAME,
        PositioningStrategies.IMPORT_LAST_NAME
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
