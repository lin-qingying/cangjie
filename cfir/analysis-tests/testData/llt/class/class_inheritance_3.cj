

interface Base3 {
    func foo(): Int64
}

interface Data {
    func foo(): Int64 {
        return 1
    }
    func foo(a: Int64): Int64 {
        return a
    }
}

class Test <: Base3 & Data {}

main(): Int64 {
    var obj = Test()
    if (obj.foo() == 1 && obj.foo(0) == 0) {
        return 0
    }

    return 1
}
