// MODULE: scriptMain
// MODULE_KIND: ScriptSource
// MAIN_MODULE
// MAIN_FILE_NAME: scriptMain.cjs
// EXPECTED_PRIMARY_MODULE_SHAPE: ScriptModule
// EXPECTED_BINARY_ARTIFACT_MODULE_SHAPE: LibraryBinaryModule
// EXPECTED_AUXILIARY_MODULE_SHAPE: BuiltinsModule
// EXPECTED_AUXILIARY_MODULE_SHAPE: ScriptDependenciesModule
// EXPECTED_DIRECT_REGULAR_DEPENDENCY: <test-builtins>
// EXPECTED_DIRECT_REGULAR_DEPENDENCY: scriptMain.scriptDependencies
// EXPECTED_IS_RESOLVABLE: true
// FILE: scriptMain.cjs
package sample.script

class ScriptEntry {
}

func runScript(): ScriptEntry {
    return ScriptEntry()
}
