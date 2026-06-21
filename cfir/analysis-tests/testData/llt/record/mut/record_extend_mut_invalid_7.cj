


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
    var fn = <!USE_MUTABLE_FUNC_ALONE!>obj.foo<!>
    <!USE_MUTABLE_FUNC_ALONE!>obj.foo<!>
    return <!USE_MUTABLE_FUNC_ALONE!>obj.foo<!>
}

main() {
    return 1
}
