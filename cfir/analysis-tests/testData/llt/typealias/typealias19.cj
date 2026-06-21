


class Type<T, K> {
    public init(a: T, b: K) {}
}

type A1<T> = Type<Rune, T>

type B1<X> = A1<X>

<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>t<!>ype C1<x> = B1<Int64>

<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>t<!>ype A2<T, K> = Type<Rune, T>

<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>t<!>ype B2<X> = A2<Int64, X>

type C2 = B2<Rune>

main(): Int64 {
    C1<Rune>(r'a', 1)
    C2(r'a', 1)
    return 0
}
