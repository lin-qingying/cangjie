

struct Rectangle {
    public let width: Int64
    public let length: Int64

    public init() {
        this.width = 10
        this.length = 20
    }

    public init(width: Int64, length: Int64) {
        this()
        this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>width<!> = width
        this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>length<!> = length
    }
}

main(): Int64 {
    var rec = Rectangle(100, 200)
    if (rec.width == 100 && rec.length == 200) {
        return 0
    }
    return 1
}
