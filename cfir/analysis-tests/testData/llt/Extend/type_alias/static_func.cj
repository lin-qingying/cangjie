

type int = Int32

extend Int32 {
    public static func f() {
        1
    }
    public static prop i: int {
        get() {
            3
        }
    }
}

extend int {
    public static func g() {
        2
    }
    public static prop j: int {
        get() {
            4
        }
    }
}

var a = int.f()
var b = int.i

main() {
    if (a != 1) {
        return 1
    }
    if (Int32.g() != 2) {
        return 2
    }
    if (b != 3) {
        return 3
    }
    if (Int32.j != 4) {
        return 4
    }
    return 0
}
