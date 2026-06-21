


struct Rectangle {
    public var width: Int32 = 1
    public let length: Int32 = 1
    public func set() {
        <!CANNOT_MODIFY_VAR!>width<!> += 1
    }
}

main(): Int64 {
    return 0
}
