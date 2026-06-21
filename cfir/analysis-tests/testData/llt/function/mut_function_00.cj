

interface I {
    mut func f(): Unit
}

struct Foo <: I {
    var i = 0
    public mut func f(): Unit {
        i += 1
    }
}

class Bar <: I {
    var i = 0
    public func f(): Unit {
        i += 1
    }
}

func g1<T>(v: T): Unit where T <: I {
    v.<!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>f<!>()
}

func g2<T>(v: T): Unit where T <: Bar & I {
    v.f()
}

main() {}
