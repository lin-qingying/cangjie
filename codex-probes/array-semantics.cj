main(): Int64 {
    var a: Int64 = 5
    var b: Int32 = 3
    var c = Array<Int32>(a, b)
    var d = Array<Int32>(a, [b])
    var f: Array<Int64> = Array<Int32>(a, b)
    var i: Int64 = 1
    var nested: Array<Array<Int64>> = Array<Array<Int64>>(i, Array<Int64>(i, i))
    let arr1: Array<Rune> = Array(3, repeat: 3)
    let arr2: Array<Rune> = Array<Int64>(3, repeat: 3)
    return 0
}
