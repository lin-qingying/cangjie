

interface Data {
    static func foo(a: Int64): Int64

    func fooo(a!: Int64): Int64
}

interface Data2 {
    static func foo(a: Int64): Int64 {
        return 2
    }
}

interface Data3 {
    func fooo(a!: Int64): Int64 {
        return 2
    }
}

class Test <: Data & Data2 & Data3 {}

main(): Int64 {
    var res1: Int64 = Test().fooo(a: 2)
    var res2: Int64 = Test.foo(2)
    if (res1 != 2 || res2 != 2) {
        return 1
    }
    return 0
}
