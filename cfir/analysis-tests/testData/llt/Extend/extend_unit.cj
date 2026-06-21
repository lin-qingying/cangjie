

extend Unit {
    public func add() {
        print("unit")
    }
}

func foo(a: Unit) {
}

main() {
    let a: Array<Int32>
    foo(());
    ().add()
    0
}
