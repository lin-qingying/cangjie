package cjmp_unmatched_group_probe

public specific class ExtendBox<U> {
    public specific init() { }
}

specific extend<V> ExtendBox<V> {
    public specific func g(value: String): Unit { }
}

public func instantiateString(value: ExtendBox<String>): Unit { }
public func instantiateInt64(value: ExtendBox<Int64>): Unit { }
