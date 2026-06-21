


open class A {}

class B <: A {}

// non-classlike types should be able to downcast to its real type
main() {
    let f0: (A) -> B = {a: A => B()}
    let f: (B) -> A = f0
    if (f is (A) -> B) {
        return 0
    }
    return 1
}
