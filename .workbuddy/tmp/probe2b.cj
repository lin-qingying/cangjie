struct R1 {
    public var i = 0
    public mut func foo(): Unit {
        i += 1
    }
}

func gooLet() {
    let obj = R1()
    obj.foo
    return obj.foo
}

func gooCall() {
    var obj = R1()
    obj.foo()
    return
}

main() {
    return 1
}
