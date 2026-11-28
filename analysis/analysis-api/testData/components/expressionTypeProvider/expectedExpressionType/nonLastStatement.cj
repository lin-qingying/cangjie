// FILE: nonLastStatement.cj
package sample.expectedtype

class User {
}

func sequence(): Int64 {
    <expr>User()</expr>
    return 1
}
