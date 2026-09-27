package cjmp_p

public common class Box<T> {
    public common init()
    public common func identity(value: T): T
}

public common class Holder<T> {
    public common init() {}
    public common func defaultIdentity(value: T): T { value }
}

common extend<T> Holder<T> {
    public common func extendIdentity(value: T): T { value }
}
