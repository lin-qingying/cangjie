

struct R1 {
    public var i = 0
}

extend R1 {
    public mut func foo1() {
        this.i = this.i + 1
        i = i + 1
    }
}

main() {
    var obj = R1()
    obj.foo1()
    if (obj.i == 2) {
        return 0
    }
    return 1
}
