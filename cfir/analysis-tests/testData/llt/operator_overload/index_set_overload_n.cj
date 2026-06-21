


class A {
    operator func [](arg1: Int64, arg2: String, <!INVALID_SUBSCRIPT_ASSIGN_PARAMETER!>x<!>!: Int64): Int64 {
        return 1
    }
}
