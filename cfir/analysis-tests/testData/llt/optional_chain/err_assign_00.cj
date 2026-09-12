


struct S {
    S(var x!: Int64) {}

    mut operator func [](i: Int64, value!: Int64) {}
}

main() {
    var s: ?S = S(x: 3)
    <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>s?<!>[0] = 9
    return
}
