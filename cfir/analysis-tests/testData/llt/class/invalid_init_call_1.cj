

struct GenRec {
    public let b: Int32 = 1
    public var c: Int64 = 2
    public init(str: String) {
        if (str == "hi") {
            <!CANNOT_ASSIGN_TO_IMMUTABLE!>this.b = 2<!>
            <!ILLEGAL_THIS_OR_SUPER_CALL, NO_VALUE_FOR_PARAMETER!>this()<!>
            this.c = 4
        } else {
            <!CANNOT_ASSIGN_TO_IMMUTABLE!>this.b = b + 2<!>
            this.c = this.c + 1
        }
    }
}

main(): Int64 {
    var rec = GenRec("hi")
    print(rec.b.toString())
    print(rec.c.toString())
    if (rec.c == 4 && rec.b == 2) {
        return 0
    }
    return 1
}
