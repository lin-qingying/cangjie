

struct C {
    static var a: Range<Int64>
    static let b: Int64
    static init() {
        a = 1..10
        b = 11
    }
}

var x = C.a

main() {
    if (C.a.start != 1 || C.a.end != 10 || x != C.a) {
        return 1
    }
    if (C.b != 11) {
        return 2
    }
    return 0
}
