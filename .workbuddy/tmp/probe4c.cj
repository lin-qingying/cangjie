struct R1 {
    public var i = 0
    public func gooA(): R1 {
        return this
    }
    public func gooB() {
        var a = this
        return a
    }
    public func gooC() {
        this.i = 2
        return
    }
}
main() { return 1 }
