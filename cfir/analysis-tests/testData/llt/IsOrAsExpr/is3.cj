

open class A {}

open class B <: A {}

class C <: B {}

func test(): A {
    return C()
}

main() {
    var res = test() is C
    if (res) {
        return 0
    }
    return 1
}
