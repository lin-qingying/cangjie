


open class C {
    func coo() {
        return 0
    }
}

interface Tr {
    func bar(): Int64
}

class D <: C {}

extend D <: Tr {
    public func bar(): Int64 {
        return 0
    }
}

func foo<T>(a: T) where T <: C & Tr {
    return a.coo() + a.bar()
}

main(): Int64 {
    foo<D>(D())
    return 0
}
