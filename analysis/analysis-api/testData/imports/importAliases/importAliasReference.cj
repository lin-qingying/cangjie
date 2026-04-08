// FILE: importAliasReference.cj
// TARGET_NAME: welcome
// EXPECTED_ALIAS_NAME: welcome
package sample.imports.aliases

import sample.imports.aliases.greet as welcome

func greet(): Int64 {
    return 0
}

func consume(): Int64 {
    return welcome()
}
