


open class A {
    var a_ = ""
    public mut open prop a: String {
        get() {
            a_
        }
        set(v) {
            a_ = v
        }
    }
    init() {
        println("A")
    }
}

class B <: A {
    public mut override prop a: String {
        set(v) {}
    }
}

interface AA {
    mut prop a: Int64 {
        get() {
            0
        }
        set(v) {}
    }
    mut prop b: Int64 {
        get() {
            0
        }
        set(v) {}
    }
}

class BB <: AA {
    <!PROPERTY_MUST_IMPLEMENT_BOTH!>public mut override prop a: Int64 {
        set(v) {}
    }<!>
    <!PROPERTY_MUST_IMPLEMENT_BOTH!>public mut override prop b: Int64 {
        get() {
            1
        }
    }<!>
}

main(): Int64 {
    return 0
}
