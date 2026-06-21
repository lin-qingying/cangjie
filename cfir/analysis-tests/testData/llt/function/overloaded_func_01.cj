

func g(a: Bool) {
    0
}

func g(a: Option<Bool>) {
    true
}

func call<T1, T2>(f: (T1) -> T2, x: T1) {
    f(x)
}

main() {
    return call(g, true)
}
