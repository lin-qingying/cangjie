

interface I<T> {}

func f1<T1>(x: ?T1): Unit where T1 <: I<T1> {}

func f2<T2>(x: T2): Unit where T2 <: I<T2> {
    f1(x) // ok, x --> Some(x), T1 --> T2
}

main() {}
