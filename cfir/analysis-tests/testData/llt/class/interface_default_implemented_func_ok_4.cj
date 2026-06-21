

interface I1 {
    func foo1(): Int64 {
        return 0
    }
}

struct R1 {
    public func foo1(): Int64 {
        return 0
    }
}

extend R1 <: I1 { // ok

}

main() {
    return 0
}
