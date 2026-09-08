

public class Base {
    public <!IGNORE_OPEN!>open<!> func test(): Int64 {
        return 1
    }
}

class Test {
    public var i: Int64 = 0
    public init() {
        this.i = Base().test()
    }
}

main() {
    Test().i - 1
}
