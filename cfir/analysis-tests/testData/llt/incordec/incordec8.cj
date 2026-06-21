


// 成员函数中b++
main(): Int64 {
    return run()
}

class COO {
    public var a: Int32 = 1
    public var b: Int32 = 0
    public func foo(x: Int32): Int32 {
        b = 1
        b++
        return a + b + x
    }
}

func run(): Int64 {
    var coo: COO = COO()
    var res1: Int32 = coo.foo(1)
    if (res1 == 4) {
        return 0
    }
    return 1
}
