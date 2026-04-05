// FILE: packageScopeQueries.cjs
// PACKAGE_SCOPE_AVAILABLE_NAME: ScriptPackageEntry
// PACKAGE_SCOPE_AVAILABLE_NAME: buildScriptEntry
// PACKAGE_SCOPE_CLASSIFIER: ScriptPackageEntry
// PACKAGE_SCOPE_CALLABLE: buildScriptEntry

package sample.scope.script

class ScriptPackageEntry {}

func buildScriptEntry(): ScriptPackageEntry {
    return ScriptPackageEntry()
}
