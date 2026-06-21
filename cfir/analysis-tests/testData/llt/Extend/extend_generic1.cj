

class A<T> {}

interface EQ {}

class S <: EQ {}

extend<T> A<T> where T <: EQ {
    public func add() {}
}

main() {
    let a: A<S> = A<S>()
    a.add()
    let b: A<Int32> = A<Int32>()
    b.<!NOT_MEMBER_OF!>add<!>()
    0
}
