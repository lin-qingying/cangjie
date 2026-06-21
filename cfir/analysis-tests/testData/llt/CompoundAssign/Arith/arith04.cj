


class Data {
    public static var a: Int64 = 24
    public static var b: Int64 = 3
    public var m: Int32 = 1
    public init() {
        Data.a /= 7
        Data.b **= 4
    }
}

main(): Int64 {
    Data().m++
    if (Data.a == 3 && Data.b == 81) {
        return 0
    }
    return 1
}
