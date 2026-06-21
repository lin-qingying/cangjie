


class C<E> {
    public let myData: Array<E>
    public init(a: E) {
        myData = Array<E>(1, repeat: a)
    }
    public func get(): E {
        return myData[0];
    }
}

func f<F>(a: F): F {
    let myData = C<F>(a)
    return myData.get();
}

main() {
    f<Int64>(1)
    f<C<Int64>>(C(2))
    return 0
}
