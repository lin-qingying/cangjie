

class A {
    static var AFLAG: Bool
    static init() {
        AFLAG = B.<!USED_BEFORE_INITIALIZATION!>BFLAG<!>
    }
}

class B {
    public static let BFLAG: Bool = true
}

main(): Int64 {
    0
}
