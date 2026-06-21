

struct S0 {
    var x0 = 0
}

struct S1 {
    var x1 = S0()

    func f() {
        <!CANNOT_MODIFY_VAR!>x1<!>.x0 += 1
    }
}

main() {}
