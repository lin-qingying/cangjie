private let a = 1
private let a1  : Int8 = 1
private func private() {

}

private class B {
    private let bb = 1
    private prop cc: Int {
        get() {1}
    }
    private func bbb(){}
}
private interface A {
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>private<!> prop cc: Int {
        get() {1}
    }
     public func bbsdb(){}
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>private<!> func bbb(){}
}
