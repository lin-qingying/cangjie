


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
        println("In extend B!");
        1
    }
}

main(): Int64 {
    var m: E = F(B(), B())

    let res: Int64 = match (m) {
        case F(x: I, y: B) =>
            x.f();
            y.f();
            y.g()
        case _ => 0
    }

    res
}

enum E {
    C(B) | F(B, I)
}

