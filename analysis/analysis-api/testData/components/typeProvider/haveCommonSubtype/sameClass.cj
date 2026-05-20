// FILE: sameClass.cj

package sample.typeprovider

class User {
}

func left(): User {
    return User()
}

func right(): User {
    return User()
}

func use(): Int64 {
    let a = <expr_left>left()</expr_left>
    let b = <expr_right>right()</expr_right>
    return 1
}
