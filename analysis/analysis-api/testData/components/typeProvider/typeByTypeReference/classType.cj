// FILE: classType.cj
package sample.typeprovider.refs

class User {
}

func makeUser(): <caret>User {
    return User()
}
