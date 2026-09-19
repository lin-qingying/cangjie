interface I1 {
    mut func foo(): Unit {
    }
    func goo1() {
        return foo
    }
    func goo2() {
        return foo()
    }
}
main() {
    return 1
}
