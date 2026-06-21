


// accessibility: functions in the extend body cannot access private members inside the class body
class A {
    private func goo() {}
}

extend A {
    func foo() {
        <!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>g<!>oo()
        0
    }
}
