

interface I {
    func h1(): Int64
    func h2(): Int64 {
        h3() * 10
    }
    func h3(): Int64
    func h4(): Int64 {
        1
    }
}

struct C {}

extend C <: I {
    public func h1(): Int64 {
        h2() * 10
    }
    public func h3(): Int64 {
        h4() * 10
    }
}

main() {
    var c: C = C()
    return c.h1() + c.h2() + c.h3() + c.h4() - 1111
}
