

interface I {
    func h1(): Int64
    func h2(): Int64 {
        h1()
    }
}

enum C {
    a
}

extend C <: I {
    public func h1(): Int64 {
        1
    }
}

main() {
    var c: C = C.a
    return c.h2() - 1
}
