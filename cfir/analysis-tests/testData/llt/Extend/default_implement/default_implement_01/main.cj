// FILE: pkg.cj


package pkg

public interface I1<T> {
    func foo(): Unit {
        println(1)
    }
}

public class A {}

extend A <: I1<Int64> {}

// FILE: main.cj


import pkg.*

interface Real<T> <: I1<T> {
    func foo(): Unit {
        println(2)
    }
}

extend A <: Real<Int64> {}

main() {
    A().foo()
}
