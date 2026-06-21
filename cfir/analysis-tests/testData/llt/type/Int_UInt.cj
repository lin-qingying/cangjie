


func foo(a: Int): UInt {
    return UInt64(a)
}

main() {
    var a = 10
    var b: Int64 = 5
    var c: Int = 5
    var d: UInt64 = 5
    var e: UInt = 5
    return Int64(foo(a) * 2 - d - e) - b - c
}
