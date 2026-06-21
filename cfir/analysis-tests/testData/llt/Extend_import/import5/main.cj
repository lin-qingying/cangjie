// FILE: p1.cj


package p1

public class Foo<T> {}

extend<T> Foo<T> {
    public func foo(a: T) {
        print("hello")
    }
}

public interface I<T> {
    func test(a: T): Unit
}

extend<T> Foo<T> <: I<T> {
    public func test(a: T) {
        print("test")
    }
}

// FILE: main.cj




internal import p1.*

main() {
    let a: Foo<Int32> = Foo<Int32>()
    a.foo(1)
    a.test(1)
    0
}
