package cjmp_multi_extend

public specific class Box<U> {
    public specific init() {}
}

specific extend<V> Box<V> {
    public specific func second(value: V): V { value }
}
