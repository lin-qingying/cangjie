// FILE: resolveSymbol.cjs
// TARGET_CALL: makeUser()
// TARGET_NAME: makeUser
// EXPECTED_CALLABLE_NAME: makeUser

package sample.script

class User {
}

func makeUser(): User {
    return User()
}

func useUser(): User {
    return makeUser()
}
