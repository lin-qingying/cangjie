


public open class Class257 {
    public var a = 6
    public func test() {
        this |> func937
    }
}

public func func937(ins: Class257) {
    return ins.a
}

main(): Int64 {
    var res = Class257().test()
    if (res == 6) {
        return 0
    }
    return 1
}
