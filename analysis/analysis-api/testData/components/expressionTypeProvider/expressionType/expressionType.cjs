// FILE: expressionType.cjs
// TARGET_CLASS: User
// TARGET_CALL: makeUser()
// EXPECTED_EXPRESSION_TYPE: sample.script.User

package sample.script

class User {
}

func makeUser(): User {
    return User()
}

func useUser(): User {
    return makeUser()
}
