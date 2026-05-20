// FILE: functionReturnType.cj
package sample.types.returntype

class User {
}

func ma<caret>keUser(): User {
    return User()
}
