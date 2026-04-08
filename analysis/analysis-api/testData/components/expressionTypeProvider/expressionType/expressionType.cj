// FILE: expressionType.cj
// TARGET_CLASS: User
// TARGET_CALL: makeUser()
// EXPECTED_EXPRESSION_TYPE: sample.User

package sample

class User {
}

func makeUser(): User {
    return User()
}

func useUser(): User {
    return makeUser()
}
