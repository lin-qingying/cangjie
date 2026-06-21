


class C<T> {
    C(let v: T) {}

    func transform<R>(f: (T) -> R) {
        f(v)
    }
}

main() {
    let c = C(1)
    c.transform({x => x.toString()})
    ()
}
