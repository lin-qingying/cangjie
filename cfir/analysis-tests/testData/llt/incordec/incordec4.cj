


main(): Int64 {
    return run()
}

class COO {
    public var a: Int32 = 6
    public var b: Int32 = 0
    public func foo1(x: Int32): Int32 {
        b = 1
        b++
        return a + b + x
    }
    public func foo2(x: Int32): Int32 {
        a--
        return a + b + x
    }
}

func run(): Int64 {
    var coo: COO = COO()
    var res1: Int32 = coo.foo1(1)
    var res2: Int32 = coo.foo2(1)
    if (res1 == 9 && res2 == 8) {
        return 0
    }
    return 1
}
