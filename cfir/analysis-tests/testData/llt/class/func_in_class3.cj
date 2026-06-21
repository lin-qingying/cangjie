

open class C <: B {}

open class B <: A {}

open class A {
    public func f(): A {
        return C()
    }
}

main(): Int64 {
    return 0
}
