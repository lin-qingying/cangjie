// FILE: file2.cj


class A {
    static let a = 1
    static const b = 1
}

// FILE: file1.cj



main() {
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>A.a++<!>
    A.<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> += 1
}
