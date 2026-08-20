

class Data {
    var b: Int32
    public let p0: Int32
    public <!CLASS_UNINITIALIZED_FIELD!>Data<!>(x: Int32, var <!REDECLARATION!>p0<!>: Int32) {
        this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>p0<!> = 3
    }
}

func run(): Int32 {
    let a: Int32 = 5
    var data: Data = Data(a, a)
    return data.p0 - 5
}

main(): Int64 {
    return Int64(run())
}
