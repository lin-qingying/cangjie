

class A<T> {}

extend<T> A<T> where T <: ToString {
    func f() {
        1
    }
}

extend<T> A<T> {
    func f2() {
        <!UNRESOLVED_REFERENCE!>f<!>
        this.<!NOT_MEMBER_OF!>f<!>
        this.<!NOT_MEMBER_OF!>f<!>()
    }
}

main() {}
