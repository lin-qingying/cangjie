

class A {
    static init() {
        C.fun()
    }
}

class C {
    static let NUM = 8
    public static func fun(): Int64 {
        println(<!USED_BEFORE_INITIALIZATION!>NUM<!>)
        <!USED_BEFORE_INITIALIZATION!>NUM<!>
    }
}

main(): Int64 {
    0
}
