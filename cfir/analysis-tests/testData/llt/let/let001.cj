

struct WorldRecord {
    public var foo: Int64 = 0
    public let bar: Int64 = 1
    public let lambda: (Unit) -> Int64 = <!TYPE_MISMATCH!>{=> <!ILLEGAL_USAGE_OF_MEMBER!>foo<!>}<!>
    public func baz() {
        foo + 1
        <!CANNOT_ASSIGN_TO_IMMUTABLE, CANNOT_MODIFY_VAR!>bar<!> = bar + 1
    }
}

func F4() {
    var police = 0
    let thief = 1
    let f4 = <!USE_FUNC_CAPTURE_VAR_ALONE!>{=> police + thief}<!>

    func f5() {
        return police + thief
    }

    var arrh = [1, 2, 3, 4]
    for (i in arrh) {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>i<!> = i + 1
    }

    let a = match (police) {
        case b => {
            =>
            <!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = b + 1
            b
        }
    }
}

func F5(a: Int64) {
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>a<!> = a + 1
    let b: Int64
    while (true) {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 9
    }
    a
}
