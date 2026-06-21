

class A<T> {
    A(_: T) {}
    init(_: Int64) {}
}

main() {
    let _ = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>A(0)<!>
}
