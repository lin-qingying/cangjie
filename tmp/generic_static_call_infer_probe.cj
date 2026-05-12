class Box<T> {
    static func id(x: T): T {
        return x
    }
}

func useQualifier(): Int64 {
    return Box.id(1)
}
