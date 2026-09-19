struct R1 {
    public var i = 0
    public mut func foo(): Unit { i += 1 }
    public func goo1() {
        return foo
    }
    public func goo2() {
        return foo()
    }
}
main() {
    return 1
}
