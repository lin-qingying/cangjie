// FILE: pkg.cj


package pkg

public open class A<T> {
    public func test() {
        0
    }
}

public class B <: A<Int32> {}

public class C {
    public let v = B()
}

// FILE: test.cj



internal import pkg.*

main() {
    let ins = C()
    return ins.v.test()
}
