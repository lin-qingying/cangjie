

open class A {
    public func f(): A {
        return B()
    }
}

open class B <: A {}

main(): Int64 {
    return 0
}
