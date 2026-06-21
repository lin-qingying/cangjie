


interface I<T> <: Equatable<T> where T <: Equatable<T> {
    operator func ==(a: T): Bool {
        true
    }
    operator func !=(a: T): Bool {
        false
    }
}

class A<T> where T <: I<T> {
    let a: T
    init(v: T) {
        a = v
    }
    func test(val: T): Bool {
        return val == a // Test for this type check
    }
}

class B <: I<B> {}

main(): Int64 {
    let b = B()
    let a = A<B>(b)
    a.test(b)
    return 0
}
