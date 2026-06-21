

open class Boo {
    public var x: Int64
    public init(x: Int64) {
        this.x = x
    }
}

class Coo <: Boo {
    public var y: Int64
    public init(x: Int64) {
        try {
            <!ILLEGAL_THIS_OR_SUPER_CALL!>super<!>(x)
            this.y = x
        } catch (e: NegativeArraySizeException) {
            throw e
        }
    }
}

main() {}
