

public struct S {
    public var x: Int64 = 0
}

public class A {
    private var a: S = S()
    public mut prop s: S {
        get() {
            a
        }
        set(v) {
            a = v
        }
    }
    public func test(): Unit {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>s.x<!> = 1
    }
}

main() {}
