

interface I1 {
    static func foo(): Int64 {
        return 1
    }
}

interface I2 {
    static func foo(): Int64 {
        return 2
    }
}

class C1 <: I1 {
    public static func foo(): Int64 {
        return 3
    }
}

main(): Int64 {
    var a: Int64 = C1.foo()
    if (a == 3) {
        return 0
    }
    return 1
}
