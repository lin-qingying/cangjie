

class B {
    func g(): Int64 {
        println("func g in B");
        10
    }
}

interface I {
    func f(): Int64
}

extend B <: I {
    public func f() {
        println("func f in Extend B");
        1
    }
}

main(): Int64 {
    var m: I = B()

    let res: Int64 = match (m) {
        case x: B =>
            var y: Option<B> = x
            match (y) {
                case Some(z) =>
                    z.f()
                    z.g()
                case <!UNREACHABLE_PATTERN!>_<!> => 0
            }
        case _ => 0
    }

    res
}
