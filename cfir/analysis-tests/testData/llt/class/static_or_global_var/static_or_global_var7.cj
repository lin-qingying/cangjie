

class A {
    static init() {
        C.fun()
    }
}

func getNum() {
    8
}

class C {
    static let NUM = getNum()
    public static func fun(): Int64 {
        println(<!USED_BEFORE_INITIALIZATION!>NUM<!>)
        <!USED_BEFORE_INITIALIZATION!>NUM<!>
    }
}

main(): Int64 {
    0
}
