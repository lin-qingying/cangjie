

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
            <!ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER!>super<!>(x)
            this.y = x
        } catch (e: NegativeArraySizeException) {
            throw e
        }
    }
}

main() {}
