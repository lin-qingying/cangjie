// FILE: pkg.cj


package pkg

public interface I<T> {}

public open class A {}

extend A <: I<A> {}

// FILE: main.cj


internal import pkg.*

class B <: A {}

main() {
    let v: Any = B()
    return 0
}
