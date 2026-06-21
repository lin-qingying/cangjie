

interface I {
    static func a(): Int64 {
        return 1
    }
}

class Test <: I {}

main(): Int64 {
    var x: Int64 = Test.a()
    if (x == 1) {
        return 0
    }
    return -1
}

