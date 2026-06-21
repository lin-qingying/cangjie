// FILE: pkg.cj


package f_collection

public import std.collection.{HashMap, TreeMap}

public interface ValueEqualMap<K, V> <: ValueContainsMap<K, V> where K <: Equatable<K>, V <: Equal<V> {
    func containsValue(value: V): Bool {
        return false
    }
}

public interface ValueContainsMap<K, V> where K <: Equatable<K> {
    func containsValue(value: V, fn: (V, V) -> Bool): Bool {
        return false
    }
}

public class LinkedHashMap<K, V> {}

extend<K, V> LinkedHashMap<K, V> <: ValueEqualMap<K, V> where K <: Hashable & Equatable<K>, V <: Equal<V> {}

extend<K, V> LinkedHashMap<K, V> <: ValueContainsMap<K, V> where K <: Equatable<K>, V <: Equal<V> {}

// FILE: main.cj



import f_collection.*

main() {
    LinkedHashMap<Int64, Int64>()
    0
}
