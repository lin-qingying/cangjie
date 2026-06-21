


interface I {
    func f(): Int64
}

open class C1 {}

class C2 <: C1 {}

class A<T> {}

extend<T> A<T> where T <: C1 {
    public func f() {
        1
    }
}

extend<T> A<T> <: I where T <: C2 {}

main() {
    return A<C1>().f() + A<C2>().f() - 2
}
