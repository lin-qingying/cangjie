

interface IA {
    mut prop x: Int64
    prop y: Int64
}

struct A <: IA {
    public mut prop x: Int64 {
        get() {
            0
        }
        set(v) {}
    }
    public prop y: Int64 {
        get() {
            1
        }
    }
}

func goo(a: A): Unit {
    a.<!CANNOT_ASSIGN_TO_IMMUTABLE!>x<!> = a.x + 1
    println(a.y)
}

func foo<T>(t: T): Unit where T <: IA {
    t.<!CANNOT_ASSIGN_TO_IMMUTABLE!>x<!> = t.x + 1
    println(t.y)
}
main() {}
