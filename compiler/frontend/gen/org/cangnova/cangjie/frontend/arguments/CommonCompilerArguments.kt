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

    var warnOff: Array<String> = emptyArray()
        set(value) {
            checkFrozen()
            field = value
        }

    var warnOn: Array<String> = emptyArray()
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

    var compileCjd: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    var debug: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    var optimizationLevel: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var outputFile: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var outputDirectory: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var cjmpCommonPart: Array<String> = emptyArray()
        set(value) {
            checkFrozen()
            field = value
        }

    var cjmpCommonPartChir: Array<String> = emptyArray()
        set(value) {
            checkFrozen()
            field = value
        }

    var cjmpCompileCommon: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

}
