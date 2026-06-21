

interface I1 {}

interface I2 <: I1 {}

interface I3 {
    func foo() {
        println("foo3")
    }
}

interface I4 {
    func foo() {
        println("foo4")
    }
}

class C {}

extend C <: I2 & I4 {}

extend C <: I1 & I3 {}

main() {
    C().<!AMBIGUOUS_FUNCTION_CALL!>foo<!>()
}
