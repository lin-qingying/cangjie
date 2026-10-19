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

    var importPath: Array<String> = emptyArray()
        set(value) {
            checkFrozen()
            field = value
        }

    var moduleName: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var compilePackage: Array<String> = emptyArray()
        set(value) {
            checkFrozen()
            field = value
        }

    var noSubPackage: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    var noPrelude: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    var conditionalCompilationConfig: Array<String> = emptyArray()
        set(value) {
            checkFrozen()
            field = value
        }

    var compileTest: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    var compileTestsOnly: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    var exportForTest: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    var mock: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var emitJvm: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    var jvmOutputDirectory: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var compileMacro: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    var debugMacro: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    var parallelMacroExpansion: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    var dumpMacro: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    var diagnosticFormat: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var warnOn: Array<String> = emptyArray()
        set(value) {
            checkFrozen()
            field = value
        }

    var warnOff: Array<String> = emptyArray()
        set(value) {
            checkFrozen()
            field = value
        }

    var errorCountLimit: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var dumpTokens: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    var dumpParse: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    var dumpAst: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    var typecheck: Boolean = false
        set(value) {
            checkFrozen()
            field = value
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
