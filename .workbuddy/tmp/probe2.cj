struct R1 {
    public var i = 0
    public mut func foo(): Unit {
        i += 1
    }
}

func goo() {
    var obj = R1()
    var fn = obj.foo
    obj.foo
    return obj.foo
}

main() {
    return 1
}
