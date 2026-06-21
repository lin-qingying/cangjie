// FILE: pkg.cj


package pkg

public open class A<T> {
    public func test() {
        0
    }
}

public open class B <: A<Int32> {}

// FILE: test.cj



internal import pkg.*

class C <: B {
    func foo() {
        return test()
    }
}

main() {
    let ins = C()
    return ins.foo()
}
