


type A<K> = structA<K>

<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>t<!>ype B<T> = A<Rune>

type C = B<Int64>

<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>t<!>ype D<T> = C

struct structA<K> {
    public var b: K
    public structA(var y: K) {
        this.b = y
    }

    public static prop e: Bool {
        get() {
            true
        }
    }
}

main(): Int64 {
    A<Bool>(A<Bool>.e)
    D<structA<Int64>>(r'a')
    D<String>.e
    return 0
}
