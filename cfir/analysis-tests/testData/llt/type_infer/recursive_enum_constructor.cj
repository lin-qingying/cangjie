enum Chain<T> {
    Cons(T, Chain<T>) | Nil
}

func expectInt64Chain(_: Chain<Int64>) {}
func expectBoolChain(_: Chain<Bool>) {}

main() {
    let inferred = Cons(0, Cons(1, Cons(2, Nil)))
    let annotated: Chain<Int64> = Cons(0, Cons(1, Nil))
    expectInt64Chain(inferred)
    expectInt64Chain(annotated)
    expectBoolChain(<!ARGUMENT_TYPE_MISMATCH!>inferred<!>)
    0
}
