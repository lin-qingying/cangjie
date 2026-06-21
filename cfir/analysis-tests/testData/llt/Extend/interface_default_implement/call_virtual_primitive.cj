

interface I {
    func h1(): Int64
    func h2(): Int64 {
        h1()
    }
}

extend Int32 <: I {
    public func h1(): Int64 {
        1
    }
}

main() {
    return 1.h2() - 1
}
