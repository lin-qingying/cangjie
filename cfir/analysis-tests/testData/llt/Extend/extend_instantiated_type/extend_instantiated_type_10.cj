

class A<T> {}

interface I {
    func i1() {
        44
    }
}

extend<M, N> A<(M, N)> <: I {
    func goo() {
        33
    }
}

main() {
    let a = A<(Rune, Int64)>()
    let v1 = a.goo()
    if (v1 != 33) {
        return 1
    }
    let v2: I = a
    let v3 = v2.i1();
    if (v3 != 44) {
        return 2
    }
    return 0
}
