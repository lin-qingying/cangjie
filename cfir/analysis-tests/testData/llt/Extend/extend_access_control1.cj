


// accessibility: functions in the extend body cannot access private members inside the class body

class A {}

extend A {
    private func test() {}
    func foo() {
        test()
        0
    }
}

main() {
    let a: A = A()
    a.<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>test<!>()
    a.foo()
    0
}
