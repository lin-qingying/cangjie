

class A {
    func foo() {
        this.<!NOT_MEMBER_OF!>bar<!>() // error
        <!UNRESOLVED_REFERENCE!>bar<!>() // error
    }
}

extend A {
    func bar() {}
}

struct S {
    func foo() {
        this.<!NOT_MEMBER_OF!>bar<!>() // error
        <!UNRESOLVED_REFERENCE!>bar<!>() // error
    }
}

extend S {
    func bar() {}
}

enum E {
    EE

    func foo() {
        this.<!NOT_MEMBER_OF!>bar<!>() // error
        <!UNRESOLVED_REFERENCE!>bar<!>() // error
    }
}

extend E {
    func bar() {}
}

main() {}
