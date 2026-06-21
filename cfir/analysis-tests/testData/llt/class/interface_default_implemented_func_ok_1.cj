

interface I1 {
    func foo1(): Int64 {
        return 0
    }
}

struct R1 <: I1 { // ok
    public func foo1(): Int64 {
        return 0
    }
}

main() {
    return 0
}
