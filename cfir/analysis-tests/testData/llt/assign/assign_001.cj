

func foo1(): Int32 {
    1
}

func foo2(): Int32 {
    2
}

main(): Int64 {
    <!UNQUALIFIED_LEFT_VALUE_ASSIGNED!>foo1<!> = foo2
    return 0
}
