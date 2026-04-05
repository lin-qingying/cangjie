// FILE: topLevelLookup.cj
// TARGET_CLASS: User
// TARGET_FUNCTION: greet
// EXPECTED_FILE_SYMBOL_NAME: topLevelLookup.cj
// EXPECTED_FILE_SYMBOL_PACKAGE: sample.symbols
// EXPECTED_PACKAGE_SYMBOL_FQ_NAME: sample.symbols
// EXPECTED_CLASS_ID: sample.symbols.User
// EXPECTED_CALLABLE_ID: sample.symbols.greet

package sample.symbols

class User {
}

func greet(): User {
    return User()
}
