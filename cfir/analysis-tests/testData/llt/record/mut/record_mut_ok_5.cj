

interface I1 {
    mut func foo1(): Unit
}

struct R1 <: I1 {
    public var i = 0
    public mut func foo1(): Unit {
        i = i + 1
    }
}

main() {
    var a = R1()
    var b: I1 = a
    b.foo1()
    if (a.i == 0) {
        return 0
    }
    return 1
}
