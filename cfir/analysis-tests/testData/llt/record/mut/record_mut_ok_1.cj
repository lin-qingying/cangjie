

struct R1 {
    public var i = 0

    public mut func foo1(): Unit {
    }

    public mut func foo2() {
        this.i = this.i + 1
        i = i + 1
    }
}

main() {
    var obj = R1()
    obj.foo2()
    if (obj.i == 2) {
        return 0
    }
    return 1
}
