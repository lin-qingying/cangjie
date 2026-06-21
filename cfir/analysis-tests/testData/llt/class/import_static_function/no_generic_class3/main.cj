// FILE: pkg/pkg.cj


package pkg

public open class innerInterface {
    public static func staticFunc(): Int32 {
        0
    }
}

public open class innerClass <: innerInterface {
    public static redef func staticFunc(): Int32 {
        0
    }
}

// FILE: main.cj



internal import pkg.*

class D <: innerClass {}

main(): Int64 {
    return 0
}
