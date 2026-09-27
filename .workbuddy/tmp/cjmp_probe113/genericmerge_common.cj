package cjmp_genericmerge

public common interface I<T> {
    public common func f(value: T): Unit { }
}

public common class C<T> <: I<T> {
    public common init()
}
