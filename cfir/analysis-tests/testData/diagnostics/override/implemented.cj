

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
class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>B<!> <: A {
    <!NOTHING_TO_OVERRIDE!>override<!> func b() {

    }

    override private func <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>c<!>(): Unit {

    }

    override func <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>d<!>(): Unit {

    }

    override public func e(): Unit {

    }

    public override prop f: Int {
        get() {
            return 0
        }
    }

    override prop <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>h<!>: Int {
        get() {
            return 0
        }
    }

    redef static func <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>g<!>(): Unit {

    }

    public redef static func i(): Unit {

    }

    <!REDEF_INSTANCE_ERROR!>redef<!> func j(): Unit {

    }

    <!NOTHING_TO_OVERRIDE!>redef<!> static func k(): Unit {

    }
}
