func identity<T>(value: T): T {
    return value
}

func check(): Unit {
    let value: Int64 = identity(1.1)
}
