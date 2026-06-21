

interface I1<T> {
    mut func foo1(x: T) {
        return x
    }
}

struct R1<T> <: I1<T> {
    public mut func foo1(x: T) {
        return x
    }
}

main() {
    return 0
}
