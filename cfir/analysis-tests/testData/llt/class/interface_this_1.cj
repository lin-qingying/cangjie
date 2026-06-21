

interface I0 {
    func foo(): Int64 {
        return 1
    }

    func foo1(): Int64 {
        var res = this.foo()
        return res
    }
}

class C0 <: I0 {}

main(): Int64 {
    var obj = C0()
    var x = obj.foo1()
    if (x == 1) {
        return 0
    }
    return 1
}
