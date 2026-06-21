


internal import std.collection.*

func containsAll<T, C>(left: Set<T>, right: C): Bool where T <: Equatable<T>, C <: Collection<T> {
    for (v in right) {
        if (!(left.contains(v))) {
            return false
        }
    }
    true
}

main() {
    if (containsAll<Int64, Array<Int64>>(HashSet<Int64>(), [])) {
        return 0
    }
    return 1
}
