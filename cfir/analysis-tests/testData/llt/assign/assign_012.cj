

import std.reflect.TypeInfo

interface I<T> {
    func foo(): Unit {
        println(TypeInfo.of<T>())
    }
}

class A<T> {}

class B<T> {}

extend<X> A<B<X>> <: I<X> {}

main() {
    var a: I<Int64> = <!TYPE_MISMATCH!>A<B<Int64>>()<!>
    a.foo()
}
