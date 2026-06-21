


// test case: re-enter Unit type if expr generation
class ClassB {
    public var var1: Unit = ();
    public func testa(a!: Int64 = 1): Int64 {
        1
    }
}

var b = {
    =>
    if (true) {}
    else {}
    ClassB();
}().testa()

main(): Int64 {
    return 0
}
