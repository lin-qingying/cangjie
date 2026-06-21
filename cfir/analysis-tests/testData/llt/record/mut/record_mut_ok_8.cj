

interface I1 {
    mut func foo(): Unit
}

struct R1 <: I1 {
    public var i = 0

    public mut func foo(): Unit {
        i = i + 1
    }
}

main() {
    var obj: I1 = R1()
    var fn = obj.foo
    return 0
}
