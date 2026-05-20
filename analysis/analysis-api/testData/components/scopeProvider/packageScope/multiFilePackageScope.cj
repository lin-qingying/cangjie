// MODULE: main
// MAIN_MODULE
// MAIN_FILE_NAME: main.cj
// FILE: helper.cj
package analysis.scopes.multifile

class HelperOnly {}

func makeHelperOnly(): HelperOnly {
    return HelperOnly()
}

// FILE: main.cj
// PACKAGE_SCOPE_AVAILABLE_NAME: MainOnly
// PACKAGE_SCOPE_AVAILABLE_NAME: makeMainOnly
// PACKAGE_SCOPE_AVAILABLE_NAME: HelperOnly
// PACKAGE_SCOPE_AVAILABLE_NAME: makeHelperOnly
// PACKAGE_SCOPE_CLASSIFIER: MainOnly
// PACKAGE_SCOPE_CLASSIFIER: HelperOnly
// PACKAGE_SCOPE_CALLABLE: makeMainOnly
// PACKAGE_SCOPE_CALLABLE: makeHelperOnly
package analysis.scopes.multifile

class MainOnly {}

func makeMainOnly(): MainOnly {
    return MainOnly()
}
