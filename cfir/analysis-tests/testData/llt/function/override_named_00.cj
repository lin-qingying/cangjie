

open class A {
    protected open func f(a!: Int32): Int32 {
        a + 1
    }
}

class B <: A {
    protected override func f(a!: Int32): Int32 {
        a + 2
    }
}

main() {
    B().f(a: 3)
}
