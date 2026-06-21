

class A<T> {}

interface EQ {}

class S <: EQ {}

extend<T> A<T> where T <: EQ {
    public func add(a: T) {}
}

main() {
    let a: A<S> = A<S>()
    let s: S = S()
    a.add(s)
    0
}
