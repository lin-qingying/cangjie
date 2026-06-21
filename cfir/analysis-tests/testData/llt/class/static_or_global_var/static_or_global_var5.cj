

class A {
    static init() {
        println(B.<!USED_BEFORE_INITIALIZATION!>FLAG<!>)
    }
}

class B {
    public static let FLAG: Bool = true
}

main(): Int64 {
    0
}
