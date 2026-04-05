// FILE: basicRendering.cj
// TARGET_CLASS: User
// TARGET_FUNCTION: greet
// TARGET_CALL: buildUser()
// EXPECTED_RENDERED_CLASS_SYMBOL: sample.renderer.User
// EXPECTED_RENDERED_CALLABLE_SYMBOL: greet(value: User): User
// EXPECTED_RENDERED_TYPE: sample.renderer.User

package sample.renderer

class User {
}

func greet(value: User): User {
    return value
}

func buildUser(): User {
    return User()
}
