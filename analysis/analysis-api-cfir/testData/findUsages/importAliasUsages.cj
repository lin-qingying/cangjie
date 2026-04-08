// MODULE: main
// MAIN_MODULE
// MAIN_FILE_NAME: importAliasUsages.cj
// FILE: provider.cj
package sample.provider

func greet() {
}

// FILE: importAliasUsages.cj
package sample.consumer

import sample.provider.greet as welcome

func firstCaller() {
    welcome()
}

func secondCaller() {
    welcome()
}
