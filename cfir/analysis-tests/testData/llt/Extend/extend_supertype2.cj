


interface ToObj {
    func toObj(): Object
}

open class Obj {
    public open func f(): Unit {
        print("f in Obj!\n")
    }
}

extend Obj <: ToObj {
    public func toObj(): Obj {
        print("Hello, extend in Obj!\n")
        return this
    }
}

open class SubObj <: Obj {
    public override func f(): Unit {
        print("f in SubObj!\n")
    }
}

func g(): ToObj {
    return SubObj()
}

main(): Int64 {
    var a = g()
    a.toObj()
    (a as SubObj)?.f()

    return 0
}
