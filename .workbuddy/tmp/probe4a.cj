struct R1 {
    public var i = 0
    public mut func foo5(): R1 {
        return this
    }
    public mut func foo6() {
        var a = this
        return a
    }
    public func gooPlain() {
        this
    }
}
main() { return 1 }
