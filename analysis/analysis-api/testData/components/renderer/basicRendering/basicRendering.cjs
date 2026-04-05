// FILE: basicRendering.cjs
// TARGET_CLASS: User
// TARGET_FUNCTION: greet
// TARGET_CALL: buildUser()
// EXPECTED_RENDERED_CLASS_SYMBOL: sample.renderer.script.User
// EXPECTED_RENDERED_CALLABLE_SYMBOL: greet(value: User): User
// EXPECTED_RENDERED_TYPE: sample.renderer.script.User

package sample.renderer.script

class User {
}

func greet(value: User): User {
    return value
}

func buildUser(): User {
    return User()
}
