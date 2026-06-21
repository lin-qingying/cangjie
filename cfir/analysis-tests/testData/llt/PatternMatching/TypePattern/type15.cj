

// Option box shouldn't work at pattern matching
package test

import std.collection.*

main() {
    0
}

public class ListView<T> {
    private let _data: List<T> = ArrayList<T>()
    public func toString() {
        "${_data.toArray() as String}"
    }
}
