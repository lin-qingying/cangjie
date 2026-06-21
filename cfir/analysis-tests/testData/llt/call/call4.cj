

open class A {}

open class B <: A {}

open class C <: B {}

class D <: C {}

func foo<X>(a: X, b: X, c: (X) -> Unit): Int64 {
    return 1
}

func foo(a: B, b: C, c: (A) -> Unit): Int64 {
    return 2
}

main() {
    var a = foo(B(), D(), {d: A =>})
    if (a != 2) {
        return 1
    }
    return 0
}
