

interface I<T> {
    func foo(a: T): T {
        return a
    }
}

interface I2 <: I<Bool> {
    func bar(a: Int32): Int32
}

class B <: I2 {
    public func bar(a: Int32): Int32 {
        this.foo(false)
        return 0
    }
}

main(): Int64 {
    var a: I2 = B()
    return 0
}
