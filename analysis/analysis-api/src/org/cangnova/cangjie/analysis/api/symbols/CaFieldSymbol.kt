package org.cangnova.cangjie.analysis.api.symbols

abstract class CaFieldSymbol : CaVariableSymbol() {
  abstract  val isStatic: Boolean

    abstract val isConst: Boolean
}
