


interface I<T> {
    prop abc: Int64
}

class B<X> <: I<X> {
    public prop abc: Int64 {
        get() {
            111
        }
    }
}

func test<T>(inst: T) where T <: I<Int64> {
    return inst.abc
}

main(): Int64 {
    let b = B<Int64>()
    if (test(b) != 111) {
        return 1
    }
    return 0
}
