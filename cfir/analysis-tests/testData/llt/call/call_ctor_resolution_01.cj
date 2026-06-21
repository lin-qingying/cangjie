

public class Binary<T> <: Collection<T> where T <: Equatable<T> & ToString {
    init(v: T) {
        println(1)
    }

    init(v: Array<T>) {
        println(2)
    }
    public func toString() {
        ""
    }
    public prop size: Int64 {
        get() {
            0
        }
    }
    public func isEmpty() {
        true
    }
    public func iterator() {
        return It<T>()
    }
}

public class It<T> <: Iterator<T> where T <: Equatable<T> & ToString {
    public func next(): Option<T> {
        None
    }
}

main() {
    let v = Binary([1, 2, 3])
    0
}
