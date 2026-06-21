// FILE: pkg/pkg.cj


package pkg

public interface I {
    prop p: Int64 {
        get() {
            return h1() + h2()
        }
    }
    func h1(): Int64
    func h2(): Int64
}

// FILE: main.cj


internal import pkg.*

struct R {
    public func h1(): Int64 {
        1
    }
}

extend R <: I {
    public func h2(): Int64 {
        10
    }
}

class C {
    public func h1(): Int64 {
        2
    }
}

extend C <: I {
    public func h2(): Int64 {
        20
    }
}

enum E {
    a

    public func h1(): Int64 {
        3
    }
}

extend E <: I {
    public func h2(): Int64 {
        30
    }
}

main(): Int64 {
    var r = R()
    if (r.p != 11) {
        return 1
    }
    var c = C()
    if (c.p != 22) {
        return 2
    }
    var e = E.a
    if (e.p != 33) {
        return 3
    }
    return 0
}
