// FILE: symbolPointerRestore.cj
// TARGET_NAME: greet
// EXPECTED_CALLABLE_NAME: greet

package sample.sessions.pointer

func greet(): Int64 {
    return 1
}

func consume(): Int64 {
    return greet()
}
