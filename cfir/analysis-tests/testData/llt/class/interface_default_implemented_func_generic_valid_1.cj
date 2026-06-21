

interface I1<T> {
    func foo1(a: T): Int64 {
        return 0
    }
}

struct R1<T> <: I1<T> {}

main() {
    return R1<Int64>().foo1(1)
}
