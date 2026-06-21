// FILE: pkgs/pkgs.cj


package pkgs

public struct A<K> {
    public var value: K
    public init(val: K) {
        value = val
    }
}

public struct B<V> {
    public var value: A<V>
    public init(val: V) {
        value = A<V>(val)
    }
}

public class C {
    private var value: B<Int64> = B<Int64>(2)
}

public class D<T> {
    public var value: B<Int64> = B<Int64>(1)
}

// FILE: main.cj



internal import pkgs.*

main(): Int64 {
    var a = C()
    return 0
}
