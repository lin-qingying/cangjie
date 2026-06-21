


class student <: ToString {
    public var name = ""
    public init(name: String) {
        this.name = name
    }
    public func toString(): String {
        return this.name
    }
}

main(): Int64 {
    var s: student = student("Rune")
    print("${s}")
    return 0
}
