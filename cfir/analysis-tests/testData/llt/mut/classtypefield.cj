


class T {
    var i = 1
}

struct C {
    var a = T()
}

struct D {
    var d = C()
}

struct A {
    var a = T()
    var b = D()
    func foo() {
        a.i = 2 // ok
        <!CANNOT_MODIFY_VAR!>a<!> = T() // error
        <!CANNOT_MODIFY_VAR!>b<!>.d.a = T() // error
        b.d.a.i = 2 // ok
    }
}

main() {}
