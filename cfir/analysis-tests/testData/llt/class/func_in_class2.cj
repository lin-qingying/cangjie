

open class B <: A {}

open class A {
    public func f(): A {
        return B()
    }
}

main(): Int64 {
    return 0
}
