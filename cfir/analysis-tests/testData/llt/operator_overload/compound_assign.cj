


internal import std.collection.*

func IncBuf<T>(data: ArrayList<T>) {
    data[0] <!INVALID_BINARY_OPERATOR!>+<!>= 1
    return 0
}

main() {
    var gStateBufI8 = ArrayList<Int8>([-1, 1])
    IncBuf<Int8>(gStateBufI8)
    return 0
}
