

open class A {}

open class B <: A {}

class C <: B {}

main() {
    var a: Array<A> = [C()]
    var res = a[0] is C
    if (res) {
        return 0
    }
    return 1
}
