

class B {
    func g(): Int64 {
        println("func g in B!")
        10
    }
}

interface I {
    func f(): Int64
}

extend B <: I {
    public func f() {
        println("In extend B!");
        1
    }
}

main(): Int64 {
    var m: E = C(B())

    let res: Int64 = match (m) {
        case C(x: B) => x.f()
        case <!UNREACHABLE_PATTERN!>_<!> => 0
    }

    var n: E = F(B(), B())

    let res2: Int64 = match (n) {
        case F(x: I, y: B) =>
            x.f()
            y.f()
            y.g()
        case <!UNREACHABLE_PATTERN!>_<!> => 0
    }

    res + res2
}

enum E {
    C(I) | F(B, I)
}
