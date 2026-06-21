

open class C1 {
    public static func foo(): Int64 {
        return 1
    }
}

class C2 <: C1 {}

main(): Int64 {
    var a: Int64 = C2.foo()
    if (a == 1) {
        return 0
    }
    return 1
}
