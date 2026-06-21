// FILE: pkg.cj


package pkg

public open class A {
    protected open func ff() {
        1
    } // f1 only visible in current pkg
}

public open class B <: A {
    protected func ff() {
        2
    } // f2, override f1
}

public func test(ins: A) {
    return ins.ff()
}

// FILE: main.cj



internal import pkg.*

main() {
    let v = test(B())
    if (v != 2) {
        return 1
    }
    return 0
}
