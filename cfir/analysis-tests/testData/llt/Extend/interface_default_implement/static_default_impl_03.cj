

interface I1 {
    static func foo() {
        1
    }
}

interface I2 <: I1 {
    static func foo() {
        2
    }
}

class A {}

extend A <: I1 {}

extend A <: I2 {}

main(): Int64 {
    var res = A.foo()
    if (res != 2) {
        return 1
    }
    0
}
