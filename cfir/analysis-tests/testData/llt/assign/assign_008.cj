

func f() {
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>E.A<!> = E.A
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>A<!> = A
}

enum E {
    A
}

main() {}
