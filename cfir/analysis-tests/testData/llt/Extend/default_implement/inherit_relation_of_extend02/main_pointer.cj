


public interface I1<V> {
    func containsValue(value: V, fn: (V, V) -> Bool): Bool {
        false
    }
}

public interface I2<V> {
    func containsValue(value: V, fn: (V, V) -> Bool): Bool {
        false
    }
    func containsValue(value: V): Bool {
        false
    }
}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend<V> CPointer<V> <: I1<V> {}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend<V> CPointer<V> <: I2<V> {}

main() {
    0
}
