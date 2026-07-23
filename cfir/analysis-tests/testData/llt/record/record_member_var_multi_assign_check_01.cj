

struct Rectangle {
    public let width: Int64
    public let length: Int64

    public init() {
        this.width = 10
        this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>width<!> = 11
        this.length = 20
        this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>length<!> = 21
    }
}

main(): Int64 {
    var rec = Rectangle()
    if (rec.width == 11 && rec.length == 21) {
        return 0
    }

    return 1
}
