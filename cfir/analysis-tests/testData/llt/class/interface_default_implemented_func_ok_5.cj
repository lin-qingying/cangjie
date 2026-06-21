

interface I1 {
    func foo1(a: Int64): Int64 {
        return 0
    }
}

extend Int32 <: I1 {
    public func foo1(a: Int64): Int64 {
        return 0
    }
}

main() {
    return 0
}
