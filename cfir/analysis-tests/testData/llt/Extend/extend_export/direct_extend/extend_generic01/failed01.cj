// FILE: a.cj


package a

public interface Upper {}

extend Int64 <: Upper {}

public class A<T> {}

extend<T> A<T> where T <: Upper {
    public func foo() {
        println("foo")
    }
}

// FILE: failed01.cj


import a.A

main() {
    A<Int64>().<!NOT_MEMBER_OF!>foo<!>()
}
