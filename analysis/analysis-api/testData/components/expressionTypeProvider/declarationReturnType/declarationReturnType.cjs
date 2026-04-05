// FILE: declarationReturnType.cjs
// TARGET_CLASS: User
// TARGET_FUNCTION: makeUser
// EXPECTED_DECLARATION_RETURN_TYPE: sample.script.User

package sample.script

class User {
}

func makeUser(): User {
    return User()
}

func useUser(): User {
    return makeUser()
}
