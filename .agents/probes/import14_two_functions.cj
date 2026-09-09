interface I1 {
    func fCode(): String { "a" }
    func gCode(): String { "a" }
}

interface I2 {
    func fCode(): String { "b" }
    func gCode(): String { "b" }
}

class C {}

extend C <: I1 {}
extend C <: I2 {}

main() {}
