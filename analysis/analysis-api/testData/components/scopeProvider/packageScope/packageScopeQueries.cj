// FILE: packageScopeQueries.cj
// PACKAGE_SCOPE_AVAILABLE_NAME: PackageEntry
// PACKAGE_SCOPE_AVAILABLE_NAME: buildEntry
// PACKAGE_SCOPE_CLASSIFIER: PackageEntry
// PACKAGE_SCOPE_CALLABLE: buildEntry

package sample.scope

class PackageEntry {}

func buildEntry(): PackageEntry {
    return PackageEntry()
}
