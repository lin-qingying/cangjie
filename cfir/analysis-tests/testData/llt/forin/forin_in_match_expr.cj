


interface I {
    func f(): Unit
}

extend Int64 <: I {
    public func f(): Unit {
        println("f in Extend Int64")
    }
}

main() {
    var i: I = 1

    match (i) {
        case x: Int64 =>
            for (y in 1..3) {
                y.f()
            }
            x.f()
        case _ => ()
    }
}
