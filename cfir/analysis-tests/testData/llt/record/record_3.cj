

struct RA{
    public var a
    public init(input:Int32){
        a = input
    }
}

main(): Int64 {
    var l_a = 4
    var r_a = RA(2)
    l_a = r_a.a
    if (l_a == Int32(2)) {
        return 0
    }
    return 1
}
