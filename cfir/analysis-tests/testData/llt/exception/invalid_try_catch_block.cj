

func foo(a: Array<Int64>): Int64 {
    1
}

main(): Int64 {
    foo([try {
        1
    } catch (_) {
        <!UNRESOLVED_REFERENCE!>f<!> // undefined
        2
    }])
}
