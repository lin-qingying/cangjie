


// extend decl must implement abstract functions of its super interfaces
class A {}

interface I {
    func foo(): Unit
}

<!NEED_MEMBER_IMPLEMENTATION!>extend A <: I {
    // must implement foo() here
}<!>
