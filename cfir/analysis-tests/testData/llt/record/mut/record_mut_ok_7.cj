

interface I1 {
    mut func foo(): Unit
}

struct R1 <: I1 {
    public var i = 0

    public mut func foo() {
        i = i + 1
    }
}

main() {
    let obj: I1 = R1()
    obj.foo()
    return 0
}
