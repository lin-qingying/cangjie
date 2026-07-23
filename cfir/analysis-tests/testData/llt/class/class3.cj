

open class Basei {
    public func getNum1() {
        return 1
    }
    public var c = 5;
    private var t = 3
}

class Data <: Basei {
    public var a = 2;
    public var resc = <!UNRESOLVED_REFERENCE!>temp<!>;
    public var res = <!USED_BEFORE_INITIALIZATION!>getNum1<!>();
    public init() {}
}

func run(): Int64 {
    <!TYPE_MISMATCH!>var value: Data = Data()<!>
}
