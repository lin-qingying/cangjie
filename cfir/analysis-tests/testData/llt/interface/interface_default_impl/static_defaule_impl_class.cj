

interface I {
    static func f1(): Unit {
        println("I")
    }
    static func f2(): Unit {
        f1()
    }
    func f3(): Unit {
        f1()
    }
    func f4(): Unit
    func f5(): Unit {
        f4()
    }
}

open class C <: I {
    public static func f1(): Unit {
        println("C f1")
    }
    public open func f4(): Unit {
        println("C f4")
    }
}

class D <: C {
    public static func f1(): Unit {
        println("D f1")
    }
    public override func f4(): Unit {
        println("D f4")
    }
}

main() {
    D.f2()
    D().f3()
    D().f5()
}
