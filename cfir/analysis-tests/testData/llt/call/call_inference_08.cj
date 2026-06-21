

type TASK_TYPE<IN, OUT> = (IN) -> OUT

open class A<X, Y> {}

class B<T, V> <: A<T, V> {
    private let ff: TASK_TYPE<T, V>

    public init(ff: TASK_TYPE<T, V>) {
        this.ff = ff
    }

    public func apply(data: T): V {
        return ff(data)
    }

    public func next<R>(p: B<V, R>): B<T, R> {
        let f = {
            data: T =>
            let r = this.apply(data)
            p.apply(r) // Was failed to find context type variable R
        }
        let combo = B<T, R>(f)
        return combo
    }
}

main() {}
