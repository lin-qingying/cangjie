// WITH_EXTRA_CHECKERS

extend Unit {
    public func add() {
        print("unit")
    }
}

func foo(a: Unit) {
}

main() {
    let <!UNUSED_VARIABLE!>a<!>: Array<Int32>
    foo(());
    ().add()
    0
}
