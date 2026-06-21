


interface I {
    mut prop a: Int64
    prop b: Int64
}

class A {}

extend A <: I {
    <!PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT!>p<!>rop <!WEAK_VISIBILITY!>a<!>: Int64 {
        get() {
            0
        }
    }
    <!PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_IMMUT!>m<!>ut prop <!WEAK_VISIBILITY!>b<!>: Int64 {
        get() {
            0
        }
        set(v) {
            ()
        }
    }
}

main() {
    0
}
