open class Parent<T> {
    public func member(value: T): Unit {}
}

extend<T> Parent<T> {
    public func extended(): Unit {}
}

extend Parent<Int64> {
    public func concrete(): Unit {}
}

func throughBound<T>(value: T): Unit where T <: Parent<Int64> {
    value.member<Rune>(1)
    value.extended<Rune>()
    value.extended()
    value.concrete<Rune>()
}

main() {
    throughBound(Parent<Int64>())
}
