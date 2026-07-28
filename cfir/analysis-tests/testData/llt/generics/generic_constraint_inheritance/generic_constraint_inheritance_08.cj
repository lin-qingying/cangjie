

interface Printable {}

interface Runnable {}

interface I {
    // Another constraint than implementation. Must emit compilation error.
    static func id<T>(x: T): T where T <: Printable
}

class Implementation <: I {
    public static func id<T>(x: T): T where <!GENERIC_CONSTRAINT_NOT_LOOSER!>T <: Runnable<!> {
        return x
    }
}

main() {}
