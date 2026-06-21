

type E<T> = Option<T>

main() {
    let a: ?Int64 = E.None
    let b = Some<?Int64>(E.None)
    return 0
}
