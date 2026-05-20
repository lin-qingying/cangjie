// FILE: functionArgument.cj
package sample.expectedtype

class User {
}

func accept(user: User) {
    user
}

func useUser() {
    accept(<caret>User())
}
