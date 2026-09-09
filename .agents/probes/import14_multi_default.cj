interface I1 {
    prop code: String {
        get() { "a" }
    }
    func fCode(): String { "a" }
}

interface I2 {
    prop code: String {
        get() { "b" }
    }
    func fCode(): String { "b" }
}

class C {}

extend C <: I1 {}
extend C <: I2 {}

main() {}
