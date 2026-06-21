
// Was stackoverflow during type substitution. should pass
func test() {
    var a: A<Int64> = A<Int64>()
    a.test(1)
}

interface I<T> {
    func test(a: T): Int64 {
        return 0
    }
}

<!TYPE_IMPLEMENT_NON_INTERFACE!>s<!>truct A<T> <: A<A<T>> & I<T> {
}

main() {
    var a: A<Int64> = A<Int64>()
    a.test(<!TYPE_MISMATCH!>a<!>)
}


