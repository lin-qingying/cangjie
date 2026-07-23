

func f() {
    E.<!CANNOT_ASSIGN_TO_IMMUTABLE!>A<!> = E.A
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>A<!> = A
}

enum E {
    A
}

main() {}
