package sample.lowlevel.contextcollector.generated

class Box<T> {
    public prop value: T

    public init(value: T) {
        this.value = value
    }

    public func unwrap(): T {
        return <expr>value</expr>
    }
}
