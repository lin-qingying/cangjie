

open class A {
    protected open func f(a!: Int32): Int32 {
        a + 1
    }
}

class B <: A {
    protected override func <!PARAM_NAMED_MISMATCHED!>f<!>(b!: Int32): Int32 {
        b + 2
    }
}

main() {
    B().f(b: 3)
}
