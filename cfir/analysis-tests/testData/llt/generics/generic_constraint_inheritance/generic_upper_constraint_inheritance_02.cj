


interface I1 {}

interface I2 <: I1 {}

// Constraint check should pass
interface I {
    static func f<T1, T2>(): Unit where T1 <: I2 {}
}

class A <: I {
    public static func f<T1, T2>() where T1 <: I1 {}
}

main() {
}
