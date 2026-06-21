


abstract class A {
    protected open func f(): Int64
}

abstract class B <: A {
    protected override func f(): Int64
}

class C <: B {
    protected override func f(): Int64 {
        return 1
    }
}

main() {
    return C().f() - 1
}
