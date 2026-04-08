package org.cangnova.cangjie.frontend.arguments

// This file was generated automatically. See generator in :compiler:frontend-arguments-generator
// Please declare arguments in compiler/arguments/src/org/cangnova/cangjie/arguments/description/CommonCompilerArguments.kt
// DO NOT MODIFY IT MANUALLY.

abstract class CommonCompilerArguments : CommonToolArguments() {
    var autoAdvanceLanguageVersion: Boolean = true
        set(value) {
            checkFrozen()
            field = value
        }

    var autoAdvanceApiVersion: Boolean = true
        set(value) {
            checkFrozen()
            field = value
        }

    var languageVersion: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var verbose: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    var reportPerf: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    var dumpPerf: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

}
