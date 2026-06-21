

interface xxx {
    func f(): Unit
}

extend Int64 <: xxx {
    public func f(): Unit {
        println("extend in Int64")
    }
}

extend Int8 <: xxx {
    public func f(): Unit {
        println("extend in Int8")
    }
}

main() {
    var a = 10
    var v: Array<xxx> = [a, a * a]

    match (v[1]) {
        case z: Int64 => z.f()
        case _ => println("wildcard.")
    }
}
