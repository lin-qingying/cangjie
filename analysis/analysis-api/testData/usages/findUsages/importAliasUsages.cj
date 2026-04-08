// MODULE: main
// MAIN_MODULE
// MAIN_FILE_NAME: importAliasUsages.cj
// FILE: provider.cj
package sample.provider

func greet() {
}

// FILE: importAliasUsages.cj
// TARGET_KIND: IMPORT_ALIAS
// TARGET_NAME: welcome
// EXPECTED_USAGE_COUNT: 2
// EXPECTED_USAGE_SCOPE_KIND: LOCAL
package sample.consumer

import sample.provider.greet as welcome

func firstCaller() {
    welcome()
}

func secondCaller() {
    welcome()
}
