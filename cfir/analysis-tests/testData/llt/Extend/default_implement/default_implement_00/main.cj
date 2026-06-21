// FILE: pkg.cj


package pkg

public interface I {
    static func f(): Int64 {
        1
    }
}

public open class A {
    public static func f(): Int64 {
        0
    }
}

public class B <: A {}

extend B <: I {}

// FILE: main.cj


internal import pkg.*

main() {
    B.f()
}
