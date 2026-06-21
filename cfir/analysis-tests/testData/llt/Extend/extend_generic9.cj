

class Box<T> {
    public let item: T
    public init(item: T) {
        this.item = item
    }
}

extend<T> Box<T> {
    public func getItem(): T {
        return item
    }
}

main() {
    let x = Box<Int64>(0);
    return x.getItem()
}
