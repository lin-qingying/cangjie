


struct R1 {
    public var i = 0
}

extend R1 {
    public mut func foo(): Unit {
        i += 1
    }
}

func goo() {
    var obj = R1()
    var fn = obj.<!USE_MUTABLE_FUNC_ALONE!>foo<!>
    obj.<!USE_MUTABLE_FUNC_ALONE!>foo<!>
    return obj.<!USE_MUTABLE_FUNC_ALONE!>foo<!>
}

main() {
    return 1
}
