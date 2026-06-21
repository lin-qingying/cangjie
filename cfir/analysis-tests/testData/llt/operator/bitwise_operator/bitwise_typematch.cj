

main(): Int64 {
    var a: Int8 = 1
    var b: Int16 = 1
    var c: Int32 = 1
    var d: Int64 = 1
    var e: Int64 = 1
    var r3 = a << b
    var r4 = a >> c
    var r8 = 10 << c >> a ^ d | e
    return 0
}
