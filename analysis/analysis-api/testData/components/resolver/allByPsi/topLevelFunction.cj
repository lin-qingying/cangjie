// FILE: topLevelFunction.cj

package sample

func makeUser(): Int64 {
    return 42
}

func useUser(): Int64 {
    return makeUser()
}
