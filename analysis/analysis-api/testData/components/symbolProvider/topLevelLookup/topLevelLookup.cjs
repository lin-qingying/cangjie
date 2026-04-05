// FILE: topLevelLookup.cjs
// TARGET_CLASS: User
// TARGET_FUNCTION: greet
// EXPECTED_FILE_SYMBOL_NAME: topLevelLookup.cjs
// EXPECTED_FILE_SYMBOL_PACKAGE: sample.symbols.script
// EXPECTED_PACKAGE_SYMBOL_FQ_NAME: sample.symbols.script
// EXPECTED_CLASS_ID: sample.symbols.script.User
// EXPECTED_CALLABLE_ID: sample.symbols.script.greet

package sample.symbols.script

class User {
}

func greet(): User {
    return User()
}
