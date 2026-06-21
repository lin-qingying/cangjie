


import std.collection.*

public interface FieldPathComparableExpansion<F> {
    func compare(): Ordering
}

extend<K> ArrayList<K> <: FieldPathComparableExpansion<K> {
    public func compare() {
        let leftValues = this
        Ordering.GT
    }
}
