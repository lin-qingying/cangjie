

class A {
    operator func [](index: Int64, <!INVALID_SUBSCRIPT_ASSIGN_PARAMETER!>valu<!>: Int64) {}
}

main() {}
