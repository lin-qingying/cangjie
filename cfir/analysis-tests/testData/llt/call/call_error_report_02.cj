

internal import std.collection.ArrayList

main(): Int64 {
    var a1 = "abcdefg"
    var temp1 = "c"
    var temp2 = UInt8(UInt32(r'c'))
    var res1 = a1.indexOf(temp1, <!TYPE_MISMATCH!>"12"<!>)
    var res2 = a1.indexOf(temp2, <!TYPE_MISMATCH!>"12"<!>)
    return 0
}
