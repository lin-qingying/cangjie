package cjmp_genericmerge

public specific interface I<U> { }

public specific class C<U> <: I<U> {
    public specific init() { }
    public specific func f(value: U): Unit { }
}

func trigger(): C<Int64> {
    C<Int64>()
}
