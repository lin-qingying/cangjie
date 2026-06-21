

func f(a: Array<Int64>) {
    a.size
}

main() {
    f([if (true) {
        <!UNRESOLVED_REFERENCE!>x<!> = 2
        1
    } else {
        2
    }])
}
