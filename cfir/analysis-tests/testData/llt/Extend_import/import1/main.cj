// FILE: p1.cj


package p1

public class Foo {}

extend Foo {
    public func foo() {
        print("hello")
    }
}

public interface I {
    func test(): Unit
}

extend Foo <: I {
    public func test() {
        print("test")
    }
}

// FILE: main.cj



internal import p1.{Foo, I}

main() {
    let a: Foo = Foo()
    a.foo()
    a.test()
    0
}
