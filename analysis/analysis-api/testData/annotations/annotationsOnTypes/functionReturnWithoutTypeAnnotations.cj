// FILE: functionReturnWithoutTypeAnnotations.cj

package sample.annotations

class User {
}

func makeUser(): <caret>User {
    return User()
}
