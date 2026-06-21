

class C {
    public var r: Int32 = 0
    public init(x: Int32) {
        r = x
    }
    public operator func ==(b: C): Bool {
        return this.r == b.r
    }
}

func test(): Int64 {
    let a = C(0)
    let b = C(1)
    var c = C(1)
    var res = match (c) {
        case _ => -1
    }
    if (res == 0) {
        return 0
    }
    return 1
}

main(): Int64 {
    return test()
}
