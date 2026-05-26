package sample.lowlevel.lazy

class <caret>Box<T> {
    public prop value: T

    public init(value: T) {
        this.value = value
    }

    public func unwrap(): T {
        return value
    }
}

func consume(box: Box<Int64>): Int64 {
    return box.unwrap()
}
