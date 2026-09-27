package cjmp_multi_extend

public common class Box<T> {
    public common init()
}

common extend<T> Box<T> {
    public common func first(value: T): T { value }
}

common extend<T> Box<T> {
    public common func second(value: T): T
}
