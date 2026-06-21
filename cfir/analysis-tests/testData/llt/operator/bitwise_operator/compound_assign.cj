


class C {
    public operator func +(a: UInt8) {
        print("+")
    }
}

class Cls {
    public var c: C = C()
}

main() {
    var cls = Cls()
    <!TYPE_INCOMPATIBLE!>c<!>ls.c += 2
    return 0
}
