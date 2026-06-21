


main(): Int64 {
    if (Boo().d != 1) {
        return 1
    }
    return 0
}

class Boo {
    public var d: Int32 = <!TYPE_MISMATCH!>{=> 1}<!>
}
