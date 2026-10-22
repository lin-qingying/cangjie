// MODULE: main
// MAIN_FILE_NAME: useCrossFileLet.cj
// FILE: sharedLet.cj
package test

let sharedRoot: String = "/"

// FILE: useCrossFileLet.cj
package test

func useSharedLet(): String {
    return sharedRoot
}
