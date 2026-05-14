interface A {
    func a(): Unit
    func c(): Unit
    func d(): Unit
    func e(): Unit
    prop f: Int
    prop h: Int
    static func g(): Unit
    static func i(): Unit
}
class B <: A {
    override func b() {

    }

    override private func c(): Unit {

    }

    override func d(): Unit {

    }

    override public func e(): Unit {

    }

    public override prop f: Int {
        get() {
            return 0
        }
    }

    override prop h: Int {
        get() {
            return 0
        }
    }

    redef static func g(): Unit {

    }

    public redef static func i(): Unit {

    }

    redef func j(): Unit {

    }

    redef static func k(): Unit {

    }
}
