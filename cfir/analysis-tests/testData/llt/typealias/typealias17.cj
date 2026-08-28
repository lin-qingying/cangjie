


class A<T> {
    public var a: T
    public init(x: T) {
        a = x
    }
    public static func foo() {
        1
    }
    public static func goo(a: T) {
        a
    }
}

<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>type A1<T><!> = A<Rune>

<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>type A2<T, K><!> = A1<K>

type B1<T> = A<T>

<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>type B2<T, K><!> = B1<K>

main() {
    let a = A2<Int64, String>(r'a')
    if (a.a != r'a') {
        return 1
    }
    if (A2<Int64, String>.foo() != 1) {
        return 2
    }
    if (A2<Int64, String>.goo(r'b') != r'b') {
        return 3
    }

    let b = B2<Int64, Rune>(r'a')
    if (b.a != r'a') {
        return 4
    }
    if (B2<Int64, Rune>.foo() != 1) {
        return 5
    }
    if (B2<Int64, Rune>.goo(r'b') != r'b') {
        return 6
    }
    return 0
}
