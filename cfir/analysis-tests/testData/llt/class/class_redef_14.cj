

open class Boo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> where T <: Int64 {
    public static func foo<V>(x: T, y: V) {
        x + 1
    }
}

class Coo <: Boo<Int64> {
    public static redef func foo<V>(x: Int64, y: V) {
        x + 2
    }
}

main() {
    var c = Coo.foo<Int64>(0, 0)
    if (c != 2) {
        return 1
    }
    return 0
}
