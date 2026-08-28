// FILE: pkgs/pkgs.cj


package pkgs

public struct R1 {
    public var i = 0

    public mut func foo() {
        i += 1
    }
}

public let obj = R1()

// FILE: main.cj



import pkgs.*

main(): Int64 {
    <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>obj<!>.foo()
    return 1
}
