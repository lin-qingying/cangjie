// FILE: pkg.cj


package pkg

class Base {}

class A<T> {
    var v: ?T = None
}

public class B {
    var b: ?A<Base> = None
    func test() {
        if (let Some(a) <- b) {
            a.v
        }
    }
}

public open class B0 {
    var b: B
    init(a: B) {
        b = a
    }
}

public class B1<T> <: B0 {
    init(a: B) {
        super(a)
    }
    public func test(): Unit {
        b.b
    }
}

public func foo<T>() {
    return B1<T>(B())
}

// FILE: main.cj



internal import pkg.*

main() {
    foo<Int64>()
    0
}
