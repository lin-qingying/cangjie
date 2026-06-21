// FILE: a.cj


package a

public interface Upper {}

// FILE: b.cj


package b

private import a.*

public class B <: Upper {}

public class A<T> {}

extend<T> A<T> where T <: Upper {
    public func foo() {
        println("foo")
    }
}

// FILE: pass01.cj



import a.Upper
import b.*

main() {
    A<B>().foo()
}
