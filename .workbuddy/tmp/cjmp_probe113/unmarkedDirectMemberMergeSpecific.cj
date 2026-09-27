package cjmp_unmarked_probe

public specific class Box<U> {
    public specific init() {}
    public func f(value: U): Unit { }
}

public func use(value: Box<Int64>): Unit { }
