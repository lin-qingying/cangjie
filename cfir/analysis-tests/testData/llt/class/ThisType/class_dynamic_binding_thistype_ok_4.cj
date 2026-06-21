

open class A {
    func test() {
        return this
    }
    protected open func foo() {
        1
    }
}

class B <: A {
    protected override func foo() {
        2
    }
}

main() {
    let b = B()
    let x: () -> B = b.test
    let y = x() // Type of y is B
    if (y.foo() != 2) {
        return 1
    }
    return 0
}
