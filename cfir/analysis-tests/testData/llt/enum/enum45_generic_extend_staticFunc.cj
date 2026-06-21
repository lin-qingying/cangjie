

enum E<T> {
    AB
}

extend E<Rune> {
    static func f3(a: UInt32): UInt32 {
        println(a)
        a
    }
}

main() {
    var c3 = E.f3(2)
    0
}
