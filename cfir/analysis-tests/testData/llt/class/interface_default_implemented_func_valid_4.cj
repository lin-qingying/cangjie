

interface I1 {
    func foo1(): Int64 {
        return 0
    }
}

struct R1 {}

extend R1 <: I1 {}

main() {
    return R1().foo1()
}
