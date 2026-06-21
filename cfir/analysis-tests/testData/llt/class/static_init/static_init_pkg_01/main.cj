// FILE: pkg.cj


package pkg

public open class A {
    public static var a: Int64 = 1
    public static let r: Range<Int64>
    static init() {
        r = a..5
    }
}

// FILE: main.cj



internal import pkg.*

let v = A.r

main() {
    if (A.a != 1) {
        return 1
    }
    if (v.start != A.a || v.end != 5) {
        return 2
    }
    return 0
}
