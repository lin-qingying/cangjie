


struct A {
    public let a: Int32
    <!CLASS_UNINITIALIZED_FIELD!>public A(x: Int32)<!> {
    }
}

<!VALUE_TYPE_RECURSIVE!>s<!>truct Data {
    public Data() {}
    public var b = <!RECURSIVE_CONSTRUCTOR_CALL!>Data<!>()
}

<!VALUE_TYPE_RECURSIVE!>s<!>truct Data2 {
    public var b: Data2 = <!RECURSIVE_CONSTRUCTOR_CALL!>Data2<!>()
}

struct GenRec {
    public var a: Int64
    public var b: Int32
    public init(x: Int64, y: Int32, str: String) {
        this(x)
        this.b = y
    }
    <!CLASS_UNINITIALIZED_FIELD!>private GenRec(x: Int64)<!> {
        this.a = x
    }
}

main(): Int64 {
    var data2 = GenRec(3, 4, "1")
    if (data2.a == 3 && data2.b == 4) {
        return 0
    }
    return 1
}
