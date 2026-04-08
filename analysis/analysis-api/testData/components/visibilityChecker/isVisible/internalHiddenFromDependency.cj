// MODULE: provider
// FILE: provider.cj
package sample.visibility.multimodule

internal func dependencyInternalGreeting(): Int64 {
    return 0
}

// MODULE: main(provider)
// MAIN_MODULE
// MAIN_FILE_NAME: main.cj
// FILE: main.cj
// TARGET_NAME: dependencyInternalGreeting
// VISIBILITY_TARGET_KIND: TOP_LEVEL_FUNCTION
// EXPECTED_SYMBOL_VISIBILITY: INTERNAL
// EXPECTED_VISIBILITY_EXPLICIT: true
// EXPECTED_IS_VISIBLE: false
package sample.visibility.multimodule

func consume(): Int64 {
    return 0
}
