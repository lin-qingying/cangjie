

internal import std.collection.ArrayList

func test() {
    var metrics: ArrayList<Int64> = ArrayList<Int64>([Int64(<!UNRESOLVED_REFERENCE!>UndeclaredType<!>())])
}

main(): Int64 {
    test()
    return 0
}
