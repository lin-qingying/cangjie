// FILE: resolveTopLevelFunction.cj

package sample

func makeUser(): Int64 {
    return 42
}

func useUser(): Int64 {
    return <caret>makeUser()
}
