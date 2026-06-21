


// if both two functions with the same signature have default implementations, they must be overridden in the extend decl
interface A {
    func add(): Unit {}
}

interface B {
    func add(): Unit {}
}

class S {}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend S <: A & B {}
