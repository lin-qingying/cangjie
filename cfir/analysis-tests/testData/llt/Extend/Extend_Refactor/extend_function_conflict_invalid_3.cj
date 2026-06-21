


interface I1<T> {
    func foo(a: T): Int64 {
        1
    }
}

interface I2<T> {
    func foo(a: T): Int64 {
        2
    }
}

class C1<T> {}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend<T> C1<T> <: I1<T> {}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend<T> C1<T> <: I2<T> {}

main() {
    return -1
}
