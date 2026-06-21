


class Data {
    public static var a = 1
    public var b = 1
    public init() {
        Data.a++
    }
    public func f(): Int64 {
        Data.a++
        var x = 1
        return x
    }
}

main(): Int64 {
    var data = Data()
    Data().b++
    var b = Data().f()
    if (Data.a == 5) {
        return 0
    }
    return 1
}
