

class A<T> {}

interface EQ {}

class S <: EQ {}

extend Int64 <: EQ {}

extend<T> A<T> where T <: EQ {
    public func add(a: T): T {
        return a
    }
}

main() {
    let a: A<Int64> = A<Int64>()
    return a.add(1) - 1
}
