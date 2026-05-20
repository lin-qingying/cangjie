// FILE: functionReturn.cj

package sample.typeprovider

class User {
}

func makeUser(): <caret>User {
    return User()
}
