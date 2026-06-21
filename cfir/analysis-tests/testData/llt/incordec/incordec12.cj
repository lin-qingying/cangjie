


class Data {
    public static var n: Int32 = 1
    public var m: Int32 = 1
    public init(x: Int32) {
        Data.n++;
        n++
    }
    public func get(): Int32 {
        Data.n++;
        n++;
        return n
    }
}

main(): Int64 {
    let a: Int32 = 1
    Data(a).m++
    if (Data.n != 3) {
        return 1
    }
    return 0
}
