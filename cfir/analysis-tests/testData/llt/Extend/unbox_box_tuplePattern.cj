


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
    var m: (Int64, B, I) = (10, B(), B())

    let res: Int64 = match (m) {
        case (10, x: I, y: B) =>
            x.f()
            y.f()
            y.g()
        case _ => 0
    }

    res
}

