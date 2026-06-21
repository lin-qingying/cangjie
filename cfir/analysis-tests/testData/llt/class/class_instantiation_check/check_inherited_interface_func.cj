

interface I {
    static func foo(): UInt64 {
        1
    }
}

class A<T0> <: I {
    public static func foo(): UInt64 {
        1
    }
}

main(): Int64 {
    A<Int64>()
    return 0;
}
