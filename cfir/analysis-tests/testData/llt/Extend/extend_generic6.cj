

open class A<T> {}

interface EQ {}

extend Int64 <: EQ {}

extend<T> A<T> where T <: EQ {
    public func soo() {}
    func add(a: T): T {
        return a
    }
}

class B<E> <: A<Int64> {
    func test() {
        soo()
        return add(0)
    }
}

main() {
    let b = B<Int32>()
    return b.test()
}
