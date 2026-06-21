open class Boo {
    public mut prop m: Int32 {
        get() {
            return a
        }
        set(input) {
            a = input + 1
        }
    }
    public prop n: Int32 {
        get() {
            return a
        }
    }
    public var a: Int32 = 1
    public func f(): Int32 {
        m = 2
        0
    }
}

class Doo <: Boo {
    public var b: Int32 = n
    public var c: Unit = (m = 1)
    public var d: Int32 = n
    public var e: Int32 = m
    public var e1: Int32 = super.m
    public func f1(): Int32 {
        m = 2
        0
    }
}
