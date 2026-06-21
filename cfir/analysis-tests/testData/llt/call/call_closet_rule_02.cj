

open class Base {}

class Sub <: Base {}

class F<X> {
    static func foo<Y>(a: X, b: Y) {
        1
    } // foo1
    static func foo<Z>(a: Sub, b: Z) {
        2
    } // foo2
}

main() {
    /* The functions that enter the candidate set are foo1 and partial instantiated foo2: foo<Y>(a:Base, b: Y) and foo<Z>(a: Sub, b: Z) */
    var f = F<Base>.foo(Sub(), Base()) // foo2
    return f - 2
}
