


class A {
    public mut prop a: Int64 {
        get() {
            1
        }
        <!CANNOT_CURRYING!>s<!>et(v)(x) {}
    }
}

class B {
    public mut prop a: Int64 {
        <!CANNOT_HAVE_PARAMETER!>g<!>et(v) {
            1
        }
        set(v) {}
    }
}

class C {
    public mut prop a: Int64 {
        <!CANNOT_CURRYING!>g<!>et()() {
            1
        }
        set(v) {}
    }
}

main(): Int64 {
    0
}
