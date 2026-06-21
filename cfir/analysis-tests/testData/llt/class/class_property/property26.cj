


main(): Int64 {
    var a = Coo()
    if (a.n != 1) {
        return 1
    }
    a.m = 10
    if (a.m != 2) {
        return 2
    }
    return 0
}

abstract class Boo {
    public mut prop m: Int32
    public prop n: Int32
}

extend Boo {
    public mut prop <!EXTEND_MEMBER_CANNOT_SHADOW!>m<!>: Int32 {
        get() {
            return 2
        }
        set(v) {}
    }
    public prop <!EXTEND_MEMBER_CANNOT_SHADOW!>n<!>: Int32 {
        get() {
            return 1
        }
    }
}

class Coo <: Boo {
    public mut prop <!EXTEND_FUNCTION_CANNOT_OVERRIDDEN!>m<!>: Int32 {
        get() {
            return 2
        }
        set(v) {}
    }
    public prop <!EXTEND_FUNCTION_CANNOT_OVERRIDDEN!>n<!>: Int32 {
        get() {
            return 1
        }
    }
}
