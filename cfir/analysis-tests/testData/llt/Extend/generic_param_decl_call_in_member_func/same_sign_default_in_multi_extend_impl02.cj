

interface I1<T> {
    func f(): Array<T> {
        []
    }
    func <!EXTEND_MEMBER_CANNOT_SHADOW!>g<!>() {
        let v = f()
    }
}

class A<N> {}

extend A<Int64> <: I1<Int64> {
    // func foo(a: Int64) {}
}

extend A<String> <: I1<String> {
    // func foo(a: Int64) {}
}

main() {
    return 0
}
