package org.cangnova.cangjie.frontend.arguments

import java.io.Serializable

// This file was generated automatically. See generator in :compiler:frontend-arguments-generator
// Please declare arguments in compiler/arguments/src/org/cangnova/cangjie/arguments/description/CommonToolArguments.kt
// DO NOT MODIFY IT MANUALLY.

abstract class CommonToolArguments : Freezable(), Serializable {
    /** 实际命令行的规范参数及单次值，保留跨参数交错顺序；直接调用同样必须提供完整顺序。 */
    var explicitArgumentOccurrences: List<Pair<String, String>> = emptyList()
        set(value) {
            checkFrozen()
            field = java.util.Collections.unmodifiableList(value.toList())
        }

}
