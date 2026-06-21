

interface Eq<T> {
    func add(a: T): Unit
}

class C {}

extend C <: Eq<C> {
    public func add(a: C): Unit {
    }
}

extend Int32 <: Eq<Int32> {
    public func add(a: Int32): Unit {
        print("1234")
    }
}

main() {
    1.add(1)
    0
}
