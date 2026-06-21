

public open class Base {
    public open func g(): This {
        println("return Base");
        <!TYPE_MISMATCH!>Base().f()<!>
    }
    public func f(): This {
        this
    }
}

public class Sub <: Base {
    private let x: Int64 = 10
    public func subOnly() {
        println("SubOnly: ${x}")
    }
}

main() {
    Sub().g().subOnly()
}
