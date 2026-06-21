

func test<T>(a: G<T>) {
    return
}

class G<T> {
    public func foo() {
        test<T>(this)
    }
}

main() {
    var a = G<Int64>()
    return 0
}
