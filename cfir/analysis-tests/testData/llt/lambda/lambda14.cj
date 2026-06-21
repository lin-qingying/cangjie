


class A {
    init(a: () -> Option<Int64>) {}
    init(a: () -> Unit) {}
}

main() {
    A({=> return Some(0)})
    0
}
