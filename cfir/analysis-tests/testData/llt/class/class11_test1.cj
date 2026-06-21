


interface A {
    func i() {}
    static func test() {
        <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>i<!>()
    }
}
