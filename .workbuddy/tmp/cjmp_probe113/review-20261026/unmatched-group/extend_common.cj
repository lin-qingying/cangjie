package cjmp_unmatched_group_probe

public common class ExtendBox<T> {
    public common init()
}

common extend<T> ExtendBox<T> {
    public func g(value: T): Unit { }
    public func g(value: Int64): Unit { }
}
