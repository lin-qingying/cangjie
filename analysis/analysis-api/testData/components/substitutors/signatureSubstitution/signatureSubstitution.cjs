// FILE: signatureSubstitution.cjs
// TARGET_CLASS: User
// TARGET_FUNCTION: identity
// EXPECTED_SUBSTITUTED_PARAMETER_TYPE: sample.substitution.script.User
// EXPECTED_SUBSTITUTED_RETURN_TYPE: sample.substitution.script.User

package sample.substitution.script

class User {
}

func identity<T>(value: T): T {
    return value
}

func consume(): User {
    return identity(User())
}
