

internal import std.collection.*
internal import stdx.serialization.serialization.*

interface I<T> <: Serializable<T> {
    func f(): Unit
}

extend<K, V> HashMap<K, V> <: I<HashMap<K, V>> where K <: Serializable<K> & Hashable & Equatable<K>,
    V <: Serializable<V> {
    public func f(): Unit {
        println("call f")
    }
}

main() {
    var a = HashMap<Int64, String>()
    a.f()
    0
}
