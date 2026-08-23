



interface I<T> {
    func foo(a: T) {}
}

interface I2 {}

open class A {}

open class B <: A {}

class C <: B & I2 {}

class D <: I<B> & I<I2> {} // exist func foo(a: B);  func foo(a: I2)

main() {
    let d = D()
    d.foo(C())
    return 1
}

