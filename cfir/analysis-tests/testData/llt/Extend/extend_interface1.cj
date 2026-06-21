

interface Eq<T> {
    func equals(other: T): Bool
}

class A {
    public func equals(other: A): Bool {
        return true;
    }
}

extend A <: Eq<A> {}

main(): Int64 {
    let a: Eq<A> = A();
    if (!a.equals(A())) {
        return 1;
    }
    return 0;
}
