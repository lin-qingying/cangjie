

struct A {
    var x = 0
}

extend A {
    func f() {
        <!CANNOT_MODIFY_VAR!>x<!> += 1
    }
}

main() {}
