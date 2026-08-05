// DIAGNOSTICS: -warnings
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

// FILE: failed02.cj


import a.Upper

main() {
    <!UNRESOLVED_REFERENCE!>A<!><Int64>().foo()
}
