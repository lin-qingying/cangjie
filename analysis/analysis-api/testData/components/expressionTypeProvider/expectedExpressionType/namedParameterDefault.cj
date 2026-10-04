// FILE: namedParameterDefault.cj
package sample.expectedtype

class User {
}

func accept(user!: User = <expr>User()</expr>) {
    user
}
