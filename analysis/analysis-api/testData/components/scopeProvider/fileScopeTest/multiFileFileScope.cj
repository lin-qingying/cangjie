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
// FILE_SCOPE_AVAILABLE_NAME: MainOnly
// FILE_SCOPE_AVAILABLE_NAME: makeMainOnly
// FILE_SCOPE_CLASSIFIER: MainOnly
// FILE_SCOPE_CALLABLE: makeMainOnly
// FILE_SCOPE_ABSENT_NAME: HelperOnly
// FILE_SCOPE_ABSENT_NAME: makeHelperOnly
package analysis.scopes.multifile

class MainOnly {}

func makeMainOnly(): MainOnly {
    return MainOnly()
}
