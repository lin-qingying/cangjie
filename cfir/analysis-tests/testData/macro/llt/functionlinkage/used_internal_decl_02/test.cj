// FILE: pkg.cj


package pkg

internal import std.{collection.HashMap, collection.ArrayList}

class Dict<T> {
    let mapping = HashMap<Ids, T>()
}

public func handle<T>(values: Array<T>): Array<T> {
    let dict = Dict<T>()
    let result = ArrayList<T>()
    for (index in 0..values.size) {
        result.add(handleIt(index, 0, dict, values[index]))
        result.add(handleIt(0, index, dict, values[index]))
    }

    return result.toArray()
}

func handleIt<T>(first: Int64, second: Int64, dict: Dict<T>, value: T): T {
    if (let Some(existing) <- dict.mapping.get(Ids(first, second))) {
        return existing
    }

    let result = value
    dict.mapping[Ids(first, second)] = result
    result
}

struct Ids <: Hashable & Equatable<Ids> {
    Ids(
        let a: Int64,
        let b: Int64
    ) {}

    public operator func ==(other: Ids): Bool {
        a == other.a && b == other.b
    }
    public operator func !=(other: Ids): Bool {
        a != other.a || b != other.b
    }

    @OverflowWrapping
    public func hashCode(): Int64 {
        a.hashCode() + 16 * b.hashCode()
    }
}

// FILE: test.cj



internal import pkg.*

main() {
    let result = handle<Int64>([1, 2, 3])
    let str = result.toString()
    if (str != "[1, 1, 2, 2, 3, 3]") {
        return 1
    }
    return 0
}
