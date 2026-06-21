

internal import std.sort.*

main(): Int64 {
    var a1 = [[r'c', r'd'], [r'a', r'b']]
    a1.<!NOT_MEMBER_OF!>sort<!>()
    return 1
}
