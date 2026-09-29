package cjmp_p

public specific class Box<U> {
    public specific init() {}
    public specific func identity(value: U): U { value }
}

specific extend<V> Holder<V> {
    public specific func extendIdentity(value: V): V { value }
}

public func use(holder: Holder<Int64>): Int64 {
    holder.extendIdentity(1)
}
