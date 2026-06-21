

// Should only report type mismatch at line 10

class A {
    operator func -() {
        1
    }
}

main() {
    let c = 1
    let a: Rune = <!TYPE_MISMATCH!>-A()<!>
    let b = <!INVALID_UNARY_EXPR!>- r'c'<!>
}
