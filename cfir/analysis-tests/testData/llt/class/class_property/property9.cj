


open class A {
    public mut open prop i: Int64 {
        get() {
            return 0
        }
        set(v) {}
    }
}

interface B {
    mut prop i: Int32
}

class C <: A & B {
    <!PROPERTY_OVERRIDE_IMPLEMENT_TYPE_DIFF!>p<!>ublic mut override prop i: Int64 {
        get() {
            return 1
        }
        set(v) {}
    }
}

open class A1 {
    public mut open prop i: String {
        get() {
            return "0"
        }
        set(v) {}
    }
}

interface B1 {
    mut prop i: Int64
}

class C1 <: A1 & B1 {
    <!PROPERTY_OVERRIDE_IMPLEMENT_TYPE_DIFF!>p<!>ublic mut override prop i: Int64 {
        get() {
            return 1
        }
        set(v) {}
    }
}
