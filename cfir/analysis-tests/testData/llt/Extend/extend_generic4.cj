

class A<T> {}

interface EQ {}

class S <: EQ {}

extend<T> A<T> where T <: EQ {
    public func add(a: T): Int64 {
        return 0
    }
}

main() {
    let a: A<S> = A<S>()
    let s: S = S()
    return a.add(s)
}
