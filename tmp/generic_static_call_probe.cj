class Box<T> {
    static func create(): Int64 {
        return 0
    }
}

func useQualifier(): Int64 {
    return Box.create()
}
