


interface I {
    mut static prop p1: Int64
    mut prop p2: Int64
    static prop p3: Int64
    prop p4: Int64
    mut static prop p5: Int32
}

class A {}

extend A <: I {
    <!PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT!>s<!>tatic prop <!WEAK_VISIBILITY!>p1<!>: Int64 {
        get() {
            0
        }
    }
    <!PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT!>p<!>rop <!WEAK_VISIBILITY!>p2<!>: Int64 {
        get() {
            0
        }
    }
    <!PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_IMMUT!>m<!>ut static prop <!WEAK_VISIBILITY!>p3<!>: Int64 {
        get() {
            0
        }
        set(v) {}
    }
    <!PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_IMMUT!>m<!>ut prop <!WEAK_VISIBILITY!>p4<!>: Int64 {
        get() {
            0
        }
        set(v) {}
    }
    <!PROPERTY_OVERRIDE_IMPLEMENT_TYPE_DIFF!>s<!>tatic prop <!WEAK_VISIBILITY!>p5<!>: Int64 {
        get() {
            0
        }
    }
}
