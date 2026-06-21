


interface I1 {}

interface I2 {}

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

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend C <: I2 & I4 {}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend C <: I1 & I3 {}

main() {
    C().<!AMBIGUOUS_FUNCTION_CALL!>foo<!>()
}
