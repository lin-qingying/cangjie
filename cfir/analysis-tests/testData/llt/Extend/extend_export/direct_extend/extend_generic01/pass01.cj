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

// FILE: pass01.cj



import a.A
import a.Upper

main() {
    A<Int64>().foo()
}
