// FILE: declarationReturnType.cj
// TARGET_CLASS: User
// TARGET_FUNCTION: makeUser
// EXPECTED_DECLARATION_RETURN_TYPE: sample.User

package sample

class User {
}

func makeUser(): User {
    return User()
}

func useUser(): User {
    return makeUser()
}
