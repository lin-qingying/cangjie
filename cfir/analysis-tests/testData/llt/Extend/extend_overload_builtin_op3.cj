


interface Eq<T> {
    operator func ==(right: T): Bool
}

extend Int64 <: Eq<Int64> {}

struct Box<T> where T <: Eq<T> {
    public let item: T
    public init(item: T) {
        this.item = item
    }
    public operator func ==(right: Box<T>) {
        return item == right.item
    }
}

main(): Int64 {
    let a = Box<Int64>(1)
    let b = Box<Int64>(1)
    if (a == b) {
        return 0
    }
    return 1
}
