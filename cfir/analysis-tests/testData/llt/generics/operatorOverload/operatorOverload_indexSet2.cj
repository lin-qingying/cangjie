


class A {
    public operator func [](index: Int64, value!: Int64): <!INVALID_SUBSCRIPT_ASSIGN_RETURN!>Int64<!> {
        return 1;
    }
}

main(): Int64 {
    return 1;
}
