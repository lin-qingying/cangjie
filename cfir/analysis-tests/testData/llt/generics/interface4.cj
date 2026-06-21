


interface Eq<T> {
    func equals(right: T): Bool
}

struct A <: Eq<A> {
    public func equals(right: A): Bool {
        return true
    }
}

func equal<T>(left: T, right: T) where T <: Eq<T> {
    return left.equals(right)
}

main(): Int64 {
    let a = A()
    let b = a
    if (equal<A>(a, b)) {
        return 0
    }
    return 1
}
