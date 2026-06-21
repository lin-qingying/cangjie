

class A {
    operator func [](index0: Int64, value!: Int64, <!INVALID_SUBSCRIPT_ASSIGN_PARAMETER!>invalid1<!>: Int64, invalid2!: Int64) {
        0
    }
}

main() {}
