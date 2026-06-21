

interface I1 {
    mut func foo1() {
        return 0
    }
}

struct R1 <: I1 {
    public mut func foo1() {
        return 0
    }
}

main() {
    return 0
}
