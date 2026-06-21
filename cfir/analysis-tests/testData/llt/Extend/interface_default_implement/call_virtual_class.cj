

interface I {
    func h1(): Int64
    func h2(): Int64 {
        h1()
    }
}

class C {}

extend C <: I {
    public func h1(): Int64 {
        1
    }
}

main() {
    var c: C = C()
    return c.h2() - 1
}
