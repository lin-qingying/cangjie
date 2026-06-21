

class C<T> {
    public let c: Int64 = 1
}

class B<T> {
    public let b: C<T> = C<T>()
}

class A<T> {
    public let a: B<T> = B<T>()
}

main(): Int64 {
    var d: B<Int64> = B<Int64>()
    print(d.b.c.toString())
    A<Int64>()
    return 0
}
