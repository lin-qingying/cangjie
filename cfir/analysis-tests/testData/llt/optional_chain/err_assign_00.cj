


struct S {
    S(var x!: Int64) {}

    mut operator func [](i: Int64, value!: Int64) {}
}

main() {
    var s: ?S = S(x: 3)
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>s?[0]<!> = 9
    return
}
