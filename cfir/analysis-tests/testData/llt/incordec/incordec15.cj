


struct R {
    public let r: Int32
    public init(x: Int32) {
        r = x
    }
    public operator func +(inputB: R): R {
        return R(this.r + inputB.r)
    }
    public operator func -(inputB: R): Bool {
        return true
    }
}

main(): Int64 {
    var r: R = R(-1)
    var r1: R = r + R(1)
    if (r - R(1)) {
        return 0
    }
    return 1
}
